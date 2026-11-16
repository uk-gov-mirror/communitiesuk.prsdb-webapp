package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompletedPropertyHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `updates owned property details with correlated forms and checks persisted summaries`() {
        PropertyServer().use { server ->
            val result = runSimulation(server)
            assertEquals(0, result.exitCode, result.output)
            assertEquals(
                setOf("SELECTIVE_LICENCE"),
                server.posts.mapNotNull { it["licensingType"] }.toSet(),
            )
            assertEquals(34, server.posts.size)
            assertTrue(server.posts.all { it["_csrf"] == "property-csrf" && it["version"] == "property-version" })
        }
    }

    @Test
    fun `missing CSRF or a different property redirect stops before mutation`() {
        for (wrongProperty in listOf(false, true)) {
            PropertyServer(includeCsrf = wrongProperty, wrongProperty = wrongProperty).use { server ->
                val result = runSimulation(server)
                assertEquals(2, result.exitCode, result.output)
                assertTrue(server.posts.isEmpty())
            }
        }
    }

    @Test
    fun `successful statuses cannot hide unsaved property values`() {
        PropertyServer(save = false).use { server ->
            val result = runSimulation(server)
            assertEquals(2, result.exitCode, result.output)
            assertEquals(1, server.posts.size)
        }
    }

    private fun runSimulation(server: PropertyServer): GatlingTestRunner.Result =
        GatlingTestRunner(outputDirectory).run(
            BasicContractSimulation::class.java,
            mapOf(
                "gatling.contract.journey" to "completed-property",
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "synthetic-landlord",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
        )

    private class PropertyServer(
        private val includeCsrf: Boolean = true,
        private val wrongProperty: Boolean = false,
        private val save: Boolean = true,
    ) : AutoCloseable {
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val posts = ConcurrentLinkedQueue<Map<String, String>>()
        private val values = mutableMapOf<String, Map<String, String>>()
        private val licenceTypes = mutableMapOf<String, String>()
        private val steps =
            linkedMapOf(
                "update-ownership-type" to listOf(mapOf("ownershipType" to "LEASEHOLD")),
                "update-bedrooms" to listOf(mapOf("numberOfBedrooms" to "4")),
                "update-furnished-status" to listOf(mapOf("furnishedStatus" to "PART_FURNISHED")),
                "update-households-and-tenants" to listOf(mapOf("numberOfHouseholds" to "2"), mapOf("numberOfPeople" to "3")),
                "update-rent-frequency-and-amount" to listOf(mapOf("rentFrequency" to "WEEKLY"), mapOf("rentAmount" to "200")),
                "update-rent-includes-bills" to listOf(mapOf("rentIncludesBills" to "true"), mapOf("billsIncluded[2]" to "WATER")),
                "update-correspondence-email" to
                    listOf(mapOf("correspondenceEmailOption" to "DIFFERENT_EMAIL", "differentEmailAddress" to "property@example.com")),
            )
        private val summaries =
            mapOf(
                "update-ownership-type" to mapOf("How do you own this property?" to "Leasehold"),
                "update-bedrooms" to mapOf("Number of bedrooms" to "4"),
                "update-furnished-status" to mapOf("Furniture provided" to "Partly furnished"),
                "update-households-and-tenants" to mapOf("Number of households" to "2", "Number of tenants" to "3"),
                "update-rent-frequency-and-amount" to mapOf("When rent is paid" to "Weekly", "Rent amount" to "£200.00"),
                "update-rent-includes-bills" to mapOf("Rent includes bills" to "Yes", "Which bills are included" to "Water"),
                "update-correspondence-email" to mapOf("Contact email address" to "property@example.com"),
            )

        init {
            server.createContext("/") { exchange ->
                val path = exchange.requestURI.path
                val parts = path.split("/")
                val id = parts.getOrElse(3) { "" }
                val route = parts.getOrElse(4) { "" }
                val stage = parts.getOrElse(5) { "" }
                val licensed = route == "update-licensing"

                fun licenceSummary(): Map<String, String> =
                    when (licenceTypes[id]) {
                        "NO_LICENSING" -> mapOf("Licensing type" to "None")
                        else ->
                            mapOf(
                                "Licensing type" to
                                    when (licenceTypes[id]) {
                                        "SELECTIVE_LICENCE" -> "Selective licence"
                                        "HMO_MANDATORY_LICENCE" -> "HMO mandatory licence"
                                        else -> "HMO additional licence"
                                    },
                                "Licensing number" to "PERF-123",
                            )
                    }
                val details = "/landlord/property-details/$id"
                var code = 200
                var body = ""

                fun redirect(location: String) {
                    code = 302
                    exchange.responseHeaders.add("Location", location)
                }

                fun rows(entries: Map<String, String>) =
                    entries.entries.joinToString("") { (key, value) ->
                        """<div class="govuk-summary-list__row"><dt class="govuk-summary-list__key">$key</dt>
                        <dd class="govuk-summary-list__value">$value</dd></div>"""
                    }

                fun form(
                    fields: String,
                    action: String,
                ) = """<form method="post" action="$action">
                    ${if (includeCsrf) """<input type="hidden" name="_csrf" value="property-csrf">""" else ""}
                    <input type="hidden" name="version" value="property-version">$fields</form>"""

                if (exchange.requestMethod == "POST") {
                    val fields =
                        exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8).split("&").associate {
                            val pair = it.split("=", limit = 2)
                            URLDecoder.decode(pair[0], StandardCharsets.UTF_8) to
                                URLDecoder.decode(pair.getOrElse(1) { "" }, StandardCharsets.UTF_8)
                        }
                    posts.add(fields)
                    if (licensed && "licensingType" in fields) licenceTypes[id] = fields.getValue("licensingType")
                    val index = stage.toIntOrNull()
                    if (fields["_csrf"] != "property-csrf" || fields["version"] != "property-version" ||
                        exchange.requestHeaders.getFirst("Cookie") != "SESSION=property-session" ||
                        exchange.requestURI.rawQuery != "journeyId=property-$id"
                    ) {
                        code = 403
                    } else if (licensed) {
                        when {
                            stage == "0" && licenceTypes[id] != "NO_LICENSING" ->
                                redirect("$details/$route/1?journeyId=property-$id")
                            stage == "1" && fields["licenceNumber"] != "PERF-123" -> code = 400
                            stage != "check-answers" -> redirect("$details/$route/check-answers?journeyId=property-$id")
                            else -> {
                                if (save) values["$id/$route"] = licenceSummary()
                                redirect(details)
                            }
                        }
                    } else if (index != null && steps.getValue(route)[index].any { fields[it.key] != it.value }) {
                        code = 400
                    } else if (index != null && index + 1 < steps.getValue(route).size) {
                        redirect("$details/$route/${index + 1}?journeyId=property-$id")
                    } else if (index != null && steps.getValue(route).size > 1 || index != null && route == "update-correspondence-email") {
                        redirect("$details/$route/check-answers?journeyId=property-$id")
                    } else {
                        if (save) values["$id/$route"] = summaries.getValue(route)
                        redirect(details)
                    }
                } else if (route.isBlank()) {
                    exchange.responseHeaders.add("Set-Cookie", "SESSION=property-session; Path=/")
                    body = "<h1>1 Fixture Road</h1>" +
                        (steps.keys + "update-licensing").joinToString("") { """<a href="$details/$it">Change</a>""" } +
                        rows((steps.keys + "update-licensing").flatMap { values["$id/$it"].orEmpty().entries }.associate { it.toPair() })
                } else if (stage.isBlank()) {
                    val target = if (wrongProperty) "/landlord/property-details/999/$route" else "$details/$route"
                    redirect("$target/0?journeyId=property-$id")
                } else if (stage == "check-answers") {
                    val expected =
                        when (route) {
                            "update-correspondence-email" -> mapOf("Email address" to "property@example.com")
                            "update-licensing" -> licenceSummary()
                            else -> summaries.getValue(route)
                        }
                    body = form(rows(expected), "$details/$route/check-answers?journeyId=property-$id")
                } else if (licensed) {
                    val fields =
                        if (stage == "0") {
                            """<input name="licensingType" value="SELECTIVE_LICENCE">"""
                        } else {
                            """<input name="licenceNumber">"""
                        }
                    body = form(fields, "$details/$route/$stage?journeyId=property-$id")
                } else {
                    body =
                        form(
                            steps.getValue(route)[stage.toInt()].entries.joinToString("") { (name, value) ->
                                """<input name="$name" value="$value">"""
                            },
                            "$details/$route/$stage?journeyId=property-$id",
                        )
                }
                exchange.responseHeaders.add("Content-Type", "text/html")
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        override fun close() = server.stop(0)
    }
}

package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyBranchHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `extra property branches preserve indexed custom bills address email and occupancy outcomes`() {
        BranchServer().use { server ->
            val result = run(server)
            assertEquals(0, result.exitCode, result.output)
            assertTrue(server.posts.any { it["billsIncluded[11]"] == "SOMETHING_ELSE" && it["customBillsIncluded"] == "Window cleaning" })
            assertTrue(server.posts.any { it["correspondenceEmailOption"] == "ACCOUNT_EMAIL" })
            assertEquals(setOf("true", "false"), server.posts.mapNotNull { it["occupied"] }.toSet())
            assertTrue(server.posts.any { it["address"] == "1 PRSDB Performance Square, EG1 2AA" })
            assertTrue(server.posts.any { it["addressLineOne"] == "24 Gatling Correspondence Road" })
        }
    }

    @Test
    fun `extra property branches reject missing CSRF and wrong property identity before mutation`() {
        for (csrf in listOf(false, true)) {
            BranchServer(includeCsrf = csrf, correctIdentity = !csrf).use { server ->
                val result = run(server)
                assertEquals(2, result.exitCode, result.output)
                assertTrue(server.posts.isEmpty())
            }
        }
    }

    private fun run(server: BranchServer) =
        GatlingTestRunner(outputDirectory).run(
            PropertyBranchContractSimulation::class.java,
            mapOf(
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "fixture-owner",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
        )

    private class BranchServer(
        private val includeCsrf: Boolean = true,
        private val correctIdentity: Boolean = true,
    ) : AutoCloseable {
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val posts = mutableListOf<Map<String, String>>()
        private val values = mutableMapOf<String, Map<String, String>>()
        private val flows =
            mapOf(
                "update-correspondence-address" to
                    listOf(
                        mapOf("postcode" to "EG1 2AA", "houseNameOrNumber" to "1"),
                        mapOf("address" to "1 PRSDB Performance Square, EG1 2AA"),
                    ),
                "update-correspondence-email" to listOf(mapOf("correspondenceEmailOption" to "ACCOUNT_EMAIL")),
                "update-rent-includes-bills" to
                    listOf(
                        mapOf("rentIncludesBills" to "true"),
                        mapOf("billsIncluded[11]" to "SOMETHING_ELSE", "customBillsIncluded" to "Window cleaning"),
                    ),
                "update-occupancy" to listOf(mapOf("occupied" to "false")),
                "update-tenancy-details" to
                    listOf(
                        mapOf("numberOfHouseholds" to "2"), mapOf("numberOfPeople" to "3"),
                        mapOf("rentIncludesBills" to "false"), mapOf("furnishedStatus" to "PART_FURNISHED"),
                        mapOf("rentFrequency" to "WEEKLY"), mapOf("rentAmount" to "200"),
                    ),
            )
        private val chosen = mutableMapOf<String, MutableMap<String, String>>()

        private fun summary(
            route: String,
            data: Map<String, String>,
        ): Map<String, String> =
            when (route) {
                "update-correspondence-email" -> mapOf("Contact email address" to "owner@example.com")
                "update-correspondence-address" ->
                    mapOf(
                        "Contact address" to
                            if (data["address"] == "MANUAL") {
                                "24 Gatling Correspondence Road, Exampleton, EG1 2AA"
                            } else {
                                "1 PRSDB Performance Square, EG1 2AA"
                            },
                    )

                "update-rent-includes-bills" ->
                    if (data["rentIncludesBills"] == "false") {
                        mapOf("Rent includes bills" to "No")
                    } else {
                        mapOf("Rent includes bills" to "Yes", "Which bills are included" to "Window cleaning")
                    }

                "update-occupancy" -> mapOf("Is this property occupied by tenants?" to if (data["occupied"] == "true") "Yes" else "No")
                else ->
                    mapOf(
                        "Number of households" to "2",
                        "Number of tenants" to "3",
                        "Rent includes bills" to "No",
                        "Furniture provided" to "Partly furnished",
                        "When rent is paid" to "Weekly",
                        "Rent amount" to "£200.00",
                    )
            }

        private fun rows(values: Map<String, String>) =
            values.entries.joinToString("") { (key, value) ->
                """<dt class="govuk-summary-list__key">$key</dt><dd class="govuk-summary-list__value">$value</dd>"""
            }

        init {
            server.createContext("/") { exchange ->
                val parts = exchange.requestURI.path.split("/")
                val id = parts.getOrElse(3) { "" }
                val route = parts.getOrElse(4) { "" }
                val stage = parts.getOrElse(5) { "" }
                val details = "/landlord/property-details/$id"
                val key = "$id/$route"
                val data = chosen.getOrPut(key) { mutableMapOf() }
                var code = 200
                var body = ""

                fun redirect(location: String) {
                    code = 302
                    exchange.responseHeaders.add("Location", location)
                }
                if (route.isBlank()) {
                    exchange.responseHeaders.add("Set-Cookie", "SESSION=branch-session; Path=/")
                    body = "<h1>${if (correctIdentity) "1 Fixture Road" else "Wrong property"}</h1>" +
                        flows.keys.joinToString("") { """<a href="$details/$it">Change</a>""" } +
                        rows(flows.keys.flatMap { values["$id/$it"].orEmpty().entries }.associate { it.toPair() })
                } else if (stage.isBlank()) {
                    data.clear()
                    redirect("$details/$route/0?journeyId=branch-$id")
                } else if (exchange.requestMethod == "POST") {
                    val fields =
                        exchange.requestBody.readAllBytes().toString(Charsets.UTF_8).split("&").associate {
                            val pair = it.split("=", limit = 2)
                            URLDecoder.decode(pair[0], Charsets.UTF_8) to URLDecoder.decode(pair.getOrElse(1) { "" }, Charsets.UTF_8)
                        }
                    posts.add(fields)
                    data.putAll(fields)
                    if (fields["_csrf"] != "branch-csrf" || fields["version"] != "branch-version" ||
                        exchange.requestHeaders.getFirst("Cookie") != "SESSION=branch-session" ||
                        exchange.requestURI.rawQuery != "journeyId=branch-$id"
                    ) {
                        code = 403
                    } else if (stage == "check") {
                        values[key] = summary(route, data)
                        redirect(details)
                    } else {
                        val index = stage.toInt()
                        val next =
                            when {
                                route == "update-rent-includes-bills" && fields["rentIncludesBills"] == "false" -> "check"
                                route == "update-correspondence-address" && fields["address"] == "MANUAL" -> "2"
                                route == "update-correspondence-address" && index == 2 -> "check"
                                index + 1 < flows.getValue(route).size -> (index + 1).toString()
                                else -> "check"
                            }
                        redirect("$details/$route/$next?journeyId=branch-$id")
                    }
                } else {
                    val fields =
                        when {
                            stage == "check" -> emptyMap()
                            route == "update-correspondence-address" && stage == "2" ->
                                mapOf("addressLineOne" to "", "townOrCity" to "", "postcode" to "")

                            else -> flows.getValue(route)[stage.toInt()]
                        }
                    var content = fields.entries.joinToString("") { (name, value) -> """<input name="$name" value="$value">""" }
                    if (route == "update-correspondence-email") content += """<div class="govuk-hint">owner@example.com</div>"""
                    if (stage == "check") {
                        var expected = summary(route, data)
                        if (route == "update-correspondence-email") expected = mapOf("Email address" to "owner@example.com")
                        if (route == "update-correspondence-address") {
                            expected = mapOf("Postal address" to expected.getValue("Contact address"))
                        }
                        if (route == "update-occupancy") {
                            expected = mapOf("Occupied by tenants" to expected.getValue("Is this property occupied by tenants?"))
                        }
                        body += rows(expected)
                    }
                    body += """<form method="post" action="$details/$route/$stage?journeyId=branch-$id">
                        ${if (includeCsrf) """<input type="hidden" name="_csrf" value="branch-csrf">""" else ""}
                        <input type="hidden" name="version" value="branch-version">$content</form>"""
                }
                exchange.responseHeaders.add("Content-Type", "text/html")
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        override fun close() = server.stop(0)
    }
}

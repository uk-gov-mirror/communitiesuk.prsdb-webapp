package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrganisationHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `organisation administrator updates name email and phone with correlated forms`() {
        OrganisationServer().use { server ->
            val result = runSimulation(server)
            assertEquals(0, result.exitCode, result.output)
            assertEquals(18, server.posts.size)
            val profilePosts = server.posts.filter { "value" in it }
            assertEquals(6, profilePosts.size)
            assertEquals(
                setOf("Performance Organisation", "organisation@example.com", "02079460123"),
                profilePosts.map { it["value"] }.toSet(),
            )
        }
    }

    @Test
    fun `organisation administrator updates selected and manual addresses and main contact`() {
        OrganisationServer().use { server ->
            val result = runSimulation(server)
            assertEquals(0, result.exitCode, result.output)
            assertEquals(18, server.posts.size)
            assertEquals(
                setOf("1 PRSDB Performance Square, EG1 2AA", "MANUAL"),
                server.posts.mapNotNull { it["address"] }.toSet(),
            )
            assertEquals(
                setOf("77 Gatling Organisation Road"),
                server.posts.mapNotNull { it["addressLineOne"] }.toSet(),
            )
            val mainContacts = server.posts.filter { "name" in it }
            assertEquals(2, mainContacts.size)
            assertTrue(mainContacts.all { it["name"] == "Performance Main Contact" })
            assertTrue(mainContacts.all { it["emailAddress"] == "main-contact@example.com" })
            assertTrue(mainContacts.all { it["phoneNumber"] == "02079460124" })
            assertTrue(server.posts.all { it["_csrf"] == "organisation-csrf" && it["version"] == "current-version" })
        }
    }

    @Test
    fun `missing CSRF or a redirect outside the organisation update stops before posting`() {
        for (missingCsrf in listOf(false, true)) {
            OrganisationServer(includeCsrf = !missingCsrf, wrongRoute = !missingCsrf).use { server ->
                val result = runSimulation(server)
                assertEquals(2, result.exitCode, result.output)
                assertTrue(server.posts.isEmpty())
            }
        }
    }

    @Test
    fun `successful responses must preserve the organisation identity and save the changed value`() {
        for (wrongIdentity in listOf(false, true)) {
            OrganisationServer(save = false, wrongIdentity = wrongIdentity).use { server ->
                val result = runSimulation(server)
                assertEquals(2, result.exitCode, result.output)
                assertEquals(if (wrongIdentity) 0 else 1, server.posts.size)
            }
        }
    }

    private fun runSimulation(server: OrganisationServer) =
        GatlingTestRunner(outputDirectory).run(
            BasicContractSimulation::class.java,
            mapOf(
                "gatling.contract.journey" to "organisation",
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "synthetic-landlord",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
        )

    private class OrganisationServer(
        private val includeCsrf: Boolean = true,
        private val wrongRoute: Boolean = false,
        private val save: Boolean = true,
        private val wrongIdentity: Boolean = false,
    ) : AutoCloseable {
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val posts = ConcurrentLinkedQueue<Map<String, String>>()
        private val values = mutableMapOf<String, String>()
        private val details = "/landlord/landlord-details"
        private val fields =
            linkedMapOf(
                "update-organisation-name" to ("orgName" to "Organisation name"),
                "update-organisation-email" to ("emailAddress" to "Organisation email"),
                "update-organisation-phone-number" to ("phoneNumber" to "Organisation phone"),
            )
        private val addressRoute = "update-organisation-address"
        private val contactRoute = "update-organisation-main-contact"
        private var address = "1 Fixture Street, EG1 2AA"
        private var contactName = "Original Main Contact"
        private var contactEmail = "original-contact@example.com"
        private var contactPhone = "02079460000"

        init {
            server.createContext("/") { exchange ->
                val path = exchange.requestURI.path
                val route = path.removePrefix("$details/").substringBefore('/')
                var code = 200
                var body = ""

                fun redirect(location: String) {
                    code = 302
                    exchange.responseHeaders.add("Location", location)
                }

                fun row(
                    label: String,
                    value: String,
                ) = """<dt class="govuk-summary-list__key">$label</dt>
                    <dd class="govuk-summary-list__value">$value</dd>"""

                fun addressRow() =
                    row("Organisation address", address.replace(", ", "<br>")) +
                        """<a href="$details/$addressRoute/lookup-address">Change</a>"""

                fun contactRows() =
                    """<div id="organisation-contacts">
                        <h2>Main contact</h2>
                        <div class="govuk-summary-card">${row("Name", contactName)}${row("Email", contactEmail)}
                        ${row("Phone number", contactPhone)}<a href="$details/$contactRoute/organisation-main-contact">Change</a></div>
                    </div>"""
                val stage = path.removePrefix("$details/$route/").substringBefore('/')
                when {
                    path == details -> {
                        exchange.responseHeaders.add("Set-Cookie", "SESSION=organisation-session; Path=/")
                        val registration = RegistrationNumberDataModel(RegistrationNumberType.LANDLORD, 200).toString()
                        body = "<h1>${values["orgName"] ?: "Fixture organisation"}</h1>" +
                            row(
                                "Landlord Registration Number",
                                if (wrongIdentity) "L-WRONG" else registration,
                            ) +
                            addressRow() +
                            fields.entries.joinToString("") { (routeName, field) ->
                                row(field.second, values[field.first].orEmpty()) +
                                    """<a href="$details/$routeName/step">Change</a>"""
                            } + contactRows()
                    }
                    exchange.requestMethod == "POST" -> {
                        val submitted =
                            exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8).split("&").associate {
                                val parts = it.split("=", limit = 2)
                                URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to
                                    URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8)
                            }
                        posts.add(
                            if (route in fields) {
                                submitted + ("value" to submitted[fields.getValue(route).first].orEmpty())
                            } else {
                                submitted
                            },
                        )
                        if (submitted["_csrf"] != "organisation-csrf" || submitted["version"] != "current-version" ||
                            exchange.requestHeaders.getFirst("Cookie") != "SESSION=organisation-session" ||
                            exchange.requestURI.rawQuery != "journeyId=org-journey"
                        ) {
                            code = 403
                        } else {
                            when {
                                route in fields && route == "update-organisation-phone-number" &&
                                    submitted["phoneNumber"] != "02079460123" -> code = 200
                                route in fields -> {
                                    if (save) values[fields.getValue(route).first] = submitted.getValue(fields.getValue(route).first)
                                    redirect(details)
                                }
                                route == addressRoute && stage == "lookup-address" ->
                                    redirect("$details/$addressRoute/select-address?journeyId=org-journey")
                                route == addressRoute && stage == "select-address" && submitted["address"] == "MANUAL" ->
                                    redirect("$details/$addressRoute/manual-address?journeyId=org-journey")
                                route == addressRoute && stage == "select-address" -> {
                                    if (save) address = submitted.getValue("address")
                                    redirect(details)
                                }
                                route == addressRoute && stage == "manual-address" -> {
                                    if (save) {
                                        address =
                                            listOf(
                                                submitted.getValue("addressLineOne"),
                                                submitted.getValue("townOrCity"),
                                                submitted.getValue("postcode"),
                                            ).joinToString(", ")
                                    }
                                    redirect(details)
                                }
                                route == contactRoute -> {
                                    if (save) {
                                        contactName = submitted.getValue("name")
                                        contactEmail = submitted.getValue("emailAddress")
                                        contactPhone = submitted.getValue("phoneNumber")
                                    }
                                    redirect("$details#organisation-contacts")
                                }
                                else -> code = 404
                            }
                        }
                    }
                    exchange.requestURI.rawQuery == null -> {
                        redirect(
                            if (wrongRoute) {
                                "/landlord/landlord-details/update-name/step"
                            } else {
                                "$path?journeyId=org-journey"
                            },
                        )
                    }
                    route == addressRoute && stage == "lookup-address" ->
                        body = form("$path?journeyId=org-journey", """<input name="postcode"><input name="houseNameOrNumber">""")
                    route == addressRoute && stage == "select-address" ->
                        body =
                            form(
                                "$path?journeyId=org-journey",
                                """<input type="radio" name="address" value="1 PRSDB Performance Square, EG1 2AA">
                                    <input type="radio" name="address" value="MANUAL">""",
                            )
                    route == addressRoute && stage == "manual-address" ->
                        body =
                            form(
                                "$path?journeyId=org-journey",
                                """<input name="addressLineOne"><input name="townOrCity"><input name="postcode">""",
                            )
                    route == contactRoute ->
                        body =
                            form(
                                "$path?journeyId=org-journey",
                                """<input name="name"><input name="emailAddress"><input name="phoneNumber">""",
                            )
                    route !in fields -> code = 404
                    else -> {
                        body =
                            form(
                                "$path?journeyId=org-journey",
                                """<input name="${fields.getValue(route).first}">""",
                            )
                    }
                }
                exchange.responseHeaders.add("Content-Type", "text/html")
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        private fun form(
            action: String,
            inputs: String,
        ) = """<form method="post" action="$action">
            ${if (includeCsrf) """<input type="hidden" name="_csrf" value="organisation-csrf">""" else ""}
            <input type="hidden" name="version" value="current-version">$inputs</form>"""

        override fun close() = server.stop(0)
    }
}

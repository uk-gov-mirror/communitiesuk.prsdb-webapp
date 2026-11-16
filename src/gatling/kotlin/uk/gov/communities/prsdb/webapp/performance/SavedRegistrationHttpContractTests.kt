package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedRegistrationHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `resumes selected address saves bedrooms and deletes only the other saved registration`() {
        SavedServer().use { server ->
            val result = runSimulation(server)
            assertEquals(0, result.exitCode, result.output)
            assertEquals(30, server.requests.size)
            assertEquals(8, server.requests.count { it.first == "POST" })
        }
    }

    @Test
    fun `resumes and saves bedrooms when associated parties adds the joint landlords step`() {
        SavedServer(nextStep = "has-joint-landlords", nextField = "hasJointLandlords").use { server ->
            val result = runSimulation(server)
            assertEquals(0, result.exitCode, result.output)
            assertEquals(30, server.requests.size)
            assertEquals(8, server.requests.count { it.first == "POST" })
        }
    }

    @Test
    fun `rejects unexpected next steps and forms inconsistent with the redirected step`() {
        for (nextStep in listOf("unexpected-step", "has-joint-landlords")) {
            SavedServer(nextStep = nextStep).use { server ->
                val result = runSimulation(server)
                assertEquals(2, result.exitCode, result.output)
                assertTrue(server.requests.none { it.second.startsWith("/landlord/delete-incomplete-property") })
            }
        }
    }

    @Test
    fun `keeps file results without verbose scenario progress in the console`() {
        SavedServer().use { server ->
            val result = runSimulation(server)
            assertEquals(0, result.exitCode, result.output)
            assertFalse(result.output.contains("waiting:"), result.output)
            Files.walk(outputDirectory).use { paths ->
                assertTrue(paths.anyMatch { it.fileName.toString() == "simulation.log" && Files.size(it) > 0 })
            }
        }
    }

    @Test
    fun `missing CSRF stops saved-registration changes`() {
        SavedServer(includeCsrf = false).use { server ->
            val result = runSimulation(server)
            assertEquals(2, result.exitCode, result.output)
            assertTrue(server.requests.none { it.first == "POST" })
        }
    }

    @Test
    fun `resume cannot silently switch to another saved registration`() {
        SavedServer(wrongResume = true).use { server ->
            val result = runSimulation(server)
            assertEquals(2, result.exitCode, result.output)
            assertEquals(2, server.requests.size)
        }
    }

    @Test
    fun `unsaved bedroom changes fail before deletion`() {
        SavedServer(saveProgress = false).use { server ->
            val result = runSimulation(server)
            assertEquals(2, result.exitCode, result.output)
            assertTrue(server.requests.none { it.second.startsWith("/landlord/delete-incomplete-property") })
        }
    }

    @Test
    fun `successful delete statuses cannot hide a retained target or lost unrelated registration`() {
        for (loseUnrelated in listOf(false, true)) {
            SavedServer(deleteTarget = loseUnrelated, loseUnrelated = loseUnrelated).use { server ->
                val result = runSimulation(server)
                assertEquals(2, result.exitCode, result.output)
                assertEquals(15, server.requests.size)
            }
        }
    }

    private fun runSimulation(server: SavedServer): GatlingTestRunner.Result =
        GatlingTestRunner(outputDirectory).run(
            BasicContractSimulation::class.java,
            mapOf(
                "gatling.contract.journey" to "saved-registration",
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "synthetic-landlord",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
        )

    private class SavedServer(
        private val includeCsrf: Boolean = true,
        private val saveProgress: Boolean = true,
        private val deleteTarget: Boolean = true,
        private val loseUnrelated: Boolean = false,
        private val wrongResume: Boolean = false,
        private val nextStep: String = "ownership-type",
        private val nextField: String = "ownershipType",
    ) : AutoCloseable {
        val requests = ConcurrentLinkedQueue<Pair<String, String>>()
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl get() = "http://127.0.0.1:${server.address.port}"
        private val deleted = mutableSetOf<String>()
        private val bedrooms = mutableMapOf<String, String>()

        init {
            server.createContext("/") { exchange ->
                val path = exchange.requestURI.path
                val query = fields(exchange.requestURI.rawQuery.orEmpty())
                val id = query["journeyId"] ?: query["contextId"] ?: ""
                val method = exchange.requestMethod
                requests.add(method to path)
                var status = 200
                var body = ""

                fun redirect(location: String) {
                    status = 302
                    exchange.responseHeaders.add("Location", location)
                }

                fun form(input: String): String {
                    val csrf = if (includeCsrf) """<input type="hidden" name="_csrf" value="saved-csrf">""" else ""
                    return """<form method="post" action="${exchange.requestURI}">$csrf
                        <input type="hidden" name="version" value="saved-version">$input</form>"""
                }
                when {
                    path == "/landlord/incomplete-properties" -> {
                        exchange.responseHeaders.add("Set-Cookie", "SESSION=saved-session; Path=/")
                        for (pass in listOf("warmup", "measured")) {
                            body += """<a href="/landlord/register-property/resume?contextId=resume-$pass-unrelated">Other</a>
                                <a href="/landlord/delete-incomplete-property?contextId=delete-$pass-unrelated">Other</a>"""
                            if (!loseUnrelated || "delete-$pass" !in deleted) {
                                body += """<div class="govuk-summary-card"><span>1 Fixture Road, EG1 2AA</span>
                                    <a href="/landlord/register-property/resume?contextId=resume-$pass">Continue</a></div>"""
                            }
                            if ("delete-$pass" !in deleted) {
                                body += """<a href="/landlord/delete-incomplete-property?contextId=delete-$pass">Delete</a>"""
                            }
                        }
                    }

                    path.endsWith("/resume") -> {
                        val resumedId = if (wrongResume) "unrelated" else id
                        redirect("/landlord/register-property/task-list?journeyId=$resumedId&withBackUrl=fixture")
                    }
                    path.endsWith("/task-list") ->
                        body = """<a href="/landlord/register-property/lookup-address?journeyId=$id">Property details</a>"""

                    path.endsWith("/confirmation") ->
                        body = """<div class="govuk-panel"><h1>Deleted</h1></div>
                            <a href="/landlord/incomplete-properties">Incomplete properties</a>"""

                    method == "POST" -> {
                        val fields = fields(exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8))
                        if (fields["_csrf"] != "saved-csrf" || fields["version"] != "saved-version" ||
                            exchange.requestHeaders.getFirst("Cookie") != "SESSION=saved-session"
                        ) {
                            status = 403
                        } else {
                            when {
                                path.endsWith("/select-address") -> {
                                    if (fields["address"] != "1 Fixture Road, EG1 2AA") {
                                        status = 400
                                    } else {
                                        redirect("/landlord/register-property/property-type?journeyId=$id")
                                    }
                                }
                                path.endsWith("/property-type") -> {
                                    if (fields["propertyType"] != "DETACHED_HOUSE") {
                                        status = 400
                                    } else {
                                        redirect("/landlord/register-property/number-of-bedrooms?journeyId=$id")
                                    }
                                }
                                path.endsWith("/number-of-bedrooms") -> {
                                    if (saveProgress) bedrooms[id] = fields.getValue("numberOfBedrooms")
                                    redirect("/landlord/register-property/$nextStep?journeyId=$id")
                                }
                                path == "/landlord/delete-incomplete-property" -> {
                                    if (fields["wantsToProceed"] != "true") {
                                        status = 400
                                    } else {
                                        if (deleteTarget) deleted.add(id)
                                        redirect("/landlord/delete-incomplete-property/confirmation?contextId=$id")
                                    }
                                }
                                else -> status = 404
                            }
                        }
                    }
                    path.endsWith("/select-address") ->
                        body = form("""<input name="address" value="1 Fixture Road, EG1 2AA">""")
                    path.endsWith("/property-type") ->
                        body = form("""<input name="propertyType" value="DETACHED_HOUSE">""")
                    path.endsWith("/number-of-bedrooms") ->
                        body = form("""<input name="numberOfBedrooms" value="${bedrooms[id].orEmpty()}">""")
                    path.endsWith("/$nextStep") -> body = form("""<input name="$nextField">""")
                    path == "/landlord/delete-incomplete-property" ->
                        body = form("""<input name="wantsToProceed" value="true">""")
                    else -> status = 404
                }
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "text/html")
                exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        private fun fields(value: String): Map<String, String> =
            value.split("&").filter { it.isNotBlank() }.associate {
                val pair = it.split("=", limit = 2)
                URLDecoder.decode(pair[0], StandardCharsets.UTF_8) to
                    URLDecoder.decode(pair.getOrElse(1) { "" }, StandardCharsets.UTF_8)
            }

        override fun close() = server.stop(0)
    }
}

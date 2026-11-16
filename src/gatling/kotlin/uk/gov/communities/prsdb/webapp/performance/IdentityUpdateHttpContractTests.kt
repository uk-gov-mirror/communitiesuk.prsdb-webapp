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

class IdentityUpdateHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `name and date of birth updates preserve CSRF session hidden fields and saved values`() {
        IdentityServer().use { server ->
            val result = runSimulation(server)
            assertEquals(0, result.exitCode, result.output)
            assertEquals(20, server.requests.size)
            assertEquals(4, server.requests.count { it.first == "POST" })
        }
    }

    @Test
    fun `missing CSRF stops identity mutation`() {
        IdentityServer(includeCsrf = false).use { server ->
            val result = runSimulation(server)
            assertEquals(2, result.exitCode, result.output)
            assertTrue(server.requests.none { it.first == "POST" })
        }
    }

    @Test
    fun `unchanged saved identity fails despite successful HTTP statuses`() {
        IdentityServer(saveIdentity = false).use { server ->
            val result = runSimulation(server)
            assertEquals(2, result.exitCode, result.output)
            assertEquals(5, server.requests.size)
        }
    }

    private fun runSimulation(server: IdentityServer): GatlingTestRunner.Result =
        GatlingTestRunner(outputDirectory).run(
            BasicContractSimulation::class.java,
            mapOf(
                "gatling.contract.journey" to "identity",
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "synthetic-landlord",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
        )

    private class IdentityServer(
        private val includeCsrf: Boolean = true,
        private val saveIdentity: Boolean = true,
    ) : AutoCloseable {
        val requests = ConcurrentLinkedQueue<Pair<String, String>>()
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl get() = "http://127.0.0.1:${server.address.port}"
        private var name = "Original Landlord"
        private var dateOfBirth = "1 January 1980"

        init {
            server.createContext("/") { exchange ->
                val path = exchange.requestURI.path
                val method = exchange.requestMethod
                requests.add(method to path)
                val fields =
                    exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8)
                        .split("&").filter { it.isNotBlank() }.associate {
                            val parts = it.split("=", limit = 2)
                            URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to
                                URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8)
                        }
                var status = 200
                var body = ""

                fun redirect(destination: String) {
                    status = 302
                    exchange.responseHeaders.add("Location", destination)
                }
                when {
                    method == "POST" &&
                        (
                            fields["_csrf"] != "identity-csrf" || fields["version"] != "fixture-version" ||
                                exchange.requestHeaders.getFirst("Cookie") != "SESSION=identity-session" ||
                                exchange.requestURI.rawQuery != "journeyId=identity-contract"
                        ) -> status = 403

                    path == "/landlord/landlord-details" -> {
                        exchange.responseHeaders.add("Set-Cookie", "SESSION=identity-session; Path=/")
                        body = """
                            <div class="govuk-summary-list__row"><span class="govuk-summary-list__value">$name</span>
                            <a href="/landlord/landlord-details/update-name/name">Change</a></div>
                            <div class="govuk-summary-list__row"><span class="govuk-summary-list__value">$dateOfBirth</span>
                            <a href="/landlord/landlord-details/update-date-of-birth/date-of-birth">Change</a></div>
                            """
                    }

                    method == "GET" && exchange.requestURI.rawQuery == null -> redirect("$path?journeyId=identity-contract")
                    method == "GET" -> {
                        val csrf = if (includeCsrf) """<input type="hidden" name="_csrf" value="identity-csrf">""" else ""
                        body = """<form method="post" action="$path?journeyId=identity-contract">$csrf
                            <input type="hidden" name="version" value="fixture-version">
                            <input name="name"><input name="day"><input name="month"><input name="year"></form>"""
                    }

                    method == "POST" -> {
                        if (saveIdentity) {
                            if (fields.containsKey("name")) {
                                name = fields.getValue("name")
                            } else {
                                dateOfBirth = "${fields["day"]} January ${fields["year"]}"
                            }
                        }
                        redirect("/landlord/landlord-details")
                    }

                    else -> status = 404
                }
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "text/html")
                exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        override fun close() = server.stop(0)
    }
}

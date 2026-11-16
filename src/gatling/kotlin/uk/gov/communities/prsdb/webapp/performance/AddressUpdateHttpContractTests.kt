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

class AddressUpdateHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `selected and manual address updates preserve session CSRF hidden fields and journey URL`() {
        AddressServer().use { server ->
            val result = runSimulation(server)

            assertEquals(0, result.exitCode, result.output)
            assertEquals(32, server.requests.size)
            assertEquals(10, server.requests.count { it.first == "POST" })
        }
    }

    @Test
    fun `missing CSRF on lookup stops address mutation`() {
        AddressServer(includeCsrf = false).use { server ->
            val result = runSimulation(server)

            assertEquals(2, result.exitCode, result.output)
            assertTrue(server.requests.none { it.first == "POST" })
        }
    }

    @Test
    fun `unchanged saved address fails even when every request returns its expected status`() {
        AddressServer(saveAddress = false).use { server ->
            val result = runSimulation(server)

            assertEquals(2, result.exitCode, result.output)
            assertTrue(server.requests.any { it.first == "POST" })
            assertEquals(7, server.requests.size)
            assertEquals(2, server.requests.count { it.second == "/landlord/landlord-details" })
        }
    }

    private fun runSimulation(server: AddressServer): GatlingTestRunner.Result =
        GatlingTestRunner(outputDirectory).run(
            BasicContractSimulation::class.java,
            mapOf(
                "gatling.contract.journey" to "address",
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "synthetic-landlord",
                "gatling.basic.registrationSubject" to "synthetic-registration",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
        )

    private class AddressServer(
        private val includeCsrf: Boolean = true,
        private val saveAddress: Boolean = true,
    ) : AutoCloseable {
        val requests = ConcurrentLinkedQueue<Pair<String, String>>()
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        private var address = "Original address"
        val baseUrl get() = "http://127.0.0.1:${server.address.port}"

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

                fun form(inputs: String): String {
                    val csrf = if (includeCsrf) """<input type="hidden" name="_csrf" value="address-csrf">""" else ""
                    return """<form method="post" action="$path?journeyId=address-contract">$csrf
                        <input type="hidden" name="version" value="fixture-version">$inputs</form>"""
                }
                when {
                    method == "POST" &&
                        (
                            fields["_csrf"] != "address-csrf" || fields["version"] != "fixture-version" ||
                                exchange.requestHeaders.getFirst("Cookie") != "SESSION=address-session" ||
                                exchange.requestURI.rawQuery != "journeyId=address-contract"
                        ) -> status = 403

                    path == "/landlord/landlord-details" -> {
                        exchange.responseHeaders.add("Set-Cookie", "SESSION=address-session; Path=/")
                        body = """<div class="govuk-summary-list__row"><span class="govuk-summary-list__value">$address</span>
                            <a href="/landlord/landlord-details/update-address/lookup-address">Change</a></div>"""
                    }

                    path.endsWith("/lookup-address") && exchange.requestURI.rawQuery == null ->
                        redirect("$path?journeyId=address-contract")

                    path.endsWith("/lookup-address") && method == "GET" ->
                        body = form("""<input name="postcode"><input name="houseNameOrNumber">""")

                    path.endsWith("/lookup-address") -> redirect("/address/select-address?journeyId=address-contract")
                    path.endsWith("/select-address") && method == "GET" ->
                        body =
                            form(
                                """<input name="address" value="1 PRSDB Performance Square, EG1 2AA">
                                    <input name="address" value="MANUAL">""",
                            )

                    path.endsWith("/select-address") && fields["address"] == "MANUAL" ->
                        redirect("/address/manual-address?journeyId=address-contract")

                    path.endsWith("/select-address") && fields["address"] == "1 PRSDB Performance Square, EG1 2AA" -> {
                        if (saveAddress) address = "1 PRSDB Performance Square, EG1 2AA"
                        redirect("/landlord/landlord-details")
                    }

                    path.endsWith("/manual-address") && method == "GET" ->
                        body = form("""<input name="addressLineOne"><input name="townOrCity"><input name="postcode">""")

                    path.endsWith("/manual-address") -> {
                        if (saveAddress) address = "${fields["addressLineOne"]}, ${fields["townOrCity"]}, ${fields["postcode"]}"
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

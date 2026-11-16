package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals

class LandlordReadHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `reads discover owned property links and validate address and compliance content`() {
        ReadServer().use { server ->
            val result = runSimulation(server)
            assertEquals(0, result.exitCode, result.output)
            assertEquals(10, server.requests.size)
            assertEquals(2, server.requests.count { it == "/landlord/property-details/431?backLink=fixture" })
        }
    }

    @Test
    fun `a successful response for a different property fails the read journey`() {
        ReadServer(wrongProperty = true).use { server ->
            val result = runSimulation(server)
            assertEquals(2, result.exitCode, result.output)
            assertEquals(3, server.requests.size)
        }
    }

    @Test
    fun `an expired session cannot turn a read journey into a successful login page`() {
        ReadServer(expiredSession = true).use { server ->
            val result = runSimulation(server)
            assertEquals(2, result.exitCode, result.output)
            assertEquals(listOf("/landlord/dashboard", "/landlord/landlord-details"), server.requests.toList())
        }
    }

    private fun runSimulation(server: ReadServer): GatlingTestRunner.Result =
        GatlingTestRunner(outputDirectory).run(
            BasicContractSimulation::class.java,
            mapOf(
                "gatling.contract.journey" to "landlord-read",
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "synthetic-landlord",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
        )

    private class ReadServer(
        private val wrongProperty: Boolean = false,
        private val expiredSession: Boolean = false,
    ) : AutoCloseable {
        val requests = ConcurrentLinkedQueue<String>()
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl get() = "http://127.0.0.1:${server.address.port}"

        init {
            server.createContext("/") { exchange ->
                val path = exchange.requestURI.path
                requests.add(exchange.requestURI.toString())
                var status = 200
                val body =
                    when {
                        path == "/landlord/dashboard" -> {
                            exchange.responseHeaders.add("Set-Cookie", "SESSION=read-session; Path=/")
                            """<main><h1>Fixture Landlord</h1>
                                <a href="/landlord/landlord-details#registered-properties">Properties</a>
                                <a href="/landlord/incomplete-properties">Incomplete</a>
                                <a href="/landlord/compliance-actions">Actions</a></main>"""
                        }

                        expiredSession -> {
                            status = 302
                            exchange.responseHeaders.add("Location", "/login")
                            ""
                        }

                        exchange.requestHeaders.getFirst("Cookie") != "SESSION=read-session" -> {
                            status = 403
                            ""
                        }

                        path == "/landlord/landlord-details" ->
                            """<main><h1>Landlord details</h1><div id="registered-properties">
                                <a href="/landlord/property-details/431?backLink=fixture">1 Fixture Road, EG1 2AA</a></div></main>"""

                        path == "/landlord/property-details/431" ->
                            """<main><h1>${if (wrongProperty) "99 Unrelated Road" else "1 Fixture Road<br>EG1 2AA"}</h1>
                                <div id="property-details"><dl class="govuk-summary-list"></dl></div>
                                <div id="compliance-information"><dl class="govuk-summary-list"></dl></div></main>"""

                        path == "/landlord/compliance-actions" -> "<main><h1>Compliance actions</h1></main>"
                        path == "/landlord/incomplete-properties" -> "<main><h1>Incomplete property details</h1></main>"
                        else -> {
                            status = 404
                            ""
                        }
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

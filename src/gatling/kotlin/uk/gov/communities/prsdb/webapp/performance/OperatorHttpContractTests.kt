package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.test.assertEquals

class OperatorHttpContractTests {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `dashboard checks the invitation form without calling metrics or passcode dependencies`() {
        for (includeCsrf in listOf(true, false)) {
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            val paths = mutableListOf<String>()
            server.createContext("/") { exchange ->
                paths.add(exchange.requestURI.path)
                val body =
                    """<main><a href="/system-operator/invite-lc-admin">Invite</a>
                    <a href="/system-operator/metrics">Metrics</a>
                    <form method="post" action="${exchange.requestURI.path}">
                    ${if (includeCsrf) """<input type="hidden" name="_csrf" value="csrf">""" else ""}
                    <input type="hidden" name="version" value="current"></form></main>"""
                        .toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "text/html")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            server.start()
            try {
                val result =
                    GatlingTestRunner(directory).run(
                        OperatorContractSimulation::class.java,
                        mapOf(
                            "gatling.target" to "local",
                            "gatling.baseUrl" to "http://127.0.0.1:${server.address.port}",
                            "gatling.simulatorUrl" to "http://127.0.0.1:3000",
                            "gatling.basic.mode" to "baseline",
                            "gatling.basic.landlordSubject" to "operator",
                        ),
                    )
                assertEquals(if (includeCsrf) 0 else 2, result.exitCode, result.output)
                assertEquals(
                    listOf(
                        "/system-operator/dashboard",
                        "/system-operator/manage-local-council-admins",
                        "/system-operator/invite-lc-admin",
                    ),
                    paths,
                )
            } finally {
                server.stop(0)
            }
        }
    }
}

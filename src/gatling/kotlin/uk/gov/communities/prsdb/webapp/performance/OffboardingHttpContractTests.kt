package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.gov.communities.prsdb.webapp.controllers.DeregisterPropertyController
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.test.assertEquals

class OffboardingHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `property deregistration correlates protected forms and reaches confirmation`() {
        for (includeCsrf in listOf(true, false)) {
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            val route = DeregisterPropertyController.PROPERTY_DEREGISTRATION_ROUTE.replace("{propertyOwnershipId}", "101")
            val details = PropertyDetailsController.LANDLORD_PROPERTY_DETAILS_ROUTE.replace("{propertyOwnershipId}", "101")
            val posts = mutableListOf<Map<String, String>>()
            var completed = false
            server.createContext("/") { exchange ->
                val path = exchange.requestURI.path
                var code = 200
                var body = ""

                fun redirect(destination: String) {
                    code = 302
                    exchange.responseHeaders.add("Location", "$destination?journeyId=offboarding-contract")
                }
                if (exchange.requestMethod == "POST") {
                    val fields =
                        exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8).split("&").associate {
                            val pair = it.split("=", limit = 2)
                            URLDecoder.decode(pair[0], StandardCharsets.UTF_8) to
                                URLDecoder.decode(pair.getOrElse(1) { "" }, StandardCharsets.UTF_8)
                        }
                    posts.add(fields)
                    if (fields != mapOf("_csrf" to "offboarding-csrf", "version" to "current") ||
                        exchange.requestURI.rawQuery != "journeyId=offboarding-contract" ||
                        exchange.requestHeaders.getFirst("Cookie") != "SESSION=offboarding"
                    ) {
                        code = 403
                    } else if (path == "$route/info") {
                        redirect("$route/start")
                    } else if (path == "$route/confirm") {
                        completed = true
                        redirect("$route/confirmation")
                    } else {
                        code = 404
                    }
                } else {
                    when {
                        path == details -> body = """<a href="$route/deregister">Deregister</a>"""
                        path == "$route/deregister" && exchange.requestURI.rawQuery == null -> redirect(path)
                        path == "$route/deregister" -> redirect("$route/info")
                        path == "$route/start" -> redirect("$route/confirm")
                        path == "$route/info" || path == "$route/confirm" ->
                            body = """<form method="post" action="">
                                ${if (includeCsrf) """<input type="hidden" name="_csrf" value="offboarding-csrf">""" else ""}
                                <input type="hidden" name="version" value="current"></form>"""

                        path == "$route/confirmation" && completed ->
                            body = """<h1>Deregistered</h1><p>1 Fixture Road<br>Testtown<br>AB1 2CD</p>"""

                        else -> code = 404
                    }
                }
                exchange.responseHeaders.add("Content-Type", "text/html")
                exchange.responseHeaders.add("Set-Cookie", "SESSION=offboarding; Path=/")
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
            try {
                val result =
                    GatlingTestRunner(outputDirectory).run(
                        OffboardingContractSimulation::class.java,
                        mapOf(
                            "gatling.target" to "local",
                            "gatling.baseUrl" to "http://127.0.0.1:${server.address.port}",
                            "gatling.simulatorUrl" to "http://127.0.0.1:3000",
                            "gatling.basic.landlordSubject" to "contract-landlord",
                            "gatling.basic.mode" to "baseline",
                            "gatling.basic.iterations" to "1",
                        ),
                    )
                assertEquals(if (includeCsrf) 0 else 2, result.exitCode, result.output)
                assertEquals(if (includeCsrf) 2 else 0, posts.size)
                assertEquals(includeCsrf, completed)
            } finally {
                server.stop(0)
            }
        }
    }
}

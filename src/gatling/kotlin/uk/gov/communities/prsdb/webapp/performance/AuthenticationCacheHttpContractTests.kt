package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpServer
import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals

class AuthenticationCacheHttpContractTests {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `simultaneous actors do not reuse cached One Login authorization responses`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val origin = "http://127.0.0.1:${server.address.port}"
        val authorizationResponses = ConcurrentHashMap<String, String>()
        val usedCodes = ConcurrentHashMap.newKeySet<String>()
        val authenticatedSubjects = ConcurrentHashMap.newKeySet<String>()
        server.createContext("/") { exchange ->
            var code = 200
            var body = ""

            fun redirect(location: String) {
                code = 302
                exchange.responseHeaders.add("Location", location)
            }

            when (exchange.requestURI.path) {
                "/oauth2/authorization/one-login" -> {
                    // Model the CDN minimum TTL: no-store alone does not prevent a shared cached redirect.
                    exchange.responseHeaders.add("Cache-Control", "no-store")
                    val authorization =
                        authorizationResponses.computeIfAbsent(exchange.requestURI.toString()) { "code-${authorizationResponses.size}" }
                    redirect("$origin/authorize?code=$authorization")
                }
                "/authorize" -> {
                    val authorization = exchange.requestURI.rawQuery.substringAfter("code=")
                    body =
                        """<form method="post" action="/submit">
                            <input type="hidden" name="authCode" value="$authorization">
                            <input type="hidden" name="authRequestParams" value="request">
                            <input type="hidden" name="state" value="$authorization">
                            <input name="email" value="synthetic@example.test">
                            <input name="phoneNumber" value="07700900000">
                            <input name="maxLoCAchieved" value="P2">
                            <textarea name="claims">claims</textarea>
                        </form>"""
                }
                "/submit" -> {
                    val fields =
                        exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8).split('&').associate {
                            val (name, value) = it.split('=', limit = 2)
                            URLDecoder.decode(name, StandardCharsets.UTF_8) to URLDecoder.decode(value, StandardCharsets.UTF_8)
                        }
                    redirect("$origin/callback?code=${fields.getValue("authCode")}&subject=${fields.getValue("sub")}")
                }
                "/callback" -> {
                    val fields = exchange.requestURI.rawQuery.split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
                    if (usedCodes.add(fields.getValue("code"))) {
                        exchange.responseHeaders.add("Set-Cookie", "AUTH=${fields.getValue("subject")}; Path=/; HttpOnly")
                        redirect("/protected")
                    } else {
                        redirect("/login?error")
                    }
                }
                "/protected" -> {
                    val subject = exchange.requestHeaders.getFirst("Cookie")?.substringAfter("AUTH=")
                    if (subject == null) {
                        redirect("/login")
                    } else {
                        authenticatedSubjects.add(subject)
                    }
                }
                else -> code = 404
            }
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html")
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()
        try {
            val result =
                GatlingTestRunner(directory).run(
                    AuthenticationCacheContractSimulation::class.java,
                    mapOf(
                        "gatling.target" to "local",
                        "gatling.baseUrl" to origin,
                        "gatling.simulatorUrl" to origin,
                        "gatling.basic.mode" to "baseline",
                        "gatling.basic.landlordSubject" to "synthetic",
                    ),
                )
            assertEquals(
                0,
                result.exitCode,
                "Authorizations: $authorizationResponses; used: $usedCodes; identities: $authenticatedSubjects\n${result.output}",
            )
            assertEquals(4, authorizationResponses.size)
            assertEquals((1..4).map { "actor-$it" }.toSet(), authenticatedSubjects)
        } finally {
            server.stop(0)
        }
    }
}

class AuthenticationCacheContractSimulation : Simulation() {
    init {
        val config = BasicRunConfig.from(System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) })
        val actors =
            (1..4).map {
                scenario("Actor $it")
                    .exec(OneLoginAuthentication.loginChain(config, "actor-$it"))
                    .exec(http("Authenticated identity").get("/protected").disableFollowRedirect().check(status().`is`(200)))
                    .injectOpen(atOnceUsers(1))
            }
        setUp(actors).protocols(http.baseUrl(config.baseUrl))
            .assertions(global().failedRequests().count().`is`(0L))
    }
}

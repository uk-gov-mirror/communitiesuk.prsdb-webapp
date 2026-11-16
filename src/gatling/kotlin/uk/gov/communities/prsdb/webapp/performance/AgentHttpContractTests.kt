package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `delegation submits csrf-protected forms and persists property access state`() {
        AgentServer().use { server ->
            val result = run(server)
            assertEquals(0, result.exitCode, result.output)
            assertEquals("agent-102@example.test", server.delegated[102])
            assertTrue(server.removed.isEmpty())
            assertEquals(2, server.posts.count { it["formData.emailAddress"] != null })
            assertEquals(0, server.posts.count { it["formData.wantsToProceed"] == "true" })
            assertEquals(2, server.detailsReads[102])
        }
    }

    @Test
    fun `letting-agent owner flow rejects missing csrf and an out of property form action`() {
        for (scenario in listOf("missing-csrf", "wrong-action")) {
            AgentServer(scenario).use { server ->
                val result = run(server)
                assertEquals(2, result.exitCode, result.output)
                assertTrue(server.posts.isEmpty())
                assertTrue(server.delegated.isEmpty())
            }
        }
    }

    @Test
    fun `prepare-delegation signs in each owner and leaves a family-specific delegation pending`() {
        AgentServer().use { server ->
            val ids = (101L..102L).toList()
            val families = listOf("access")
            val fixturePath = outputDirectory.resolve("agent-prepare.json")
            Files.writeString(
                fixturePath,
                ids.joinToString(",", "[", "]") { id ->
                    """{"ownerSubject":"owner-$id","propertyOwnershipId":$id,"address":"${if (id == 101L) "1" else "2"} Fixture Road"}"""
                },
            )
            val result =
                GatlingTestRunner(outputDirectory).run(
                    AgentSimulation::class.java,
                    mapOf(
                        "gatling.target" to "local",
                        "gatling.baseUrl" to server.baseUrl,
                        "gatling.simulatorUrl" to server.baseUrl,
                        "gatling.basic.landlordSubject" to "unused-subject",
                        "gatling.basic.mode" to "baseline",
                        "gatling.basic.iterations" to "1",
                        "gatling.agent.mode" to "prepare-delegation",
                        "gatling.agent.fixturePath" to fixturePath.toString(),
                    ),
                )

            assertEquals(0, result.exitCode, result.output)
            assertEquals(ids.map { "owner-$it" }, server.signedInSubjects)
            assertEquals(
                ids.mapIndexed {
                        index,
                        id,
                    ->
                    id to AgentJourney.preparationEmail(families[index % families.size], id)
                }.toMap(),
                server.delegated,
            )
            assertTrue(server.removed.isEmpty())
        }
    }

    private fun run(server: AgentServer) =
        GatlingTestRunner(outputDirectory).run(
            AgentContractSimulation::class.java,
            mapOf(
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "fixture-owner",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
        )

    private class AgentServer(
        private val scenario: String? = null,
    ) : AutoCloseable {
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val delegated = mutableMapOf<Long, String>()
        val removed = mutableSetOf<Long>()
        val posts = mutableListOf<Map<String, String>>()
        val detailsReads = mutableMapOf<Long, Int>()
        val signedInSubjects = mutableListOf<String>()

        init {
            server.createContext("/") { exchange ->
                if (handleSignIn(exchange)) return@createContext
                val parts = exchange.requestURI.path.split("/")
                val propertyId =
                    when (parts.getOrNull(2)) {
                        "property-details" -> parts.getOrNull(3)?.toLongOrNull()
                        "delegate-to-letting-agent" -> parts.getOrNull(3)?.toLongOrNull()
                        else -> null
                    }
                var code = 200
                var body = ""
                var location: String? = null

                fun redirect(target: String) {
                    code = 302
                    location = target
                }

                val hasJourney = exchange.requestURI.rawQuery.orEmpty().contains("journeyId=")
                val step = parts.lastOrNull().orEmpty()
                if (propertyId == null) {
                    code = 404
                } else if (exchange.requestMethod == "GET" && step in setOf("allow-letting-agent", "are-you-sure") && !hasJourney) {
                    redirect("${exchange.requestURI.path}?journeyId=${if (step == "are-you-sure") "remove" else "agent"}-$propertyId")
                } else if (parts.getOrNull(2) == "property-details" && parts.size <= 4) {
                    val id = propertyId
                    detailsReads[id] = (detailsReads[id] ?: 0) + 1
                    val email = delegated[id]?.takeUnless { id in removed }
                    val delegatePath = "/landlord/delegate-to-letting-agent/$id/allow-letting-agent"
                    val removePath = "/landlord/property-details/$id/remove-letting-agent/are-you-sure"
                    val address = if (id == 101L) "1 Fixture Road, EG1 2AA" else "2 Fixture Road, EG1 2AA"
                    body =
                        """<main><h1>$address</h1>
                        ${if (email == null) """<a href="$delegatePath">Delegate to a letting agent</a>""" else """<p>$email</p><a href="$removePath">Remove letting agent</a>"""}
                        </main>"""
                } else if (parts.getOrNull(2) == "delegate-to-letting-agent") {
                    val id = propertyId
                    if (exchange.requestMethod == "POST") {
                        val fields = parseForm(exchange)
                        posts.add(fields)
                        if (!validPost(exchange, fields, "agent-$id")) {
                            code = 403
                        } else {
                            delegated[id] = fields["formData.emailAddress"].orEmpty()
                            redirect("/landlord/delegate-to-letting-agent/$id/confirmation?journeyId=agent-$id")
                        }
                    } else if (step == "allow-letting-agent") {
                        val action = if (scenario == "wrong-action") "/outside" else ""
                        body = form(action, "formData.emailAddress", includeCsrf = scenario != "missing-csrf")
                    } else if (step == "confirmation") {
                        body =
                            "<main><h1>Delegation confirmation</h1><p>${delegated[id].orEmpty()}</p>" +
                            "<p>${if (id == 101L) "1 Fixture Road<br>EG1 2AA" else "2 Fixture Road<br>EG1 2AA"}</p></main>"
                    } else {
                        code = 404
                    }
                } else if (parts.getOrNull(2) == "property-details" && parts.getOrNull(4) == "remove-letting-agent") {
                    val id = propertyId
                    if (parts.getOrNull(5) == "are-you-sure") {
                        if (exchange.requestMethod == "POST") {
                            val fields = parseForm(exchange)
                            posts.add(fields)
                            if (!validPost(exchange, fields, "remove-$id")) {
                                code = 403
                            } else {
                                removed.add(id)
                                redirect("/landlord/property-details/$id/remove-letting-agent/confirmation?journeyId=remove-$id")
                            }
                        } else {
                            val action = ""
                            body =
                                """<main><h1>Remove letting agent</h1><p>${delegated[id].orEmpty()}</p>
                                <p>${if (id == 101L) "1 Fixture Road" else "2 Fixture Road"}</p>
                                ${form(action, "formData.wantsToProceed", includeCsrf = scenario != "missing-csrf", radio = true)}</main>"""
                        }
                    } else if (parts.getOrNull(5) == "confirmation") {
                        body =
                            "<main><h1>Removal confirmation</h1><p>${delegated[id].orEmpty()}</p>" +
                            "<p>${if (id == 101L) "1 Fixture Road<br>EG1 2AA" else "2 Fixture Road<br>EG1 2AA"}</p></main>"
                    } else {
                        code = 404
                    }
                } else {
                    code = 404
                }

                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                if (exchange.requestHeaders.getFirst("Cookie") == null) {
                    exchange.responseHeaders.add("Set-Cookie", "SESSION=agent-session; Path=/")
                }
                location?.let { exchange.responseHeaders.add("Location", it) }
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        private fun handleSignIn(exchange: HttpExchange): Boolean {
            var location: String? = null
            var body = ""
            when (exchange.requestURI.path) {
                "/oauth2/authorization/one-login" -> location = "$baseUrl/authorize-login"
                "/authorize-login" ->
                    body =
                        """<main><form method="post" action="/simulator/login">
                        <input type="hidden" name="authCode" value="code"><input type="hidden" name="authRequestParams" value="p">
                        <input type="hidden" name="state" value="state"><input type="hidden" name="email" value="owner@example.test">
                        <input type="hidden" name="phoneNumber" value="07700900000"><input type="hidden" name="maxLoCAchieved" value="P0">
                        <textarea name="authRequestClaims">{}</textarea></form></main>"""
                "/simulator/login" -> {
                    signedInSubjects.add(parseForm(exchange).getValue("sub"))
                    location = "/landlord/login/oauth2/code/one-login?code=code"
                }
                "/landlord/login/oauth2/code/one-login" -> location = "/"
                else -> return false
            }
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            location?.let { exchange.responseHeaders.add("Location", it) }
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(if (location == null) 200 else 302, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            return true
        }

        private fun validPost(
            exchange: HttpExchange,
            fields: Map<String, String>,
            journey: String,
        ) = fields["_csrf"] == "agent-csrf" &&
            exchange.requestHeaders.getFirst("Cookie") == "SESSION=agent-session" &&
            exchange.requestURI.rawQuery.orEmpty().contains("journeyId=$journey")

        private fun form(
            action: String,
            field: String,
            includeCsrf: Boolean,
            radio: Boolean = false,
        ): String {
            val csrf = if (includeCsrf) """<input type="hidden" name="_csrf" value="agent-csrf">""" else ""
            val input =
                if (radio) {
                    """<input type="radio" name="$field" value="true">"""
                } else {
                    """<input name="$field" value="">"""
                }
            return """<main><h1>Letting agent</h1><form method="post" action="$action">
                $csrf<input type="hidden" name="version" value="agent-version">$input</form></main>"""
        }

        private fun parseForm(exchange: HttpExchange): Map<String, String> =
            exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8)
                .split("&")
                .filter { it.isNotEmpty() }
                .associate { item ->
                    val pair = item.split("=", limit = 2)
                    URLDecoder.decode(pair[0], StandardCharsets.UTF_8) to
                        URLDecoder.decode(pair.getOrElse(1) { "" }, StandardCharsets.UTF_8)
                }

        override fun close() {
            server.stop(0)
        }
    }
}

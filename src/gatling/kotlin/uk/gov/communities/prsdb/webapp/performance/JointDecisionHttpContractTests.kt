package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JointDecisionHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `accepting adds the existing invitee to the property`() {
        DecisionServer().use { server ->
            val result = run(server)
            assertEquals(0, result.exitCode, result.output)
            assertEquals(setOf(101L, 102L), server.accepted)
            assertEquals(server.accepted, server.propertyReads)
            assertEquals(server.accepted, server.roleChecks)
            assertTrue(server.posts.any { it["formData.isInviteAccepted"] == "true" })
        }
    }

    @Test
    fun `joint invitation decision rejects missing csrf and an out of journey form action`() {
        for (scenario in listOf("missing-csrf", "wrong-action")) {
            DecisionServer(scenario).use { server ->
                val result = run(server)
                assertEquals(2, result.exitCode, result.output)
                assertTrue(server.posts.isEmpty())
                assertTrue(server.accepted.isEmpty())
            }
        }
    }

    private fun run(server: DecisionServer) =
        GatlingTestRunner(outputDirectory).run(
            JointDecisionContractSimulation::class.java,
            mapOf(
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "fixture-owner",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
        )

    private class DecisionServer(
        private val scenario: String? = null,
    ) : AutoCloseable {
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val posts = mutableListOf<Map<String, String>>()
        val accepted = mutableSetOf<Long>()
        val propertyReads = mutableSetOf<Long>()
        val roleChecks = mutableSetOf<Long>()
        private var confirmationAddress = ""

        init {
            server.createContext("/") { exchange ->
                val path = exchange.requestURI.path
                val parts = path.split("/")
                val invitationRoute = "/landlord/joint-landlord-invitation"
                val propertyId =
                    when (parts.getOrNull(2)) {
                        "property-details" -> parts.getOrNull(3)?.toLongOrNull()
                        else -> null
                    }
                var code = 200
                var body = ""
                var location: String? = null

                fun redirect(target: String) {
                    code = 302
                    location = target
                }

                if (path == invitationRoute) {
                    val token =
                        exchange.requestURI.rawQuery.orEmpty().substringAfter("token=", "")
                    val id = token.takeLast(4).toIntOrNull()?.minus(100)
                    redirect("$invitationRoute/validate-token?journeyId=decision-$id")
                } else if (path == "$invitationRoute/validate-token") {
                    val id = exchange.requestURI.rawQuery.orEmpty().substringAfter("decision-").toLongOrNull()
                    redirect("$invitationRoute/accept-or-reject?journeyId=decision-$id")
                } else if (path == "$invitationRoute/accept-or-reject") {
                    val id = exchange.requestURI.rawQuery.orEmpty().substringAfter("decision-").toLongOrNull()
                    val selectedScenario = scenario
                    val action =
                        if (selectedScenario == "wrong-action") {
                            "/outside"
                        } else {
                            ""
                        }
                    val inviteeEmail =
                        when (id) {
                            101L -> "invitee-101@example.test"
                            else -> "invitee-${id ?: "unknown"}@example.test"
                        }
                    body =
                        """<main><h1>Accept or reject invitation</h1><p>${id?.let { if (it == 102L) "2 Fixture Road<br>Fixture Town" else "1 Fixture Road<br>Fixture Town" }}</p>
                        ${form(action, "formData.isInviteAccepted", selectedScenario != "missing-csrf", radio = true)}</main>"""
                    if (exchange.requestMethod == "POST") {
                        val fields = parseForm(exchange)
                        posts.add(fields)
                        if (!validPost(exchange, fields, "decision-$id")) {
                            code = 403
                        } else if (fields["formData.isInviteAccepted"] == "true") {
                            redirect("$invitationRoute/check-user-role?journeyId=decision-$id")
                        } else {
                            code = 400
                        }
                    }
                } else if (path == "$invitationRoute/check-user-role") {
                    val id = exchange.requestURI.rawQuery.orEmpty().substringAfter("decision-").toLongOrNull()
                    roleChecks.add(requireNotNull(id))
                    redirect("$invitationRoute/confirm-landlord-for-property?journeyId=decision-$id")
                } else if (path == "$invitationRoute/confirm-landlord-for-property") {
                    val id = exchange.requestURI.rawQuery.orEmpty().substringAfter("decision-").toLongOrNull()
                    if (exchange.requestMethod == "POST") {
                        val fields = parseForm(exchange)
                        posts.add(fields)
                        if (code == 403 || !validPost(exchange, fields, "decision-$id")) {
                            code = 403
                        } else {
                            accepted.add(requireNotNull(id))
                            confirmationAddress = if (id == 102L) "2 Fixture Road" else "1 Fixture Road"
                            redirect("$invitationRoute/property-joined-confirmation")
                        }
                    } else {
                        body =
                            """<main><h1>Confirm landlord status</h1><p>${id?.let { if (it == 102L) "2 Fixture Road" else "1 Fixture Road" }}</p>
                            ${form("", null, true)}</main>"""
                    }
                } else if (path == "$invitationRoute/property-joined-confirmation") {
                    body = "<main><h1>Invitation accepted</h1><p>$confirmationAddress</p></main>"
                } else if (parts.getOrNull(2) == "property-details") {
                    val id = propertyId
                    if (id == null || id !in accepted) {
                        code = 404
                    } else {
                        propertyReads.add(id)
                        body = "<main><h1>${if (id == 102L) "2 Fixture Road" else "1 Fixture Road"}</h1></main>"
                    }
                } else {
                    code = 404
                }

                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                if (exchange.requestHeaders.getFirst("Cookie") == null) {
                    exchange.responseHeaders.add("Set-Cookie", "SESSION=decision-session; Path=/")
                }
                location?.let { exchange.responseHeaders.add("Location", it) }
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        private fun validPost(
            exchange: HttpExchange,
            fields: Map<String, String>,
            journey: String,
        ) = fields["_csrf"] == "decision-csrf" &&
            exchange.requestHeaders.getFirst("Cookie") == "SESSION=decision-session" &&
            exchange.requestURI.rawQuery.orEmpty().contains("journeyId=$journey")

        private fun form(
            action: String,
            field: String?,
            includeCsrf: Boolean,
            radio: Boolean = false,
        ): String {
            val csrf = if (includeCsrf) """<input type="hidden" name="_csrf" value="decision-csrf">""" else ""
            val input =
                when {
                    field == null -> ""
                    radio -> """<input type="radio" name="$field" value="true"><input type="radio" name="$field" value="false">"""
                    else -> """<input name="$field" value="">"""
                }
            return """<form method="post" action="$action">$csrf""" +
                """<input type="hidden" name="version" value="decision-version">$input</form>"""
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

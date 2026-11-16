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

class JointLandlordHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `decision preparation invites the fixture invitee and leaves the invitation pending`() {
        JointServer().use { server ->
            val result = run(server, mapOf("gatling.joint.mode" to "prepare-decision"))
            assertEquals(0, result.exitCode, result.output)
            assertTrue(server.posts.any { it["formData.emailAddress"] == "invitee-102@example.test" })
            assertEquals(0, server.resends)
            assertTrue(server.cancelled.isEmpty())
            assertEquals("invitee-102@example.test", server.pendingInvitationEmail(102))
        }
    }

    @Test
    fun `joint landlord flow rejects missing csrf and an out of journey form action before posting`() {
        for (scenario in listOf("missing-csrf", "wrong-action")) {
            JointServer(scenario).use { server ->
                val result = run(server)
                assertEquals(2, result.exitCode, result.output)
                assertTrue(server.posts.isEmpty())
                assertTrue(server.cancelled.isEmpty())
            }
        }
    }

    private fun run(
        server: JointServer,
        extraProperties: Map<String, String> = emptyMap(),
    ) = GatlingTestRunner(outputDirectory).run(
        JointContractSimulation::class.java,
        extraProperties +
            mapOf(
                "gatling.target" to "local",
                "gatling.baseUrl" to server.baseUrl,
                "gatling.simulatorUrl" to server.baseUrl,
                "gatling.basic.landlordSubject" to "fixture-owner",
                "gatling.basic.mode" to "baseline",
                "gatling.basic.iterations" to "1",
            ),
    )

    private class JointServer(
        private val scenario: String? = null,
    ) : AutoCloseable {
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val posts = mutableListOf<Map<String, String>>()
        val cancelled = mutableSetOf<Long>()
        val resends: Int
            get() = resendCount
        val detailsReads = mutableMapOf<Long, Int>()
        private val emails = mutableMapOf<Long, String>()
        private val invitations = mutableMapOf<Long, Long>()
        private var resendCount = 0
        private var resendFlashPending = false

        fun pendingInvitationEmail(id: Long) = emails[id]?.takeIf { id !in cancelled }

        init {
            server.createContext("/") { exchange ->
                val path = exchange.requestURI.path
                val parts = path.split("/")
                val isPropertyRoute = parts.getOrNull(2) == "property-details"
                val propertyId = if (isPropertyRoute) parts.getOrNull(3)?.toLongOrNull() else null
                val invitationId = if (!isPropertyRoute) parts.getOrNull(3)?.toLongOrNull() else null
                val queryId =
                    exchange.requestURI.rawQuery.orEmpty().split("&")
                        .mapNotNull { item ->
                            val pair = item.split("=", limit = 2)
                            if (pair.firstOrNull() == "propertyOwnershipId") pair.getOrNull(1)?.toLongOrNull() else null
                        }.firstOrNull()
                val id = propertyId ?: invitationId?.let { invitations[it] } ?: queryId
                var code = 200
                var body = ""
                var location: String? = null

                fun redirect(target: String) {
                    code = 302
                    location = target
                }

                if (id == null) {
                    code = 404
                } else if (isPropertyRoute && parts.size <= 4) {
                    detailsReads[id] = (detailsReads[id] ?: 0) + 1
                    val email = emails[id]
                    val inviteLink = "/landlord/property-details/$id/invite-joint-landlord/start"
                    body =
                        """<main><h1>${if (id == 101L) "1 Fixture Road" else "2 Fixture Road"}</h1>
                        <a href="$inviteLink">Invite a joint landlord</a>
                        ${if (email != null && id !in cancelled) {
                            val invitationIdValue = 200 + id
                            invitations[invitationIdValue] = id
                            """<p>$email</p>
                            <a href="/landlord/property-details/$id/invite-joint-landlord/resend/$invitationIdValue">Resend</a>
                            <a href="/landlord/cancel-joint-landlord-invitation/$invitationIdValue/are-you-sure">Cancel</a>"""
                        } else {
                            ""
                        }}
                        ${if (resendFlashPending) "<p>New invitation email sent to $email</p>" else ""}</main>"""
                    resendFlashPending = false
                } else if (isPropertyRoute && parts.getOrNull(4) == "invite-joint-landlord" && parts.getOrNull(5) == "start") {
                    if (exchange.requestURI.rawQuery.orEmpty().contains("journeyId=")) {
                        redirect("/landlord/property-details/$id/invite-joint-landlord/has-joint-landlords?journeyId=joint-$id")
                    } else {
                        redirect("$path?journeyId=joint-$id")
                    }
                } else if (isPropertyRoute && parts.getOrNull(4) == "invite-joint-landlord" && parts.getOrNull(5) == "resend") {
                    resendCount += 1
                    resendFlashPending = true
                    redirect("/landlord/property-details/$id#landlord-details")
                } else if (path.startsWith("/landlord/cancel-joint-landlord-invitation/") &&
                    parts.getOrNull(4) == "are-you-sure"
                ) {
                    if (exchange.requestMethod == "GET" && !exchange.requestURI.rawQuery.orEmpty().contains("journeyId=")) {
                        redirect("$path?journeyId=cancel-$id")
                    } else if (exchange.requestMethod == "POST") {
                        val fields = parseForm(exchange)
                        posts.add(fields)
                        val expectedCookie = exchange.requestHeaders.getFirst("Cookie") == "SESSION=joint-session"
                        val expectedQuery = exchange.requestURI.rawQuery.orEmpty().contains("journeyId=cancel-$id")
                        if (fields["_csrf"] != "joint-csrf" || !expectedCookie || !expectedQuery ||
                            fields["formData.wantsToProceed"] != "true"
                        ) {
                            code = 403
                        } else {
                            cancelled.add(requireNotNull(id))
                            redirect("/landlord/cancel-joint-landlord-invitation/confirmation?propertyOwnershipId=$id")
                        }
                    } else {
                        body =
                            form("formData.wantsToProceed", includeCsrf = scenario != "missing-csrf")
                                .replace("</form>", "<p>${emails[id].orEmpty()}</p></form>")
                    }
                } else if (path.startsWith("/landlord/cancel-joint-landlord-invitation/") && parts.last() == "confirmation") {
                    body = "<main><h1>Invitation cancelled</h1><p>${emails[id].orEmpty()}</p></main>"
                } else if (isPropertyRoute) {
                    val step = parts.getOrNull(5).orEmpty()
                    if (exchange.requestMethod == "POST") {
                        val fields = parseForm(exchange)
                        posts.add(fields)
                        val expectedCookie = exchange.requestHeaders.getFirst("Cookie") == "SESSION=joint-session"
                        val expectedQuery = exchange.requestURI.rawQuery.orEmpty().contains("journeyId=")
                        if (fields["_csrf"] != "joint-csrf" || !expectedCookie || !expectedQuery) {
                            code = 403
                        } else {
                            when (step) {
                                "has-joint-landlords" ->
                                    redirect(
                                        "/landlord/property-details/$id/invite-joint-landlord/invite-joint-landlord?journeyId=joint-$id",
                                    )
                                "invite-joint-landlord" -> {
                                    emails[id] = fields["formData.emailAddress"].orEmpty()
                                    redirect(
                                        "/landlord/property-details/$id/invite-joint-landlord/check-joint-landlords?journeyId=joint-$id",
                                    )
                                }
                                "check-joint-landlords" ->
                                    redirect("/landlord/property-details/$id/invite-joint-landlord/check-invitations?journeyId=joint-$id")
                                "check-invitations" ->
                                    redirect("/landlord/property-details/$id/invite-joint-landlord/confirmation?journeyId=joint-$id")
                                else -> code = 404
                            }
                        }
                    } else {
                        val htmlStep =
                            if (scenario == "wrong-action" && step == "has-joint-landlords") {
                                """<form method="post" action="/outside"><input type="hidden" name="_csrf" value="joint-csrf"></form>"""
                            } else {
                                when (step) {
                                    "has-joint-landlords" -> form("formData.hasJointLandlords", includeCsrf = scenario != "missing-csrf")
                                    "invite-joint-landlord" -> form("formData.emailAddress", includeCsrf = scenario != "missing-csrf")
                                    "check-joint-landlords", "check-invitations" ->
                                        form(null, includeCsrf = scenario != "missing-csrf")
                                    "confirmation" ->
                                        """<main><h1>Joint landlord invitations sent</h1>""" +
                                            """<a href="/landlord/property-details/$id#landlord-details">Go back</a></main>"""
                                    else -> {
                                        code = 404
                                        ""
                                    }
                                }
                            }
                        body = htmlStep.replace("</form>", """<p>${emails[id].orEmpty()}</p></form>""")
                    }
                } else {
                    code = 404
                }

                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                if (exchange.requestHeaders.getFirst("Cookie") == null) {
                    exchange.responseHeaders.add("Set-Cookie", "SESSION=joint-session; Path=/")
                }
                location?.let { exchange.responseHeaders.add("Location", it) }
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        private fun form(
            field: String?,
            includeCsrf: Boolean,
        ): String {
            val hidden = if (includeCsrf) """<input type="hidden" name="_csrf" value="joint-csrf">""" else ""
            val input =
                when (field) {
                    "formData.hasJointLandlords" -> """<input type="radio" name="$field" value="true">"""
                    null -> ""
                    else -> """<input name="$field" value="">"""
                }
            return """<main><h1>Joint landlord</h1><form method="post" action="">
                $hidden<input type="hidden" name="version" value="joint-version">$input</form></main>"""
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

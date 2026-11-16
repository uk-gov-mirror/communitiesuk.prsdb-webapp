package uk.gov.communities.prsdb.webapp.performance

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrganisationRegistrationHttpContractTests {
    @TempDir
    lateinit var outputDirectory: Path

    @Test
    fun `company registers with its Companies House number and a main contact`() {
        val server = runSuccessfully("company")
        assertEquals(listOf(listOf("COMPANY"), listOf("COMPANY")), server.saved.map { it.getValue("types") })
        assertTrue(server.saved.all { it.getValue("companyNumber").single().matches(Regex("PR\\d{6}")) })
        assertTrue(server.saved.none { "members" in it || "leadTrustee" in it || "charityNumber" in it })
        assertEquals(listOf("false", "false"), server.saved.map { it.getValue("charity").single() })
    }

    @Test
    fun `organisation registration fails when check answers does not create the organisation`() {
        RegistrationServer(save = false).use { server ->
            val result = runSimulation(server.baseUrl, "company")
            assertEquals(2, result.exitCode, result.output)
        }
    }

    private fun runSuccessfully(variant: String): RegistrationServer =
        RegistrationServer().use { server ->
            val result = runSimulation(server.baseUrl, variant)
            assertEquals(0, result.exitCode, result.output)
            assertTrue(server.posts.filter { it.path.startsWith(REGISTRATION) }.all { it.first("_csrf") == "registration-csrf" })
            assertEquals(
                listOf("contract-org-$variant-warm-up", "contract-org-$variant-1"),
                server.saved.map { it.getValue("subject").single() },
            )
            assertEquals(server.saved.map { it.getValue("subject").single() }, server.identitySubjects.toList())
            assertEquals(2, server.reauthentications.get())
            server
        }

    private fun runSimulation(
        baseUrl: String,
        variant: String,
    ) = GatlingTestRunner(outputDirectory).run(
        OrganisationRegistrationSimulation::class.java,
        mapOf(
            "gatling.organisation.registrationVariant" to variant,
            "gatling.organisation.registrationSubjectPrefix" to "contract-org",
            "gatling.target" to "local",
            "gatling.baseUrl" to baseUrl,
            "gatling.simulatorUrl" to baseUrl,
            "gatling.basic.landlordSubject" to "synthetic-landlord",
            "gatling.basic.mode" to "baseline",
            "gatling.basic.iterations" to "1",
        ),
    )

    private data class Post(
        val path: String,
        val fields: List<Pair<String, String>>,
    ) {
        fun first(name: String) = fields.firstOrNull { it.first == name }?.second
    }

    // Simulates One Login, identity verification and the organisation branch of the real registration journey.
    private class RegistrationServer(
        private val save: Boolean = true,
    ) : AutoCloseable {
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val posts = ConcurrentLinkedQueue<Post>()
        val saved = mutableListOf<Map<String, List<String>>>()
        val identitySubjects = ConcurrentLinkedQueue<String>()
        val reauthentications = java.util.concurrent.atomic.AtomicInteger()
        private val selected = "1 PRSDB Performance Square, EG1 2AA"
        private var subject: String? = null
        private var answers = mutableMapOf<String, MutableList<String>>()
        private var registeredNumber: String? = null
        private var awaitingReauthentication = false

        init {
            server.createContext("/") { exchange ->
                val response = Response()
                if (exchange.requestMethod == "POST") {
                    val post = Post(exchange.requestURI.path, decode(exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8)))
                    posts.add(post)
                    handlePost(exchange, post, response)
                } else {
                    handleGet(exchange, response)
                }
                exchange.responseHeaders.add("Content-Type", "text/html")
                response.location?.let { exchange.responseHeaders.add("Location", it) }
                val bytes = response.body.toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(response.code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
        }

        private fun handleGet(
            exchange: HttpExchange,
            response: Response,
        ) {
            val path = exchange.requestURI.path
            val query = exchange.requestURI.rawQuery
            when {
                path == "/oauth2/authorization/one-login" -> response.redirect("$baseUrl/authorize-login")
                path == "/authorize-login" ->
                    response.body =
                        """<form method="post" action="/simulator/login">
                        <input type="hidden" name="authCode" value="code"><input type="hidden" name="authRequestParams" value="params">
                        <input type="hidden" name="state" value="state"><input type="hidden" name="email" value="login@example.com">
                        <input type="hidden" name="phoneNumber" value="07700900000"><input type="hidden" name="maxLoCAchieved" value="P0">
                        <textarea name="authRequestClaims">{}</textarea></form>"""
                path == "/login/oauth2/code/one-login" && query == "code=login" -> {
                    if (awaitingReauthentication) {
                        awaitingReauthentication = false
                        reauthentications.incrementAndGet()
                    }
                    response.redirect("/")
                }
                path == "/id-verification/oauth2/authorize/one-login" ->
                    response.redirect(
                        "$baseUrl/authorize?vtr=${encode("""["Cl.Cm.P2"]""")}&claims=" +
                            encode(
                                """{"userinfo":{"https://vocab.account.gov.uk/v1/coreIdentityJWT":null,""" +
                                    """"https://vocab.account.gov.uk/v1/address":null,""" +
                                    """"https://vocab.account.gov.uk/v1/returnCode":null}}""",
                            ),
                    )
                path == "/authorize" ->
                    response.body =
                        """<form method="post" action="/simulator/identity"><input type="hidden" name="state" value="state">
                        <input data-testid="sub" name="sub"><textarea data-testid="core-identity-vc" name="coreIdentity"></textarea>
                        <textarea data-testid="postal-address-details" name="address">[]</textarea>
                        <textarea data-testid="return-codes" name="returnCodes">[]</textarea></form>"""
                path == "/login/oauth2/code/one-login" && query == "code=identity" ->
                    response.redirect(
                        "$REGISTRATION/resume?journeyId=reg",
                    )
                path == "$REGISTRATION/start" -> {
                    answers = mutableMapOf()
                    registeredNumber = null
                    awaitingReauthentication = false
                    response.body = """<a class="govuk-button" href="$REGISTRATION/privacy-notice">Start now</a>"""
                }
                path == "$REGISTRATION/privacy-notice" && query == null -> response.redirect("$path?journeyId=reg")
                path == "$REGISTRATION/verify-identity" -> response.redirect("/id-verification/oauth2/authorize/one-login")
                path == "$REGISTRATION/resume" -> response.redirect("$REGISTRATION/confirm-identity?journeyId=reg")
                path == CONFIRMATION ->
                    if (registeredNumber == null) {
                        response.code = 404
                    } else if (awaitingReauthentication) {
                        response.redirect("/oauth2/authorization/one-login")
                    } else {
                        response.body =
                            """<div class="govuk-panel__body">Your number <strong>$registeredNumber</strong></div>""" +
                            """<a href="/landlord/dashboard">Dashboard</a>"""
                    }
                path == "/landlord/dashboard" -> response.body = "<h1>Dashboard</h1>"
                path == "/landlord/landlord-details" ->
                    response.body =
                        """<dl><div class="govuk-summary-list__row"><dt class="govuk-summary-list__key">Landlord Registration Number</dt>
                        <dd class="govuk-summary-list__value">$registeredNumber</dd></div></dl>""" +
                        (saved.lastOrNull()?.values?.flatten()?.joinToString("") { "<p>$it</p>" } ?: "")
                path.startsWith("$REGISTRATION/") && query == "journeyId=reg" -> response.body = page(path)
                else -> response.code = 404
            }
        }

        private fun page(path: String): String {
            val inputs =
                when (val step = path.removePrefix("$REGISTRATION/")) {
                    "privacy-notice" -> """<input name="agreesToPrivacyNotice" type="checkbox" value="true">"""
                    "confirm-identity", "organisation-governing-body-member-list" ->
                        answers["members"].orEmpty().joinToString(
                            "",
                        ) { "<p>$it</p>" }
                    "email", "organisation-email", "lead-trustee-email" -> """<input name="emailAddress">"""
                    "phone-number", "organisation-phone-number", "lead-trustee-phone" -> """<input name="phoneNumber">"""
                    "landlord-type" ->
                        """<input type="radio" name="landlordType" value="INDIVIDUAL">""" +
                            """<input type="radio" name="landlordType" value="ORGANISATION">"""
                    "organisation-name" -> """<input name="orgName">"""
                    "organisation-type" ->
                        listOf(0 to "COMPANY", 1 to "CHARITY", 2 to "TRUST", 4 to "NONE").joinToString("") { (index, value) ->
                            """<input type="checkbox" name="orgTypes[$index]" value="$value">""" +
                                """<input type="hidden" name="_orgTypes[$index]" value="on">"""
                        }
                    "lead-trustee-name", "organisation-governing-body-member-name" -> """<input name="name">"""
                    "lead-trustee-dob", "organisation-governing-body-member-dob" ->
                        """<input name="day"><input name="month"><input name="year">"""
                    "organisation-charity" ->
                        """<input type="radio" name="charity" value="true"><input type="radio" name="charity" value="false">"""
                    "organisation-charity-registered-with" ->
                        listOf("ENGLAND_AND_WALES", "SCOTLAND", "NORTHERN_IRELAND", "NONE").joinToString("") {
                            """<input type="radio" name="charityRegisteredWith" value="$it">"""
                        }
                    "organisation-charity-number" -> """<input name="charityNumber">"""
                    "organisation-companies-house" ->
                        """<input type="radio" name="companiesHouse" value="true">""" +
                            """<input type="radio" name="companiesHouse" value="false">"""
                    "organisation-company-number" -> """<input name="companyNumber">"""
                    "organisation-governing-body-details" ->
                        """<button name="orgGovBodyDetailsMode" value="HAS_DETAILS">Continue</button>""" +
                            """<button name="orgGovBodyDetailsMode" value="NO_DETAILS">No</button>"""
                    "organisation-governing-body-who-to-provide" ->
                        listOf(
                            "DIRECTOR",
                            "TRUSTEE",
                            "PARTNER",
                            "OTHER",
                        ).joinToString("") { """<input type="radio" name="whoToProvide" value="$it">""" }
                    "organisation-main-contact" -> """<input name="name"><input name="emailAddress"><input name="phoneNumber">"""
                    "check-answers" -> answers.values.flatten().joinToString("") { "<p>$it</p>" }
                    else ->
                        when {
                            step.endsWith("/lookup-address") -> """<input name="postcode"><input name="houseNameOrNumber">"""
                            step.endsWith("/select-address") ->
                                """<input type="radio" name="address" value="$selected">""" +
                                    """<input type="radio" name="address" value="MANUAL">"""
                            else -> return "<h1>Unknown step</h1>"
                        }
                }
            return """<form method="post" action="$path?journeyId=reg">""" +
                """<input type="hidden" name="_csrf" value="registration-csrf">$inputs</form>"""
        }

        private fun handlePost(
            exchange: HttpExchange,
            post: Post,
            response: Response,
        ) {
            when (post.path) {
                "/simulator/login" -> {
                    subject = post.first("sub")
                    response.redirect("/login/oauth2/code/one-login?code=login")
                    return
                }
                "/simulator/identity" -> {
                    post.first("sub")?.let { identitySubjects.add(it) }
                    response.redirect("/login/oauth2/code/one-login?code=identity")
                    return
                }
            }
            if (post.first("_csrf") != "registration-csrf" || exchange.requestURI.rawQuery != "journeyId=reg") {
                response.code = 403
                return
            }
            val step = post.path.removePrefix("$REGISTRATION/")

            fun record(
                key: String,
                field: String,
            ) {
                answers[key] = mutableListOf(requireNotNull(post.first(field)) { "$field missing" })
            }

            val next =
                when (step) {
                    "privacy-notice" -> "verify-identity".takeIf { post.first("agreesToPrivacyNotice") == "true" }
                    "confirm-identity" -> "email"
                    "email" -> "phone-number"
                    "phone-number" -> "landlord-type"
                    "landlord-type" -> "organisation-name".takeIf { post.first("landlordType") == "ORGANISATION" }
                    "organisation-name" -> "organisation-address/lookup-address".also { record("name", "orgName") }
                    "organisation-email" -> "organisation-phone-number".also { record("email", "emailAddress") }
                    "organisation-phone-number" -> "organisation-type"
                    "organisation-type" -> {
                        val types = post.fields.filter { it.first.matches(Regex("""orgTypes\[\d+]""")) }.map { it.second }
                        answers["types"] = types.toMutableList()
                        if ("TRUST" in types) "lead-trustee-name" else "organisation-charity"
                    }
                    "lead-trustee-name" -> "lead-trustee-dob".also { record("leadTrustee", "name") }
                    "lead-trustee-dob" -> "lead-trustee-email"
                    "lead-trustee-email" -> "lead-trustee-phone".also { record("leadTrusteeEmail", "emailAddress") }
                    "lead-trustee-phone" -> "lead-trustee-address/lookup-address"
                    "organisation-charity" -> {
                        record("charity", "charity")
                        if (post.first("charity") == "true") "organisation-charity-registered-with" else "organisation-companies-house"
                    }
                    "organisation-charity-registered-with" -> "organisation-charity-number"
                    "organisation-charity-number" -> "organisation-companies-house".also { record("charityNumber", "charityNumber") }
                    "organisation-companies-house" ->
                        if (post.first("companiesHouse") == "true") "organisation-company-number" else "organisation-governing-body-details"
                    "organisation-company-number" -> "organisation-main-contact".also { record("companyNumber", "companyNumber") }
                    "organisation-governing-body-details" ->
                        "organisation-governing-body-who-to-provide".takeIf { post.first("orgGovBodyDetailsMode") == "HAS_DETAILS" }
                    "organisation-governing-body-who-to-provide" -> "organisation-governing-body-member-name"
                    "organisation-governing-body-member-name" ->
                        "organisation-governing-body-member-dob".also {
                            answers.getOrPut(
                                "members",
                            ) { mutableListOf() }.add(post.first("name")!!)
                        }
                    "organisation-governing-body-member-dob" -> "organisation-governing-body-member-address/lookup-address"
                    "organisation-governing-body-member-list" -> "organisation-main-contact"
                    "organisation-main-contact" ->
                        "check-answers".also {
                            record("mainContact", "name")
                            record("mainContactEmail", "emailAddress")
                        }
                    "check-answers" -> {
                        awaitingReauthentication = true
                        if (save) {
                            registeredNumber = "L-CCCC-CCC${saved.size + 2}"
                            saved.add(answers.mapValues { it.value.toList() } + ("subject" to listOf(subject!!)))
                        } else {
                            registeredNumber = "L-CCCC-CCCX"
                            saved.add(mapOf("subject" to listOf(subject!!)))
                        }
                        response.redirect(CONFIRMATION)
                        return
                    }
                    else ->
                        when {
                            step.endsWith("/lookup-address") && post.first("postcode") == "EG1 2AA" ->
                                step.replace(
                                    "lookup-address",
                                    "select-address",
                                )
                            step.endsWith("/select-address") && post.first("address") == selected ->
                                when (step.substringBefore('/')) {
                                    "organisation-address" -> "organisation-email"
                                    "lead-trustee-address" -> "organisation-charity"
                                    else -> "organisation-governing-body-member-list"
                                }
                            else -> null
                        }
                }
            if (next == null) {
                response.code = 400
            } else {
                response.redirect("$REGISTRATION/$next?journeyId=reg")
            }
        }

        private fun decode(body: String) =
            body.split("&").filter { it.isNotEmpty() }.map {
                val parts = it.split("=", limit = 2)
                URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8)
            }

        private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)

        override fun close() = server.stop(0)

        class Response {
            var code = 200
            var body = ""
            var location: String? = null

            fun redirect(target: String) {
                code = 302
                location = target
            }
        }
    }

    companion object {
        private const val REGISTRATION = "/landlord/register-as-a-landlord"
        private const val CONFIRMATION = "$REGISTRATION/confirmation"
    }
}

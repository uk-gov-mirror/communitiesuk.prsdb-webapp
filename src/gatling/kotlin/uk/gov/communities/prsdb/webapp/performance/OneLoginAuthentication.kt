package uk.gov.communities.prsdb.webapp.performance

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.controllers.LandlordDetailsController
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object OneLoginAuthentication {
    const val START_REQUEST = "One Login: start"
    const val CALLBACK_REQUEST = "One Login: callback"
    const val DETAILS_REQUEST = "Landlord: details"
    val applicationRequestNames = listOf(START_REQUEST, CALLBACK_REQUEST, DETAILS_REQUEST)
    const val IDENTITY_VERIFICATION_START_REQUEST = "Identity verification: start"
    const val IDENTITY_VERIFICATION_AUTHORIZE_REQUEST = "Identity verification: authorize"
    const val IDENTITY_VERIFICATION_CALLBACK_REQUEST = "Identity verification: callback"
    const val IDENTITY_VERIFICATION_RESUME_REQUEST = "Identity verification: resume"
    val identityVerificationRequestNames =
        listOf(
            IDENTITY_VERIFICATION_START_REQUEST,
            IDENTITY_VERIFICATION_AUTHORIZE_REQUEST,
            IDENTITY_VERIFICATION_CALLBACK_REQUEST,
            IDENTITY_VERIFICATION_RESUME_REQUEST,
        )

    fun loginRequestNames(requestNamePrefix: String): List<String> = listOf("$requestNamePrefix: start", "$requestNamePrefix: callback")

    fun authorizationUrlKey(requestNamePrefix: String): String = "${requestNamePrefix.replace(' ', '-')}-authorize-url"

    fun chain(
        config: BasicRunConfig,
        iterationData: BasicIterationData,
    ): ChainBuilder =
        authenticationChain(
            config,
            config.landlordSubject,
            "One Login",
            requireLandlordDetails = true,
            requestLabelPrefix = iterationData.requestNamePrefix,
        )

    fun loginChain(
        config: BasicRunConfig,
        subject: String = config.landlordSubject,
        requestNamePrefix: String = "One Login",
        requestLabelPrefix: String = "",
    ): ChainBuilder =
        authenticationChain(
            config,
            subject,
            requestNamePrefix,
            requireLandlordDetails = false,
            requestLabelPrefix = requestLabelPrefix,
        )

    private fun authenticationChain(
        config: BasicRunConfig,
        subject: String,
        requestNamePrefix: String,
        requireLandlordDetails: Boolean,
        requestLabelPrefix: String,
    ): ChainBuilder {
        val loginRequestNames = loginRequestNames(requestNamePrefix)
        val authorizeUrlKey = authorizationUrlKey(requestNamePrefix)
        var chain =
            exec(
                http("$requestLabelPrefix${loginRequestNames[0]}")
                    .get { _ -> HttpDestination.withCacheBuster("${config.baseUrl}/oauth2/authorization/one-login") }
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { HttpDestination.resolve(it, config.simulatorUrl) }
                            .saveAs(authorizeUrlKey),
                    ),
            ).exitHereIfFailed()
                .exec(authenticationCompletionChain(config, subject, requestNamePrefix, authorizeUrlKey, requestLabelPrefix))

        if (requireLandlordDetails) {
            chain = chain.exec(landlordDetailsChain(config, requestLabelPrefix))
        }
        return chain
    }

    fun landlordDetailsChain(
        config: BasicRunConfig,
        prefix: String = "",
    ): ChainBuilder =
        exec(
            http("$prefix$DETAILS_REQUEST")
                .get(LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE)
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }.`is`("text/html"))
                .check(
                    css("a[href*='/landlord-details/update-phone-number']", "href")
                        .transform { HttpDestination.resolve(it, config.baseUrl) }
                        .saveAs("phoneUpdateEntry"),
                ),
        ).exitHereIfFailed()

    fun authenticationCompletionChain(
        config: BasicRunConfig,
        subject: String,
        requestNamePrefix: String,
        authorizeUrlKey: String = authorizationUrlKey(requestNamePrefix),
        requestLabelPrefix: String = "",
    ): ChainBuilder {
        val callbackUrlKey = "${requestNamePrefix.replace(' ', '-')}-callback-url"
        val formActionKey = "${requestNamePrefix.replace(' ', '-')}-form-action"
        val hiddenNamesKey = "${requestNamePrefix.replace(' ', '-')}-hidden-names"
        val hiddenValuesKey = "${requestNamePrefix.replace(' ', '-')}-hidden-values"
        val textareaNamesKey = "${requestNamePrefix.replace(' ', '-')}-textarea-names"
        val textareaValuesKey = "${requestNamePrefix.replace(' ', '-')}-textarea-values"

        return exec(
            http("One Login simulator: authorize")
                .get("#{$authorizeUrlKey}")
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }.`is`("text/html"))
                .check(
                    css("form[method=post]", "action")
                        .transform { HttpDestination.resolve(it, config.simulatorUrl) }
                        .saveAs(formActionKey),
                )
                .check(css("form input[type=hidden]", "name").findAll().saveAs(hiddenNamesKey))
                .check(css("form input[type=hidden]", "value").findAll().saveAs(hiddenValuesKey))
                .check(css("form textarea", "name").findAll().saveAs(textareaNamesKey))
                .check(css("form textarea").findAll().saveAs(textareaValuesKey))
                .check(css("input[name=authCode]", "value").exists())
                .check(css("input[name=authRequestParams]", "value").exists())
                .check(css("input[name=state]", "value").exists())
                .check(css("input[name=email]", "value").saveAs("loginEmail"))
                .check(css("input[name=phoneNumber]", "value").saveAs("loginPhone"))
                .check(css("input[name=maxLoCAchieved]", "value").saveAs("loginConfidence")),
        ).exitHereIfFailed()
            .exec(
                http("One Login simulator: submit")
                    .post("#{$formActionKey}")
                    .disableFollowRedirect()
                    .formParamMap { session ->
                        val names = session.getList<String>(hiddenNamesKey)
                        val values = session.getList<String>(hiddenValuesKey)
                        val textareaNames = session.getList<String>(textareaNamesKey)
                        val textareaValues = session.getList<String>(textareaValuesKey)
                        require(names.size == values.size) { "Simulator hidden form fields have missing values" }
                        require(textareaNames.size == textareaValues.size) { "Simulator textarea fields have missing values" }
                        names.zip(values).toMap() + textareaNames.zip(textareaValues).toMap() +
                            mapOf(
                                "sub" to subject,
                                "email" to session.getString("loginEmail"),
                                "emailVerified" to "true",
                                "phoneNumber" to session.getString("loginPhone"),
                                "phoneNumberVerified" to "true",
                                "maxLoCAchieved" to session.getString("loginConfidence"),
                                "continue" to "continue",
                            )
                    }.check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { HttpDestination.resolve(it, config.baseUrl) }
                            .saveAs(callbackUrlKey),
                    ),
            ).exitHereIfFailed()
            .exec(
                http("$requestLabelPrefix${loginRequestNames(requestNamePrefix)[1]}")
                    .get("#{$callbackUrlKey}")
                    .disableFollowRedirect()
                    .check(status().`is`(302)),
            ).exitHereIfFailed()
    }

    fun identityVerificationChain(
        config: BasicRunConfig,
        startUrl: String,
        fixture: IdentityVerificationFixture = IdentityVerificationFixture.synthetic(),
        identitySubject: String = config.landlordSubject,
        requestLabelPrefix: String = "",
    ): ChainBuilder =
        exec(
            http("$requestLabelPrefix$IDENTITY_VERIFICATION_START_REQUEST")
                .get(startUrl)
                .disableFollowRedirect()
                .check(status().`is`(302))
                .check(
                    header("Location")
                        .transform { HttpDestination.resolve(it, config.baseUrl) }
                        .saveAs("identityAuthorizationEntryUrl"),
                ),
        ).exitHereIfFailed()
            .exec(
                http("$requestLabelPrefix$IDENTITY_VERIFICATION_AUTHORIZE_REQUEST")
                    .get("#{identityAuthorizationEntryUrl}")
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform {
                                authorizeUrlWithRequiredIdentityClaims(
                                    it,
                                    config.simulatorUrl,
                                    fixture.includeCoreIdentityClaim,
                                )
                            }
                            .saveAs("identitySimulatorAuthorizeUrl"),
                    ),
            ).exitHereIfFailed()
            .exec(
                http("One Login simulator: identity authorize")
                    .get("#{identitySimulatorAuthorizeUrl}")
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }.`is`("text/html"))
                    .check(
                        css("form[method=post]", "action")
                            .transform { HttpDestination.resolve(it, config.simulatorUrl) }
                            .saveAs("identityFormAction"),
                    )
                    .check(css("form input[type=hidden]", "name").findAll().saveAs("identityHiddenNames"))
                    .check(css("form input[type=hidden]", "value").findAll().saveAs("identityHiddenValues"))
                    .check(css("form textarea", "name").findAll().saveAs("identityTextareaNames"))
                    .check(css("form textarea").findAll().saveAs("identityTextareaValues"))
                    .check(css("input[data-testid='sub']", "name").saveAs("identitySubjectField"))
                    .check(css("textarea[data-testid='core-identity-vc']", "name").saveAs("identityCoreField"))
                    .check(css("textarea[data-testid='postal-address-details']", "name").saveAs("identityAddressField"))
                    .check(css("textarea[data-testid='return-codes']", "name").saveAs("identityReturnCodesField")),
            ).exitHereIfFailed()
            .exec(
                http("One Login simulator: submit identity")
                    .post("#{identityFormAction}")
                    .disableFollowRedirect()
                    .formParamMap { session ->
                        val hiddenNames = session.getList<String>("identityHiddenNames")
                        val hiddenValues = session.getList<String>("identityHiddenValues")
                        val textareaNames = session.getList<String>("identityTextareaNames")
                        val textareaValues = session.getList<String>("identityTextareaValues")
                        require(hiddenNames.size == hiddenValues.size) {
                            "Simulator identity form has hidden fields with missing values"
                        }
                        require(textareaNames.size == textareaValues.size) {
                            "Simulator identity form has textareas with missing values"
                        }
                        hiddenNames.zip(hiddenValues).toMap() +
                            textareaNames.zip(textareaValues).toMap() +
                            mapOf(
                                session.getString("identitySubjectField") to identitySubject,
                                session.getString("identityCoreField") to fixture.coreIdentity,
                                session.getString("identityAddressField") to fixture.address,
                                session.getString("identityReturnCodesField") to fixture.returnCodes,
                                "continue" to "continue",
                            )
                    }.check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { HttpDestination.resolve(it, config.baseUrl) }
                            .saveAs("identityCallbackUrl"),
                    ),
            ).exitHereIfFailed()
            .exec(
                http("$requestLabelPrefix$IDENTITY_VERIFICATION_CALLBACK_REQUEST")
                    .get("#{identityCallbackUrl}")
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { HttpDestination.resolve(it, config.baseUrl) }
                            .saveAs("identityVerificationResumeUrl"),
                    ),
            ).exitHereIfFailed()
            .exec(
                http("$requestLabelPrefix$IDENTITY_VERIFICATION_RESUME_REQUEST")
                    .get("#{identityVerificationResumeUrl}")
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location").transform { location ->
                            val resolved = HttpDestination.resolve(location, config.baseUrl)
                            val path = URI.create(resolved).path
                            val expectedStep =
                                if (fixture.includeCoreIdentityClaim) "confirm-identity" else "identity-not-verified"
                            require(path.endsWith("/$expectedStep")) {
                                "Identity verification resumed at an unexpected registration step"
                            }
                            resolved
                        }.saveAs("identityVerificationNextUrl"),
                    ),
            ).exitHereIfFailed()

    private fun authorizeUrlWithRequiredIdentityClaims(
        location: String,
        simulatorOrigin: String,
        includeCoreIdentityClaim: Boolean,
    ): String {
        val resolved = HttpDestination.resolve(location, simulatorOrigin)
        val authorizeUri = URI.create(resolved)
        val parameters =
            authorizeUri.rawQuery.orEmpty().split("&").associate { part ->
                val pieces = part.split("=", limit = 2)
                URLDecoder.decode(pieces[0], StandardCharsets.UTF_8) to
                    URLDecoder.decode(pieces.getOrElse(1) { "" }, StandardCharsets.UTF_8)
            }
        require(parameters["vtr"] == """["Cl.Cm.P2"]""") {
            "Identity verification authorization request is missing the required VTR"
        }
        val claims = parameters["claims"] ?: error("Identity verification authorization request is missing claims")
        require(
            listOf(
                ADDRESS_CLAIM,
                RETURN_CODE_CLAIM,
            ).plus(if (includeCoreIdentityClaim) listOf(CORE_IDENTITY_CLAIM) else emptyList())
                .all { claims.contains("\"$it\"") },
        ) { "Identity verification authorization request is missing required identity claims" }
        if (includeCoreIdentityClaim) return resolved

        val claimsObject =
            ObjectMapper().readTree(claims) as? ObjectNode
                ?: error("Identity verification authorization claims must be a JSON object")
        val userInfo =
            claimsObject.get("userinfo") as? ObjectNode
                ?: error("Identity verification authorization claims must contain userinfo")
        userInfo.remove(CORE_IDENTITY_CLAIM)
        val query =
            parameters
                .plus("claims" to claimsObject.toString())
                .map { (key, value) ->
                    "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
                }.joinToString("&")
        val rewrittenUrl =
            "${authorizeUri.scheme}://${authorizeUri.rawAuthority}${authorizeUri.rawPath}?$query"
        return HttpDestination.resolve(rewrittenUrl, simulatorOrigin)
    }

    private const val CORE_IDENTITY_CLAIM = "https://vocab.account.gov.uk/v1/coreIdentityJWT"
    private const val ADDRESS_CLAIM = "https://vocab.account.gov.uk/v1/address"
    private const val RETURN_CODE_CLAIM = "https://vocab.account.gov.uk/v1/returnCode"
}

data class IdentityVerificationFixture(
    val coreIdentity: String,
    val address: String,
    val returnCodes: String,
    val includeCoreIdentityClaim: Boolean = true,
) {
    companion object {
        fun synthetic(includeCoreIdentity: Boolean = true): IdentityVerificationFixture =
            IdentityVerificationFixture(
                coreIdentity = loadFixture("core-identity.json"),
                address = loadFixture("address.json"),
                returnCodes = loadFixture("return-codes.json"),
                includeCoreIdentityClaim = includeCoreIdentity,
            )

        private fun loadFixture(name: String): String =
            checkNotNull(IdentityVerificationFixture::class.java.getResourceAsStream("/one-login-simulator/$name")) {
                "Missing synthetic identity verification fixture: one-login-simulator/$name"
            }.bufferedReader().use { it.readText() }
    }
}

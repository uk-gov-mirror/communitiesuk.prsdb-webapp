package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CheckBuilder
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.controllers.LandlordDetailsController
import uk.gov.communities.prsdb.webapp.controllers.RegisterLandlordController
import java.net.URI
import java.util.AbstractMap.SimpleEntry

// Initial organisation landlord registration for company, charitable trust and unincorporated (other) organisations.
object OrganisationRegistrationJourney {
    const val AUTHENTICATION_PREFIX = "Organisation registration authentication"
    const val CONFIRMATION_AUTHENTICATION_PREFIX = "Organisation registration confirmation authentication"
    val variants = listOf("company")

    private const val REGISTRATION = RegisterLandlordController.LANDLORD_REGISTRATION_ROUTE
    private const val START = RegisterLandlordController.LANDLORD_REGISTRATION_START_PAGE_ROUTE
    private const val CONFIRMATION = RegisterLandlordController.LANDLORD_REGISTRATION_CONFIRMATION_ROUTE
    private const val DETAILS = LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE
    private const val SELECTED_ADDRESS = "1 PRSDB Performance Square, EG1 2AA"
    private const val NEXT = "orgRegNext"
    private const val PAGE = "orgRegPage"
    private const val ACTION = "orgRegAction"
    private const val HIDDEN_NAMES = "orgRegHiddenNames"
    private const val HIDDEN_VALUES = "orgRegHiddenValues"
    private const val CHECKBOX_NAMES = "orgRegCheckboxNames"
    private const val CHECKBOX_VALUES = "orgRegCheckboxValues"
    private const val REGISTRATION_NUMBER = "orgRegRegistrationNumber"
    private const val DASHBOARD = "orgRegDashboard"
    private const val AUTHORIZATION_ENTRY = "orgRegAuthorizationEntry"

    data class Values(
        val subject: String,
        val variant: String,
    ) {
        private val token = subject.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        private val number = (subject.hashCode().toLong() and 0x7fffffff) % 1_000_000

        val registrantEmail = "org-registrant-$token@example.invalid"
        val registrantPhone = "02079460131"
        val organisationName = "Performance Organisation $token"
        val organisationEmail = "organisation-$token@example.invalid"
        val organisationPhone = "02079460132"
        val types = listOf("COMPANY")
        val companyNumber = "PR" + String.format("%06d", number)
        val mainContactName = "Performance Main Contact $token"
        val mainContactEmail = "org-main-contact-$token@example.invalid"
        val mainContactPhone = "02079460134"

        // Values that must appear on both the check answers page and the saved landlord details page.
        val expectedAnswers: List<String> =
            listOf(organisationName, organisationEmail, mainContactName, mainContactEmail, companyNumber)

        init {
            require(variant in variants) { "Unknown organisation registration variant: $variant" }
        }
    }

    private class Step(
        val name: String,
        val fields: List<String>,
        val nextPath: String?,
        val extraChecks: List<CheckBuilder> = emptyList(),
        val values: (Session) -> List<Pair<String, String>> = { emptyList() },
    )

    fun requestNames(variant: String): List<String> {
        val values = Values("request-names", variant)
        return listOf(label(variant, "start page"), label(variant, "initialize privacy journey")) +
            formSteps(values, prePrivacy = true).flatMap { listOf("${it.name} form", it.name) } +
            OneLoginAuthentication.identityVerificationRequestNames.map { label(variant, it) } +
            formSteps(values, prePrivacy = false).flatMap { listOf("${it.name} form", it.name) } +
            listOf(
                label(variant, "complete journey"),
                label(variant, "refresh authentication"),
                label(variant, OneLoginAuthentication.loginRequestNames(CONFIRMATION_AUTHENTICATION_PREFIX).last()),
                label(variant, "confirmation"),
                label(variant, "dashboard"),
                label(variant, "saved organisation details"),
            )
    }

    fun journey(
        config: BasicRunConfig,
        subject: String,
        variant: String,
        prefix: String,
    ): ChainBuilder {
        val values = Values(subject, variant)
        val labelPrefix = "$prefix${label(variant, "")}"
        return exec { it.remove(NEXT).remove(REGISTRATION_NUMBER) }
            .exec(
                http(prefix + label(variant, "start page"))
                    .get(START)
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("a.govuk-button", "href").transform { HttpDestination.resolve(it, config.baseUrl) }.saveAs(NEXT)),
            ).exitHereIfFailed()
            .exec(
                http(prefix + label(variant, "initialize privacy journey"))
                    .get("#{$NEXT}")
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(header("Location").transform { inRegistration(it, config) }.saveAs(NEXT)),
            ).exitHereIfFailed()
            .exec(formSteps(values, prePrivacy = true).map { formStep(config, prefix, it) })
            .exec(
                OneLoginAuthentication.identityVerificationChain(
                    config,
                    "#{$NEXT}",
                    IdentityVerificationFixture.synthetic(),
                    subject,
                    labelPrefix,
                ),
            ).exec { it.set(NEXT, it.getString("identityVerificationNextUrl")) }
            .exec(formSteps(values, prePrivacy = false).map { formStep(config, prefix, it) })
            .exec(
                http(prefix + label(variant, "complete journey"))
                    .get("#{$NEXT}")
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(header("Location").transform { HttpDestination.resolve(it, config.baseUrl) }.saveAs(AUTHORIZATION_ENTRY)),
            ).exitHereIfFailed()
            .exec(
                http(prefix + label(variant, "refresh authentication"))
                    .get { session -> HttpDestination.withCacheBuster(requireNotNull(session.getString(AUTHORIZATION_ENTRY))) }
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { HttpDestination.resolve(it, config.simulatorUrl) }
                            .saveAs(OneLoginAuthentication.authorizationUrlKey(CONFIRMATION_AUTHENTICATION_PREFIX)),
                    ),
            ).exitHereIfFailed()
            .exec(
                OneLoginAuthentication.authenticationCompletionChain(
                    config,
                    subject,
                    CONFIRMATION_AUTHENTICATION_PREFIX,
                    requestLabelPrefix = labelPrefix,
                ),
            ).exec(
                http(prefix + label(variant, "confirmation"))
                    .get("#{$NEXT}")
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(
                        css(".govuk-panel__body strong")
                            .transform { number ->
                                require(number.isNotBlank()) { "Organisation registration confirmation has no registration number" }
                                normalise(number)
                            }.saveAs(REGISTRATION_NUMBER),
                    ).check(
                        css("a[href*='/landlord/dashboard']", "href")
                            .transform { HttpDestination.resolve(it, config.baseUrl) }
                            .saveAs(DASHBOARD),
                    ),
            ).exitHereIfFailed()
            .exec(
                http(prefix + label(variant, "dashboard"))
                    .get("#{$DASHBOARD}")
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("h1").exists()),
            ).exitHereIfFailed()
            .exec(
                http(prefix + label(variant, "saved organisation details"))
                    .get(DETAILS)
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(
                        css(".govuk-summary-list__key:contains('Landlord Registration Number') + .govuk-summary-list__value")
                            .find()
                            .transformWithSession { number, session -> normalise(number) == session.getString(REGISTRATION_NUMBER) }
                            .`is`(true),
                    ).check(
                        bodyString().transform { body -> values.expectedAnswers.filterNot { it in body } }.`is`(emptyList()),
                    ),
            ).exitHereIfFailed()
    }

    private fun formSteps(
        values: Values,
        prePrivacy: Boolean,
    ): List<Step> {
        val v = values.variant
        if (prePrivacy) {
            return listOf(
                Step(label(v, "accept privacy notice"), listOf("input[name=agreesToPrivacyNotice]"), null) {
                    listOf("agreesToPrivacyNotice" to "true")
                },
            )
        }
        val steps = mutableListOf<Step>()

        fun step(
            name: String,
            fields: List<String>,
            next: String?,
            extraChecks: List<CheckBuilder> = emptyList(),
            submitted: (Session) -> List<Pair<String, String>> = { emptyList() },
        ) {
            steps.add(Step(label(v, name), fields, next?.let { "$REGISTRATION/$it" } ?: CONFIRMATION, extraChecks, submitted))
        }

        fun address(
            name: String,
            route: String,
            next: String,
        ) {
            step("$name lookup", listOf("input[name=postcode]", "input[name=houseNameOrNumber]"), "$route/select-address") {
                listOf("postcode" to "EG1 2AA", "houseNameOrNumber" to "1")
            }
            step(
                "$name selection",
                listOf("input[name=address]"),
                next,
                listOf(css("form input[name=address]", "value").findAll().transform { SELECTED_ADDRESS in it }.`is`(true)),
            ) { listOf("address" to SELECTED_ADDRESS) }
        }

        step("confirm identity", listOf("input[name=_csrf]"), "email")
        step("registrant email", listOf("input[name=emailAddress]"), "phone-number") { listOf("emailAddress" to values.registrantEmail) }
        step("registrant phone", listOf("input[name=phoneNumber]"), "landlord-type") { listOf("phoneNumber" to values.registrantPhone) }
        step("landlord type", listOf("input[name=landlordType][value=ORGANISATION]"), "organisation-name") {
            listOf("landlordType" to "ORGANISATION")
        }
        step("organisation name", listOf("input[name=orgName]"), "organisation-address/lookup-address") {
            listOf("orgName" to values.organisationName)
        }
        address("organisation address", "organisation-address", "organisation-email")
        step("organisation email", listOf("input[name=emailAddress]"), "organisation-phone-number") {
            listOf("emailAddress" to values.organisationEmail)
        }
        step("organisation phone", listOf("input[name=phoneNumber]"), "organisation-type") {
            listOf("phoneNumber" to values.organisationPhone)
        }
        step(
            "organisation type",
            values.types.map { "input[type=checkbox][value=$it]" },
            "organisation-charity",
            listOf(
                css("form input[type=checkbox]", "name").findAll().saveAs(CHECKBOX_NAMES),
                css("form input[type=checkbox]", "value").findAll().saveAs(CHECKBOX_VALUES),
            ),
        ) { session -> typeFields(session, values.types) }
        step(
            "registered charity",
            listOf("input[name=charity]"),
            "organisation-companies-house",
        ) {
            listOf("charity" to "false")
        }
        step(
            "Companies House registration",
            listOf("input[name=companiesHouse]"),
            "organisation-company-number",
        ) { listOf("companiesHouse" to "true") }
        step("company number", listOf("input[name=companyNumber]"), "organisation-main-contact") {
            listOf("companyNumber" to values.companyNumber)
        }
        step(
            "main contact",
            listOf("input[name=name]", "input[name=emailAddress]", "input[name=phoneNumber]"),
            "check-answers",
        ) {
            listOf(
                "name" to values.mainContactName,
                "emailAddress" to values.mainContactEmail,
                "phoneNumber" to values.mainContactPhone,
            )
        }
        step(
            "check answers",
            emptyList(),
            null,
            listOf(bodyString().transform { body -> values.expectedAnswers.filterNot { it in body } }.`is`(emptyList())),
        )
        return steps
    }

    private fun typeFields(
        session: Session,
        types: List<String>,
    ): List<Pair<String, String>> {
        // Spring binds the checkboxes as indexed list entries, so post each target under its rendered name.
        val checkboxes = session.getList<String>(CHECKBOX_VALUES).zip(session.getList<String>(CHECKBOX_NAMES)).toMap()
        return types.map { requireNotNull(checkboxes[it]) { "Organisation type form has no $it checkbox" } to it }
    }

    private fun formStep(
        config: BasicRunConfig,
        prefix: String,
        step: Step,
    ): ChainBuilder {
        var page =
            http("$prefix${step.name} form")
                .get("#{$NEXT}")
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(css(".govuk-error-summary").notExists())
                .check(css("form[method=post]", "action").saveAs(ACTION))
                .check(css("form input[name=_csrf]", "value").exists())
                .check(css("form input[type=hidden]", "name").findAll().saveAs(HIDDEN_NAMES))
                .check(css("form input[type=hidden]", "value").findAll().saveAs(HIDDEN_VALUES))
        for (field in step.fields) page = page.check(css("form $field").exists())
        for (check in step.extraChecks) page = page.check(check)
        return exec { it.set(PAGE, it.getString(NEXT)) }
            .exec(page)
            .exitHereIfFailed()
            .pause(config.thinkTime)
            .exec(
                http("$prefix${step.name}")
                    .post { session ->
                        inRegistration(
                            HttpDestination.resolve(
                                requireNotNull(session.getString(ACTION)),
                                config.baseUrl,
                                requireNotNull(session.getString(PAGE)),
                            ),
                            config,
                        )
                    }.disableFollowRedirect()
                    .formParamSeq { session ->
                        val names = session.getList<String>(HIDDEN_NAMES)
                        val hidden = session.getList<String>(HIDDEN_VALUES)
                        require(names.size == hidden.size) { "Incomplete organisation registration hidden fields" }
                        val submitted = step.values(session)
                        val overridden = submitted.map { it.first }.toSet()
                        (names.zip(hidden).filter { it.first !in overridden } + submitted)
                            .map<Pair<String, String>, Map.Entry<String, Any>> { SimpleEntry(it.first, it.second) }
                    }.check(status().`is`(302))
                    .check(
                        header("Location").transform { location ->
                            val resolved = HttpDestination.resolve(location, config.baseUrl)
                            val path = URI.create(resolved).path
                            step.nextPath?.let {
                                require(path == it) { "Organisation registration redirected to $path instead of $it" }
                            }
                            resolved
                        }.saveAs(NEXT),
                    ),
            ).exitHereIfFailed()
    }

    private fun inRegistration(
        location: String,
        config: BasicRunConfig,
    ): String {
        val resolved = HttpDestination.resolve(location, config.baseUrl)
        val path = URI.create(resolved).path
        require(path.startsWith("$REGISTRATION/")) { "Organisation registration left its expected journey: $path" }
        return resolved
    }

    private fun normalise(value: String) = value.trim().replace(Regex("\\s+"), " ")

    private fun label(
        variant: String,
        name: String,
    ) = "Organisation registration ($variant): $name"
}

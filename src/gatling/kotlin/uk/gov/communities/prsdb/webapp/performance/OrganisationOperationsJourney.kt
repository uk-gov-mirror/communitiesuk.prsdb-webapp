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
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.controllers.AcceptOrganisationalLandlordUserInvitationController
import uk.gov.communities.prsdb.webapp.controllers.InviteOrganisationalLandlordUserController
import uk.gov.communities.prsdb.webapp.controllers.LandlordDetailsController
import uk.gov.communities.prsdb.webapp.controllers.TeamMembersController
import uk.gov.communities.prsdb.webapp.journeys.acceptOrganisationalLandlordInvitation.steps.AcceptOrganisationalLandlordInvitationCheckAnswersStep
import uk.gov.communities.prsdb.webapp.journeys.acceptOrganisationalLandlordInvitation.steps.CheckUserIsLandlordStep
import uk.gov.communities.prsdb.webapp.journeys.acceptOrganisationalLandlordInvitation.steps.EmailAddressStep
import uk.gov.communities.prsdb.webapp.journeys.acceptOrganisationalLandlordInvitation.steps.FullNameStep
import uk.gov.communities.prsdb.webapp.journeys.acceptOrganisationalLandlordInvitation.steps.JoinOrganisationStep
import uk.gov.communities.prsdb.webapp.journeys.acceptOrganisationalLandlordInvitation.steps.ValidateTokenStep
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import java.net.URI
import java.util.AbstractMap.SimpleEntry

object OrganisationOperationsJourney {
    private const val DETAILS = LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE
    private const val TEAM = TeamMembersController.TEAM_MEMBERS_ROUTE
    private const val INVITE_ROUTE = InviteOrganisationalLandlordUserController.INVITE_ORGANISATIONAL_LANDLORD_USER_ROUTE
    private const val ACCEPT = AcceptOrganisationalLandlordUserInvitationController.ACCEPT_INVITATION_ROUTE
    private const val ACCEPT_CONFIRMATION = AcceptOrganisationalLandlordUserInvitationController.ACCEPT_INVITATION_CONFIRMATION_ROUTE
    private const val ACCEPT_AUTHENTICATION = "Organisation team accept authentication"
    private const val ACCEPT_CONFIRMATION_AUTHENTICATION = "Organisation team accept confirmation authentication"
    private const val SIGN_IN = "/oauth2/authorization/one-login"
    private const val ADMINISTRATORS_TAB = TeamMembersController.ADMINISTRATORS_FRAGMENT
    private const val EDITORS_TAB = TeamMembersController.EDITORS_FRAGMENT
    internal const val PAGE = "orgOpPage"
    internal const val NEXT = "orgOpNext"
    private const val ACTION = "orgOpAction"
    private const val HIDDEN_NAMES = "orgOpHiddenNames"
    private const val HIDDEN_VALUES = "orgOpHiddenValues"
    private const val SIGN_IN_ENTRY = "orgOpSignInEntry"
    private const val MEMBER = "orgOpMember"
    private const val EDITOR_COUNT = "orgOpEditorCount"
    private const val ADMIN_COUNT = "orgOpAdminCount"

    val teamRequestNames =
        listOf("Organisation team: members") +
            listOf("start", "form", "submit", "members").map { "Organisation team invite kept: $it" }

    val acceptRequestNames =
        listOf("start", "validate token", "sign-in required").map { "Organisation team accept: $it" } +
            OneLoginAuthentication.loginRequestNames(ACCEPT_AUTHENTICATION) +
            listOf(
                "check landlord",
                "join form",
                "join submit",
                "name form",
                "name submit",
                "email form",
                "email submit",
                "check answers",
                "confirm",
                "confirmation sign-in required",
            ).map { "Organisation team accept: $it" } +
            OneLoginAuthentication.loginRequestNames(ACCEPT_CONFIRMATION_AUTHENTICATION) +
            listOf("confirmation", "saved details").map { "Organisation team accept: $it" }

    val accessLevelRequestNames =
        listOf("members", "start", "form", "submit", "saved members").map { "Organisation team access level: $it" }

    fun acceptSubject(
        runId: String,
        fixture: OrganisationFixture,
    ): String = "urn:fdc:gov.uk:2022:gatling-org-accept-$runId-${fixture.organisationId}"

    fun teamEmail(
        runId: String,
        fixture: OrganisationFixture,
        kind: String,
    ): String = "org-team-$runId-${fixture.organisationId}-$kind@example.com"

    fun team(
        config: BasicRunConfig,
        fixture: OrganisationFixture,
        runId: String,
        prefix: String,
    ): ChainBuilder {
        val names = teamRequestNames.map { prefix + it }.iterator()
        val kept = teamEmail(runId, fixture, "kept")
        return exec(
            teamPage(config, names.next())
                .check(bodyString().transform { kept !in it }.`is`(true))
                .check(css("a[href^='$INVITE_ROUTE/']", "href").saveAs(NEXT)),
        ).exitHereIfFailed()
            .exec(invite(config, names, kept, css("a[href^='$INVITE_ROUTE/']", "href").saveAs(NEXT)))
    }

    // A fresh One Login user accepts a seeded pending invitation, signing in mid-journey and again after acceptance clears the
    // security context, then proves membership by viewing the organisation's saved details.
    fun accept(
        config: BasicRunConfig,
        fixture: OrganisationFixture,
        runId: String,
        prefix: String,
    ): ChainBuilder {
        val names = acceptRequestNames.map { prefix + it }.iterator()
        val token = requireNotNull(fixture.invitationToken) { "Organisation ${fixture.organisationId} has no pending invitation token" }
        val subject = acceptSubject(runId, fixture)
        val email = teamEmail(runId, fixture, "accepted")
        val memberName = "Gatling Invitee ${fixture.organisationId}"
        val registration = RegistrationNumberDataModel(RegistrationNumberType.LANDLORD, fixture.registrationNumber).toString()
        return exec(
            http(names.next()).get(config.baseUrl + ACCEPT + "?token=" + token)
                .disableFollowRedirect()
                .check(status().`is`(302))
                .check(
                    header("Location").transform {
                        val resolved = scoped(it, config, ACCEPT)
                        require(URI.create(resolved).path == "$ACCEPT/${ValidateTokenStep.ROUTE_SEGMENT}") {
                            "Organisation invitation did not start at token validation: $resolved"
                        }
                        resolved
                    }.saveAs(PAGE),
                ),
        ).exitHereIfFailed()
            .exec(redirect(config, names.next(), ACCEPT, "$ACCEPT/${CheckUserIsLandlordStep.ROUTE_SEGMENT}"))
            .exec(signIn(config, names, subject, ACCEPT_AUTHENTICATION, prefix))
            .exec(redirect(config, names.next(), ACCEPT, "$ACCEPT/${JoinOrganisationStep.ROUTE_SEGMENT}"))
            .exec(
                page(
                    config,
                    names.next(),
                    ACCEPT,
                    emptyList(),
                    listOf(css("p.govuk-body").findAll().transform { it.any { p -> normalise(fixture.name) in normalise(p) } }.`is`(true)),
                ),
            ).exec(submit(config, names.next(), ACCEPT, "$ACCEPT/${FullNameStep.ROUTE_SEGMENT}") { emptyList() })
            .exec(page(config, names.next(), ACCEPT, listOf("name")))
            .exec(submit(config, names.next(), ACCEPT, "$ACCEPT/${EmailAddressStep.ROUTE_SEGMENT}") { listOf("name" to memberName) })
            .exec(page(config, names.next(), ACCEPT, listOf("emailAddress")))
            .exec(
                submit(config, names.next(), ACCEPT, "$ACCEPT/${AcceptOrganisationalLandlordInvitationCheckAnswersStep.ROUTE_SEGMENT}") {
                    listOf("emailAddress" to email)
                },
            ).exec(
                page(
                    config,
                    names.next(),
                    ACCEPT,
                    emptyList(),
                    listOf(bodyString().transform { memberName in it && email in it }.`is`(true)),
                ),
            ).exec(submit(config, names.next(), ACCEPT, ACCEPT_CONFIRMATION) { emptyList() })
            .exec(signIn(config, names, subject, ACCEPT_CONFIRMATION_AUTHENTICATION, prefix))
            .exec(
                http(names.next()).get { session -> requireNotNull(session.getString(PAGE)) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("#joined-organisation").transform { normalise(fixture.name) in normalise(it) }.`is`(true))
                    .check(css("#confirmation-email").transform { email in it }.`is`(true)),
            ).exitHereIfFailed()
            .exec(detailsPage(config, names.next(), registration))
            .exitHereIfFailed()
    }

    fun changeAccessLevel(
        config: BasicRunConfig,
        prefix: String,
    ): ChainBuilder {
        val names = accessLevelRequestNames.map { prefix + it }.iterator()
        return exec(memberTeamPage(config, names.next()))
            .exitHereIfFailed()
            .exec(start(config, names.next(), TEAM))
            .exec(page(config, names.next(), TEAM, listOf("role"), listOf(css("form input[name=role][value=ADMIN]").exists())))
            .exec(submit(config, names.next(), TEAM, TEAM) { listOf("role" to "ADMIN") })
            .exec(updatedTeamPage(config, names.next(), "Access level changed", " is now an administrator.", 1))
            .exitHereIfFailed()
    }

    private fun signIn(
        config: BasicRunConfig,
        names: Iterator<String>,
        subject: String,
        authenticationPrefix: String,
        labelPrefix: String,
    ): ChainBuilder {
        val required = names.next()
        val start = names.next()
        names.next() // The shared completion chain labels the callback request itself.
        return exec(
            http(required).get { session -> requireNotNull(session.getString(PAGE)) }
                .disableFollowRedirect()
                .check(status().`is`(302))
                .check(
                    header("Location").transform {
                        val resolved = HttpDestination.resolve(it, config.baseUrl)
                        require(URI.create(resolved).path == SIGN_IN) { "Organisation invitation did not require sign-in: $resolved" }
                        resolved
                    }.saveAs(SIGN_IN_ENTRY),
                ),
        ).exitHereIfFailed()
            .exec(
                http(start).get { session -> requireNotNull(session.getString(SIGN_IN_ENTRY)) }
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { HttpDestination.resolve(it, config.simulatorUrl) }
                            .saveAs(OneLoginAuthentication.authorizationUrlKey(authenticationPrefix)),
                    ),
            ).exitHereIfFailed()
            .exec(
                OneLoginAuthentication.authenticationCompletionChain(
                    config,
                    subject,
                    authenticationPrefix,
                    requestLabelPrefix = labelPrefix,
                ),
            )
    }

    private fun memberTeamPage(
        config: BasicRunConfig,
        name: String,
    ) = teamPage(config, name)
        .check(css("#$EDITORS_TAB a[href*='/change-access-level/']", "href").saveAs(NEXT))
        .check(css("#$EDITORS_TAB .govuk-summary-list__value").transform { normalise(it) }.saveAs(MEMBER))
        .check(css("a.govuk-tabs__tab[href='#$EDITORS_TAB']").transform { tabCount(it) }.saveAs(EDITOR_COUNT))
        .check(css("a.govuk-tabs__tab[href='#$ADMINISTRATORS_TAB']").transform { tabCount(it) }.saveAs(ADMIN_COUNT))

    // Tab counts are reloaded from the saved team, so they prove the change persisted rather than just the flash banner.
    private fun updatedTeamPage(
        config: BasicRunConfig,
        name: String,
        heading: String,
        bannerSuffix: String,
        administratorsAdded: Int,
    ) = teamPage(config, name)
        .check(css(".govuk-notification-banner__heading").transform { normalise(it) }.`is`(heading))
        .check(
            css(".govuk-notification-banner__content p.govuk-body")
                .transformWithSession { text, session -> normalise(text) == session.getString(MEMBER) + bannerSuffix }
                .`is`(true),
        ).check(
            css("a.govuk-tabs__tab[href='#$EDITORS_TAB']")
                .transformWithSession { text, session -> tabCount(text) == session.getInt(EDITOR_COUNT) - 1 }
                .`is`(true),
        ).check(
            css("a.govuk-tabs__tab[href='#$ADMINISTRATORS_TAB']")
                .transformWithSession { text, session -> tabCount(text) == session.getInt(ADMIN_COUNT) + administratorsAdded }
                .`is`(true),
        )

    private fun tabCount(text: String): Int =
        requireNotNull(Regex("""\((\d+)\)""").find(text)) { "Team members tab has no count: $text" }.groupValues[1].toInt()

    private fun invite(
        config: BasicRunConfig,
        names: Iterator<String>,
        email: String,
        nextLink: CheckBuilder,
    ): ChainBuilder =
        exec(start(config, names.next(), INVITE_ROUTE))
            .exec(page(config, names.next(), INVITE_ROUTE, listOf("emailAddress", "role")))
            .exec(submit(config, names.next(), INVITE_ROUTE, TEAM) { listOf("emailAddress" to email, "role" to "EDITOR") })
            .exec(teamPage(config, names.next()).check(bodyString().transform { email in it }.`is`(true)).check(nextLink))
            .exitHereIfFailed()

    private fun detailsPage(
        config: BasicRunConfig,
        name: String,
        registration: String,
    ) = http(name).get { HttpDestination.withCacheBuster(config.baseUrl + DETAILS) }
        .disableFollowRedirect()
        .check(status().`is`(200))
        .check(css(row("Landlord Registration Number")).transform { it.trim() }.`is`(registration))

    private fun teamPage(
        config: BasicRunConfig,
        name: String,
    ) = http(name).get { HttpDestination.withCacheBuster(config.baseUrl + TEAM) }
        .disableFollowRedirect()
        .check(status().`is`(200))
        .check(css(".govuk-error-summary").notExists())

    internal fun start(
        config: BasicRunConfig,
        name: String,
        route: String,
    ): ChainBuilder =
        exec(
            http(name).get { session -> scoped(requireNotNull(session.getString(NEXT)), config, route) }
                .disableFollowRedirect()
                .check(status().`is`(302))
                .check(header("Location").transform { scoped(it, config, route) }.saveAs(PAGE)),
        ).exitHereIfFailed()

    internal fun page(
        config: BasicRunConfig,
        name: String,
        route: String,
        fields: List<String>,
        extraChecks: List<CheckBuilder> = emptyList(),
    ): ChainBuilder {
        var request =
            http(name).get { session -> scoped(requireNotNull(session.getString(PAGE)), config, route) }
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(css(".govuk-error-summary").notExists())
                .check(css("form[method=post]", "action").saveAs(ACTION))
                .check(css("form input[name=_csrf]", "value").exists())
                .check(css("form input[type=hidden]", "name").findAll().saveAs(HIDDEN_NAMES))
                .check(css("form input[type=hidden]", "value").findAll().saveAs(HIDDEN_VALUES))
        for (field in fields) request = request.check(css("form input[name=$field]").exists())
        for (check in extraChecks) request = request.check(check)
        return exec(request).exitHereIfFailed().pause(config.thinkTime)
    }

    internal fun submit(
        config: BasicRunConfig,
        name: String,
        route: String,
        nextPath: String,
        values: (Session) -> List<Pair<String, String>>,
    ): ChainBuilder =
        exec(
            http(name).post { session ->
                scoped(
                    HttpDestination.resolve(
                        requireNotNull(session.getString(ACTION)),
                        config.baseUrl,
                        requireNotNull(session.getString(PAGE)),
                    ),
                    config,
                    route,
                )
            }.disableFollowRedirect()
                .formParamSeq { session ->
                    val hiddenNames = session.getList<String>(HIDDEN_NAMES)
                    val hiddenValues = session.getList<String>(HIDDEN_VALUES)
                    require(hiddenNames.size == hiddenValues.size) { "Incomplete organisation form hidden fields" }
                    val submitted = values(session)
                    val overridden = submitted.map { it.first }.toSet()
                    (hiddenNames.zip(hiddenValues).filter { it.first !in overridden } + submitted)
                        .map<Pair<String, String>, Map.Entry<String, Any>> { SimpleEntry(it.first, it.second) }
                }.check(status().`is`(302))
                .check(
                    header("Location").transform {
                        val resolved = HttpDestination.resolve(it, config.baseUrl)
                        val actualPath = URI.create(resolved).path
                        require(actualPath == nextPath) { "Organisation journey redirected to $actualPath instead of $nextPath" }
                        if (actualPath.startsWith("$route/")) scoped(resolved, config, route) else resolved
                    }.saveAs(PAGE),
                ),
        ).exitHereIfFailed()

    internal fun redirect(
        config: BasicRunConfig,
        name: String,
        route: String,
        nextPath: String,
    ): ChainBuilder =
        exec(
            http(name).get { session -> scoped(requireNotNull(session.getString(PAGE)), config, route) }
                .disableFollowRedirect()
                .check(status().`is`(302))
                .check(
                    header("Location").transform {
                        val resolved = scoped(it, config, route)
                        require(
                            URI.create(resolved).path == nextPath,
                        ) { "Organisation journey redirected to $resolved instead of $nextPath" }
                        resolved
                    }.saveAs(PAGE),
                ),
        ).exitHereIfFailed()

    private fun normalise(value: String) = value.trim().replace(Regex("\\s+"), " ")

    private fun row(label: String) = ".govuk-summary-list__key:contains('$label') + .govuk-summary-list__value"

    internal fun scoped(
        location: String,
        config: BasicRunConfig,
        route: String,
    ): String {
        val resolved = HttpDestination.resolve(location, config.baseUrl)
        val path = URI.create(resolved).path
        require(path.startsWith("$route/")) { "Organisation journey left its expected route: $path" }
        return resolved
    }
}

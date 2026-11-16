package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.controllers.AcceptOrRejectJointLandlordInvitationController
import uk.gov.communities.prsdb.webapp.controllers.InviteJointLandlordController
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import java.net.URI

object JointLandlordJourney {
    val decisionRequestNames =
        listOf(
            "invitation start", "validate invitation token", "accept or reject invitation", "submit invitation decision",
            "check user role", "confirm landlord status", "submit acceptance", "acceptance confirmation", "joined property",
        ).map { "Joint invitation: $it" }

    val requestNames =
        listOf(
            "property details",
            "start invitation",
            "initialise invitation",
            "has joint landlords",
            "submit joint status",
            "invite email",
            "submit invitation",
            "review invited email",
            "confirm invited email",
            "review invitations",
            "send invitations",
            "invitation confirmation",
            "pending invitations",
        ).map { "Joint landlord: $it" }

    val preparationRequestNames = requestNames.take(requestNames.indexOf("Joint landlord: pending invitations") + 1)

    fun chain(
        config: BasicRunConfig,
        fixture: JointLandlordFixture,
        prefix: String = "",
    ): ChainBuilder {
        val detailsPath =
            PropertyDetailsController.LANDLORD_PROPERTY_DETAILS_ROUTE
                .replace("{propertyOwnershipId}", fixture.propertyOwnershipId.toString())
        val invitationPath = InviteJointLandlordController.getInviteJointLandlordRoute(fixture.propertyOwnershipId)
        val invitedEmail = requireNotNull(fixture.invitedEmail) { "Invitation preparation requires an existing-user email" }
        val names = requestNames.map { prefix + it }.iterator()

        val invitationSent =
            exec(
                http(names.next())
                    .get { HttpDestination.withCacheBuster(config.baseUrl + detailsPath) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("h1").exists())
                    .check(css("a[href*='/invite-joint-landlord/start']", "href").saveAs("jointStartLink")),
            ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get("#{jointStartLink}")
                        .disableFollowRedirect()
                        .check(status().`is`(302))
                        .check(
                            header("Location")
                                .transform { JointAgentJourneySupport.initialisedStep(it, config, "$invitationPath/start") }
                                .saveAs("jointNext"),
                        ),
                ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get("#{jointNext}")
                        .disableFollowRedirect()
                        .check(status().`is`(302))
                        .check(
                            header("Location").transform { scoped(it, config, invitationPath, "has-joint-landlords") }.saveAs("jointNext"),
                        ),
                ).exitHereIfFailed()
                .exec(getForm(config, names.next(), "jointNext", invitationPath, "has-joint-landlords", "hasJointLandlords"))
                .exec(submitForm(config, names.next(), "jointNext", invitationPath, "joint", "true", "jointNext"))
                .exec(getForm(config, names.next(), "jointNext", invitationPath, "invite-joint-landlord", "emailAddress"))
                .exec(submitForm(config, names.next(), "jointNext", invitationPath, "joint", invitedEmail, "jointNext"))
                .exec(
                    http(names.next())
                        .get("#{jointNext}")
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(bodyString().transform { it.contains(invitedEmail) }.`is`(true))
                        .check(css("form[method=post]", "action").saveAs("jointAction"))
                        .check(css("form input[name=_csrf]", "value").exists())
                        .check(css("form input[type=hidden]", "name").findAll().saveAs("jointHiddenNames"))
                        .check(css("form input[type=hidden]", "value").findAll().saveAs("jointHiddenValues")),
                ).exitHereIfFailed()
                .exec(submitForm(config, names.next(), "jointNext", invitationPath, "joint", null, "jointNext"))
                .exec(
                    http(names.next())
                        .get("#{jointNext}")
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(bodyString().transform { it.contains(invitedEmail) }.`is`(true))
                        .check(css("form[method=post]", "action").saveAs("jointAction"))
                        .check(css("form input[name=_csrf]", "value").exists())
                        .check(css("form input[type=hidden]", "name").findAll().saveAs("jointHiddenNames"))
                        .check(css("form input[type=hidden]", "value").findAll().saveAs("jointHiddenValues")),
                ).exitHereIfFailed()
                .exec(submitForm(config, names.next(), "jointNext", invitationPath, "joint", null, "jointNext"))
                .exec(
                    http(names.next())
                        .get("#{jointNext}")
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(bodyString().transform { it.contains("Joint landlord invitations sent") }.`is`(true))
                        .check(css("a[href^='$detailsPath']").exists()),
                ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get { session -> HttpDestination.withCacheBuster(config.baseUrl + detailsPath) }
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(bodyString().transform { it.contains(invitedEmail) }.`is`(true)),
                ).exitHereIfFailed()
        return invitationSent
    }

    fun decisionChain(
        config: BasicRunConfig,
        fixture: JointLandlordFixture,
        prefix: String = "",
    ): ChainBuilder {
        requireNotNull(fixture.token) { "A joint invitation token is required for an invitation decision" }
        requireNotNull(fixture.inviteeSubject) { "An invitee subject is required for an invitation decision" }
        val names = decisionRequestNames.map { prefix + it }.iterator()
        val invitationRoute = AcceptOrRejectJointLandlordInvitationController.ACCEPT_OR_REJECT_JOINT_LANDLORD_INVITATION_ROUTE
        val confirmationRoute = AcceptOrRejectJointLandlordInvitationController.JOINT_LANDLORD_INVITATION_ACCEPTED_CONFIRMATION_ROUTE
        var chain =
            exec(
                http(names.next())
                    .get("$invitationRoute?token=${fixture.token}")
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { scoped(it, config, invitationRoute, "validate-token") }
                            .saveAs("jointDecisionNext"),
                    ),
            ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get("#{jointDecisionNext}")
                        .disableFollowRedirect()
                        .check(status().`is`(302))
                        .check(
                            header("Location")
                                .transform { scoped(it, config, invitationRoute, "accept-or-reject") }
                                .saveAs("jointDecisionNext"),
                        ),
                ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get("#{jointDecisionNext}")
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(bodyString().transform { containsAddress(it, fixture.address) }.`is`(true))
                        .check(css("form[method=post]", "action").saveAs("jointDecisionAction"))
                        .check(css("form input[name=_csrf]", "value").exists())
                        .check(css("form input[type=hidden]", "name").findAll().saveAs("jointDecisionHiddenNames"))
                        .check(css("form input[type=hidden]", "value").findAll().saveAs("jointDecisionHiddenValues"))
                        .check(css("form input[name$='isInviteAccepted']", "name").saveAs("jointDecisionField")),
                ).exitHereIfFailed()
                .exec(
                    postDecisionForm(
                        config,
                        names.next(),
                        invitationRoute,
                        "jointDecisionNext",
                        mapOf("isInviteAccepted" to "true"),
                        "check-user-role",
                    ),
                )
        chain =
            chain.exec(
                http(names.next())
                    .get("#{jointDecisionNext}")
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { scoped(it, config, invitationRoute, "confirm-landlord-for-property") }
                            .saveAs("jointDecisionNext"),
                    ),
            ).exitHereIfFailed().exec(
                http(names.next())
                    .get("#{jointDecisionNext}")
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(bodyString().transform { containsAddress(it, fixture.address) }.`is`(true))
                    .check(css("form[method=post]", "action").saveAs("jointDecisionAction"))
                    .check(css("form input[name=_csrf]", "value").exists())
                    .check(css("form input[type=hidden]", "name").findAll().saveAs("jointDecisionHiddenNames"))
                    .check(css("form input[type=hidden]", "value").findAll().saveAs("jointDecisionHiddenValues")),
            ).exitHereIfFailed()
                .exec(
                    postDecisionForm(
                        config,
                        names.next(),
                        invitationRoute,
                        "jointDecisionNext",
                        emptyMap(),
                        confirmationRoute.substringAfterLast('/'),
                    ),
                )
        chain =
            chain.exec(
                http(names.next())
                    .get("#{jointDecisionNext}")
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(bodyString().transform { containsAddress(it, fixture.address) }.`is`(true)),
            ).exitHereIfFailed()
        val details =
            PropertyDetailsController.LANDLORD_PROPERTY_DETAILS_ROUTE
                .replace("{propertyOwnershipId}", fixture.propertyOwnershipId.toString())
        chain =
            chain.exec(
                http(names.next())
                    .get { HttpDestination.withCacheBuster(config.baseUrl + details) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(bodyString().transform { containsAddress(it, fixture.address) }.`is`(true)),
            ).exitHereIfFailed()
        return chain
    }

    internal fun containsAddress(
        body: String,
        address: String,
    ): Boolean {
        val text = normalisedAddressText(body.replace("&#39;", "'").replace("&amp;", "&"))
        return address.split(",").map(::normalisedAddressText).filter { it.isNotEmpty() }.all { text.contains(it) }
    }

    private fun normalisedAddressText(value: String) = value.lowercase().filter { it.isLetterOrDigit() }

    private fun getForm(
        config: BasicRunConfig,
        name: String,
        urlKey: String,
        route: String,
        expectedSegment: String,
        fieldSuffix: String,
    ): ChainBuilder =
        exec(
            http(name)
                .get { session ->
                    val page = requireNotNull(session.getString(urlKey))
                    require(URI.create(page).path == "$route/$expectedSegment") {
                        "Joint-landlord journey reached an unexpected step: ${URI.create(page).path}"
                    }
                    page
                }.disableFollowRedirect()
                .check(status().`is`(200))
                .check(css("form[method=post]", "action").saveAs("jointAction"))
                .check(css("form input[name=_csrf]", "value").exists())
                .check(css("form input[type=hidden]", "name").findAll().saveAs("jointHiddenNames"))
                .check(css("form input[type=hidden]", "value").findAll().saveAs("jointHiddenValues"))
                .check(css("form input[name$='$fieldSuffix']", "name").saveAs("jointInputField")),
        ).exitHereIfFailed()

    private fun submitForm(
        config: BasicRunConfig,
        name: String,
        urlKey: String,
        route: String,
        key: String,
        value: String?,
        nextKey: String,
    ): ChainBuilder =
        exec(
            http(name)
                .post { session ->
                    JointAgentJourneySupport.formAction(
                        requireNotNull(session.getString("${key}Action")),
                        config,
                        requireNotNull(session.getString(urlKey)),
                    )
                }.disableFollowRedirect()
                .formParamMap { session ->
                    val field = session.getString("${key}InputField")
                    val values =
                        if (value == null || field == null) {
                            emptyMap()
                        } else {
                            mapOf(field to value)
                        }
                    hiddenFormParameters(session, "jointHiddenNames", "jointHiddenValues") + values
                }.check(status().`is`(302))
                .check(
                    header("Location")
                        .transform { scoped(it, config, route, null) }
                        .saveAs(nextKey),
                ),
        ).exitHereIfFailed()

    private fun hiddenFormParameters(
        session: io.gatling.javaapi.core.Session,
        namesKey: String,
        valuesKey: String,
    ): Map<String, String> {
        val names = session.getList<String>(namesKey)
        val values = session.getList<String>(valuesKey)
        require(names.size == values.size) { "A joint-landlord form omitted a hidden field value" }
        return names.zip(values).toMap()
    }

    private fun postDecisionForm(
        config: BasicRunConfig,
        name: String,
        route: String,
        pageKey: String,
        fields: Map<String, String>,
        expectedSegment: String,
    ): ChainBuilder =
        exec { it }.pause(config.thinkTime).exec(
            http(name)
                .post { session ->
                    JointAgentJourneySupport.formAction(
                        requireNotNull(session.getString("jointDecisionAction")),
                        config,
                        requireNotNull(session.getString(pageKey)),
                    )
                }.disableFollowRedirect()
                .formParamMap { session ->
                    val names = session.getList<String>("jointDecisionHiddenNames")
                    val values = session.getList<String>("jointDecisionHiddenValues")
                    require(names.size == values.size) { "A joint invitation form omitted a hidden field value" }
                    val submitted =
                        fields.mapKeys { (field, _) ->
                            if (field == "isInviteAccepted") requireNotNull(session.getString("jointDecisionField")) else field
                        }
                    names.zip(values).toMap() + submitted
                }.check(status().`is`(302))
                .check(
                    header("Location")
                        .transform { scoped(it, config, route, expectedSegment) }
                        .saveAs("jointDecisionNext"),
                ),
        ).exitHereIfFailed()

    private fun scoped(
        location: String,
        config: BasicRunConfig,
        expectedRoute: String,
        expectedSegment: String?,
    ): String {
        val resolved = HttpDestination.resolve(location, config.baseUrl)
        val path = URI.create(resolved).path
        require(path == expectedRoute || path.startsWith("$expectedRoute/")) {
            "Joint-landlord journey left its allocated route: $path"
        }
        if (expectedSegment != null) {
            require(path.endsWith("/$expectedSegment")) {
                "Joint-landlord journey reached an unexpected step: $path"
            }
        }
        return resolved
    }
}

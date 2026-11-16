package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.controllers.CancelLettingAgentDelegationController
import uk.gov.communities.prsdb.webapp.controllers.DelegateToLettingAgentController
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentInvitationController
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentPropertyDetailsController
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentUpdateLicensingController
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentUpdateRentFrequencyAndAmountController
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import java.net.URI
import java.util.UUID

object AgentJourney {
    val ownerRequestNames =
        listOf(
            "property details",
            "initialise delegation",
            "delegate form",
            "submit delegation",
            "delegation confirmation",
            "delegated property details",
        ).map { "Letting agent: $it" }

    fun accessRequestNames(hasPassword: Boolean) =
        listOfNotNull(
            "open invitation",
            "resolve password status",
            "enter password form",
            "submit password",
            if (hasPassword) null else "password confirmation",
            "agent property details",
            "start licensing update",
            "licensing type form",
            "submit licensing type",
            "licence number form",
            "submit licence number",
            "licensing answers",
            "save licensing",
            "saved licensing details",
            "start tenancy update",
            "rent frequency form",
            "submit rent frequency",
            "rent amount form",
            "submit rent amount",
            "tenancy answers",
            "save tenancy",
            "saved tenancy details",
        ).map { "Letting agent access: $it" }

    val preparationRequestNames = ownerRequestNames

    val preparationFamilies = listOf("access")

    fun preparationPrefix(family: String) = "Prepare $family delegation: "

    fun preparationEmail(
        family: String,
        propertyOwnershipId: Long,
    ): String {
        require(family in preparationFamilies) { "Unsupported letting-agent preparation family: $family" }
        return "agent-$family-$propertyOwnershipId@example.test"
    }

    fun ownerChain(
        config: BasicRunConfig,
        fixture: AgentFixture,
        prefix: String = "",
        preparedEmail: String? = null,
    ): ChainBuilder {
        val propertyId = fixture.propertyOwnershipId
        val detailsPath = PropertyDetailsController.LANDLORD_PROPERTY_DETAILS_ROUTE.replace("{propertyOwnershipId}", propertyId.toString())
        val delegationPath =
            DelegateToLettingAgentController.DELEGATE_TO_LETTING_AGENT_ROUTE.replace(
                "{propertyOwnershipId}",
                propertyId.toString(),
            )
        val removalPath =
            CancelLettingAgentDelegationController.REMOVE_LETTING_AGENT_ROUTE.replace(
                "{propertyOwnershipId}",
                propertyId.toString(),
            )
        val agentEmail = preparedEmail ?: "agent-$propertyId@example.test"
        val names = ownerRequestNames.map { prefix + it }.iterator()

        val delegated =
            exec(
                http(names.next())
                    .get { HttpDestination.withCacheBuster(config.baseUrl + detailsPath) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("h1").exists())
                    .check(css("a[href*='$delegationPath/allow-letting-agent']", "href").saveAs("agentDelegatePath")),
            ).exitHereIfFailed()
                .exec(initialise(config, names.next(), "agentDelegatePath", "$delegationPath/allow-letting-agent"))
                .exec(
                    http(names.next())
                        .get("#{agentDelegatePath}")
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(css("h1").exists())
                        .check(css("form[method=post]", "action").saveAs("agentAction"))
                        .check(css("form input[name=_csrf]", "value").exists())
                        .check(css("form input[type=hidden]", "name").findAll().saveAs("agentHiddenNames"))
                        .check(css("form input[type=hidden]", "value").findAll().saveAs("agentHiddenValues"))
                        .check(css("form input[name$='emailAddress']", "name").saveAs("agentEmailField")),
                ).exitHereIfFailed().pause(config.thinkTime)
                .exec(
                    http(names.next())
                        .post { session ->
                            JointAgentJourneySupport.formAction(
                                requireNotNull(session.getString("agentAction")),
                                config,
                                requireNotNull(session.getString("agentDelegatePath")),
                            )
                        }.disableFollowRedirect()
                        .formParamMap { session ->
                            hiddenFormParameters(session) +
                                (requireNotNull(session.getString("agentEmailField")) to agentEmail)
                        }.check(status().`is`(302))
                        .check(
                            header("Location")
                                .transform { scoped(it, config, delegationPath, "confirmation") }
                                .saveAs("agentConfirmationPath"),
                        ),
                ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get("#{agentConfirmationPath}")
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(bodyString().transform { it.contains(agentEmail) }.`is`(true))
                        .check(bodyString().transform { JointLandlordJourney.containsAddress(it, fixture.address) }.`is`(true)),
                ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get { HttpDestination.withCacheBuster(config.baseUrl + detailsPath) }
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(bodyString().transform { it.contains(agentEmail) }.`is`(true))
                        .check(css("a[href*='$removalPath/are-you-sure']", "href").saveAs("agentRemovalPath")),
                ).exitHereIfFailed()
        return delegated
    }

    fun accessAndUpdateChain(
        config: BasicRunConfig,
        fixture: AgentFixture,
        password: String,
        prefix: String = "",
    ): ChainBuilder {
        requireNotNull(fixture.token) { "A letting-agent access token is required for password setup" }
        requireNotNull(fixture.invitedEmail) { "A letting-agent email is required for password setup" }
        require(fixture.hasPassword != null) { "Access fixtures must include the password status" }
        require(password.isNotBlank()) { "A nonblank performance password is required" }
        val token = UUID.fromString(fixture.token)
        val names = accessRequestNames(fixture.hasPassword).map { prefix + it }.iterator()
        val invitationRoute = LettingAgentInvitationController.LETTING_AGENT_INVITATION_ROUTE
        val passwordStep = if (fixture.hasPassword) "enter-password" else "set-password"
        val passwordFields =
            if (fixture.hasPassword) {
                mapOf("password" to password)
            } else {
                mapOf("password" to password, "confirmPassword" to password)
            }
        val propertyDetails =
            LettingAgentPropertyDetailsController.LETTING_AGENT_PROPERTY_DETAILS_ROUTE.replace(
                "{token}",
                token.toString(),
            )
        val licensingRoute = LettingAgentUpdateLicensingController.LETTING_AGENT_UPDATE_LICENSING_ROUTE.replace("{token}", token.toString())
        val tenancyRoute =
            LettingAgentUpdateRentFrequencyAndAmountController.LETTING_AGENT_UPDATE_RENT_FREQUENCY_AND_AMOUNT_ROUTE
                .replace("{token}", token.toString())
        val licenceNumber = "GATLING-${fixture.propertyOwnershipId}"
        val rentAmount = "200"
        val chain =
            exec(
                http(names.next())
                    .get("$invitationRoute?token=$token")
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { scoped(it, config, invitationRoute, "start") }
                            .saveAs("agentNext"),
                    ),
            ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get("#{agentNext}")
                        .disableFollowRedirect()
                        .check(status().`is`(302))
                        .check(
                            header("Location")
                                .transform { scoped(it, config, invitationRoute, passwordStep) }
                                .saveAs("agentNext"),
                        ),
                ).exitHereIfFailed()
                .exec(
                    getAccessForm(
                        config,
                        names.next(),
                        "agentNext",
                        invitationRoute,
                        passwordStep,
                        passwordFields,
                    ),
                ).exec(
                    if (fixture.hasPassword) {
                        postAccessForm(config, names.next(), invitationRoute, passwordFields, detailsPath = propertyDetails)
                    } else {
                        postAccessForm(config, names.next(), invitationRoute, passwordFields, "confirmation")
                            .exec(
                                http(names.next())
                                    .get("#{agentNext}")
                                    .disableFollowRedirect()
                                    .check(status().`is`(200))
                                    .check(bodyString().transform { it.contains("Property password created") }.`is`(true))
                                    .check(bodyString().transform { it.contains(propertyDetails) }.`is`(true)),
                            ).exitHereIfFailed()
                    },
                )
                .exec(
                    http(names.next())
                        .get { HttpDestination.withCacheBuster(config.baseUrl + propertyDetails) }
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(bodyString().transform { JointLandlordJourney.containsAddress(it, fixture.address) }.`is`(true))
                        .check(css("a[href*='$licensingRoute/licensing-type']", "href").saveAs("agentLicensingStart"))
                        .check(css("a[href*='$tenancyRoute/rent-frequency']", "href").saveAs("agentTenancyStart")),
                ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get("#{agentLicensingStart}")
                        .disableFollowRedirect()
                        .check(status().`is`(302))
                        .check(
                            header("Location")
                                .transform { scoped(it, config, licensingRoute, "licensing-type") }
                                .saveAs("agentNext"),
                        ),
                ).exitHereIfFailed()
                .exec(
                    getAccessForm(
                        config,
                        names.next(),
                        "agentNext",
                        licensingRoute,
                        "licensing-type",
                        mapOf("licensingType" to "SELECTIVE_LICENCE"),
                    ),
                ).exec(
                    postAccessForm(
                        config,
                        names.next(),
                        licensingRoute,
                        mapOf("licensingType" to "SELECTIVE_LICENCE"),
                        "selective-licence",
                    ),
                )
                .exec(
                    getAccessForm(
                        config,
                        names.next(),
                        "agentNext",
                        licensingRoute,
                        "selective-licence",
                        mapOf("licenceNumber" to licenceNumber),
                    ),
                ).exec(
                    postAccessForm(
                        config,
                        names.next(),
                        licensingRoute,
                        mapOf("licenceNumber" to licenceNumber),
                        "check-licensing-answers",
                    ),
                )
                .exec(
                    getAccessForm(
                        config,
                        names.next(),
                        "agentNext",
                        licensingRoute,
                        "check-licensing-answers",
                        emptyMap(),
                        expectedText = licenceNumber,
                    ),
                ).exec(
                    postAccessForm(config, names.next(), licensingRoute, emptyMap(), detailsPath = propertyDetails),
                )
                .exec(
                    http(names.next())
                        .get { HttpDestination.withCacheBuster(config.baseUrl + propertyDetails) }
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(
                            bodyString()
                                .transform { JointLandlordJourney.containsAddress(it, fixture.address) && it.contains(licenceNumber) }
                                .`is`(true),
                        )
                        .check(bodyString().transform { it.contains("Selective licence") }.`is`(true)),
                ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get("#{agentTenancyStart}")
                        .disableFollowRedirect()
                        .check(status().`is`(302))
                        .check(
                            header("Location")
                                .transform { scoped(it, config, tenancyRoute, "rent-frequency") }
                                .saveAs("agentNext"),
                        ),
                ).exitHereIfFailed()
                .exec(
                    getAccessForm(
                        config,
                        names.next(),
                        "agentNext",
                        tenancyRoute,
                        "rent-frequency",
                        mapOf("rentFrequency" to "WEEKLY"),
                    ),
                ).exec(
                    postAccessForm(
                        config,
                        names.next(),
                        tenancyRoute,
                        mapOf("rentFrequency" to "WEEKLY"),
                        "rent-amount",
                    ),
                )
                .exec(
                    getAccessForm(
                        config,
                        names.next(),
                        "agentNext",
                        tenancyRoute,
                        "rent-amount",
                        mapOf("rentAmount" to rentAmount),
                    ),
                ).exec(
                    postAccessForm(
                        config,
                        names.next(),
                        tenancyRoute,
                        mapOf("rentAmount" to rentAmount),
                        "rent-frequency-and-amount-check-your-answers",
                    ),
                )
                .exec(
                    getAccessForm(
                        config,
                        names.next(),
                        "agentNext",
                        tenancyRoute,
                        "rent-frequency-and-amount-check-your-answers",
                        emptyMap(),
                        expectedText = rentAmount,
                    ),
                ).exec(postAccessForm(config, names.next(), tenancyRoute, emptyMap(), detailsPath = propertyDetails))
                .exec(
                    http(names.next())
                        .get { HttpDestination.withCacheBuster(config.baseUrl + propertyDetails) }
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(bodyString().transform { it.contains("Weekly") && it.contains("£$rentAmount") }.`is`(true)),
                ).exitHereIfFailed()
        return chain
    }

    private fun getAccessForm(
        config: BasicRunConfig,
        name: String,
        pageKey: String,
        route: String,
        step: String,
        fields: Map<String, String>,
        expectedText: String? = null,
    ): ChainBuilder {
        var request =
            http(name)
                .get { session ->
                    val page = requireNotNull(session.getString(pageKey))
                    require(URI.create(HttpDestination.resolve(page, config.baseUrl)).path == "$route/$step") {
                        "Letting-agent form page does not match the current step"
                    }
                    page
                }.disableFollowRedirect()
                .check(status().`is`(200))
                .check(css("form[method=post]", "action").saveAs("agentFormAction"))
                .check(css("form input[name=_csrf]", "value").exists())
                .check(css("form input[type=hidden]", "name").findAll().saveAs("agentFormHiddenNames"))
                .check(css("form input[type=hidden]", "value").findAll().saveAs("agentFormHiddenValues"))
                .check(bodyString().transform { expectedText == null || it.contains(expectedText) }.`is`(true))
        for (field in fields.keys) {
            request = request.check(css("form input[name='$field']", "name").saveAs("agentFormField-$field"))
        }
        return exec(request).exitHereIfFailed().pause(config.thinkTime)
    }

    private fun postAccessForm(
        config: BasicRunConfig,
        name: String,
        route: String,
        fields: Map<String, String>,
        nextStep: String? = null,
        detailsPath: String? = null,
    ): ChainBuilder =
        exec(
            http(name)
                .post { session ->
                    JointAgentJourneySupport.formAction(
                        requireNotNull(session.getString("agentFormAction")),
                        config,
                        requireNotNull(session.getString("agentNext")),
                    )
                }.disableFollowRedirect()
                .formParamMap { session ->
                    val names = session.getList<String>("agentFormHiddenNames")
                    val values = session.getList<String>("agentFormHiddenValues")
                    require(names.size == values.size) { "A letting-agent form omitted a hidden field value" }
                    val submitted =
                        fields.map { (field, value) ->
                            requireNotNull(session.getString("agentFormField-$field")) to value
                        }.toMap()
                    names.zip(values).toMap() + submitted
                }.check(status().`is`(302))
                .check(
                    header("Location")
                        .transform { location ->
                            if (detailsPath != null) {
                                val resolved = HttpDestination.resolve(location, config.baseUrl)
                                require(URI.create(resolved).path == detailsPath) {
                                    "Letting-agent update did not return to property details"
                                }
                                resolved
                            } else {
                                scoped(requireNotNull(location), config, route, requireNotNull(nextStep))
                            }
                        }.saveAs("agentNext"),
                ),
        ).exitHereIfFailed()

    private fun scoped(
        location: String,
        config: BasicRunConfig,
        route: String,
        expectedSegment: String,
    ): String {
        val resolved = HttpDestination.resolve(location, config.baseUrl)
        val path = URI.create(resolved).path
        require(path.startsWith("$route/") && path.endsWith("/$expectedSegment")) {
            "Letting-agent journey redirected outside its expected route: $path"
        }
        return resolved
    }

    private fun hiddenFormParameters(session: io.gatling.javaapi.core.Session): Map<String, String> {
        val names = session.getList<String>("agentHiddenNames")
        val values = session.getList<String>("agentHiddenValues")
        require(names.size == values.size) { "A letting-agent form omitted a hidden field value" }
        return names.zip(values).toMap()
    }

    private fun initialise(
        config: BasicRunConfig,
        name: String,
        pageKey: String,
        entry: String,
    ): ChainBuilder =
        exec(
            http(name)
                .get("#{$pageKey}")
                .disableFollowRedirect()
                .check(status().`is`(302))
                .check(header("Location").transform { JointAgentJourneySupport.initialisedStep(it, config, entry) }.saveAs(pageKey)),
        ).exitHereIfFailed()
}

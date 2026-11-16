package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.controllers.DeregisterPropertyController
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import java.net.URI

object OffboardingJourney {
    val requestNames =
        listOf(
            "property details", "start deregistration", "check can deregister", "deregistration information",
            "continue deregistration", "check pending invitations", "confirm deregistration",
            "submit deregistration", "deregistration confirmation",
        ).map { "Offboarding deregister-property: $it" }

    fun chain(
        config: BasicRunConfig,
        fixture: OffboardingFixture,
        prefix: String = "",
    ): ChainBuilder {
        val names = requestNames.map { prefix + it }.iterator()
        val id = fixture.propertyOwnershipId.toString()
        val details = PropertyDetailsController.LANDLORD_PROPERTY_DETAILS_ROUTE.replace("{propertyOwnershipId}", id)
        val route = DeregisterPropertyController.PROPERTY_DEREGISTRATION_ROUTE.replace("{propertyOwnershipId}", id)
        return exec(
            http(names.next())
                .get { HttpDestination.withCacheBuster(config.baseUrl + details) }
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(
                    css("a[href*='$route/deregister']", "href").transform { HttpDestination.resolve(it, config.baseUrl) }
                        .saveAs("offNext"),
                ),
        ).exitHereIfFailed()
            .exec(
                http(names.next()).get("#{offNext}").disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location").transform {
                            JointAgentJourneySupport.initialisedStep(it, config, "$route/deregister")
                        }.saveAs("offNext"),
                    ),
            ).exitHereIfFailed()
            .exec(redirectingStep(config, names.next(), route, "info"))
            .exec(getForm(config, names.next(), route, "info"))
            .exec(submitForm(config, names.next(), route, "start"))
            .exec(redirectingStep(config, names.next(), route, "confirm"))
            .exec(getForm(config, names.next(), route, "confirm"))
            .exec(submitForm(config, names.next(), route, "confirmation"))
            .exec(
                http(names.next()).get("#{offNext}").disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(bodyString().transform { JointLandlordJourney.containsAddress(it, fixture.address) }.`is`(true))
                    .check(bodyString().transform { it.contains("Deregistered") }.`is`(true)),
            ).exitHereIfFailed()
    }

    private fun redirectingStep(
        config: BasicRunConfig,
        name: String,
        route: String,
        nextSegment: String,
    ): ChainBuilder =
        exec(
            http(name).get("#{offNext}").disableFollowRedirect()
                .check(status().`is`(302))
                .check(header("Location").transform { scoped(it, config, route, nextSegment) }.saveAs("offNext")),
        ).exitHereIfFailed()

    private fun getForm(
        config: BasicRunConfig,
        name: String,
        route: String,
        segment: String,
    ): ChainBuilder =
        exec(
            http(name).get { session -> scoped(requireNotNull(session.getString("offNext")), config, route, segment) }
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(css("form[method=post]", "action").saveAs("offAction"))
                .check(css("form input[name=_csrf]", "value").exists())
                .check(css("form input[type=hidden]", "name").findAll().saveAs("offHiddenNames"))
                .check(css("form input[type=hidden]", "value").findAll().saveAs("offHiddenValues")),
        ).exitHereIfFailed().pause(config.thinkTime)

    private fun submitForm(
        config: BasicRunConfig,
        name: String,
        route: String,
        nextSegment: String,
    ): ChainBuilder =
        exec(
            http(name).post { session ->
                JointAgentJourneySupport.formAction(
                    requireNotNull(session.getString("offAction")),
                    config,
                    requireNotNull(session.getString("offNext")),
                )
            }.disableFollowRedirect()
                .formParamMap { session ->
                    val names = session.getList<String>("offHiddenNames")
                    val values = session.getList<String>("offHiddenValues")
                    require(names.size == values.size) { "An offboarding form omitted a hidden field value" }
                    names.zip(values).toMap()
                }.check(status().`is`(302))
                .check(header("Location").transform { scoped(it, config, route, nextSegment) }.saveAs("offNext")),
        ).exitHereIfFailed()

    private fun scoped(
        location: String,
        config: BasicRunConfig,
        route: String,
        segment: String,
    ): String {
        val resolved = HttpDestination.resolve(location, config.baseUrl)
        require(URI.create(resolved).path == "$route/$segment") { "Offboarding journey reached an unexpected step" }
        return resolved
    }
}

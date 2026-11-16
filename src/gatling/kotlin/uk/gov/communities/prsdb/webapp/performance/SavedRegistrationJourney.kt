package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.controllers.LandlordController
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController
import java.net.URI

object SavedRegistrationJourney {
    val requestNames =
        listOf(
            "list", "resume", "task list", "selected address form", "select address", "property type form", "property type",
            "bedrooms form", "save bedrooms", "next step", "saved bedrooms", "delete form", "delete", "confirmation", "remaining records",
        ).map { "Saved registration: $it" }

    fun chain(
        config: BasicRunConfig,
        fixture: SavedRegistrationFixture,
        prefix: String,
    ): ChainBuilder {
        val resumeLink = link("/resume", fixture.resumeJourneyId)
        val deleteLink = link("/delete-incomplete-property", fixture.deleteJourneyId)
        val selectedUrl =
            "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/select-address?journeyId=${fixture.resumeJourneyId}"
        val bedroomsUrl =
            "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/number-of-bedrooms?journeyId=${fixture.resumeJourneyId}"
        return exec(
            http("$prefix${requestNames[0]}")
                .get(LandlordController.INCOMPLETE_PROPERTIES_URL)
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(css(resumeLink, "href").saveAs("savedResumeUrl"))
                .check(css(deleteLink, "href").saveAs("savedDeleteUrl"))
                .check(css(".govuk-summary-card").findAll().transform { cards -> cards.any { it.contains(fixture.address) } }.`is`(true)),
        ).exitHereIfFailed()
            .exec(
                http("$prefix${requestNames[1]}")
                    .get { session -> HttpDestination.resolve(requireNotNull(session.getString("savedResumeUrl")), config.baseUrl) }
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location").transform {
                            val resolved = HttpDestination.resolve(it, config.baseUrl)
                            val uri = URI.create(resolved)
                            val journeyParameter = uri.rawQuery.orEmpty().split("&").filter { it.substringBefore('=') == "journeyId" }
                            require(
                                uri.path == "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/task-list" &&
                                    journeyParameter.singleOrNull() == "journeyId=${fixture.resumeJourneyId}",
                            ) {
                                "Unexpected saved-registration resume query: ${uri.rawQuery}; expected journeyId=${fixture.resumeJourneyId}"
                            }
                            resolved
                        }.saveAs("savedNextUrl"),
                    ),
            ).exitHereIfFailed()
            .exec(
                http("$prefix${requestNames[2]}")
                    .get("#{savedNextUrl}")
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("a[href*='/lookup-address?']", "href").exists()),
            ).exitHereIfFailed()
            .exec { session -> session.set("savedNextUrl", selectedUrl) }
            .exec(form(config, "$prefix${requestNames[3]}", "input[name=address]", fixture.address))
            .exec(submit(config, "$prefix${requestNames[4]}", mapOf("address" to fixture.address)))
            .exec(form(config, "$prefix${requestNames[5]}", "input[name=propertyType]"))
            .exec(submit(config, "$prefix${requestNames[6]}", mapOf("propertyType" to "DETACHED_HOUSE")))
            .exec(form(config, "$prefix${requestNames[7]}", "input[name=numberOfBedrooms]"))
            .exec(submit(config, "$prefix${requestNames[8]}", mapOf("numberOfBedrooms" to "2")))
            .exec { session ->
                val field =
                    when (URI.create(requireNotNull(session.getString("savedNextUrl"))).path) {
                        "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/ownership-type" -> "ownershipType"
                        "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/has-joint-landlords" -> "hasJointLandlords"
                        else -> error("Unexpected next saved-registration step")
                    }
                session.set("savedNextField", field)
            }.exitHereIfFailed()
            .exec(form(config, "$prefix${requestNames[9]}", "input[name=#{savedNextField}]"))
            .exec(
                http("$prefix${requestNames[10]}")
                    .get { HttpDestination.withCacheBuster(HttpDestination.resolve(bedroomsUrl, config.baseUrl)) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("input[name=numberOfBedrooms]", "value").`is`("2")),
            ).exitHereIfFailed()
            .exec { session -> session.set("savedNextUrl", requireNotNull(session.getString("savedDeleteUrl"))) }
            .exec(form(config, "$prefix${requestNames[11]}", "input[name=wantsToProceed][value=true]"))
            .exec(submit(config, "$prefix${requestNames[12]}", mapOf("wantsToProceed" to "true")))
            .exec(
                http("$prefix${requestNames[13]}")
                    .get("#{savedNextUrl}")
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css(".govuk-panel h1").exists())
                    .check(css("a[href='/landlord/incomplete-properties']").exists()),
            ).exitHereIfFailed()
            .exec(
                http("$prefix${requestNames[14]}")
                    .get {
                        HttpDestination.withCacheBuster(
                            HttpDestination.resolve(LandlordController.INCOMPLETE_PROPERTIES_URL, config.baseUrl),
                        )
                    }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css(deleteLink).notExists())
                    .check(css(resumeLink).exists()),
            ).exitHereIfFailed()
    }

    private fun link(
        path: String,
        journeyId: String,
    ): String = "a[href*='$path?'][href*='contextId=$journeyId&'], a[href*='$path?'][href$='contextId=$journeyId']"

    private fun form(
        config: BasicRunConfig,
        name: String,
        field: String,
        expectedOption: String? = null,
    ): ChainBuilder {
        var request =
            http(name).get("#{savedPageUrl}").disableFollowRedirect()
                .check(status().`is`(200))
                .check(header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }.`is`("text/html"))
                .check(css(".govuk-error-summary").notExists())
                .check(css("form[method=post]", "action").saveAs("savedFormAction"))
                .check(css("form input[name=_csrf]", "value").exists())
                .check(css("form input[type=hidden]", "name").findAll().saveAs("savedHiddenNames"))
                .check(css("form input[type=hidden]", "value").findAll().saveAs("savedHiddenValues"))
                .check(css(field).exists())
        if (expectedOption != null) {
            request = request.check(css(field, "value").findAll().transform { it.contains(expectedOption) }.`is`(true))
        }
        return exec { session ->
            session.set("savedPageUrl", HttpDestination.resolve(requireNotNull(session.getString("savedNextUrl")), config.baseUrl))
        }.exec(request).exitHereIfFailed().pause(config.thinkTime)
    }

    private fun submit(
        config: BasicRunConfig,
        name: String,
        fields: Map<String, String>,
    ): ChainBuilder =
        exec(
            http(name).post { session ->
                HttpDestination.resolve(
                    requireNotNull(session.getString("savedFormAction")),
                    config.baseUrl,
                    requireNotNull(session.getString("savedPageUrl")),
                )
            }.disableFollowRedirect()
                .formParamMap { session ->
                    val names = session.getList<String>("savedHiddenNames")
                    val values = session.getList<String>("savedHiddenValues")
                    require(names.size == values.size) { "Saved-registration hidden field values are missing" }
                    names.zip(values).toMap() + fields
                }.check(status().`is`(302))
                .check(header("Location").transform { HttpDestination.resolve(it, config.baseUrl) }.saveAs("savedNextUrl")),
        ).exitHereIfFailed()
}

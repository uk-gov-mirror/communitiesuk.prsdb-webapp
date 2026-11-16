package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.controllers.LandlordDetailsController

object LandlordIdentityUpdateJourney {
    fun requestNames(dateOfBirth: Boolean): List<String> =
        listOf("details", "start", "form", "submit", "saved details").map { "${label(dateOfBirth)}: $it" }

    fun chain(
        config: BasicRunConfig,
        iterationData: BasicIterationData,
        dateOfBirth: Boolean,
    ): ChainBuilder {
        val route = if (dateOfBirth) "update-date-of-birth" else "update-name"
        val row = ".govuk-summary-list__row:has(a[href*='/$route'])"
        val iteration = iterationData.profilePhoneNumber.takeLast(3).toInt()
        val day = iteration % 28 + 1
        val name = "Performance Landlord ${'A' + iteration % 26}"
        val expectedValue = if (dateOfBirth) "$day January 1990" else name
        val fields =
            if (dateOfBirth) {
                mapOf("day" to day.toString(), "month" to "1", "year" to "1990")
            } else {
                mapOf("name" to name)
            }
        val names = requestNames(dateOfBirth).map { iterationData.requestNamePrefix + it }
        return exec(
            http(names[0])
                .get { HttpDestination.withCacheBuster(config.baseUrl + LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE) }
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(
                    css("$row a", "href")
                        .transform { HttpDestination.resolve(it, config.baseUrl) }
                        .saveAs("identityUpdateEntry"),
                ),
        ).exitHereIfFailed()
            .exec(
                http(names[1])
                    .get("#{identityUpdateEntry}")
                    .disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { HttpDestination.resolve(it, config.baseUrl) }
                            .saveAs("identityUpdatePage"),
                    ),
            ).exitHereIfFailed()
            .exec(
                http(names[2])
                    .get("#{identityUpdatePage}")
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css(".govuk-error-summary").notExists())
                    .check(css("form[method=post]", "action").saveAs("identityUpdateAction"))
                    .check(css("input[name=_csrf]", "value").exists())
                    .check(css("form input[type=hidden]", "name").findAll().saveAs("identityUpdateHiddenNames"))
                    .check(css("form input[type=hidden]", "value").findAll().saveAs("identityUpdateHiddenValues"))
                    .check(css("input[name='${if (dateOfBirth) "day" else "name"}']").exists()),
            ).exitHereIfFailed()
            .exec(
                http(names[3])
                    .post { session ->
                        HttpDestination.resolve(
                            requireNotNull(session.getString("identityUpdateAction")),
                            config.baseUrl,
                            requireNotNull(session.getString("identityUpdatePage")),
                        )
                    }.disableFollowRedirect()
                    .formParamMap { session ->
                        val hiddenNames = session.getList<String>("identityUpdateHiddenNames")
                        val hiddenValues = session.getList<String>("identityUpdateHiddenValues")
                        require(hiddenNames.size == hiddenValues.size) { "Identity update form has incomplete hidden fields" }
                        hiddenNames.zip(hiddenValues).toMap() + fields
                    }.check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { HttpDestination.resolve(it, config.baseUrl) }
                            .saveAs("identityUpdateResult"),
                    ),
            ).exitHereIfFailed()
            .exec(
                http(names[4])
                    .get { session -> HttpDestination.withCacheBuster(requireNotNull(session.getString("identityUpdateResult"))) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css(".govuk-error-summary").notExists())
                    .check(css("$row .govuk-summary-list__value").transform { it.trim() }.`is`(expectedValue)),
            ).exitHereIfFailed()
    }

    private fun label(dateOfBirth: Boolean) = if (dateOfBirth) "Date of birth update" else "Name update"
}

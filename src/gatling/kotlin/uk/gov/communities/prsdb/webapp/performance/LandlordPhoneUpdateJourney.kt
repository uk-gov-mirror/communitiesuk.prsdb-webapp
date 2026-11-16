package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

object LandlordPhoneUpdateJourney {
    const val START_REQUEST = "Phone update: start"
    const val FORM_REQUEST = "Phone update: form"
    const val SUBMIT_REQUEST = "Phone update: submit"
    const val DETAILS_REQUEST = "Phone update: saved details"
    val requestNames = listOf(START_REQUEST, FORM_REQUEST, SUBMIT_REQUEST, DETAILS_REQUEST)

    fun chain(
        config: BasicRunConfig,
        iterationData: BasicIterationData,
    ): ChainBuilder {
        val requestNamePrefix = iterationData.requestNamePrefix
        val phoneNumber = iterationData.profilePhoneNumber
        return exec(
            http("$requestNamePrefix$START_REQUEST")
                .get("#{phoneUpdateEntry}")
                .disableFollowRedirect()
                .check(status().`is`(302))
                .check(
                    header("Location")
                        .transform { HttpDestination.resolve(it, config.baseUrl) }
                        .saveAs("phonePageUrl"),
                ),
        ).exitHereIfFailed()
            .exec(
                http("$requestNamePrefix$FORM_REQUEST")
                    .get("#{phonePageUrl}")
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }.`is`("text/html"))
                    .check(css(".govuk-error-summary").notExists())
                    .check(css("form[method=post]", "action").saveAs("phoneFormAction"))
                    .check(css("input[name=_csrf]", "value").saveAs("phoneCsrf"))
                    .check(css("input[name=phoneNumber]").exists()),
            ).exitHereIfFailed().pause(config.thinkTime)
            .exec(
                http("$requestNamePrefix$SUBMIT_REQUEST")
                    .post { session ->
                        HttpDestination.resolve(
                            requireNotNull(session.getString("phoneFormAction")),
                            config.baseUrl,
                            requireNotNull(session.getString("phonePageUrl")),
                        )
                    }.disableFollowRedirect()
                    .formParam("_csrf", "#{phoneCsrf}")
                    .formParam("phoneNumber", phoneNumber)
                    .check(status().`is`(302))
                    .check(
                        header("Location")
                            .transform { HttpDestination.resolve(it, config.baseUrl) }
                            .saveAs("phoneResultUrl"),
                    ),
            ).exitHereIfFailed()
            .exec(
                http("$requestNamePrefix$DETAILS_REQUEST")
                    .get { session -> HttpDestination.withCacheBuster(requireNotNull(session.getString("phoneResultUrl"))) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }.`is`("text/html"))
                    .check(css(".govuk-error-summary").notExists())
                    .check(
                        css(
                            ".govuk-summary-list__row:has(a[href*='/update-phone-number']) .govuk-summary-list__value",
                        ).transform { it.trim() }.`is`(phoneNumber),
                    )
                    .check(
                        css(".govuk-summary-list__row:has(a[href*='/update-email']) a", "href")
                            .transform { HttpDestination.resolve(it, config.baseUrl) }
                            .saveAs("emailUpdateEntry"),
                    ),
            ).exitHereIfFailed()
    }
}

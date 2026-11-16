package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import io.gatling.javaapi.http.HttpRequestActionBuilder
import uk.gov.communities.prsdb.webapp.controllers.LandlordController

object LandlordReadJourney {
    val requestNames = listOf("dashboard", "property records", "property details", "actions", "incomplete properties").map { "Browse: $it" }

    fun chain(
        config: BasicRunConfig,
        iterationData: BasicIterationData,
    ): ChainBuilder {
        val prefix = iterationData.requestNamePrefix
        val propertyLink = "#registered-properties a[href*='/landlord/property-details/']"
        return exec(
            read("$prefix${requestNames[0]}") { LandlordController.LANDLORD_DASHBOARD_URL }
                .check(css("a[href*='#registered-properties']", "href").find().saveAs("readRecordsUrl"))
                .check(css("a[href='/landlord/incomplete-properties']", "href").find().saveAs("readIncompleteUrl"))
                .check(
                    css("a[href='/landlord/compliance-actions'], a[href='/landlord/outstanding-actions']", "href")
                        .find().saveAs("readActionsUrl"),
                ),
        ).exitHereIfFailed()
            .exec(
                readLink("$prefix${requestNames[1]}", "readRecordsUrl", config)
                    .check(css(propertyLink, "href").find().saveAs("readPropertyUrl"))
                    .check(css(propertyLink).find().saveAs("readPropertyAddress")),
            ).exitHereIfFailed()
            .exec(
                readLink("$prefix${requestNames[2]}", "readPropertyUrl", config)
                    .check(
                        css("h1").transform(::addressCharacters).`is` { session ->
                            addressCharacters(requireNotNull(session.getString("readPropertyAddress")))
                        },
                    )
                    .check(css("#property-details .govuk-summary-list").exists())
                    .check(css("#compliance-information .govuk-summary-list").exists()),
            ).exitHereIfFailed()
            .exec(
                readLink("$prefix${requestNames[3]}", "readActionsUrl", config)
                    .check(css("h1").transform { it.trim() }.`in`("Compliance actions", "Outstanding actions")),
            ).exitHereIfFailed()
            .exec(
                readLink("$prefix${requestNames[4]}", "readIncompleteUrl", config)
                    .check(css("h1").transform { it.trim() }.`is`("Incomplete property details")),
            ).exitHereIfFailed()
    }

    private fun read(
        name: String,
        path: (Session) -> String,
    ): HttpRequestActionBuilder =
        http(name).get { session -> path(session) }.disableFollowRedirect()
            .check(status().`is`(200))
            .check(header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }.`is`("text/html"))
            .check(css("main h1").exists())
            .check(css(".govuk-error-summary").notExists())

    private fun readLink(
        name: String,
        sessionKey: String,
        config: BasicRunConfig,
    ): HttpRequestActionBuilder =
        read(name) { session ->
            HttpDestination.resolve(requireNotNull(session.getString(sessionKey)), config.baseUrl)
        }

    private fun addressCharacters(address: String) = address.filter { it.isLetterOrDigit() }
}

package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.controllers.LandlordDetailsController

object LandlordAddressUpdateJourney {
    private const val ADDRESS_ROW = ".govuk-summary-list__row:has(a[href*='/update-address/'])"

    fun requestNames(manual: Boolean): List<String> {
        val names =
            listOf("details", "start", "lookup form", "lookup", "selection form", "select") +
                (if (manual) listOf("manual form", "submit manual") else emptyList()) +
                listOf("saved details")
        return names.map { label(manual, it) }
    }

    private fun label(
        manual: Boolean,
        request: String,
    ): String = "Address update (${if (manual) "manual" else "selected"}): $request"

    fun chain(
        config: BasicRunConfig,
        iterationData: BasicIterationData,
        manual: Boolean,
    ): ChainBuilder {
        val prefix = iterationData.requestNamePrefix
        val firstLine = "${iterationData.profilePhoneNumber.takeLast(4)} Gatling Profile Road"
        val expectedAddress = if (manual) "$firstLine, Exampleton, EG1 2AA" else "1 PRSDB Performance Square, EG1 2AA"
        var chain =
            exec(
                http("$prefix${label(manual, "details")}")
                    .get(LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE)
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("$ADDRESS_ROW a", "href").saveAs("addressNextUrl")),
            ).exitHereIfFailed()
                .exec(
                    http("$prefix${label(manual, "start")}")
                        .get { session -> HttpDestination.resolve(requireNotNull(session.getString("addressNextUrl")), config.baseUrl) }
                        .disableFollowRedirect()
                        .check(status().`is`(302))
                        .check(header("Location").saveAs("addressNextUrl")),
                ).exitHereIfFailed()
                .exec(formPage(config, prefix, label(manual, "lookup form"), "input[name=postcode]"))
                .exec(submitForm(config, prefix, label(manual, "lookup"), mapOf("postcode" to "EG1 2AA", "houseNameOrNumber" to "1")))
                .exec(
                    formPage(
                        config,
                        prefix,
                        label(manual, "selection form"),
                        "input[name=address]",
                        expectedOption = if (manual) "MANUAL" else expectedAddress,
                    ),
                )
                .exec(
                    submitForm(
                        config,
                        prefix,
                        label(manual, "select"),
                        mapOf("address" to if (manual) "MANUAL" else expectedAddress),
                    ),
                )
        if (manual) {
            chain =
                chain.exec(formPage(config, prefix, label(manual, "manual form"), "input[name=addressLineOne]"))
                    .exec(
                        submitForm(
                            config,
                            prefix,
                            label(manual, "submit manual"),
                            mapOf("addressLineOne" to firstLine, "townOrCity" to "Exampleton", "postcode" to "EG1 2AA"),
                        ),
                    )
        }
        return chain.exec { session ->
            val resolved = HttpDestination.resolve(requireNotNull(session.getString("addressNextUrl")), config.baseUrl)
            require(java.net.URI.create(resolved).path == LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE)
            session.set("addressDetailsUrl", resolved)
        }.exitHereIfFailed()
            .exec(
                http("$prefix${label(manual, "saved details")}")
                    .get { session -> HttpDestination.withCacheBuster(requireNotNull(session.getString("addressDetailsUrl"))) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }.`is`("text/html"))
                    .check(css(".govuk-error-summary").notExists())
                    .check(css("$ADDRESS_ROW .govuk-summary-list__value").transform { it.trim() }.`is`(expectedAddress)),
            ).exitHereIfFailed()
    }

    private fun formPage(
        config: BasicRunConfig,
        prefix: String,
        name: String,
        requiredField: String,
        expectedOption: String? = null,
    ): ChainBuilder {
        var request =
            http("$prefix$name")
                .get("#{addressPageUrl}")
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(header("Content-Type").transform { it.substringBefore(';').trim().lowercase() }.`is`("text/html"))
                .check(css(".govuk-error-summary").notExists())
                .check(css("form[method=post]", "action").saveAs("addressFormAction"))
                .check(css("form input[name=_csrf]", "value").exists())
                .check(css("form input[type=hidden]", "name").findAll().saveAs("addressHiddenNames"))
                .check(css("form input[type=hidden]", "value").findAll().saveAs("addressHiddenValues"))
                .check(css(requiredField).exists())
        if (expectedOption != null) {
            request =
                request.check(
                    css("input[name=address]", "value").findAll().transform { it.contains(expectedOption) }.`is`(true),
                )
        }
        return exec { session ->
            session.set("addressPageUrl", HttpDestination.resolve(requireNotNull(session.getString("addressNextUrl")), config.baseUrl))
        }.exec(request).exitHereIfFailed()
    }

    private fun submitForm(
        config: BasicRunConfig,
        prefix: String,
        name: String,
        fields: Map<String, String>,
    ): ChainBuilder =
        exec(
            http("$prefix$name")
                .post { session ->
                    HttpDestination.resolve(
                        requireNotNull(session.getString("addressFormAction")),
                        config.baseUrl,
                        requireNotNull(session.getString("addressPageUrl")),
                    )
                }.disableFollowRedirect()
                .formParamMap { session ->
                    val names = session.getList<String>("addressHiddenNames")
                    val values = session.getList<String>("addressHiddenValues")
                    require(names.size == values.size) { "Address form has hidden fields with missing values" }
                    names.zip(values).toMap() + fields
                }.check(status().`is`(302))
                .check(header("Location").saveAs("addressNextUrl")),
        ).exitHereIfFailed()
}

package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.controllers.LandlordDetailsController
import uk.gov.communities.prsdb.webapp.controllers.UpdateOrganisationLandlordEmailController
import uk.gov.communities.prsdb.webapp.controllers.UpdateOrganisationLandlordNameController
import uk.gov.communities.prsdb.webapp.controllers.UpdateOrganisationLandlordPhoneNumberController
import uk.gov.communities.prsdb.webapp.controllers.UpdateOrganisationMainContactController
import uk.gov.communities.prsdb.webapp.controllers.UpdateOrganisationalLandlordAddressController
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import java.net.URI

object OrganisationProfileJourney {
    private data class Update(val route: String, val field: String, val value: String, val label: String) {
        val names get() = listOf("details", "start", "form", "submit", "saved details").map { "Organisation $field: $it" }
    }

    const val NAME = "Performance Organisation"
    private const val DETAILS = LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE
    private const val ADDRESS_ROUTE = UpdateOrganisationalLandlordAddressController.UPDATE_ORG_ADDRESS_ROUTE
    private const val MAIN_CONTACT_ROUTE = UpdateOrganisationMainContactController.UPDATE_ORG_MAIN_CONTACT_ROUTE
    private const val SELECTED_ADDRESS = "1 PRSDB Performance Square, EG1 2AA"
    private const val MANUAL_ADDRESS_LINE = "77 Gatling Organisation Road"
    private const val MANUAL_ADDRESS_TOWN = "Exampleton"
    private const val MANUAL_ADDRESS_POSTCODE = "EG1 2AA"
    private const val MAIN_CONTACT_NAME = "Performance Main Contact"
    private const val MAIN_CONTACT_EMAIL = "main-contact@example.com"
    private const val MAIN_CONTACT_PHONE = "02079460124"
    private val updates =
        listOf(
            Update(UpdateOrganisationLandlordNameController.UPDATE_ORG_NAME_ROUTE, "orgName", NAME, "Organisation name"),
            Update(
                UpdateOrganisationLandlordEmailController.UPDATE_ORG_EMAIL_ROUTE,
                "emailAddress",
                "organisation@example.com",
                "Organisation email",
            ),
            Update(
                UpdateOrganisationLandlordPhoneNumberController.UPDATE_ORG_PHONE_NUMBER_ROUTE,
                "phoneNumber",
                "02079460123",
                "Organisation phone",
            ),
        )

    private fun addressUpdateNames(manual: Boolean): List<String> =
        (
            listOf("details", "start", "lookup form", "lookup", "selection form", "select") +
                (if (manual) listOf("manual form", "submit manual") else emptyList()) +
                "saved details"
        ).map { "Organisation address (${if (manual) "manual" else "selected"}): $it" }

    private val mainContactUpdateNames =
        listOf("details", "start", "form", "submit", "saved details").map { "Organisation main contact: $it" }
    val requestNames =
        updates.flatMap { it.names } + addressUpdateNames(manual = false) +
            addressUpdateNames(manual = true) + mainContactUpdateNames

    fun chain(
        config: BasicRunConfig,
        fixture: OrganisationFixture,
        prefix: String,
    ): ChainBuilder =
        exec(
            updates.mapIndexed { index, update ->
                val names = update.names.map { prefix + it }
                val registration = RegistrationNumberDataModel(RegistrationNumberType.LANDLORD, fixture.registrationNumber).toString()
                exec(
                    http(names[0]).get { HttpDestination.withCacheBuster(config.baseUrl + DETAILS) }
                        .disableFollowRedirect()
                        .check(status().`is`(200))
                        .check(css("h1").transform { it.trim() }.`is`(if (index == 0) fixture.name else NAME))
                        .check(css(row("Landlord Registration Number")).transform { it.trim() }.`is`(registration))
                        .check(css("a[href^='${update.route}/']", "href").saveAs("orgNext")),
                ).exitHereIfFailed()
                    .exec(
                        http(names[1]).get { session -> scoped(requireNotNull(session.getString("orgNext")), config, update.route) }
                            .disableFollowRedirect()
                            .check(status().`is`(302))
                            .check(header("Location").transform { scoped(it, config, update.route) }.saveAs("orgPage")),
                    ).exitHereIfFailed()
                    .exec(
                        http(names[2]).get("#{orgPage}").disableFollowRedirect()
                            .check(status().`is`(200))
                            .check(css(".govuk-error-summary").notExists())
                            .check(css("form[method=post]", "action").saveAs("orgAction"))
                            .check(css("form input[name=_csrf]", "value").exists())
                            .check(css("form input[name=${update.field}]").exists())
                            .check(css("form input[type=hidden]", "name").findAll().saveAs("orgHiddenNames"))
                            .check(css("form input[type=hidden]", "value").findAll().saveAs("orgHiddenValues")),
                    ).exitHereIfFailed().pause(config.thinkTime)
                    .exec(
                        http(names[3]).post { session ->
                            scoped(
                                HttpDestination.resolve(
                                    requireNotNull(session.getString("orgAction")),
                                    config.baseUrl,
                                    requireNotNull(session.getString("orgPage")),
                                ),
                                config,
                                update.route,
                            )
                        }.disableFollowRedirect()
                            .formParamMap { session ->
                                val names = session.getList<String>("orgHiddenNames")
                                val values = session.getList<String>("orgHiddenValues")
                                require(names.size == values.size) { "Incomplete organisation form hidden fields" }
                                names.zip(values).toMap() + (update.field to update.value)
                            }
                            .check(status().`is`(302))
                            .check(
                                header("Location").transform { URI.create(HttpDestination.resolve(it, config.baseUrl)).path }.`is`(DETAILS),
                            ),
                    ).exitHereIfFailed()
                    .exec(
                        http(names[4]).get { HttpDestination.withCacheBuster(config.baseUrl + DETAILS) }
                            .disableFollowRedirect()
                            .check(status().`is`(200))
                            .check(css("h1").transform { it.trim() }.`is`(NAME))
                            .check(css(row("Landlord Registration Number")).transform { it.trim() }.`is`(registration))
                            .check(css(row(update.label)).transform { it.trim() }.`is`(update.value)),
                    ).exitHereIfFailed()
            },
        ).exec(addressUpdate(config, prefix, manual = false))
            .exec(addressUpdate(config, prefix, manual = true))
            .exec(mainContactUpdate(config, prefix))

    private fun addressUpdate(
        config: BasicRunConfig,
        prefix: String,
        manual: Boolean,
    ): ChainBuilder {
        val names = addressUpdateNames(manual).map { prefix + it }.iterator()
        val addressValue =
            if (manual) {
                "$MANUAL_ADDRESS_LINE $MANUAL_ADDRESS_TOWN $MANUAL_ADDRESS_POSTCODE"
            } else {
                SELECTED_ADDRESS
            }
        val lookupRoute = "$ADDRESS_ROUTE/lookup-address"
        val selectRoute = "$ADDRESS_ROUTE/select-address"
        val manualRoute = "$ADDRESS_ROUTE/manual-address"
        val selected = if (manual) "MANUAL" else SELECTED_ADDRESS
        var chain =
            exec(
                http(names.next())
                    .get { HttpDestination.withCacheBuster(config.baseUrl + DETAILS) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("h1").transform { it.trim() }.`is`(NAME))
                    .check(css(row("Organisation address")).exists())
                    .check(css("a[href^='$lookupRoute']", "href").saveAs("orgAddressNext")),
            ).exitHereIfFailed()
                .exec(
                    http(names.next()).get { session ->
                        scoped(requireNotNull(session.getString("orgAddressNext")), config, lookupRoute)
                    }.disableFollowRedirect()
                        .check(status().`is`(302))
                        .check(header("Location").transform { scoped(it, config, lookupRoute) }.saveAs("orgAddressNext")),
                ).exitHereIfFailed()
                .exec(
                    formPage(
                        config,
                        names.next(),
                        lookupRoute,
                        fields = setOf("postcode", "houseNameOrNumber"),
                        nextKey = "orgAddressNext",
                    ),
                ).exec(
                    submitForm(
                        config,
                        names.next(),
                        lookupRoute,
                        mapOf("postcode" to "EG1 2AA", "houseNameOrNumber" to "1"),
                        nextRoute = selectRoute,
                    ),
                ).exec(
                    formPage(
                        config,
                        names.next(),
                        selectRoute,
                        fields = setOf("address"),
                        expectedOption = selected,
                        nextKey = "orgAddressNext",
                    ),
                ).exec(
                    submitForm(
                        config,
                        names.next(),
                        selectRoute,
                        mapOf("address" to selected),
                        nextRoute = if (manual) manualRoute else DETAILS,
                    ),
                )
        if (manual) {
            chain =
                chain.exec(
                    formPage(
                        config,
                        names.next(),
                        manualRoute,
                        fields = setOf("addressLineOne", "townOrCity", "postcode"),
                        nextKey = "orgAddressNext",
                    ),
                ).exec(
                    submitForm(
                        config,
                        names.next(),
                        manualRoute,
                        mapOf(
                            "addressLineOne" to MANUAL_ADDRESS_LINE,
                            "townOrCity" to MANUAL_ADDRESS_TOWN,
                            "postcode" to MANUAL_ADDRESS_POSTCODE,
                        ),
                        nextRoute = DETAILS,
                    ),
                )
        } else {
            // The selected-address path has no additional manual form request.
            chain =
                chain.exec(
                    exec { session ->
                        val next = requireNotNull(session.getString("orgAddressNext"))
                        require(URI.create(next).path == DETAILS) { "Selected organisation address did not return to details" }
                        session
                    },
                )
        }
        return chain.exec(
            http(names.next())
                .get { HttpDestination.withCacheBuster(config.baseUrl + DETAILS) }
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(css("h1").transform { it.trim() }.`is`(NAME))
                .check(css(row("Organisation address")).transform { compactAddress(it) }.`is`(compactAddress(addressValue))),
        ).exitHereIfFailed()
    }

    private fun mainContactUpdate(
        config: BasicRunConfig,
        prefix: String,
    ): ChainBuilder {
        val names = mainContactUpdateNames.map { prefix + it }.iterator()
        val route = "$MAIN_CONTACT_ROUTE/organisation-main-contact"
        return exec(
            http(names.next()).get { HttpDestination.withCacheBuster(config.baseUrl + DETAILS) }
                .disableFollowRedirect()
                .check(status().`is`(200))
                .check(css("h1").transform { it.trim() }.`is`(NAME))
                .check(css("a[href^='$route']", "href").saveAs("orgContactNext")),
        ).exitHereIfFailed()
            .exec(
                http(names.next()).get { session ->
                    scoped(requireNotNull(session.getString("orgContactNext")), config, MAIN_CONTACT_ROUTE)
                }.disableFollowRedirect()
                    .check(status().`is`(302))
                    .check(header("Location").transform { scoped(it, config, MAIN_CONTACT_ROUTE) }.saveAs("orgContactNext")),
            ).exitHereIfFailed()
            .exec(
                formPage(
                    config,
                    names.next(),
                    MAIN_CONTACT_ROUTE,
                    fields = setOf("name", "emailAddress", "phoneNumber"),
                    pageKey = "orgContactPage",
                    nextKey = "orgContactNext",
                    actionKey = "orgContactAction",
                    hiddenNamesKey = "orgContactHiddenNames",
                    hiddenValuesKey = "orgContactHiddenValues",
                ),
            ).exec(
                submitForm(
                    config,
                    names.next(),
                    MAIN_CONTACT_ROUTE,
                    mapOf("name" to MAIN_CONTACT_NAME, "emailAddress" to MAIN_CONTACT_EMAIL, "phoneNumber" to MAIN_CONTACT_PHONE),
                    nextRoute = DETAILS,
                    pageKey = "orgContactPage",
                    nextKey = "orgContactNext",
                    actionKey = "orgContactAction",
                    hiddenNamesKey = "orgContactHiddenNames",
                    hiddenValuesKey = "orgContactHiddenValues",
                ),
            ).exec(
                http(names.next()).get { HttpDestination.withCacheBuster(config.baseUrl + DETAILS) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("h1").transform { it.trim() }.`is`(NAME))
                    .check(css("#organisation-contacts ${row("Name")}").transform { it.trim() }.`is`(MAIN_CONTACT_NAME))
                    .check(css("#organisation-contacts ${row("Email")}").transform { it.trim() }.`is`(MAIN_CONTACT_EMAIL))
                    .check(css("#organisation-contacts ${row("Phone number")}").transform { it.trim() }.`is`(MAIN_CONTACT_PHONE)),
            ).exitHereIfFailed()
    }

    private fun formPage(
        config: BasicRunConfig,
        name: String,
        route: String,
        fields: Set<String>,
        expectedOption: String? = null,
        pageKey: String = "orgAddressPage",
        nextKey: String = "orgAddressNext",
        actionKey: String = "orgAddressAction",
        hiddenNamesKey: String = "orgAddressHiddenNames",
        hiddenValuesKey: String = "orgAddressHiddenValues",
    ): ChainBuilder {
        var request =
            http(name).get("#{$pageKey}").disableFollowRedirect()
                .check(status().`is`(200))
                .check(css(".govuk-error-summary").notExists())
                .check(css("form[method=post]", "action").saveAs(actionKey))
                .check(css("form input[name=_csrf]", "value").exists())
                .check(css("form input[type=hidden]", "name").findAll().saveAs(hiddenNamesKey))
                .check(css("form input[type=hidden]", "value").findAll().saveAs(hiddenValuesKey))
        for (field in fields) request = request.check(css("form input[name=$field]").exists())
        if (expectedOption != null) {
            request = request.check(css("form input[name=address]", "value").findAll().transform { it.contains(expectedOption) }.`is`(true))
        }
        return exec { session ->
            val nextUrl = HttpDestination.resolve(requireNotNull(session.getString(nextKey)), config.baseUrl)
            session.set(pageKey, scoped(nextUrl, config, route))
        }.exec(request).exitHereIfFailed().pause(config.thinkTime)
    }

    private fun submitForm(
        config: BasicRunConfig,
        name: String,
        route: String,
        fields: Map<String, String>,
        nextRoute: String,
        pageKey: String = "orgAddressPage",
        nextKey: String = "orgAddressNext",
        actionKey: String = "orgAddressAction",
        hiddenNamesKey: String = "orgAddressHiddenNames",
        hiddenValuesKey: String = "orgAddressHiddenValues",
    ): ChainBuilder =
        exec(
            http(name).post { session ->
                scoped(
                    HttpDestination.resolve(
                        requireNotNull(session.getString(actionKey)),
                        config.baseUrl,
                        requireNotNull(session.getString(pageKey)),
                    ),
                    config,
                    route,
                )
            }.disableFollowRedirect()
                .formParamMap { session ->
                    val hiddenNames = session.getList<String>(hiddenNamesKey)
                    val hiddenValues = session.getList<String>(hiddenValuesKey)
                    require(hiddenNames.size == hiddenValues.size) { "Incomplete organisation form hidden fields" }
                    hiddenNames.zip(hiddenValues).toMap() + fields
                }
                .check(status().`is`(302))
                .check(
                    header("Location").transform {
                        val resolved = HttpDestination.resolve(it, config.baseUrl)
                        val actualPath = URI.create(resolved).path
                        require(actualPath == nextRoute) {
                            "Organisation update redirected to $actualPath instead of $nextRoute"
                        }
                        if (nextRoute != DETAILS) scoped(resolved, config, nextRoute) else resolved
                    }.saveAs(nextKey),
                ),
        ).exitHereIfFailed()

    // Organisation addresses render as one paragraph per line, so compare without separators.
    private fun compactAddress(value: String) = value.replace(Regex("[\\s,]+"), "")

    private fun row(label: String) = ".govuk-summary-list__key:contains('$label') + .govuk-summary-list__value"

    private fun scoped(
        location: String,
        config: BasicRunConfig,
        route: String,
    ): String {
        val resolved = HttpDestination.resolve(location, config.baseUrl)
        val path = URI.create(resolved).path
        require(path == route || path.startsWith("$route/")) { "Organisation update left its expected journey" }
        return resolved
    }
}

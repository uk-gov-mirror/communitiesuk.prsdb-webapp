package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import java.net.URI

object CompletedPropertyUpdateJourney {
    internal data class Update(
        val route: String,
        val forms: List<Map<String, String>>,
        val expected: Map<String, String>,
        val checkAnswers: Boolean = forms.size > 1,
        val checkAnswersExpected: Map<String, String> = expected,
        val label: String = route,
        val captureAccountEmail: Boolean = false,
    ) {
        val names: List<String>
            get() =
                (
                    listOf("details", "start") + forms.indices.flatMap { listOf("form ${it + 1}", "submit ${it + 1}") } +
                        (if (checkAnswers) listOf("check answers", "confirm") else emptyList()) + "saved details"
                )
                    .map { "Property $label: $it" }
    }

    private val updates =
        listOf(
            Update(
                "update-ownership-type",
                listOf(mapOf("ownershipType" to "LEASEHOLD")),
                mapOf("How do you own this property?" to "Leasehold"),
            ),
            Update("update-bedrooms", listOf(mapOf("numberOfBedrooms" to "4")), mapOf("Number of bedrooms" to "4")),
            Update(
                "update-furnished-status",
                listOf(mapOf("furnishedStatus" to "PART_FURNISHED")),
                mapOf("Furniture provided" to "Partly furnished"),
            ),
            Update(
                "update-households-and-tenants",
                listOf(mapOf("numberOfHouseholds" to "2"), mapOf("numberOfPeople" to "3")),
                mapOf("Number of households" to "2", "Number of tenants" to "3"),
            ),
            Update(
                "update-rent-frequency-and-amount",
                listOf(mapOf("rentFrequency" to "WEEKLY"), mapOf("rentAmount" to "200")),
                mapOf("When rent is paid" to "Weekly", "Rent amount" to "£200.00"),
            ),
            Update(
                "update-rent-includes-bills",
                listOf(mapOf("rentIncludesBills" to "true"), mapOf("billsIncluded" to "WATER")),
                mapOf("Rent includes bills" to "Yes", "Which bills are included" to "Water"),
            ),
            Update(
                "update-correspondence-email",
                listOf(mapOf("correspondenceEmailOption" to "DIFFERENT_EMAIL", "differentEmailAddress" to "property@example.com")),
                mapOf("Contact email address" to "property@example.com"),
                checkAnswers = true,
                checkAnswersExpected = mapOf("Email address" to "property@example.com"),
            ),
        ) +
            Update(
                "update-licensing",
                listOf(mapOf("licensingType" to "SELECTIVE_LICENCE"), mapOf("licenceNumber" to "PERF-123")),
                mapOf("Licensing type" to "Selective licence", "Licensing number" to "PERF-123"),
                label = "licensing SELECTIVE_LICENCE",
            )

    val requestNames: List<String> = updates.flatMap { it.names }

    fun chain(
        config: BasicRunConfig,
        fixture: CompletedPropertyFixture,
        prefix: String,
    ): ChainBuilder = exec(updates.map { update(config, fixture, prefix, it) })

    internal fun update(
        config: BasicRunConfig,
        fixture: CompletedPropertyFixture,
        prefix: String,
        update: Update,
    ): ChainBuilder {
        val details =
            PropertyDetailsController.LANDLORD_PROPERTY_DETAILS_ROUTE.replace(
                "{propertyOwnershipId}",
                fixture.propertyOwnershipId.toString(),
            )
        val route = "$details/${update.route}"
        val names = update.names.map { prefix + it }.iterator()
        var chain =
            exec(
                http(names.next())
                    .get { HttpDestination.withCacheBuster(config.baseUrl + details) }
                    .disableFollowRedirect()
                    .check(status().`is`(200))
                    .check(css("h1").transform(::addressCharacters).`is`(addressCharacters(fixture.address)))
                    .check(css("a[href*='$route']", "href").saveAs("propertyNext")),
            ).exitHereIfFailed()
                .exec(
                    http(names.next())
                        .get { session -> scoped(requireNotNull(session.getString("propertyNext")), config, route) }
                        .disableFollowRedirect()
                        .check(status().`is`(302))
                        .check(header("Location").transform { scoped(it, config, route) }.saveAs("propertyNext")),
                ).exitHereIfFailed()
        for (fields in update.forms) {
            chain =
                chain.exec(form(config, route, names.next(), fields, captureAccountEmail = update.captureAccountEmail))
                    .exec(submit(config, route, names.next(), fields))
        }
        if (update.checkAnswers) {
            chain =
                chain.exec(form(config, route, names.next(), emptyMap(), update.checkAnswersExpected))
                    .exec(submit(config, route, names.next(), emptyMap()))
        }
        var saved =
            http(names.next())
                .get { session ->
                    val result = scoped(requireNotNull(session.getString("propertyNext")), config, route)
                    require(URI.create(result).path == details) { "Property update did not return to the owned property" }
                    HttpDestination.withCacheBuster(result)
                }.disableFollowRedirect()
                .check(status().`is`(200))
                .check(css("h1").transform(::addressCharacters).`is`(addressCharacters(fixture.address)))
                .check(css(".govuk-error-summary").notExists())
        for ((label, value) in update.expected) {
            saved =
                saved.check(
                    css(row(label)).transform { normalise(label, it) }.`is` { session ->
                        normalise(label, if (value == ACCOUNT_EMAIL) requireNotNull(session.getString("propertyAccountEmail")) else value)
                    },
                )
        }
        return chain.exec(saved).exitHereIfFailed()
    }

    private fun row(label: String) = ".govuk-summary-list__key:contains('$label') + .govuk-summary-list__value"

    private fun addressCharacters(address: String) = address.filter { it.isLetterOrDigit() }

    private fun normalise(
        label: String,
        value: String,
    ) = if (label.contains("address", ignoreCase = true) && !label.contains("email", ignoreCase = true)) {
        addressCharacters(value)
    } else {
        value.trim()
    }

    internal const val ACCOUNT_EMAIL = "@account-email"

    private fun scoped(
        location: String,
        config: BasicRunConfig,
        route: String,
    ): String {
        val resolved = HttpDestination.resolve(location, config.baseUrl)
        val path = URI.create(resolved).path
        require(path == route || path.startsWith("$route/") || path == route.substringBeforeLast('/')) {
            "Property update left its allocated property/route: $path"
        }
        return resolved
    }

    private fun form(
        config: BasicRunConfig,
        route: String,
        name: String,
        fields: Map<String, String>,
        expected: Map<String, String> = emptyMap(),
        captureAccountEmail: Boolean = false,
    ): ChainBuilder {
        var request =
            http(name).get("#{propertyPage}").disableFollowRedirect()
                .check(status().`is`(200))
                .check(css(".govuk-error-summary").notExists())
                .check(css("form[method=post]", "action").saveAs("propertyAction"))
                .check(css("form input[name=_csrf]", "value").exists())
                .check(css("form input[type=hidden]", "name").findAll().saveAs("propertyHiddenNames"))
                .check(css("form input[type=hidden]", "value").findAll().saveAs("propertyHiddenValues"))
        for ((field, value) in fields) {
            request =
                if (field == "billsIncluded") {
                    request.check(css("form input[name^='billsIncluded['][value=$value]", "name").saveAs("propertyBillsField"))
                } else {
                    request.check(css("form [name=$field]").exists())
                }
        }
        if (captureAccountEmail) {
            request =
                request.check(
                    css(".govuk-hint").findAll().transform { hints ->
                        hints.map { it.trim() }.single { it.contains('@') }
                    }.saveAs("propertyAccountEmail"),
                )
        }
        for ((label, value) in expected) {
            request =
                request.check(
                    css(row(label)).transform { normalise(label, it) }.`is` { session ->
                        normalise(label, if (value == ACCOUNT_EMAIL) requireNotNull(session.getString("propertyAccountEmail")) else value)
                    },
                )
        }
        return exec { session ->
            session.set("propertyPage", scoped(requireNotNull(session.getString("propertyNext")), config, route))
        }.exec(request).exitHereIfFailed().pause(config.thinkTime)
    }

    private fun submit(
        config: BasicRunConfig,
        route: String,
        name: String,
        fields: Map<String, String>,
    ): ChainBuilder =
        exec(
            http(name).post { session ->
                scoped(
                    HttpDestination.resolve(
                        requireNotNull(session.getString("propertyAction")),
                        config.baseUrl,
                        requireNotNull(session.getString("propertyPage")),
                    ),
                    config,
                    route,
                )
            }.disableFollowRedirect()
                .formParamMap { session ->
                    val names = session.getList<String>("propertyHiddenNames")
                    val values = session.getList<String>("propertyHiddenValues")
                    require(names.size == values.size) { "Incomplete property form hidden values" }
                    val submitted =
                        if ("billsIncluded" in fields) {
                            val name = requireNotNull(session.getString("propertyBillsField"))
                            fields - "billsIncluded" + (name to fields.getValue("billsIncluded"))
                        } else {
                            fields
                        }
                    names.zip(values).toMap() + submitted
                }.check(status().`is`(302))
                .check(header("Location").transform { scoped(it, config, route) }.saveAs("propertyNext")),
        ).exitHereIfFailed()
}

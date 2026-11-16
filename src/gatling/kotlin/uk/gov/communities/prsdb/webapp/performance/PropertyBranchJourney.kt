package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.exec
import uk.gov.communities.prsdb.webapp.performance.CompletedPropertyUpdateJourney.Update

object PropertyBranchJourney {
    private val address = "1 PRSDB Performance Square, EG1 2AA"
    private val manualAddress = "24 Gatling Correspondence Road, Exampleton, EG1 2AA"
    private val updates =
        listOf(
            Update(
                "update-correspondence-address",
                listOf(mapOf("postcode" to "EG1 2AA", "houseNameOrNumber" to "1"), mapOf("address" to address)),
                mapOf("Contact address" to address),
                checkAnswersExpected = mapOf("Postal address" to address),
                label = "selected correspondence address",
            ),
            Update(
                "update-correspondence-address",
                listOf(
                    mapOf("postcode" to "EG1 2AA", "houseNameOrNumber" to "1"),
                    mapOf("address" to "MANUAL"),
                    mapOf("addressLineOne" to "24 Gatling Correspondence Road", "townOrCity" to "Exampleton", "postcode" to "EG1 2AA"),
                ),
                mapOf("Contact address" to manualAddress),
                checkAnswersExpected = mapOf("Postal address" to manualAddress),
                label = "manual correspondence address",
            ),
            Update(
                "update-correspondence-email",
                listOf(mapOf("correspondenceEmailOption" to "ACCOUNT_EMAIL")),
                mapOf("Contact email address" to CompletedPropertyUpdateJourney.ACCOUNT_EMAIL),
                checkAnswers = true,
                checkAnswersExpected = mapOf("Email address" to CompletedPropertyUpdateJourney.ACCOUNT_EMAIL),
                label = "account correspondence email",
                captureAccountEmail = true,
            ),
            Update(
                "update-rent-includes-bills",
                listOf(
                    mapOf("rentIncludesBills" to "true"),
                    mapOf("billsIncluded" to "SOMETHING_ELSE", "customBillsIncluded" to "Window cleaning"),
                ),
                mapOf("Rent includes bills" to "Yes", "Which bills are included" to "Window cleaning"),
                label = "custom bills",
            ),
            Update(
                "update-rent-includes-bills",
                listOf(mapOf("rentIncludesBills" to "false")),
                mapOf("Rent includes bills" to "No"),
                checkAnswers = true,
                label = "bills excluded",
            ),
            Update(
                "update-occupancy",
                listOf(mapOf("occupied" to "false")),
                mapOf("Is this property occupied by tenants?" to "No"),
                checkAnswers = true,
                checkAnswersExpected = mapOf("Occupied by tenants" to "No"),
                label = "vacant",
            ),
            Update(
                "update-occupancy",
                listOf(mapOf("occupied" to "true")),
                mapOf("Is this property occupied by tenants?" to "Yes"),
                checkAnswers = true,
                checkAnswersExpected = mapOf("Occupied by tenants" to "Yes"),
                label = "occupied",
            ),
            Update(
                "update-tenancy-details",
                listOf(
                    mapOf("numberOfHouseholds" to "2"),
                    mapOf("numberOfPeople" to "3"),
                    mapOf("rentIncludesBills" to "false"),
                    mapOf("furnishedStatus" to "PART_FURNISHED"),
                    mapOf("rentFrequency" to "WEEKLY"),
                    mapOf("rentAmount" to "200"),
                ),
                mapOf(
                    "Number of households" to "2",
                    "Number of tenants" to "3",
                    "Rent includes bills" to "No",
                    "Furniture provided" to "Partly furnished",
                    "When rent is paid" to "Weekly",
                    "Rent amount" to "£200.00",
                ),
                label = "tenancy provide-later completion",
            ),
        )
    val requestNames = updates.flatMap { it.names }

    fun chain(
        config: BasicRunConfig,
        fixture: CompletedPropertyFixture,
        prefix: String,
    ) = exec(updates.map { CompletedPropertyUpdateJourney.update(config, fixture, prefix, it) })
}

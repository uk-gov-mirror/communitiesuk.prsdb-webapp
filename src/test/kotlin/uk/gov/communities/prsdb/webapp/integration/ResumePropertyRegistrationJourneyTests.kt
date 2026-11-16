package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.repository.AddressRepository
import uk.gov.communities.prsdb.webapp.helpers.NftDataFaker
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BackLink
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordIncompletePropertiesPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LookupAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfBedroomsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.PropertyTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SelectAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.TaskListPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.journeys.JourneyIdProvider
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ResumePropertyRegistrationJourneyTests :
    IntegrationTestWithMutableData("data-mockuser-landlord-with-one-incomplete-property.sql") {
    @Test
    fun `generated incomplete registration resumes and saves progress with its selected address intact`(
        page: Page,
        @Autowired jdbcTemplate: JdbcTemplate,
        @Autowired addressRepository: AddressRepository,
    ) {
        val existingAddress = addressRepository.findById(1).orElseThrow()
        val address =
            Address(
                AddressDataModel.fromAddress(existingAddress).copy(buildingNumber = "1"),
                existingAddress.localCouncil,
            )
        jdbcTemplate.update(
            "UPDATE saved_journey_state SET serialized_state = ? WHERE id = 1",
            NftDataFaker.generateIncompletePropertyJourneyState(address),
        )

        navigator.goToLandlordIncompleteProperties().firstSummaryCard.continueLink.clickAndWait()
        assertPageIs(page, TaskListPagePropertyRegistration::class)
        page.navigate(
            "http://localhost:$port${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}" +
                "/select-address?${JourneyIdProvider.PARAMETER_NAME}=1",
        )
        assertPageIs(page, SelectAddressFormPagePropertyRegistration::class).selectAddressAndSubmit(address.singleLineAddress)
        assertPageIs(page, PropertyTypeFormPagePropertyRegistration::class).submitPropertyType(PropertyType.DETACHED_HOUSE)
        assertPageIs(page, NumberOfBedroomsFormPagePropertyRegistration::class).submitNumOfBedrooms(2)

        val savedState =
            Json.parseToJsonElement(
                jdbcTemplate.queryForObject("SELECT serialized_state FROM saved_journey_state WHERE id = 1", String::class.java)!!,
            ).jsonObject
        assertEquals(
            listOf(AddressDataModel.fromAddress(address)),
            Json.decodeFromString<List<AddressDataModel>>(savedState.getValue("cachedAddresses").jsonPrimitive.content),
        )
        assertEquals(
            "2",
            savedState.getValue("journeyData").jsonObject.getValue("number-of-bedrooms").jsonObject
                .getValue("numberOfBedrooms").jsonPrimitive.content,
        )
    }

    @Test
    fun `resuming an incomplete property registration via the continue link restores saved journey state`(page: Page) {
        val incompletePropertiesPage = navigator.goToLandlordIncompleteProperties()
        incompletePropertiesPage.firstSummaryCard.continueLink.clickAndWait()

        val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

        assertFalse(
            taskListPage.taskHasStatus("Property details", "Not\u00A0started"),
            "Property details task should not be 'Not started' after restoring saved journey state",
        )
    }

    @Test
    fun `task list back link navigates to incomplete properties page after resuming registration`(page: Page) {
        val incompletePropertiesPage = navigator.goToLandlordIncompleteProperties()
        incompletePropertiesPage.firstSummaryCard.continueLink.clickAndWait()
        val taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

        taskListPage.backLink.clickAndWait()
        assertPageIs(page, LandlordIncompletePropertiesPage::class)
    }

    @Test
    fun `task list back link navigates to incomplete properties page after resuming registration and returning from a task`(page: Page) {
        val incompletePropertiesPage = navigator.goToLandlordIncompleteProperties()
        incompletePropertiesPage.firstSummaryCard.continueLink.clickAndWait()
        var taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

        taskListPage.clickAboutYourPropertyTaskWithName("Property details")
        assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)

        val backLink = BackLink.default(page)
        backLink.clickAndWait()
        taskListPage = assertPageIs(page, TaskListPagePropertyRegistration::class)

        taskListPage.backLink.clickAndWait()
        assertPageIs(page, LandlordIncompletePropertiesPage::class)
    }
}

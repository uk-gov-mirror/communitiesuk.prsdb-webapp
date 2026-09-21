package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.MANUAL_ADDRESS_CHOSEN
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BackLink
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceEmailFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceLookupAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceManualAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceSelectAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CorrespondenceEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.CorrespondenceAddressTask
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.CorrespondenceEmailFormModel
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PropertyRegistrationCorrespondenceCyaSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    private val accountEmail = "alex.surname@example.com"
    private val differentEmail = "council.contact@example.com"
    private val originalPostalAddressLines = listOf("1 Fictional Road", "FA1 1AA")
    private val propertyAddressLines = listOf("1 Street Address", "City", "AB1 2CD")

    @BeforeEach
    fun enableFeatureFlags() {
        featureFlagManager.enableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
        featureFlagManager.enableFeature(CORRESPONDENCE_ADDRESS)
        featureFlagManager.disableFeature(DELEGATE_TO_LETTING_AGENT)
    }

    @Test
    fun `contact details appear between ownership and occupancy with exact rows and independent change links`(page: Page) {
        val checkAnswersPage = goToCheckAnswers(page)

        assertEquals(
            listOf(
                "About your property",
                "Property details",
                "Ownership and landlords",
                "Who the council should contact",
                "Tell us if your property\u2019s occupied",
                "How your property\u2019s rented out",
            ),
            checkAnswersPage.restructuredSectionHeadings.take(6),
        )
        assertThat(checkAnswersPage.correspondenceRowKeys).hasText(arrayOf("Email address", "Postal address"))
        assertContactDetails(checkAnswersPage)

        val emailChangeLink = checkAnswersPage.summaryList.correspondenceEmailRow.actions.firstActionLink
        val postalChangeLink = checkAnswersPage.summaryList.correspondencePostalAddressRow.actions.firstActionLink
        BaseComponent.assertThat(emailChangeLink).hasText("Change")
        BaseComponent.assertThat(postalChangeLink).hasText("Change")
        BaseComponent.assertThat(emailChangeLink).isVisible()
        BaseComponent.assertThat(postalChangeLink).isVisible()

        val emailChangeUrl = URI(checkNotNull(emailChangeLink.locator.getAttribute("href")))
        val postalChangeUrl = URI(checkNotNull(postalChangeLink.locator.getAttribute("href")))
        val registrationRoute = RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE
        assertEquals("$registrationRoute/${CorrespondenceEmailStep.ROUTE_SEGMENT}", emailChangeUrl.path)
        assertEquals(
            "$registrationRoute/${CorrespondenceAddressTask.ROUTE_SEGMENT}/${LookupAddressStep.ROUTE_SEGMENT}",
            postalChangeUrl.path,
        )
        assertTrue(checkNotNull(emailChangeUrl.query).matches(Regex("^journeyId=.+$")))
        assertTrue(checkNotNull(postalChangeUrl.query).matches(Regex("^journeyId=.+$")))
        assertNotEquals(emailChangeUrl.query, postalChangeUrl.query)
        assertNotEquals(URI(page.url()).query, emailChangeUrl.query)
        assertNotEquals(URI(page.url()).query, postalChangeUrl.query)
    }

    @Test
    fun `an occupied registration with landlord provided details shows the selected different email`(page: Page) {
        featureFlagManager.enableFeature(DELEGATE_TO_LETTING_AGENT)
        val checkAnswersPage =
            goToCheckAnswers(
                page,
                PropertyStateSessionBuilder
                    .beforePropertyRegistrationCheckAnswersOccupied()
                    .withBedrooms()
                    .withCompletedCorrespondence()
                    .withSubmittedValue(
                        CorrespondenceEmailStep.ROUTE_SEGMENT,
                        CorrespondenceEmailFormModel().apply {
                            whichEmail = CorrespondenceEmailOption.DIFFERENT_EMAIL
                            differentEmailAddress = differentEmail
                        },
                    ),
            )

        assertContactDetails(checkAnswersPage, email = differentEmail)
        assertThat(checkAnswersPage.summaryList.occupancyQuestionRow.value).hasText("Yes")
        assertThat(checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.value).hasText("I will provide these details")
    }

    @Test
    fun `the account email is shown instead of stale different email data when account email is selected`(page: Page) {
        val checkAnswersPage =
            goToCheckAnswers(
                page,
                completedState().withSubmittedValue(
                    CorrespondenceEmailStep.ROUTE_SEGMENT,
                    CorrespondenceEmailFormModel().apply {
                        whichEmail = CorrespondenceEmailOption.ACCOUNT_EMAIL
                        differentEmailAddress = differentEmail
                    },
                ),
            )

        assertContactDetails(checkAnswersPage)
    }

    @Test
    fun `email changes are prefilled and return directly to CYA without changing either address`(page: Page) {
        var checkAnswersPage = goToCheckAnswers(page)
        val checkAnswersUrl = page.url()
        checkAnswersPage.summaryList.correspondenceEmailRow.clickFirstActionLinkAndWait()
        var emailPage = assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)

        assertEquals(CorrespondenceEmailOption.ACCOUNT_EMAIL.name, emailPage.form.whichEmailRadios.selectedValue)
        BaseComponent.assertThat(emailPage.form.whichEmailRadios).containsText(accountEmail)
        emailPage.submitDifferentEmail(differentEmail)

        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(page).hasURL(checkAnswersUrl)
        assertContactDetails(checkAnswersPage, email = differentEmail)

        checkAnswersPage.summaryList.correspondenceEmailRow.clickFirstActionLinkAndWait()
        emailPage = assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
        assertEquals(CorrespondenceEmailOption.DIFFERENT_EMAIL.name, emailPage.form.whichEmailRadios.selectedValue)
        BaseComponent.assertThat(emailPage.form.differentEmailInput).hasValue(differentEmail)
        emailPage.submitAccountEmail()

        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(page).hasURL(checkAnswersUrl)
        assertContactDetails(checkAnswersPage)
    }

    @Test
    fun `email Back does not apply an unsubmitted email or selection`(page: Page) {
        var checkAnswersPage =
            goToCheckAnswers(
                page,
                completedState().withSubmittedValue(
                    CorrespondenceEmailStep.ROUTE_SEGMENT,
                    CorrespondenceEmailFormModel().apply {
                        whichEmail = CorrespondenceEmailOption.DIFFERENT_EMAIL
                        differentEmailAddress = differentEmail
                    },
                ),
            )
        val checkAnswersUrl = page.url()
        checkAnswersPage.summaryList.correspondenceEmailRow.clickFirstActionLinkAndWait()
        val emailPage = assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
        BaseComponent.assertThat(emailPage.form.differentEmailInput).hasValue(differentEmail)
        emailPage.form.differentEmailInput.fill("unsubmitted@example.com")
        emailPage.form.whichEmailRadios.selectValue(CorrespondenceEmailOption.ACCOUNT_EMAIL)

        BackLink.default(page).clickAndWait()

        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(page).hasURL(checkAnswersUrl)
        assertContactDetails(checkAnswersPage, email = differentEmail)
    }

    @Test
    fun `invalid email submission stays in the child journey and Back preserves the CYA answers`(page: Page) {
        var checkAnswersPage = goToCheckAnswers(page)
        val checkAnswersUrl = page.url()
        checkAnswersPage.summaryList.correspondenceEmailRow.clickFirstActionLinkAndWait()
        var emailPage = assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)

        emailPage.submitDifferentEmail("not-an-email")

        emailPage = assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
        BaseComponent.assertThat(emailPage.errorSummary).containsText("Enter an email address in the correct format")
        BaseComponent.assertThat(emailPage.form.differentEmailInput).hasValue("not-an-email")
        BackLink.default(page).clickAndWait()

        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(page).hasURL(checkAnswersUrl)
        assertContactDetails(checkAnswersPage)
    }

    @Test
    fun `selecting a new correspondence lookup address returns directly to CYA and only changes the postal address`(page: Page) {
        var checkAnswersPage =
            goToCheckAnswers(
                page,
                completedState().withSubmittedValue(
                    CorrespondenceEmailStep.ROUTE_SEGMENT,
                    CorrespondenceEmailFormModel().apply {
                        whichEmail = CorrespondenceEmailOption.DIFFERENT_EMAIL
                        differentEmailAddress = differentEmail
                    },
                ),
            )
        val checkAnswersUrl = page.url()
        checkAnswersPage.summaryList.correspondencePostalAddressRow.clickFirstActionLinkAndWait()
        val lookupPage = assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyRegistration::class)
        BaseComponent.assertThat(lookupPage.form.postcodeInput).hasValue("FA1 1AA")
        BaseComponent.assertThat(lookupPage.form.houseNameOrNumberInput).hasValue("1")

        lookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AB", "2")
        val selectPage = assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyRegistration::class)
        selectPage.selectAddressAndSubmit("2 Fake Way")

        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(page).hasURL(checkAnswersUrl)
        assertContactDetails(checkAnswersPage, email = differentEmail, postalAddressLines = listOf("2 Fake Way", "FA1 1AB"))
    }

    @Test
    fun `manual correspondence address completion returns directly to CYA with every address line`(page: Page) {
        var checkAnswersPage = goToCheckAnswers(page)
        val checkAnswersUrl = page.url()
        checkAnswersPage.summaryList.correspondencePostalAddressRow.clickFirstActionLinkAndWait()
        val lookupPage = assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyRegistration::class)
        lookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AB", "2")
        val selectPage = assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyRegistration::class)
        selectPage.selectAddressAndSubmit(MANUAL_ADDRESS_CHOSEN)
        val manualPage = assertPageIs(page, CorrespondenceManualAddressFormPagePropertyRegistration::class)

        manualPage.submitAddress(
            addressLineOne = "Flat 4",
            addressLineTwo = "12 Test Road",
            townOrCity = "Leeds",
            county = "West Yorkshire",
            postcode = "LS1 1AA",
        )

        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(page).hasURL(checkAnswersUrl)
        assertContactDetails(
            checkAnswersPage,
            postalAddressLines = listOf("Flat 4", "12 Test Road", "Leeds", "West Yorkshire", "LS1 1AA"),
        )
    }

    @Test
    fun `postal lookup Back does not apply unsubmitted search details`(page: Page) {
        var checkAnswersPage = goToCheckAnswers(page)
        val checkAnswersUrl = page.url()
        checkAnswersPage.summaryList.correspondencePostalAddressRow.clickFirstActionLinkAndWait()
        var lookupPage = assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyRegistration::class)
        lookupPage.form.postcodeInput.fill("FA1 1AB")
        lookupPage.form.houseNameOrNumberInput.fill("2")

        BackLink.default(page).clickAndWait()

        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(page).hasURL(checkAnswersUrl)
        assertContactDetails(checkAnswersPage)

        checkAnswersPage.summaryList.correspondencePostalAddressRow.clickFirstActionLinkAndWait()
        lookupPage = assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyRegistration::class)
        BaseComponent.assertThat(lookupPage.form.postcodeInput).hasValue("FA1 1AA")
        BaseComponent.assertThat(lookupPage.form.houseNameOrNumberInput).hasValue("1")
    }

    @Test
    fun `abandoning a postal child journey after lookup and manual selection preserves the original address`(page: Page) {
        var checkAnswersPage = goToCheckAnswers(page)
        val checkAnswersUrl = page.url()
        checkAnswersPage.summaryList.correspondencePostalAddressRow.clickFirstActionLinkAndWait()
        var lookupPage = assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyRegistration::class)
        lookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AB", "2")
        var selectPage = assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyRegistration::class)
        selectPage.selectAddressAndSubmit(MANUAL_ADDRESS_CHOSEN)
        val manualPage = assertPageIs(page, CorrespondenceManualAddressFormPagePropertyRegistration::class)
        manualPage.form.addressLineOneInput.fill("Unsubmitted address")

        BackLink.default(page).clickAndWait()
        selectPage = assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyRegistration::class)
        assertEquals(MANUAL_ADDRESS_CHOSEN, selectPage.form.addressRadios.selectedValue)
        BackLink.default(page).clickAndWait()
        lookupPage = assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyRegistration::class)
        BaseComponent.assertThat(lookupPage.form.postcodeInput).hasValue("FA1 1AB")
        BackLink.default(page).clickAndWait()

        checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(page).hasURL(checkAnswersUrl)
        assertContactDetails(checkAnswersPage)
    }

    @Test
    fun `delegated occupied registrations show council contact details separately from the letting agent email`(page: Page) {
        featureFlagManager.enableFeature(DELEGATE_TO_LETTING_AGENT)
        val checkAnswersPage =
            goToCheckAnswers(
                page,
                completedState()
                    .withOccupancyStatus(true)
                    .withLettingAgentProvidesRentalDetails()
                    .withSubmittedValue(
                        LettingAgentEmailStep.ROUTE_SEGMENT,
                        AllowLettingAgentEmailFormModel().apply { emailAddress = "letting.agent@example.com" },
                    ),
            )

        assertContactDetails(checkAnswersPage)
        assertThat(checkAnswersPage.correspondenceRowKeys).hasText(arrayOf("Email address", "Postal address"))
        assertThat(checkAnswersPage.summaryList.occupancyQuestionRow.value).hasText("Yes")
        assertThat(checkAnswersPage.summaryList.lettingAgentEmailRow.value).hasText("letting.agent@example.com")
        BaseComponent.assertThat(checkAnswersPage.epcHeading).isHidden()
    }

    @Test
    fun `unoccupied registrations retain council contact details when letting agent delegation is enabled`(page: Page) {
        featureFlagManager.enableFeature(DELEGATE_TO_LETTING_AGENT)
        val checkAnswersPage = goToCheckAnswers(page)

        assertContactDetails(checkAnswersPage)
        assertThat(checkAnswersPage.summaryList.occupancyQuestionRow.value).hasText("No")
        BaseComponent.assertThat(checkAnswersPage.lettingAgentDelegationUnoccupiedPanel).isVisible()
    }

    @Test
    fun `disabling correspondence hides the contact subsection and rows even when correspondence data is present`(page: Page) {
        featureFlagManager.disableFeature(CORRESPONDENCE_ADDRESS)
        val checkAnswersPage = goToCheckAnswers(page)

        BaseComponent.assertThat(checkAnswersPage.correspondenceHeading).isHidden()
        assertThat(checkAnswersPage.correspondenceRowKeys).hasCount(0)
        assertThat(checkAnswersPage.summaryList.correspondenceEmailRow.key).hasCount(0)
        assertThat(checkAnswersPage.summaryList.correspondencePostalAddressRow.key).hasCount(0)
        assertThat(checkAnswersPage.summaryList.propertyAddressRow.value.locator("p")).hasText(propertyAddressLines.toTypedArray())
        BaseComponent.assertThat(checkAnswersPage.occupancyHeading).isVisible()
    }

    private fun completedState() =
        PropertyStateSessionBuilder
            .beforePropertyRegistrationCheckAnswers()
            .withBedrooms()
            .withCompletedCorrespondence()

    private fun goToCheckAnswers(
        page: Page,
        stateBuilder: PropertyStateSessionBuilder = completedState(),
    ): CheckAnswersPagePropertyRegistration {
        val taskListPage = navigator.goToRestructuredPropertyRegistrationTaskList(stateBuilder)
        taskListPage.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
        return assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
    }

    private fun assertContactDetails(
        checkAnswersPage: CheckAnswersPagePropertyRegistration,
        email: String = accountEmail,
        postalAddressLines: List<String> = originalPostalAddressLines,
    ) {
        BaseComponent.assertThat(checkAnswersPage.correspondenceHeading).isVisible()
        assertThat(checkAnswersPage.summaryList.correspondenceEmailRow.value).hasText(email)
        assertThat(checkAnswersPage.summaryList.correspondencePostalAddressRow.value.locator("p"))
            .hasText(postalAddressLines.toTypedArray())
        assertThat(checkAnswersPage.summaryList.propertyAddressRow.value.locator("p")).hasText(propertyAddressLines.toTypedArray())
    }
}

package uk.gov.communities.prsdb.webapp.integration

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.LocatorAssertions
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.MANUAL_ADDRESS_CHOSEN
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordDetailsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LookupAddressFormPageUpdateLandlordDetails
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ManualAddressFormPageUpdateLandlordDetails
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.SelectAddressFormPageUpdateLandlordDetails
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.updateLandlordDetailsPages.DateOfBirthFormPageUpdateLandlordDetails
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.updateLandlordDetailsPages.EmailFormPageUpdateLandlordDetails
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.updateLandlordDetailsPages.NameFormPageUpdateLandlordDetails
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.updateLandlordDetailsPages.PhoneNumberFormPageUpdateLandlordDetails
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.testHelpers.extensions.getFormattedUkPhoneNumber

class LandlordDetailsUpdateJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()

    @Nested
    inner class NameUpdates : NestedIntegrationTestWithMutableData("data-unverified-landlord.sql") {
        @Test
        fun `An unverified landlord can update their name`(page: Page) {
            // Details page
            var landlordDetailsPage = navigator.goToLandlordDetails()
            landlordDetailsPage.personalDetailsSummaryList.nameRow.actions.firstActionLink
                .clickAndWait()
            val updateNamePage = assertPageIs(page, NameFormPageUpdateLandlordDetails::class)

            // Update Name page
            val newName = "new landlord name"
            updateNamePage.submitName(newName)
            landlordDetailsPage = assertPageIs(page, LandlordDetailsPage::class)

            // Check changes have occurred
            assertThat(landlordDetailsPage.personalDetailsSummaryList.nameRow.value).containsText(newName)
        }
    }

    @Nested
    inner class DateOfBirthUpdates : NestedIntegrationTestWithMutableData("data-unverified-landlord.sql") {
        @Test
        fun `An unverified landlord can update their date of birth`(page: Page) {
            // Details page
            var landlordDetailsPage = navigator.goToLandlordDetails()
            landlordDetailsPage.personalDetailsSummaryList.dateOfBirthRow.actions.firstActionLink
                .clickAndWait()
            val updateDateOfBirthPage = assertPageIs(page, DateOfBirthFormPageUpdateLandlordDetails::class)

            // Update DOB page
            val newDateOfBirth = LocalDate(1990, 1, 1)
            updateDateOfBirthPage.submitDate(newDateOfBirth)
            landlordDetailsPage = assertPageIs(page, LandlordDetailsPage::class)

            // Check changes have occurred
            assertThat(landlordDetailsPage.personalDetailsSummaryList.dateOfBirthRow.value)
                .containsText(formatDateOfBirth(newDateOfBirth), LocatorAssertions.ContainsTextOptions().setIgnoreCase(true))
        }
    }

    @Nested
    inner class EmailUpdates {
        @Test
        fun `A landlord can update their email address`(page: Page) {
            var landlordDetailsPage = navigator.goToLandlordDetails()
            landlordDetailsPage.personalDetailsSummaryList.emailRow.actions.firstActionLink
                .clickAndWait()
            val updateEmailPage = assertPageIs(page, EmailFormPageUpdateLandlordDetails::class)
            // This is unique to the update journey, not on property reg
            assertThat(updateEmailPage.insetText).containsText(
                "This will not change any email addresses we show to councils on your property registrations. " +
                    "Check your registrations are correct after making this update.",
            )

            val newEmail = "newEmail@test.com"
            updateEmailPage.submitEmail(newEmail)
            landlordDetailsPage = assertPageIs(page, LandlordDetailsPage::class)

            assertThat(landlordDetailsPage.personalDetailsSummaryList.emailRow.value).containsText(newEmail)
        }
    }

    @Nested
    inner class PhoneNumberUpdates {
        @Test
        fun `A landlord can update their phone number`(page: Page) {
            // Details page
            var landlordDetailsPage = navigator.goToLandlordDetails()
            landlordDetailsPage.personalDetailsSummaryList.phoneNumberRow.actions.firstActionLink
                .clickAndWait()
            val updatePhoneNumberPage = assertPageIs(page, PhoneNumberFormPageUpdateLandlordDetails::class)

            // Update Phone Number page
            val newPhoneNumber = phoneNumberUtil.getFormattedUkPhoneNumber()
            updatePhoneNumberPage.submitPhoneNumber(newPhoneNumber)
            landlordDetailsPage = assertPageIs(page, LandlordDetailsPage::class)

            // Check changes have occurred
            assertThat(landlordDetailsPage.personalDetailsSummaryList.phoneNumberRow.value).containsText(newPhoneNumber)
        }
    }

    @Nested
    inner class AddressUpdates {
        @Test
        fun `A landlord can update their address (selected)`(page: Page) {
            // Details page
            var landlordDetailsPage = navigator.goToLandlordDetails()
            landlordDetailsPage.personalDetailsSummaryList.addressRow.actions.firstActionLink
                .clickAndWait()
            val lookupAddressPage = assertPageIs(page, LookupAddressFormPageUpdateLandlordDetails::class)

            // Lookup Address page
            lookupAddressPage.submitPostcodeAndBuildingNameOrNumber("EG1 2AA", "1")
            val selectAddressPage = assertPageIs(page, SelectAddressFormPageUpdateLandlordDetails::class)

            // Select Address page
            val newSelectedAddress = "1 PRSDB Square, EG1 2AA"
            BaseComponent.assertThat(selectAddressPage.warning).isVisible()
            selectAddressPage.selectAddressAndSubmit(newSelectedAddress)
            landlordDetailsPage = assertPageIs(page, LandlordDetailsPage::class)

            // Check changes have occurred
            assertThat(landlordDetailsPage.personalDetailsSummaryList.addressRow.value).containsText(newSelectedAddress)
        }

        @Test
        fun `A landlord can update their address (manual)`(page: Page) {
            // Details page
            var landlordDetailsPage = navigator.goToLandlordDetails()
            landlordDetailsPage.personalDetailsSummaryList.addressRow.actions.firstActionLink
                .clickAndWait()
            val lookupAddressPage = assertPageIs(page, LookupAddressFormPageUpdateLandlordDetails::class)

            // Lookup Address page
            lookupAddressPage.submitPostcodeAndBuildingNameOrNumber("EG1 2AA", "1")
            val selectAddressPage = assertPageIs(page, SelectAddressFormPageUpdateLandlordDetails::class)

            // Select Address page
            selectAddressPage.selectAddressAndSubmit(MANUAL_ADDRESS_CHOSEN)
            val manualAddressPage = assertPageIs(page, ManualAddressFormPageUpdateLandlordDetails::class)

            // Manual Address page
            val newFirstLine = "3 Example Road"
            val newTown = "Vilton"
            val newPostcode = "AB1 9YZ"
            BaseComponent.assertThat(manualAddressPage.warning).isVisible()
            manualAddressPage.submitAddress(newFirstLine, townOrCity = newTown, postcode = newPostcode)
            landlordDetailsPage = assertPageIs(page, LandlordDetailsPage::class)

            // Check changes have occurred
            val newSingleLineAddress = AddressDataModel.manualAddressDataToSingleLineAddress(newFirstLine, newTown, newPostcode)
            assertThat(landlordDetailsPage.personalDetailsSummaryList.addressRow.value).containsText(newSingleLineAddress)
        }
    }

    private fun formatDateOfBirth(date: LocalDate): String = "${date.dayOfMonth} ${date.month} ${date.year}"
}

package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasSupplyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HmoAdditionalLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HmoMandatoryLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LicensingTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideLicensingLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder

class PropertyRegistrationPropertyAndLicensingSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @Nested
    inner class PropertyTypeStep {
        @Test
        fun `Submitting with no propertyType selected returns an error`() {
            val propertyTypePage = navigator.skipToPropertyRegistrationPropertyTypePage()
            propertyTypePage.form.submit()
            assertThat(propertyTypePage.form.getErrorMessage()).containsText("Select the type of property")
        }

        @Test
        fun `Submitting with the Other propertyType selected but an empty customPropertyType field returns an error`() {
            val propertyTypePage = navigator.skipToPropertyRegistrationPropertyTypePage()
            propertyTypePage.submitCustomPropertyType("")
            assertThat(propertyTypePage.form.getErrorMessage()).containsText("Enter the property type")
        }
    }

    @Nested
    inner class OwnershipTypeStep {
        @Test
        fun `Submitting with no ownershipType selected returns an error`() {
            val ownershipTypePage = navigator.skipToPropertyRegistrationOwnershipTypePage()
            ownershipTypePage.form.submit()
            assertThat(ownershipTypePage.form.getErrorMessage()).containsText("Select the ownership type")
        }
    }

    @Nested
    inner class LicensingTypeStep {
        @Test
        fun `Submitting with no licensingType selected returns an error`() {
            val licensingTypePage = navigator.skipToPropertyRegistrationOccupiedLicensingTypePage()
            licensingTypePage.form.submitPrimaryButton()
            assertThat(licensingTypePage.form.getErrorMessage()).containsText("Select the type of licensing for the property")
        }

        @Test
        fun `Error summary link targets the first radio option when a divider is in the list`() {
            val licensingTypePage = navigator.skipToPropertyRegistrationOccupiedLicensingTypePage()
            licensingTypePage.form.submitPrimaryButton()

            assertThat(licensingTypePage.page.locator(".govuk-error-summary a[href='#licensingType']")).hasCount(1)
            assertThat(licensingTypePage.page.locator("#licensingType")).hasAttribute("type", "radio")
        }

        @Test
        fun `Submitting with an HMO mandatory licence redirects to the next step`(page: Page) {
            val licensingTypePage = navigator.skipToPropertyRegistrationOccupiedLicensingTypePage()
            licensingTypePage.submitLicensingType(LicensingType.HMO_MANDATORY_LICENCE)
            val licenseNumberPage = assertPageIs(page, HmoMandatoryLicenceFormPagePropertyRegistration::class)
            BaseComponent
                .assertThat(licenseNumberPage.form.sectionHeader)
                .containsText("Tell us if your property needs a license")
        }

        @Test
        fun `Submitting with an HMO additional licence redirects to the next step`(page: Page) {
            val licensingTypePage = navigator.skipToPropertyRegistrationOccupiedLicensingTypePage()
            licensingTypePage.submitLicensingType(LicensingType.HMO_ADDITIONAL_LICENCE)
            val licenseNumberPage = assertPageIs(page, HmoAdditionalLicenceFormPagePropertyRegistration::class)
            BaseComponent
                .assertThat(licenseNumberPage.form.sectionHeader)
                .containsText("Tell us if your property needs a license")
        }

        @Test
        fun `Submitting provide this later on licensing routes to occupied provide licensing later page`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationOwnershipType()
                        .withBedrooms()
                        .withOwnershipType()
                        .withHasNoJointLandlords()
                        .withOccupancyStatus(true)
                        .withLandlordProvidesRentalDetails(),
                )
            taskListPage.clickRentedOutTaskWithName("Tell us if your property needs a license")
            val licensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)
            assertThat(licensingTypePage.provideThisLaterButton).isVisible()

            licensingTypePage.submitProvideThisLater()
            val provideLicensingLaterPage = assertPageIs(page, ProvideLicensingLaterFormPagePropertyRegistration::class)

            BaseComponent
                .assertThat(provideLicensingLaterPage.heading)
                .containsText("Provide details about property licensing later")
            BaseComponent.assertThat(provideLicensingLaterPage.insetText).isVisible()
        }

        @Test
        fun `Submitting provide this later on licensing routes to unoccupied provide licensing later page`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationOwnershipType()
                        .withBedrooms()
                        .withOwnershipType()
                        .withHasNoJointLandlords()
                        .withOccupancyStatus(false),
                )
            taskListPage.clickRentedOutTaskWithName("Tell us if your property needs a license")
            val licensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)
            assertThat(licensingTypePage.provideThisLaterButton).isVisible()

            licensingTypePage.submitProvideThisLater()
            val provideLicensingLaterPage = assertPageIs(page, ProvideLicensingLaterFormPagePropertyRegistration::class)

            BaseComponent
                .assertThat(provideLicensingLaterPage.heading)
                .containsText("Provide details about property licensing later")
            BaseComponent.assertThat(provideLicensingLaterPage.insetText).isHidden()
        }
    }

    @Nested
    inner class SelectiveLicenceStep {
        @Test
        fun `Submitting with no licence number returns an error`() {
            val selectiveLicencePage = navigator.skipToPropertyRegistrationOccupiedSelectiveLicencePage()
            selectiveLicencePage.form.submit()
            assertThat(selectiveLicencePage.form.getErrorMessage()).containsText("Enter the selective licence number")
        }

        @Test
        fun `Submitting with a very long licence number returns an error`() {
            val selectiveLicencePage = navigator.skipToPropertyRegistrationOccupiedSelectiveLicencePage()
            val aVeryLongString =
                "This string is very long, so long that it is not feasible that it is a real licence number " +
                    "- therefore if it is submitted there will in fact be an error rather than a successful submission." +
                    " It is actually quite difficult for a string to be long enough to trigger this error, because the" +
                    " maximum length has been selected to be permissive of id numbers we do not expect while still having " +
                    "a cap reachable with a little effort."
            selectiveLicencePage.submitLicenseNumber(aVeryLongString)
            assertThat(selectiveLicencePage.form.getErrorMessage()).containsText("The licensing number is too long")
        }
    }

    @Nested
    inner class HmoMandatoryLicenceStep {
        @Test
        fun `Submitting with a licence number redirects to the next step`(page: Page) {
            val hmoMandatoryLicencePage = navigator.skipToPropertyRegistrationOccupiedHmoMandatoryLicencePage()
            hmoMandatoryLicencePage.submitLicenseNumber("licence number")
            assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)
        }

        @Test
        fun `Submitting with no licence number returns an error`() {
            val hmoMandatoryLicencePage = navigator.skipToPropertyRegistrationOccupiedHmoMandatoryLicencePage()
            hmoMandatoryLicencePage.form.submit()
            assertThat(hmoMandatoryLicencePage.form.getErrorMessage()).containsText("Enter the HMO Mandatory licence number")
        }

        @Test
        fun `Submitting with a very long licence number returns an error`() {
            val hmoMandatoryLicencePage = navigator.skipToPropertyRegistrationOccupiedHmoMandatoryLicencePage()
            val aVeryLongString =
                "This string is very long, so long that it is not feasible that it is a real licence number " +
                    "- therefore if it is submitted there will in fact be an error rather than a successful submission." +
                    " It is actually quite difficult for a string to be long enough to trigger this error, because the" +
                    " maximum length has been selected to be permissive of id numbers we do not expect while still having " +
                    "a cap reachable with a little effort."
            hmoMandatoryLicencePage.submitLicenseNumber(aVeryLongString)
            assertThat(hmoMandatoryLicencePage.form.getErrorMessage()).containsText("The licensing number is too long")
        }
    }

    @Nested
    inner class HmoAdditionalLicenceStep {
        @Test
        fun `Submitting with a licence number redirects to the next step`(page: Page) {
            val hmoAdditionalLicencePage = navigator.skipToPropertyRegistrationOccupiedHmoAdditionalLicencePage()
            hmoAdditionalLicencePage.submitLicenseNumber("licence number")
            assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)
        }

        @Test
        fun `Submitting with no licence number returns an error`() {
            val hmoAdditionalLicencePage = navigator.skipToPropertyRegistrationOccupiedHmoAdditionalLicencePage()
            hmoAdditionalLicencePage.form.submit()
            assertThat(hmoAdditionalLicencePage.form.getErrorMessage()).containsText("Enter the HMO additional licence number")
        }

        @Test
        fun `Submitting with a very long licence number returns an error`() {
            val hmoAdditionalLicencePage = navigator.skipToPropertyRegistrationOccupiedHmoAdditionalLicencePage()
            val aVeryLongString =
                "This string is very long, so long that it is not feasible that it is a real licence number " +
                    "- therefore if it is submitted there will in fact be an error rather than a successful submission." +
                    " It is actually quite difficult for a string to be long enough to trigger this error, because the" +
                    " maximum length has been selected to be permissive of id numbers we do not expect while still having " +
                    "a cap reachable with a little effort."
            hmoAdditionalLicencePage.submitLicenseNumber(aVeryLongString)
            assertThat(hmoAdditionalLicencePage.form.getErrorMessage()).containsText("The licensing number is too long")
        }
    }

    @Nested
    inner class NumberOfBedroomsStep {
        val numberOfBedroomsErrorMessage = "Enter the number of bedrooms, like 3 or 8"

        @Test
        fun `Submitting with a blank numberOfBedrooms field returns an error`() {
            val bedroomsPage = navigator.skipToPropertyRegistrationBedroomsPage()
            bedroomsPage.form.submit()
            assertThat(bedroomsPage.form.getErrorMessage()).containsText(numberOfBedroomsErrorMessage)
        }

        @Test
        fun `Submitting with a non-numerical value in the numberOfBedrooms field returns an error`() {
            val bedroomsPage = navigator.skipToPropertyRegistrationBedroomsPage()
            bedroomsPage.submitNumOfBedrooms("not-a-number")
            assertThat(bedroomsPage.form.getErrorMessage()).containsText(numberOfBedroomsErrorMessage)
        }

        @Test
        fun `Submitting with a non-integer number in the numberOfBedrooms field returns an error`() {
            val bedroomsPage = navigator.skipToPropertyRegistrationBedroomsPage()
            bedroomsPage.submitNumOfBedrooms("2.3")
            assertThat(bedroomsPage.form.getErrorMessage()).containsText(numberOfBedroomsErrorMessage)
        }

        @Test
        fun `Submitting with a negative integer in the numberOfBedrooms field returns an error`() {
            val bedroomsPage = navigator.skipToPropertyRegistrationBedroomsPage()
            bedroomsPage.submitNumOfBedrooms("-2")
            assertThat(bedroomsPage.form.getErrorMessage()).containsText(numberOfBedroomsErrorMessage)
        }

        @Test
        fun `Submitting with a zero integer in the numberOfBedrooms field returns an error`() {
            val bedroomsPage = navigator.skipToPropertyRegistrationBedroomsPage()
            bedroomsPage.submitNumOfBedrooms(0)
            assertThat(bedroomsPage.form.getErrorMessage()).containsText(numberOfBedroomsErrorMessage)
        }
    }
}

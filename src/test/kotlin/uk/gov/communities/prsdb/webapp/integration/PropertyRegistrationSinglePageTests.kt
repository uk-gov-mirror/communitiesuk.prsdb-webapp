package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ErrorPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.AlreadyRegisteredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HmoAdditionalLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HmoMandatoryLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LicensingTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LookupAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ManualAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NoAddressFoundFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfPeopleFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OccupancyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OwnershipTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel

class PropertyRegistrationSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    @Nested
    inner class TaskListStep {
        @Test
        fun `Completing preceding steps will show a task as not started and completed steps as complete`(page: Page) {
            navigator.skipToPropertyRegistrationOccupancyPage()
            val taskListPage = navigator.goToPropertyRegistrationTaskList()
            assert(taskListPage.taskHasStatus("Enter the property address", "Complete"))
            assert(taskListPage.taskHasStatus("Select the type of property", "Complete"))
            assert(taskListPage.taskHasStatus("Tell us how you own the property", "Complete"))
            assert(taskListPage.taskHasStatus("Add details about any property licensing", "Complete"))
            assert(taskListPage.taskHasStatus("Add tenancy details for the property", "Not started"))
        }

        @Test
        fun `Completing first step of a task will show a task as in progress and completed steps as complete`(page: Page) {
            navigator.skipToPropertyRegistrationHmoAdditionalLicencePage()
            val taskListPage = navigator.goToPropertyRegistrationTaskList()
            assert(taskListPage.taskHasStatus("Enter the property address", "Complete"))
            assert(taskListPage.taskHasStatus("Select the type of property", "Complete"))
            assert(taskListPage.taskHasStatus("Tell us how you own the property", "Complete"))
            assert(taskListPage.taskHasStatus("Add details about any property licensing", "In progress"))
            assert(taskListPage.taskHasStatus("Add tenancy details for the property", "Cannot start"))
        }
    }

    @Nested
    inner class LookupAddressAndNoAddressFoundSteps {
        @Test
        fun `Submitting with empty data fields returns an error`(page: Page) {
            val lookupAddressPage = navigator.goToPropertyRegistrationLookupAddressPage()
            lookupAddressPage.form.submit()
            assertThat(lookupAddressPage.form.getErrorMessage("postcode")).containsText("Enter a postcode")
            assertThat(lookupAddressPage.form.getErrorMessage("houseNameOrNumber")).containsText("Enter a house name or number")
        }

        @Test
        fun `If no English addresses are found, user can search again or enter address manually via the No Address Found step`(page: Page) {
            // Lookup address finds no English results
            val houseNumber = "NOT A HOUSE NUMBER"
            val postcode = "NOT A POSTCODE"
            val lookupAddressPage = navigator.goToPropertyRegistrationLookupAddressPage()
            lookupAddressPage.submitPostcodeAndBuildingNameOrNumber(postcode, houseNumber)

            // redirect to noAddressFoundPage
            val noAddressFoundPage = BasePage.assertPageIs(page, NoAddressFoundFormPagePropertyRegistration::class)
            BaseComponent
                .assertThat(noAddressFoundPage.heading)
                .containsText("No matching address in England or Wales found for $postcode and $houseNumber")

            // Search Again
            noAddressFoundPage.searchAgain.clickAndWait()
            val lookupAddressPageAgain = BasePage.assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)
            lookupAddressPageAgain.submitPostcodeAndBuildingNameOrNumber(postcode, houseNumber)

            // Submit no address found page
            val noAddressFoundPageAgain = BasePage.assertPageIs(page, NoAddressFoundFormPagePropertyRegistration::class)
            noAddressFoundPageAgain.form.submit()
            BasePage.assertPageIs(page, ManualAddressFormPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class SelectAddressStep {
        @Test
        fun `Submitting with no option selected returns an error`(page: Page) {
            val selectAddressPage = navigator.skipToPropertyRegistrationSelectAddressPage()
            selectAddressPage.form.submit()
            assertThat(selectAddressPage.form.getErrorMessage()).containsText("Select an address")
        }

        @Test
        fun `Clicking Search Again navigates to the previous step`(page: Page) {
            val selectAddressPage = navigator.skipToPropertyRegistrationSelectAddressPage()
            selectAddressPage.searchAgain.clickAndWait()
            BasePage.assertPageIs(page, LookupAddressFormPagePropertyRegistration::class)
        }

        @Test
        fun `Selecting an already-registered address navigates to the AlreadyRegistered step`(page: Page) {
            val alreadyRegisteredAddress = AddressDataModel("1 Example Road", uprn = 1123456)
            val selectAddressPage = navigator.skipToPropertyRegistrationSelectAddressPage(listOf(alreadyRegisteredAddress))
            selectAddressPage.selectAddressAndSubmit(alreadyRegisteredAddress.singleLineAddress)
            BasePage.assertPageIs(page, AlreadyRegisteredFormPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class ManualAddressEntryStep {
        @Test
        fun `Submitting empty data fields returns errors`(page: Page) {
            val manualAddressPage = navigator.skipToPropertyRegistrationManualAddressPage()
            manualAddressPage.submitAddress()
            assertThat(manualAddressPage.form.getErrorMessage("addressLineOne"))
                .containsText("Enter the first line of an address, typically the building and street")
            assertThat(manualAddressPage.form.getErrorMessage("townOrCity")).containsText("Enter town or city")
            assertThat(manualAddressPage.form.getErrorMessage("postcode")).containsText("Enter postcode")
        }
    }

    @Nested
    inner class SelectLocalAuthorityStep {
        @Test
        fun `Submitting without selecting an LA return an error`(page: Page) {
            val selectLocalAuthorityPage = navigator.skipToPropertyRegistrationSelectLocalAuthorityPage()
            selectLocalAuthorityPage.form.submit()
            assertThat(selectLocalAuthorityPage.form.getErrorMessage("localAuthorityId"))
                .containsText("Select a local council to continue")
        }
    }

    @Nested
    inner class PropertyTypeStep {
        @Test
        fun `Submitting with no propertyType selected returns an error`(page: Page) {
            val propertyTypePage = navigator.skipToPropertyRegistrationPropertyTypePage()
            propertyTypePage.form.submit()
            assertThat(propertyTypePage.form.getErrorMessage()).containsText("Select the type of property")
        }

        @Test
        fun `Submitting with the Other propertyType selected but an empty customPropertyType field returns an error`(page: Page) {
            val propertyTypePage = navigator.skipToPropertyRegistrationPropertyTypePage()
            propertyTypePage.submitCustomPropertyType("")
            assertThat(propertyTypePage.form.getErrorMessage()).containsText("Enter the property type")
        }
    }

    @Nested
    inner class OwnershipTypeStep {
        @Test
        fun `Submitting with no ownershipType selected returns an error`(page: Page) {
            val ownershipTypePage = navigator.skipToPropertyRegistrationOwnershipTypePage()
            ownershipTypePage.form.submit()
            assertThat(ownershipTypePage.form.getErrorMessage()).containsText("Select the ownership type")
        }
    }

    @Nested
    inner class LicensingTypeStep {
        @Test
        fun `Submitting with no licensingType selected returns an error`(page: Page) {
            val licensingTypePage = navigator.skipToPropertyRegistrationLicensingTypePage()
            licensingTypePage.form.submit()
            assertThat(licensingTypePage.form.getErrorMessage()).containsText("Select the type of licensing for the property")
        }

        @Test
        fun `Submitting with an HMO mandatory licence redirects to the next step`(page: Page) {
            val licensingTypePage = navigator.skipToPropertyRegistrationLicensingTypePage()
            licensingTypePage.submitLicensingType(LicensingType.HMO_MANDATORY_LICENCE)
            val licenseNumberPage = BasePage.assertPageIs(page, HmoMandatoryLicenceFormPagePropertyRegistration::class)
            BaseComponent
                .assertThat(licenseNumberPage.form.sectionHeader)
                .containsText("Section 1 of 2 \u2014 Register your property details")
        }

        @Test
        fun `Submitting with an HMO additional licence redirects to the next step`(page: Page) {
            val licensingTypePage = navigator.skipToPropertyRegistrationLicensingTypePage()
            licensingTypePage.submitLicensingType(LicensingType.HMO_ADDITIONAL_LICENCE)
            val licenseNumberPage = BasePage.assertPageIs(page, HmoAdditionalLicenceFormPagePropertyRegistration::class)
            BaseComponent
                .assertThat(licenseNumberPage.form.sectionHeader)
                .containsText("Section 1 of 2 \u2014 Register your property details")
        }
    }

    @Nested
    inner class SelectiveLicenceStep {
        @Test
        fun `Submitting with no licence number returns an error`(page: Page) {
            val selectiveLicencePage = navigator.skipToPropertyRegistrationSelectiveLicencePage()
            selectiveLicencePage.form.submit()
            assertThat(selectiveLicencePage.form.getErrorMessage()).containsText("Enter the selective licence number")
        }

        @Test
        fun `Submitting with a very long licence number returns an error`(page: Page) {
            val selectiveLicencePage = navigator.skipToPropertyRegistrationSelectiveLicencePage()
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
            val hmoMandatoryLicencePage = navigator.skipToPropertyRegistrationHmoMandatoryLicencePage()
            hmoMandatoryLicencePage.submitLicenseNumber("licence number")
            BasePage.assertPageIs(page, OccupancyFormPagePropertyRegistration::class)
        }

        @Test
        fun `Submitting with no licence number returns an error`(page: Page) {
            val hmoMandatoryLicencePage = navigator.skipToPropertyRegistrationHmoMandatoryLicencePage()
            hmoMandatoryLicencePage.form.submit()
            assertThat(hmoMandatoryLicencePage.form.getErrorMessage()).containsText("Enter the HMO Mandatory licence number")
        }

        @Test
        fun `Submitting with a very long licence number returns an error`(page: Page) {
            val hmoMandatoryLicencePage = navigator.skipToPropertyRegistrationHmoMandatoryLicencePage()
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
            val hmoAdditionalLicencePage = navigator.skipToPropertyRegistrationHmoAdditionalLicencePage()
            hmoAdditionalLicencePage.submitLicenseNumber("licence number")
            BasePage.assertPageIs(page, OccupancyFormPagePropertyRegistration::class)
        }

        @Test
        fun `Submitting with no licence number returns an error`(page: Page) {
            val hmoAdditionalLicencePage = navigator.skipToPropertyRegistrationHmoAdditionalLicencePage()
            hmoAdditionalLicencePage.form.submit()
            assertThat(hmoAdditionalLicencePage.form.getErrorMessage()).containsText("Enter the HMO additional licence number")
        }

        @Test
        fun `Submitting with a very long licence number returns an error`(page: Page) {
            val hmoAdditionalLicencePage = navigator.skipToPropertyRegistrationHmoAdditionalLicencePage()
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
    inner class OccupancyStep {
        @Test
        fun `Submitting with no occupancy option selected returns an error`(page: Page) {
            val occupancyPage = navigator.skipToPropertyRegistrationOccupancyPage()
            occupancyPage.form.submit()
            assertThat(occupancyPage.form.getErrorMessage()).containsText("Select whether the property is occupied")
        }
    }

    @Nested
    inner class NumberOfHouseholdsStep {
        @Test
        fun `Submitting with a blank numberOfHouseholds field returns an error`(page: Page) {
            val householdsPage = navigator.skipToPropertyRegistrationHouseholdsPage()
            householdsPage.form.submit()
            assertThat(householdsPage.form.getErrorMessage()).containsText("Enter the number of households living in your property")
        }

        @Test
        fun `Submitting with a non-numerical value in the numberOfHouseholds field returns an error`(page: Page) {
            val householdsPage = navigator.skipToPropertyRegistrationHouseholdsPage()
            householdsPage.submitNumberOfHouseholds("not-a-number")
            assertThat(householdsPage.form.getErrorMessage())
                .containsText("Number of households in your property must be a positive, whole number, like 3")
        }

        @Test
        fun `Submitting with a non-integer number in the numberOfHouseholds field returns an error`(page: Page) {
            val householdsPage = navigator.skipToPropertyRegistrationHouseholdsPage()
            householdsPage.submitNumberOfHouseholds("2.3")
            assertThat(householdsPage.form.getErrorMessage())
                .containsText("Number of households in your property must be a positive, whole number, like 3")
        }

        @Test
        fun `Submitting with a negative integer in the numberOfHouseholds field returns an error`(page: Page) {
            val householdsPage = navigator.skipToPropertyRegistrationHouseholdsPage()
            householdsPage.submitNumberOfHouseholds(-2)
            assertThat(householdsPage.form.getErrorMessage())
                .containsText("Number of households in your property must be a positive, whole number, like 3")
        }

        @Test
        fun `Submitting with a zero integer in the numberOfHouseholds field returns an error`(page: Page) {
            val householdsPage = navigator.skipToPropertyRegistrationHouseholdsPage()
            householdsPage.submitNumberOfHouseholds(0)
            assertThat(householdsPage.form.getErrorMessage())
                .containsText("Number of households in your property must be a positive, whole number, like 3")
        }
    }

    @Nested
    inner class NumberOfPeopleStep {
        @Test
        fun `Submitting with a blank numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToPropertyRegistrationPeoplePage()
            peoplePage.form.submit()
            assertThat(peoplePage.form.getErrorMessage()).containsText("Enter the number of people living in your property")
        }

        @Test
        fun `Submitting with a non-numerical value in the numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToPropertyRegistrationPeoplePage()
            peoplePage.submitNumOfPeople("not-a-number")
            assertThat(peoplePage.form.getErrorMessage())
                .containsText("Number of people in your property must be a positive, whole number, like 3")
        }

        @Test
        fun `Submitting with a non-integer number in the numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToPropertyRegistrationPeoplePage()
            peoplePage.submitNumOfPeople("2.3")
            assertThat(peoplePage.form.getErrorMessage())
                .containsText("Number of people in your property must be a positive, whole number, like 3")
        }

        @Test
        fun `Submitting with a negative integer in the numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToPropertyRegistrationPeoplePage()
            peoplePage.submitNumOfPeople("-2")
            assertThat(peoplePage.form.getErrorMessage())
                .containsText("Number of people in your property must be a positive, whole number, like 3")
        }

        @Test
        fun `Submitting with a zero integer in the numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToPropertyRegistrationPeoplePage()
            peoplePage.submitNumOfPeople(0)
            assertThat(peoplePage.form.getErrorMessage())
                .containsText("Number of people in your property must be a positive, whole number, like 3")
        }

        @Test
        fun `Submitting with an integer in the numberOfPeople field that is less than the numberOfHouseholds returns an error`(
            page: Page,
        ) {
            val householdsPage = navigator.skipToPropertyRegistrationHouseholdsPage()
            householdsPage.submitNumberOfHouseholds(3)
            val peoplePage = BasePage.assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            peoplePage.submitNumOfPeople(2)
            assertThat(peoplePage.form.getErrorMessage())
                .containsText(
                    "The number of people in the property must be the same as or higher than the number of households in the property",
                )
        }
    }

    @Nested
    inner class Confirmation {
        @Test
        fun `Navigating here with an incomplete form returns a 400 error page`(page: Page) {
            navigator.navigateToPropertyRegistrationConfirmationPage()
            val errorPage = BasePage.assertPageIs(page, ErrorPage::class)
            BaseComponent.assertThat(errorPage.heading).containsText("Sorry, there is a problem with the service")
        }
    }

    @Nested
    inner class PropertyRegistrationStepCheckAnswers {
        @Test
        fun `After changing an answer, submitting a full section returns the CYA page`(page: Page) {
            var checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPage()

            checkAnswersPage.form.summaryList.ownershipRow.actions.actionLink
                .clickAndWait()
            val ownershipPage = BasePage.assertPageIs(page, OwnershipTypeFormPagePropertyRegistration::class)

            ownershipPage.submitOwnershipType(OwnershipType.LEASEHOLD)
            checkAnswersPage = BasePage.assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.form.summaryList.licensingRow.actions.actionLink
                .clickAndWait()
            val licensingTypePage = BasePage.assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)

            licensingTypePage.submitLicensingType(LicensingType.HMO_ADDITIONAL_LICENCE)
            val licenceNumberPage = BasePage.assertPageIs(page, HmoAdditionalLicenceFormPagePropertyRegistration::class)
            licenceNumberPage.submitLicenseNumber("licence number")
            BasePage.assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }
    }
}

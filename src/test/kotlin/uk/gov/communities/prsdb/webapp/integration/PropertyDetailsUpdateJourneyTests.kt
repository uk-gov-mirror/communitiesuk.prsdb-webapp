package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.enums.BillsIncluded
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.controllers.UpdateOccupancyController
import uk.gov.communities.prsdb.webapp.database.entity.LettingAgentAccess
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.PropertyDetailsPageLandlordView
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.BillsIncludedFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CheckCorrespondenceAddressAnswersPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CheckHouseholdsAnswersPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CheckLicensingAnswersPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CheckOccupancyAnswersPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CheckRentFrequencyAndAmountAnswersPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CorrespondenceLookupAddressFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CorrespondenceManualAddressFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CorrespondenceNoAddressFoundFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.CorrespondenceSelectAddressFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.FurnishedStatusFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.HmoAdditionalLicenceFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.HmoMandatoryLicenceFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.HouseholdsNumberOfPeopleFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.LicensingTypeFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.NumberOfBedroomsFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.NumberOfHouseholdsFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyBillsIncludedFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyFurnishedStatusFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyLettingAgentInterruptionPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyNumberOfBedroomsFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyNumberOfHouseholdsFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyNumberOfPeopleFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyRentAmountFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyRentFrequencyFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OccupancyRentIncludesBillsFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.OwnershipTypeFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.RentAmountFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.RentFrequencyFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.RentIncludesBillsFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.SelectiveLicenceFormPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages.UpdateOccupancyCheckYourAnswersPagePropertyDetailsUpdate
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.occupancy.OccupancyLettingAgentInterruptionStep
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals

class PropertyDetailsUpdateJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    @Autowired
    lateinit var lettingAgentAccessRepository: LettingAgentAccessRepository

    @Autowired
    lateinit var propertyOwnershipRepository: PropertyOwnershipRepository

    private val propertyOwnershipId = 1L
    private val urlArguments = mapOf("propertyOwnershipId" to propertyOwnershipId.toString())

    @Nested
    inner class RestructureAndSkippingEnabled {
        @BeforeEach
        fun enableRestructureAndSkippingFlag() {
            featureFlagManager.enableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
        }

        @Nested
        inner class OwnershipTypeUpdates {
            @Test
            fun `A property can have its ownership type updated`(page: Page) {
                // Details page
                var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsPage.propertyDetailsSummaryList.ownershipTypeRow.clickFirstActionLinkAndWait()
                val updateOwnershipTypePage =
                    assertPageIs(page, OwnershipTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update Ownership Type page
                updateOwnershipTypePage.submitOwnershipType(OwnershipType.LEASEHOLD)
                propertyDetailsPage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(propertyDetailsPage.propertyDetailsSummaryList.ownershipTypeRow.value).containsText("Leasehold")
            }
        }

        @Nested
        inner class ResumingAbandonedUpdateAfterCompletingAnother {
            @Test
            fun `resuming an abandoned update after completing another update on the same property starts fresh without a conflict`(
                page: Page,
            ) {
                val newNumberOfBedrooms = 4

                // Start (but abandon) a bedrooms update - this stores the property's current last-modified date in the session
                var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.clickFirstActionLinkAndWait()
                assertPageIs(page, NumberOfBedroomsFormPagePropertyDetailsUpdate::class, urlArguments)

                // Complete a different update (ownership type) on the same property, bumping its last-modified date
                propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsPage.propertyDetailsSummaryList.ownershipTypeRow.clickFirstActionLinkAndWait()
                val updateOwnershipTypePage =
                    assertPageIs(page, OwnershipTypeFormPagePropertyDetailsUpdate::class, urlArguments)
                updateOwnershipTypePage.submitOwnershipType(OwnershipType.LEASEHOLD)
                propertyDetailsPage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Re-enter the abandoned bedrooms update via the change link and submit it
                propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.clickFirstActionLinkAndWait()
                val updateNumberOfBedroomsPage =
                    assertPageIs(page, NumberOfBedroomsFormPagePropertyDetailsUpdate::class, urlArguments)
                updateNumberOfBedroomsPage.submitNumOfBedrooms(newNumberOfBedrooms)

                // The update completes without an update-conflict error and the change is applied
                propertyDetailsPage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)
                assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.value)
                    .containsText(newNumberOfBedrooms.toString())
            }

            @Test
            fun `resuming an abandoned multi-step update after completing another update discards the earlier progress`(page: Page) {
                // Partially complete (but abandon) a multi-step households-and-tenants update - after submitting the first
                // step the second step is reachable and its URL is stored in the session
                var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsPage.propertyDetailsSummaryList.numberOfHouseholdsRow.clickFirstActionLinkAndWait()
                val numberOfHouseholdsPage =
                    assertPageIs(page, NumberOfHouseholdsFormPagePropertyDetailsUpdate::class, urlArguments)
                numberOfHouseholdsPage.submitNumberOfHouseholds(1)
                assertPageIs(page, HouseholdsNumberOfPeopleFormPagePropertyDetailsUpdate::class, urlArguments)
                val numberOfPeopleStepUrl = page.url().substringBefore("?")

                // Complete a different update (ownership type) on the same property, bumping its last-modified date
                propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsPage.propertyDetailsSummaryList.ownershipTypeRow.clickFirstActionLinkAndWait()
                val updateOwnershipTypePage =
                    assertPageIs(page, OwnershipTypeFormPagePropertyDetailsUpdate::class, urlArguments)
                updateOwnershipTypePage.submitOwnershipType(OwnershipType.LEASEHOLD)
                assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Attempt to resume the abandoned households-and-tenants update at the number-of-people step. Because the
                // property has changed, the stale journey is discarded and restarted, so the number-of-people step is no
                // longer reachable and we are redirected out to the property details page - the earlier progress is gone.
                page.navigate(numberOfPeopleStepUrl)
                assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)
            }
        }

        @Nested
        inner class CorrespondenceAddressUpdates {
            @BeforeEach
            fun enableCorrespondenceAddressFlag() {
                featureFlagManager.enableFeature(CORRESPONDENCE_ADDRESS)
            }

            @Test
            fun `A property's correspondence address can be updated by selecting an address`(page: Page) {
                var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsPage.propertyDetailsSummaryList.contactAddressRow.clickFirstActionLinkAndWait()
                val lookupAddressPage =
                    assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyDetailsUpdate::class, urlArguments)

                assertThat(lookupAddressPage.heading).containsText("Where the council should send post about this property")
                lookupAddressPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AA", "1")
                val selectAddressPage =
                    assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyDetailsUpdate::class, urlArguments)

                selectAddressPage.selectAddressAndSubmit("1 Fictional Road, FA1 1AA")
                val checkAnswersPage =
                    assertPageIs(page, CheckCorrespondenceAddressAnswersPagePropertyDetailsUpdate::class, urlArguments)

                assertContains(
                    checkAnswersPage.summaryName.getText(),
                    "You updated who the council should contact for this property",
                )
                assertThat(checkAnswersPage.summaryList.postalAddressRow.value).containsText("1 Fictional Road")
                assertThat(checkAnswersPage.summaryList.postalAddressRow.value).containsText("FA1 1AA")
                assertThat(checkAnswersPage.warning).isVisible()
                assertThat(checkAnswersPage.form.submitButton).containsText("Confirm and submit update")

                checkAnswersPage.confirm()
                assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)
                assertThat(page.locator(".govuk-notification-banner--success")).isHidden()
                assertEquals(
                    "1 Fictional Road, FA1 1AA",
                    propertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnershipId)!!.correspondenceAddress!!.singleLineAddress,
                )
            }

            @Test
            fun `A property's correspondence address can be updated by manual address entry`(page: Page) {
                val registeredAddressBefore =
                    propertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnershipId)!!.address.singleLineAddress

                navigator
                    .goToPropertyDetailsLandlordView(propertyOwnershipId)
                    .propertyDetailsSummaryList.contactAddressRow
                    .clickFirstActionLinkAndWait()
                val lookupAddressPage =
                    assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyDetailsUpdate::class, urlArguments)

                lookupAddressPage.submitPostcodeAndBuildingNameOrNumber("NOT A POSTCODE", "NOT A HOUSE NUMBER")
                val noAddressFoundPage =
                    assertPageIs(page, CorrespondenceNoAddressFoundFormPagePropertyDetailsUpdate::class, urlArguments)
                assertThat(noAddressFoundPage.heading).containsText(
                    "No matching address in England or Wales found for NOT A POSTCODE and NOT A HOUSE NUMBER",
                )

                noAddressFoundPage.form.submit()
                val manualAddressPage =
                    assertPageIs(page, CorrespondenceManualAddressFormPagePropertyDetailsUpdate::class, urlArguments)

                manualAddressPage.submitAddress(
                    addressLineOne = "24 Manual Street",
                    townOrCity = "London",
                    postcode = "SW1A 1AA",
                )
                val checkAnswersPage =
                    assertPageIs(page, CheckCorrespondenceAddressAnswersPagePropertyDetailsUpdate::class, urlArguments)
                assertThat(checkAnswersPage.summaryList.postalAddressRow.value).containsText("24 Manual Street")
                assertThat(checkAnswersPage.summaryList.postalAddressRow.value).containsText("London")
                assertThat(checkAnswersPage.summaryList.postalAddressRow.value).containsText("SW1A 1AA")

                checkAnswersPage.confirm()
                assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)
                val propertyOwnership = propertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnershipId)!!
                assertEquals("24 Manual Street, London, SW1A 1AA", propertyOwnership.correspondenceAddress!!.singleLineAddress)
                assertEquals(registeredAddressBefore, propertyOwnership.address.singleLineAddress)
            }

            @Test
            fun `A property's correspondence address can be changed from the CYA page before confirming`(page: Page) {
                navigator
                    .goToPropertyDetailsLandlordView(propertyOwnershipId)
                    .propertyDetailsSummaryList.contactAddressRow
                    .clickFirstActionLinkAndWait()
                var lookupAddressPage =
                    assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyDetailsUpdate::class, urlArguments)

                lookupAddressPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AA", "1")
                assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyDetailsUpdate::class, urlArguments)
                    .selectAddressAndSubmit("1 Fictional Road, FA1 1AA")
                val checkAnswersPage =
                    assertPageIs(page, CheckCorrespondenceAddressAnswersPagePropertyDetailsUpdate::class, urlArguments)

                checkAnswersPage.summaryList.postalAddressRow.clickFirstActionLinkAndWait()
                lookupAddressPage =
                    assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyDetailsUpdate::class, urlArguments)
                lookupAddressPage.submitPostcodeAndBuildingNameOrNumber("EG1 2AA", "1")
                assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyDetailsUpdate::class, urlArguments)
                    .selectAddressAndSubmit("1 PRSDB Square, EG1 2AA")
                val updatedCheckAnswersPage =
                    assertPageIs(page, CheckCorrespondenceAddressAnswersPagePropertyDetailsUpdate::class, urlArguments)

                assertThat(updatedCheckAnswersPage.summaryList.postalAddressRow.value).containsText("1 PRSDB Square")
                assertThat(updatedCheckAnswersPage.summaryList.postalAddressRow.value).containsText("EG1 2AA")
            }
        }

        @Nested
        inner class LicenceUpdates {
            // Property 1 is licensing-provide-later, so the standard layout shows a "Provide this later" row rather than an
            // editable "Licensing type" row. These update-an-existing-licence tests use property 7, which has a real licence.
            private val propertyOwnershipId = 7L
            private val urlArguments = mapOf("propertyOwnershipId" to propertyOwnershipId.toString())

            @Test
            fun `A property can have its licensing updated to a selective licence`(page: Page) {
                val newLicenceNumber = "SL123"

                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage =
                    assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to selective
                updateLicensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
                val updateLicenceNumberPage =
                    assertPageIs(page, SelectiveLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(newLicenceNumber)
                val checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(checkLicensingAnswersPage.summaryName.getText(), "You have updated the property licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("Selective licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingNumberRow.value).containsText(newLicenceNumber)
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.value).containsText("Selective licence")
                assertThat(propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingNumberRow.value).containsText(
                    newLicenceNumber,
                )
            }

            @Test
            fun `A property can have its licensing updated to a HMO Mandatory licence`(page: Page) {
                val newLicenceNumber = "MAND123"

                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage =
                    assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to HMO mandatory
                updateLicensingTypePage.submitLicensingType(LicensingType.HMO_MANDATORY_LICENCE)
                val updateLicenceNumberPage =
                    assertPageIs(page, HmoMandatoryLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(newLicenceNumber)
                val checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(checkLicensingAnswersPage.summaryName.getText(), "You have updated the property licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("HMO mandatory licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingNumberRow.value).containsText(newLicenceNumber)
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(
                    propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.value,
                ).containsText("HMO mandatory licence")
                assertThat(propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingNumberRow.value).containsText(
                    newLicenceNumber,
                )
            }

            @Test
            fun `A property can have its licensing updated to a HMO additional licence`(page: Page) {
                val newLicenceNumber = "ADD123"

                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage =
                    assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to HMO additional
                updateLicensingTypePage.submitLicensingType(LicensingType.HMO_ADDITIONAL_LICENCE)
                val updateLicenceNumberPage =
                    assertPageIs(page, HmoAdditionalLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(newLicenceNumber)
                val checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(checkLicensingAnswersPage.summaryName.getText(), "You have updated the property licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("HMO additional licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingNumberRow.value).containsText(newLicenceNumber)
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(
                    propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.value,
                ).containsText("HMO additional licence")
                assertThat(propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingNumberRow.value).containsText(
                    newLicenceNumber,
                )
            }

            @Test
            fun `A property can have its licensing removed`(page: Page) {
                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage =
                    assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to no licensing
                updateLicensingTypePage.submitLicensingType(LicensingType.NO_LICENSING)
                val checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(
                    checkLicensingAnswersPage.summaryName.getText(),
                    "You have removed this property’s licence",
                )
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("None")
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.value).containsText("None")
            }

            @Test
            fun `Update licensing flow does not show provide this later action`(page: Page) {
                featureFlagManager.enableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)

                val propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                assertThat(updateLicensingTypePage.provideThisLaterButton).isHidden()
            }

            @Test
            fun `A property can have its licensing number updated again from the check licensing answers page`(page: Page) {
                val firstNewLicenceNumber = "SL456"
                val secondNewLicenceNumber = "SL789"

                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage =
                    assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to selective
                updateLicensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
                var updateLicenceNumberPage =
                    assertPageIs(page, SelectiveLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(firstNewLicenceNumber)
                var checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Click change link for Licensing Number
                checkLicensingAnswersPage.summaryList.licensingNumberRow
                    .clickFirstActionLinkAndWait()
                updateLicenceNumberPage =
                    assertPageIs(page, SelectiveLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(secondNewLicenceNumber)
                checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(checkLicensingAnswersPage.summaryName.getText(), "You have updated the property licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("Selective licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingNumberRow.value).containsText(
                    secondNewLicenceNumber,
                )
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingTypeRow.value).containsText("Selective licence")
                assertThat(
                    propertyDetailsUpdatePage.propertyDetailsSummaryList.licensingNumberRow.value,
                ).containsText(secondNewLicenceNumber)
            }
        }

        @Nested
        inner class TenancyAndRentalInformation {
            private val occupiedPropertyOwnershipId = 1L
            private val occupiedPropertyUrlArguments =
                mapOf("propertyOwnershipId" to occupiedPropertyOwnershipId.toString())

            private val vacantPropertyOwnershipId = 7L
            private val vacantPropertyUrlArguments =
                mapOf("propertyOwnershipId" to vacantPropertyOwnershipId.toString())

            @Nested
            inner class OccupancyUpdates {
                @BeforeEach
                fun disableDelegateToLettingAgentFlag() {
                    // Without delegation the redesigned occupancy journey is a single page with no check answers page
                    featureFlagManager.disableFeature(DELEGATE_TO_LETTING_AGENT)
                }

                @Test
                fun `A property can have its occupancy updated from occupied to vacant`(page: Page) {
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow).isVisible()
                    val originalNumberOfBedrooms =
                        propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.value
                            .textContent()
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.value).containsText("1")
                    propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.clickFirstActionLinkAndWait()
                    val updateOccupancyPage =
                        assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)

                    // Update occupancy to vacant and submit directly (single-page update, no check answers page)
                    assertThat(updateOccupancyPage.form.fieldsetHeading).containsText("Update whether your property is occupied by tenants")
                    updateOccupancyPage.submitIsVacant()

                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check changes have occurred
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.value).containsText("No")
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow).isVisible()
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.value)
                        .containsText(originalNumberOfBedrooms)
                }

                @Test
                fun `A property can have its occupancy updated from vacant to occupied`(page: Page) {
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(vacantPropertyOwnershipId)
                    propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.clickFirstActionLinkAndWait()
                    val updateOccupancyPage =
                        assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, vacantPropertyUrlArguments)

                    // Update occupancy to occupied and submit directly (single-page update, no check answers page)
                    assertThat(updateOccupancyPage.form.fieldsetHeading).containsText("Update whether your property is occupied by tenants")
                    updateOccupancyPage.submitIsOccupied()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, vacantPropertyUrlArguments)

                    // The occupancy status is updated and the property defaults to providing tenancy details later
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.value).containsText("Yes")
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.tenancyRow.value).containsText("Provide")
                }
            }

            @Nested
            inner class OccupancyUpdatesWithLettingAgentDelegation {
                private val undelegatedPropertyOwnershipId = 4L
                private val undelegatedPropertyUrlArguments =
                    mapOf("propertyOwnershipId" to undelegatedPropertyOwnershipId.toString())

                @BeforeEach
                fun enableDelegateToLettingAgentFlag() {
                    featureFlagManager.enableFeature(DELEGATE_TO_LETTING_AGENT)
                }

                @Test
                fun `making a delegated property vacant shows the interruption and removes the delegation`(page: Page) {
                    // The property starts occupied and delegated to a letting agent
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    assertThat(propertyDetailsPage.removeLettingAgentLink.locator).isVisible()
                    propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.clickFirstActionLinkAndWait()

                    // Occupancy question page, then the interruption page and the check answers page (only shown
                    // when delegation is enabled)
                    val updateOccupancyPage =
                        assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)
                    updateOccupancyPage.submitIsVacant()
                    val interruptionPage =
                        assertPageIs(
                            page,
                            OccupancyLettingAgentInterruptionPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    assertThat(interruptionPage.heading).containsText("Are you sure you want to change this?")
                    assertThat(interruptionPage.body)
                        .containsText("your letting agent or property manager will be removed from this registration")

                    interruptionPage.submit()
                    val checkAnswersPage =
                        assertPageIs(
                            page,
                            UpdateOccupancyCheckYourAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    checkAnswersPage.confirm()

                    // Back on the property record: occupancy is updated and the delegation has been removed
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.value).containsText("No")
                    // The property is now vacant, so neither the remove nor the delegate letting agent link is shown
                    assertThat(propertyDetailsPage.removeLettingAgentLink.locator).hasCount(0)
                    assertThat(propertyDetailsPage.delegateToLettingAgentLink.locator).hasCount(0)
                }

                @Test
                fun `keeping a delegated property occupied shows the check answers page and retains the delegation`(page: Page) {
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    assertThat(propertyDetailsPage.removeLettingAgentLink.locator).isVisible()
                    propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.clickFirstActionLinkAndWait()

                    val updateOccupancyPage =
                        assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)
                    updateOccupancyPage.submitIsOccupied()
                    val checkAnswersPage =
                        assertPageIs(
                            page,
                            UpdateOccupancyCheckYourAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    checkAnswersPage.confirm()

                    // The property is still occupied, so the delegation is retained
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.value).containsText("Yes")
                    assertThat(propertyDetailsPage.removeLettingAgentLink.locator).isVisible()
                }

                @Test
                fun `going back from the interruption returns to the occupancy page without saving the change`(page: Page) {
                    val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.clickFirstActionLinkAndWait()
                    val updateOccupancyPage =
                        assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)

                    updateOccupancyPage.submitIsVacant()
                    val interruptionPage =
                        assertPageIs(
                            page,
                            OccupancyLettingAgentInterruptionPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    interruptionPage.goBackLink.clickAndWait()
                    assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)

                    // The change was never confirmed, so the property is still occupied and still delegated
                    val unchangedPropertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    assertThat(unchangedPropertyDetailsPage.propertyDetailsSummaryList.occupancyRow.value).containsText("Yes")
                    assertThat(unchangedPropertyDetailsPage.removeLettingAgentLink.locator).isVisible()
                }

                @Test
                fun `making an undelegated property vacant skips the interruption`(page: Page) {
                    val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(undelegatedPropertyOwnershipId)
                    propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.clickFirstActionLinkAndWait()
                    val updateOccupancyPage =
                        assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, undelegatedPropertyUrlArguments)

                    updateOccupancyPage.submitIsVacant()

                    val checkAnswersPage =
                        assertPageIs(
                            page,
                            UpdateOccupancyCheckYourAnswersPagePropertyDetailsUpdate::class,
                            undelegatedPropertyUrlArguments,
                        )
                    checkAnswersPage.confirm()

                    val updatedPropertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, undelegatedPropertyUrlArguments)
                    assertThat(updatedPropertyDetailsPage.propertyDetailsSummaryList.occupancyRow.value).containsText("No")
                }

                @Test
                fun `navigating directly to the interruption for an undelegated property redirects to the property record`(page: Page) {
                    navigator.navigate(
                        UpdateOccupancyController.getUpdateOccupancyRoute(undelegatedPropertyOwnershipId) +
                            "/${OccupancyLettingAgentInterruptionStep.ROUTE_SEGMENT}",
                    )
                    assertPageIs(page, PropertyDetailsPageLandlordView::class, undelegatedPropertyUrlArguments)
                }

                @Test
                fun `a property delegated after the journey started still shows the interruption`(page: Page) {
                    // Start the journey while the property has no letting agent, so any cached delegation would be false
                    val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(undelegatedPropertyOwnershipId)
                    propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.clickFirstActionLinkAndWait()
                    val updateOccupancyPage =
                        assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, undelegatedPropertyUrlArguments)

                    // The property is delegated elsewhere while the journey is in flight
                    delegatePropertyToLettingAgent(undelegatedPropertyOwnershipId)

                    updateOccupancyPage.submitIsVacant()

                    // The interruption is shown, because the delegation is read live rather than from the journey state
                    assertPageIs(
                        page,
                        OccupancyLettingAgentInterruptionPagePropertyDetailsUpdate::class,
                        undelegatedPropertyUrlArguments,
                    )
                }

                @Test
                fun `the occupancy answer can be changed from the check answers page`(page: Page) {
                    // The property starts occupied and delegated; make it vacant to reach the check answers page
                    navigator
                        .goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                        .propertyDetailsSummaryList.occupancyRow
                        .clickFirstActionLinkAndWait()

                    assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)
                        .submitIsVacant()
                    // Making a delegated property vacant shows the interruption before the check answers page
                    assertPageIs(
                        page,
                        OccupancyLettingAgentInterruptionPagePropertyDetailsUpdate::class,
                        occupiedPropertyUrlArguments,
                    ).submit()
                    val checkAnswersPage =
                        assertPageIs(
                            page,
                            UpdateOccupancyCheckYourAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    assertThat(checkAnswersPage.summaryList.occupancyRow.value).containsText("No")

                    // Change the answer back to occupied; keeping it occupied does not show the interruption
                    checkAnswersPage.clickChangeOccupancy()
                    assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)
                        .submitIsOccupied()
                    val updatedCheckAnswersPage =
                        assertPageIs(
                            page,
                            UpdateOccupancyCheckYourAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    assertThat(updatedCheckAnswersPage.summaryList.occupancyRow.value).containsText("Yes")

                    // Submitting keeps the property occupied and retains the delegation
                    updatedCheckAnswersPage.confirm()
                    val propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.occupancyRow.value).containsText("Yes")
                    assertThat(propertyDetailsPage.removeLettingAgentLink.locator).isVisible()
                }

                private fun delegatePropertyToLettingAgent(propertyOwnershipId: Long) {
                    val propertyOwnership = propertyOwnershipRepository.findById(propertyOwnershipId).get()
                    lettingAgentAccessRepository.save(
                        LettingAgentAccess(UUID.randomUUID(), "letting.agent@example.com", propertyOwnership),
                    )
                }
            }

            @Nested
            inner class HouseholdsAndTenantsUpdates {
                @Test
                fun `A property can have just their number of households and people updated`(page: Page) {
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.propertyDetailsSummaryList.numberOfHouseholdsRow.clickFirstActionLinkAndWait()
                    val updateNumberOfHouseholdsPage =
                        assertPageIs(
                            page,
                            NumberOfHouseholdsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update number of households
                    val newNumberOfHouseholds = 1
                    assertThat(updateNumberOfHouseholdsPage.header).containsText("Households in your property")
                    updateNumberOfHouseholdsPage.submitNumberOfHouseholds(newNumberOfHouseholds)
                    val updateNumberOfPeoplePage =
                        assertPageIs(
                            page,
                            HouseholdsNumberOfPeopleFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update number of people
                    val newNumberOfPeople = 3
                    assertThat(updateNumberOfPeoplePage.header).containsText("Update how many people live in your property")
                    updateNumberOfPeoplePage.submitNumOfPeople(newNumberOfPeople)
                    val checkOccupancyAnswersPage =
                        assertPageIs(
                            page,
                            CheckHouseholdsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check occupancy answers
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfHouseholdsRow).containsText(
                        newNumberOfHouseholds.toString(),
                    )
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfPeopleRow).containsText(newNumberOfPeople.toString())
                    checkOccupancyAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check changes have occurred
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfHouseholdsRow.value)
                        .containsText(newNumberOfHouseholds.toString())
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfPeopleRow.value)
                        .containsText(newNumberOfPeople.toString())
                }

                @Test
                fun `Leading zeros are stripped from households and people on the CYA page`(page: Page) {
                    // Details page
                    val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.propertyDetailsSummaryList.numberOfHouseholdsRow.clickFirstActionLinkAndWait()
                    val updateNumberOfHouseholdsPage =
                        assertPageIs(
                            page,
                            NumberOfHouseholdsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Submit number of households with leading zeros
                    updateNumberOfHouseholdsPage.submitNumberOfHouseholds("003")
                    val updateNumberOfPeoplePage =
                        assertPageIs(
                            page,
                            HouseholdsNumberOfPeopleFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Submit number of people with leading zeros
                    updateNumberOfPeoplePage.submitNumOfPeople("007")
                    val checkOccupancyAnswersPage =
                        assertPageIs(
                            page,
                            CheckHouseholdsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check CYA page displays values without leading zeros
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfHouseholdsRow).containsText("3")
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfPeopleRow).containsText("7")
                }
            }

            @Nested
            inner class NumberOfBedroomsUpdates {
                @Test
                fun `A property can have just its number of bedrooms updated`(page: Page) {
                    val newNumberOfBedrooms = 4
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    // Assert initial number of bedrooms is not 4
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.value)
                        .not()
                        .containsText(newNumberOfBedrooms.toString())
                    propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.clickFirstActionLinkAndWait()
                    val updateNumberOfBedroomsPage =
                        assertPageIs(
                            page,
                            NumberOfBedroomsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update number of bedrooms
                    assertThat(updateNumberOfBedroomsPage.header).containsText("Update how many bedrooms are in your property")
                    updateNumberOfBedroomsPage.submitNumOfBedrooms(newNumberOfBedrooms)
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check change has occurred
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.value)
                        .containsText(newNumberOfBedrooms.toString())
                }

                @Test
                fun `An unoccupied property can have its number of bedrooms updated on the standalone page`(page: Page) {
                    val newNumberOfBedrooms = 4

                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(vacantPropertyOwnershipId)
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow).isVisible()
                    propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.clickFirstActionLinkAndWait()
                    val updateNumberOfBedroomsPage =
                        assertPageIs(page, NumberOfBedroomsFormPagePropertyDetailsUpdate::class, vacantPropertyUrlArguments)

                    // Update number of bedrooms
                    assertThat(updateNumberOfBedroomsPage.header).containsText("Update how many bedrooms are in your property")
                    updateNumberOfBedroomsPage.submitNumOfBedrooms(newNumberOfBedrooms)
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, vacantPropertyUrlArguments)

                    // Check change has occurred
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.numberOfBedroomsRow.value)
                        .containsText(newNumberOfBedrooms.toString())
                }
            }

            @Nested
            inner class RentIncludesBills {
                @Test
                fun `A property can have its rent includes bills status updated`(page: Page) {
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    // Assert initial rent includes bills status is not Yes
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.rentIncludesBillsRow.value)
                        .not()
                        .containsText("Yes")
                    propertyDetailsPage.propertyDetailsSummaryList.rentIncludesBillsRow.clickFirstActionLinkAndWait()
                    val updateRentIncludesBillsPage =
                        assertPageIs(
                            page,
                            RentIncludesBillsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update rent includes bills to yes
                    assertThat(updateRentIncludesBillsPage.form.fieldsetHeading).containsText("Update whether the rent includes bills")
                    updateRentIncludesBillsPage.submitIsIncluded()
                    val billsIncludedPage =
                        assertPageIs(
                            page,
                            BillsIncludedFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update bills included
                    val expectedBillsIncluded = "Gas, Electricity, Water"
                    assertThat(billsIncludedPage.form.fieldsetHeading).containsText("Update which of these you include in the rent")
                    billsIncludedPage.selectGasElectricityWater()
                    billsIncludedPage.form.submit()
                    val checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check answers
                    assertThat(checkYourAnswersPage.summaryList.rentIncludesBillsRow).containsText("Yes")
                    assertThat(checkYourAnswersPage.summaryList.billsIncludedRow).containsText(expectedBillsIncluded)
                    checkYourAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.rentIncludesBillsRow.value)
                        .containsText("Yes")
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.billsIncludedRow).containsText(
                        expectedBillsIncluded,
                    )
                }

                @Test
                fun `Changing the rent includes bills status from the CYA page updates the property with the correct values`(page: Page) {
                    // start update journey
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.propertyDetailsSummaryList.rentIncludesBillsRow.clickFirstActionLinkAndWait()
                    var updateRentIncludesBillsPage =
                        assertPageIs(
                            page,
                            RentIncludesBillsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    // Select yes for rent includes bills
                    updateRentIncludesBillsPage.submitIsIncluded()
                    val billsIncludedPage =
                        assertPageIs(
                            page,
                            BillsIncludedFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    // Select bills included and submit
                    billsIncludedPage.selectGasElectricityWater()
                    billsIncludedPage.form.submit()
                    var checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Change rent includes bills answer to no
                    checkYourAnswersPage.summaryList.rentIncludesBillsRow.clickFirstActionLinkAndWait()
                    updateRentIncludesBillsPage =
                        assertPageIs(
                            page,
                            RentIncludesBillsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    updateRentIncludesBillsPage.submitIsNotIncluded()
                    checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Confirm answers
                    assertThat(checkYourAnswersPage.summaryList.rentIncludesBillsRow).containsText("No")
                    assertThat(checkYourAnswersPage.summaryList.billsIncludedRow).isHidden()
                    checkYourAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check update is correct
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.rentIncludesBillsRow.value)
                        .containsText("No")
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.billsIncludedRow).isHidden()
                }

                @Test
                fun `Changing the bills included answer from the CYA page updates the property with the correct values`(page: Page) {
                    // Start update journey and reach the CYA page with bills included set
                    val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.propertyDetailsSummaryList.rentIncludesBillsRow.clickFirstActionLinkAndWait()
                    val updateRentIncludesBillsPage =
                        assertPageIs(
                            page,
                            RentIncludesBillsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    updateRentIncludesBillsPage.submitIsIncluded()
                    val initialBillsIncludedPage =
                        assertPageIs(
                            page,
                            BillsIncludedFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    initialBillsIncludedPage.selectGasElectricityWater()
                    initialBillsIncludedPage.form.submit()
                    var checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Click the change link on the bills included row
                    checkYourAnswersPage.summaryList.billsIncludedRow.clickFirstActionLinkAndWait()
                    val billsIncludedPage =
                        assertPageIs(
                            page,
                            BillsIncludedFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Change the selection and submit
                    billsIncludedPage.form.billsIncludedCheckboxes.checkCheckbox(BillsIncluded.COUNCIL_TAX.toString())
                    billsIncludedPage.form.submit()
                    checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Confirm new value shown on CYA and persisted to property details
                    val expectedBillsIncluded = "Gas, Electricity, Water, Council Tax"
                    assertThat(checkYourAnswersPage.summaryList.billsIncludedRow).containsText(expectedBillsIncluded)
                    checkYourAnswersPage.confirm()
                    val updatedPropertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)
                    assertThat(updatedPropertyDetailsPage.propertyDetailsSummaryList.billsIncludedRow)
                        .containsText(expectedBillsIncluded)
                }
            }

            @Nested
            inner class FurnishedStatusUpdates {
                @Test
                fun `A property can have just its furniture status updated`(page: Page) {
                    val newFurnishedStatusValue = "Partly furnished"
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    // Assert initial furnished status is not FurnishedStatus.PART_FURNISHED
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.furnishedStatusRow.value)
                        .not()
                        .containsText(newFurnishedStatusValue)
                    propertyDetailsPage.propertyDetailsSummaryList.furnishedStatusRow.clickFirstActionLinkAndWait()
                    val updateFurnishedStatusPage =
                        assertPageIs(
                            page,
                            FurnishedStatusFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update furnished status
                    val newFurnishedStatus = FurnishedStatus.PART_FURNISHED
                    assertThat(updateFurnishedStatusPage.form.fieldsetHeading)
                        .containsText("Update is the property furnished")
                    updateFurnishedStatusPage.submitFurnishedStatus(newFurnishedStatus)
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check change has occurred
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.furnishedStatusRow.value)
                        .containsText(newFurnishedStatusValue)
                }
            }

            @Nested
            inner class RentFrequencyAndAmountUpdates {
                @Test
                fun `A property can have its rentFrequency and amount updated`(page: Page) {
                    val newRentFrequency = RentFrequency.WEEKLY
                    val newRentFrequencyDisplayName = "Weekly"
                    val newRentAmount = "200"
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    // Assert initial rent frequency is not newRentFrequency
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.rentFrequencyRow.value)
                        .not()
                        .containsText(newRentFrequencyDisplayName)
                    // Assert initial rent amount is not newRentAmount
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.rentAmountRow.value)
                        .not()
                        .containsText(newRentAmount)
                    propertyDetailsPage.propertyDetailsSummaryList.rentFrequencyRow.clickFirstActionLinkAndWait()
                    val rentFrequencyPage =
                        assertPageIs(
                            page,
                            RentFrequencyFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update rent frequency
                    assertThat(rentFrequencyPage.header).containsText("Update when you charge rent")
                    rentFrequencyPage.selectRentFrequency(newRentFrequency)
                    rentFrequencyPage.form.submit()
                    val rentAmountPage =
                        assertPageIs(page, RentAmountFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)

                    // Update rent amount
                    assertThat(rentAmountPage.header).containsText("Update your weekly rent")
                    rentAmountPage.submitRentAmount(newRentAmount)
                    val checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentFrequencyAndAmountAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check answers
                    assertThat(checkYourAnswersPage.summaryList.rentFrequencyRow).containsText(
                        newRentFrequencyDisplayName,
                    )
                    assertThat(checkYourAnswersPage.summaryList.rentAmountRow).containsText(newRentAmount)
                    checkYourAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.rentFrequencyRow).containsText(
                        newRentFrequencyDisplayName,
                    )
                    assertThat(propertyDetailsPage.propertyDetailsSummaryList.rentAmountRow).containsText(newRentAmount)
                }

                @Test
                fun `Leading zeros are stripped from rent amount on the CYA page`(page: Page) {
                    // Details page
                    val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.propertyDetailsSummaryList.rentFrequencyRow.clickFirstActionLinkAndWait()
                    val rentFrequencyPage =
                        assertPageIs(
                            page,
                            RentFrequencyFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Submit any rent frequency
                    rentFrequencyPage.selectRentFrequency(RentFrequency.MONTHLY)
                    rentFrequencyPage.form.submit()
                    val rentAmountPage =
                        assertPageIs(page, RentAmountFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)

                    // Submit rent amount with leading zeros
                    rentAmountPage.submitRentAmount("00500.50")
                    val checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentFrequencyAndAmountAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check CYA page displays value without leading zeros
                    assertThat(checkYourAnswersPage.summaryList.rentAmountRow).containsText("£500.50")
                }
            }
        }
    }

    // TODO PDJB-1340: Remove tests when the PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING Feature Flag is removed
    @Nested
    inner class RestructureAndSkippingDisabled {
        @BeforeEach
        fun disableRestructureAndSkippingFlag() {
            featureFlagManager.disableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
        }

        @Nested
        inner class OwnershipTypeUpdates {
            @Test
            fun `A property can have its ownership type updated`(page: Page) {
                // Details page
                var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsPage.beforePdjb939SummaryList.ownershipTypeRow.clickFirstActionLinkAndWait()
                val updateOwnershipTypePage = assertPageIs(page, OwnershipTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update Ownership Type page
                updateOwnershipTypePage.submitOwnershipType(OwnershipType.LEASEHOLD)
                propertyDetailsPage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(propertyDetailsPage.beforePdjb939SummaryList.ownershipTypeRow.value).containsText("Leasehold")
            }
        }

        @Nested
        inner class LicenceUpdates {
            @Test
            fun `A property can have its licensing updated to a selective licence`(page: Page) {
                val newLicenceNumber = "SL123"

                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to selective
                updateLicensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
                val updateLicenceNumberPage =
                    assertPageIs(page, SelectiveLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(newLicenceNumber)
                val checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(checkLicensingAnswersPage.summaryName.getText(), "You have updated the property licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("Selective licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingNumberRow.value).containsText(newLicenceNumber)
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.value).containsText("Selective licence")
                assertThat(propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingNumberRow.value).containsText(newLicenceNumber)
            }

            @Test
            fun `A property can have its licensing updated to a HMO Mandatory licence`(page: Page) {
                val newLicenceNumber = "MAND123"

                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to HMO mandatory
                updateLicensingTypePage.submitLicensingType(LicensingType.HMO_MANDATORY_LICENCE)
                val updateLicenceNumberPage =
                    assertPageIs(page, HmoMandatoryLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(newLicenceNumber)
                val checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(checkLicensingAnswersPage.summaryName.getText(), "You have updated the property licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("HMO mandatory licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingNumberRow.value).containsText(newLicenceNumber)
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(
                    propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.value,
                ).containsText("HMO mandatory licence")
                assertThat(propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingNumberRow.value).containsText(newLicenceNumber)
            }

            @Test
            fun `A property can have its licensing updated to a HMO additional licence`(page: Page) {
                val newLicenceNumber = "ADD123"

                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to HMO additional
                updateLicensingTypePage.submitLicensingType(LicensingType.HMO_ADDITIONAL_LICENCE)
                val updateLicenceNumberPage =
                    assertPageIs(page, HmoAdditionalLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(newLicenceNumber)
                val checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(checkLicensingAnswersPage.summaryName.getText(), "You have updated the property licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("HMO additional licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingNumberRow.value).containsText(newLicenceNumber)
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(
                    propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.value,
                ).containsText("HMO additional licence")
                assertThat(propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingNumberRow.value).containsText(newLicenceNumber)
            }

            @Test
            fun `A property can have its licensing removed`(page: Page) {
                // A property ownership with an existing licence
                val propertyOwnershipId = 7L
                val urlArguments = mapOf("propertyOwnershipId" to propertyOwnershipId.toString())

                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to no licensing
                updateLicensingTypePage.submitLicensingType(LicensingType.NO_LICENSING)
                val checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(
                    checkLicensingAnswersPage.summaryName.getText(),
                    "You have removed this property’s licence",
                )
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("None")
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.value).containsText("None")
            }

            @Test
            fun `A property can have its licensing number updated again from the check licensing answers page`(page: Page) {
                val firstNewLicenceNumber = "SL456"
                val secondNewLicenceNumber = "SL789"

                // Details page
                var propertyDetailsUpdatePage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
                propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.clickFirstActionLinkAndWait()
                val updateLicensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence to selective
                updateLicensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
                var updateLicenceNumberPage =
                    assertPageIs(page, SelectiveLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(firstNewLicenceNumber)
                var checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Click change link for Licensing Number
                checkLicensingAnswersPage.summaryList.licensingNumberRow
                    .clickFirstActionLinkAndWait()
                updateLicenceNumberPage =
                    assertPageIs(page, SelectiveLicenceFormPagePropertyDetailsUpdate::class, urlArguments)

                // Update licence number
                updateLicenceNumberPage.submitLicenseNumber(secondNewLicenceNumber)
                checkLicensingAnswersPage =
                    assertPageIs(page, CheckLicensingAnswersPagePropertyDetailsUpdate::class, urlArguments)

                // Check licensing answers
                assertContains(checkLicensingAnswersPage.summaryName.getText(), "You have updated the property licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingTypeRow.value).containsText("Selective licence")
                assertThat(checkLicensingAnswersPage.summaryList.licensingNumberRow.value).containsText(
                    secondNewLicenceNumber,
                )
                checkLicensingAnswersPage.confirm()
                propertyDetailsUpdatePage = assertPageIs(page, PropertyDetailsPageLandlordView::class, urlArguments)

                // Check changes have occurred
                assertThat(propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingTypeRow.value).containsText("Selective licence")
                assertThat(
                    propertyDetailsUpdatePage.beforePdjb939SummaryList.licensingNumberRow.value,
                ).containsText(secondNewLicenceNumber)
            }
        }

        @Nested
        inner class TenancyAndRentalInformation {
            private val occupiedPropertyOwnershipId = 1L
            private val occupiedPropertyUrlArguments =
                mapOf("propertyOwnershipId" to occupiedPropertyOwnershipId.toString())

            private val vacantPropertyOwnershipId = 7L
            private val vacantPropertyUrlArguments =
                mapOf("propertyOwnershipId" to vacantPropertyOwnershipId.toString())

            @Nested
            inner class OccupancyUpdates {
                @Test
                fun `A property can have its occupancy updated from occupied to vacant`(page: Page) {
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.beforePdjb939SummaryList.occupancyRow.clickFirstActionLinkAndWait()
                    val updateOccupancyPage =
                        assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)

                    // Update occupancy to vacant
                    assertThat(updateOccupancyPage.form.fieldsetHeading).containsText("Update whether your property is occupied by tenants")
                    updateOccupancyPage.submitIsVacant()
                    val checkOccupancyAnswersPage =
                        assertPageIs(
                            page,
                            CheckOccupancyAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check occupancy answers
                    assertThat(checkOccupancyAnswersPage.summaryList.occupancyRow).containsText("No")
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfHouseholdsRow).isHidden()
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfPeopleRow).isHidden()
                    checkOccupancyAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check changes have occurred
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.occupancyRow.value).containsText("No")
                }

                @Test
                fun `A property can have its occupancy updated from vacant to occupied`(page: Page) {
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(vacantPropertyOwnershipId)
                    propertyDetailsPage.beforePdjb939SummaryList.occupancyRow.clickFirstActionLinkAndWait()
                    val updateOccupancyPage = assertPageIs(page, OccupancyFormPagePropertyDetailsUpdate::class, vacantPropertyUrlArguments)

                    // Update occupancy to occupied
                    assertThat(updateOccupancyPage.form.fieldsetHeading).containsText("Update whether your property is occupied by tenants")
                    updateOccupancyPage.submitIsOccupied()
                    val updateNumberOfHouseholdsPage =
                        assertPageIs(
                            page,
                            OccupancyNumberOfHouseholdsFormPagePropertyDetailsUpdate::class,
                            vacantPropertyUrlArguments,
                        )

                    // Update number of households
                    val newNumberOfHouseholds = 1
                    assertThat(updateNumberOfHouseholdsPage.header).containsText("Update how many households live in your property")
                    updateNumberOfHouseholdsPage.submitNumberOfHouseholds(newNumberOfHouseholds)
                    val updateNumberOfPeoplePage =
                        assertPageIs(
                            page,
                            OccupancyNumberOfPeopleFormPagePropertyDetailsUpdate::class,
                            vacantPropertyUrlArguments,
                        )

                    // Update number of people
                    val newNumberOfPeople = 3
                    assertThat(updateNumberOfPeoplePage.header).containsText("Update how many people live in your property")
                    updateNumberOfPeoplePage.submitNumOfPeople(newNumberOfPeople)
                    val bedroomsPage =
                        assertPageIs(
                            page,
                            OccupancyNumberOfBedroomsFormPagePropertyDetailsUpdate::class,
                            vacantPropertyUrlArguments,
                        )

                    // Update number of bedrooms
                    val newNumberOfBedrooms = 3
                    assertThat(bedroomsPage.header).containsText("Update how many bedrooms are in your property")
                    bedroomsPage.submitNumOfBedrooms(newNumberOfBedrooms)
                    val rentIncludesBillsPage =
                        assertPageIs(
                            page,
                            OccupancyRentIncludesBillsFormPagePropertyDetailsUpdate::class,
                            vacantPropertyUrlArguments,
                        )

                    // Update rent include bills
                    assertThat(rentIncludesBillsPage.form.fieldsetHeading).containsText("Update whether the rent includes bills")
                    rentIncludesBillsPage.submitIsIncluded()
                    val billsIncludedPage =
                        assertPageIs(
                            page,
                            OccupancyBillsIncludedFormPagePropertyDetailsUpdate::class,
                            vacantPropertyUrlArguments,
                        )

                    // Update bills included
                    val expectedBillsIncluded = "Gas, Electricity, Water"
                    assertThat(billsIncludedPage.form.fieldsetHeading).containsText("Update which of these you include in the rent")
                    billsIncludedPage.selectGasElectricityWater()
                    billsIncludedPage.form.submit()
                    val furnishedPage =
                        assertPageIs(
                            page,
                            OccupancyFurnishedStatusFormPagePropertyDetailsUpdate::class,
                            vacantPropertyUrlArguments,
                        )

                    // Update furnished status
                    val expectedFurnishedStatus = "Furnished"
                    assertThat(
                        furnishedPage.form.fieldsetHeading,
                    ).containsText("Update is the property furnished")
                    furnishedPage.submitFurnishedStatus(FurnishedStatus.FURNISHED)
                    val rentFrequencyPage =
                        assertPageIs(
                            page,
                            OccupancyRentFrequencyFormPagePropertyDetailsUpdate::class,
                            vacantPropertyUrlArguments,
                        )

                    // Update rent frequency
                    val expectedRentFrequency = "Weekly"
                    assertThat(rentFrequencyPage.header).containsText("Update when you charge rent")
                    rentFrequencyPage.selectRentFrequency(RentFrequency.WEEKLY)
                    rentFrequencyPage.form.submit()
                    val rentAmountPage =
                        assertPageIs(
                            page,
                            OccupancyRentAmountFormPagePropertyDetailsUpdate::class,
                            vacantPropertyUrlArguments,
                        )

                    // Update rent amount
                    val expectedRentAmount = "£400"
                    assertThat(rentAmountPage.header).containsText("Update your weekly rent")
                    rentAmountPage.submitRentAmount("400")
                    val checkOccupancyAnswersPage =
                        assertPageIs(
                            page,
                            CheckOccupancyAnswersPagePropertyDetailsUpdate::class,
                            vacantPropertyUrlArguments,
                        )
                    // Check occupancy answers
                    assertThat(checkOccupancyAnswersPage.summaryList.occupancyRow).containsText("Yes")
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfHouseholdsRow).containsText(
                        newNumberOfHouseholds.toString(),
                    )
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfPeopleRow).containsText(newNumberOfPeople.toString())
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfBedroomsRow).containsText(
                        newNumberOfBedrooms.toString(),
                    )
                    assertThat(checkOccupancyAnswersPage.summaryList.rentIncludesBillsRow).containsText("Yes")
                    assertThat(checkOccupancyAnswersPage.summaryList.billsIncludedRow).containsText(
                        expectedBillsIncluded,
                    )
                    assertThat(checkOccupancyAnswersPage.summaryList.furnishedStatusRow).containsText(
                        expectedFurnishedStatus,
                    )
                    assertThat(checkOccupancyAnswersPage.summaryList.rentFrequencyRow).containsText(
                        expectedRentFrequency,
                    )
                    assertThat(checkOccupancyAnswersPage.summaryList.rentAmountRow).containsText(expectedRentAmount)
                    checkOccupancyAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, vacantPropertyUrlArguments)

                    // Check changes have occurred
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.occupancyRow.value).containsText("Yes")
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.numberOfHouseholdsRow.value)
                        .containsText(newNumberOfHouseholds.toString())
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.numberOfPeopleRow.value)
                        .containsText(newNumberOfPeople.toString())
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.numberOfBedroomsRow.value)
                        .containsText(newNumberOfBedrooms.toString())
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentIncludesBillsRow.value).containsText("Yes")
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.billsIncludedRow.value).containsText(expectedBillsIncluded)
                    assertThat(
                        propertyDetailsPage.beforePdjb939SummaryList.furnishedStatusRow.value,
                    ).containsText(expectedFurnishedStatus)
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentFrequencyRow.value).containsText(expectedRentFrequency)
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentAmountRow.value).containsText(expectedRentAmount)
                }
            }

            @Nested
            inner class HouseholdsAndTenantsUpdates {
                @Test
                fun `A property can have just their number of households and people updated`(page: Page) {
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.beforePdjb939SummaryList.numberOfHouseholdsRow.clickFirstActionLinkAndWait()
                    val updateNumberOfHouseholdsPage =
                        assertPageIs(
                            page,
                            NumberOfHouseholdsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update number of households
                    val newNumberOfHouseholds = 1
                    assertThat(updateNumberOfHouseholdsPage.header).containsText("Update how many households live in your property")
                    updateNumberOfHouseholdsPage.submitNumberOfHouseholds(newNumberOfHouseholds)
                    val updateNumberOfPeoplePage =
                        assertPageIs(
                            page,
                            HouseholdsNumberOfPeopleFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update number of people
                    val newNumberOfPeople = 3
                    assertThat(updateNumberOfPeoplePage.header).containsText("Update how many people live in your property")
                    updateNumberOfPeoplePage.submitNumOfPeople(newNumberOfPeople)
                    val checkOccupancyAnswersPage =
                        assertPageIs(
                            page,
                            CheckHouseholdsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check occupancy answers
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfHouseholdsRow).containsText(
                        newNumberOfHouseholds.toString(),
                    )
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfPeopleRow).containsText(newNumberOfPeople.toString())
                    checkOccupancyAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check changes have occurred
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.numberOfHouseholdsRow.value)
                        .containsText(newNumberOfHouseholds.toString())
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.numberOfPeopleRow.value)
                        .containsText(newNumberOfPeople.toString())
                }

                @Test
                fun `Leading zeros are stripped from households and people on the CYA page`(page: Page) {
                    // Details page
                    val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.beforePdjb939SummaryList.numberOfHouseholdsRow.clickFirstActionLinkAndWait()
                    val updateNumberOfHouseholdsPage =
                        assertPageIs(
                            page,
                            NumberOfHouseholdsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Submit number of households with leading zeros
                    updateNumberOfHouseholdsPage.submitNumberOfHouseholds("003")
                    val updateNumberOfPeoplePage =
                        assertPageIs(
                            page,
                            HouseholdsNumberOfPeopleFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Submit number of people with leading zeros
                    updateNumberOfPeoplePage.submitNumOfPeople("007")
                    val checkOccupancyAnswersPage =
                        assertPageIs(
                            page,
                            CheckHouseholdsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check CYA page displays values without leading zeros
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfHouseholdsRow).containsText("3")
                    assertThat(checkOccupancyAnswersPage.summaryList.numberOfPeopleRow).containsText("7")
                }
            }

            @Nested
            inner class NumberOfBedroomsUpdates {
                @Test
                fun `A property can have just its number of bedrooms updated`(page: Page) {
                    val newNumberOfBedrooms = 4
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    // Assert initial number of bedrooms is not 4
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.numberOfBedroomsRow.value)
                        .not()
                        .containsText(newNumberOfBedrooms.toString())
                    propertyDetailsPage.beforePdjb939SummaryList.numberOfBedroomsRow.clickFirstActionLinkAndWait()
                    val updateNumberOfBedroomsPage =
                        assertPageIs(
                            page,
                            NumberOfBedroomsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update number of bedrooms
                    assertThat(updateNumberOfBedroomsPage.header).containsText("Update how many bedrooms are in your property")
                    updateNumberOfBedroomsPage.submitNumOfBedrooms(newNumberOfBedrooms)
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check change has occurred
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.numberOfBedroomsRow.value)
                        .containsText(newNumberOfBedrooms.toString())
                }
            }

            @Nested
            inner class RentIncludesBills {
                @Test
                fun `A property can have its rent includes bills status updated`(page: Page) {
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    // Assert initial rent includes bills status is not Yes
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentIncludesBillsRow.value)
                        .not()
                        .containsText("Yes")
                    propertyDetailsPage.beforePdjb939SummaryList.rentIncludesBillsRow.clickFirstActionLinkAndWait()
                    val updateRentIncludesBillsPage =
                        assertPageIs(
                            page,
                            RentIncludesBillsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update rent includes bills to yes
                    assertThat(updateRentIncludesBillsPage.form.fieldsetHeading).containsText("Update whether the rent includes bills")
                    updateRentIncludesBillsPage.submitIsIncluded()
                    val billsIncludedPage =
                        assertPageIs(
                            page,
                            BillsIncludedFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update bills included
                    val expectedBillsIncluded = "Gas, Electricity, Water"
                    assertThat(billsIncludedPage.form.fieldsetHeading).containsText("Update which of these you include in the rent")
                    billsIncludedPage.selectGasElectricityWater()
                    billsIncludedPage.form.submit()
                    val checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check answers
                    assertThat(checkYourAnswersPage.summaryList.rentIncludesBillsRow).containsText("Yes")
                    assertThat(checkYourAnswersPage.summaryList.billsIncludedRow).containsText(expectedBillsIncluded)
                    checkYourAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentIncludesBillsRow.value)
                        .containsText("Yes")
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.billsIncludedRow).containsText(expectedBillsIncluded)
                }

                @Test
                fun `Changing the rent includes bills status from the CYA page updates the property with the correct values`(page: Page) {
                    // start update journey
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.beforePdjb939SummaryList.rentIncludesBillsRow.clickFirstActionLinkAndWait()
                    var updateRentIncludesBillsPage =
                        assertPageIs(
                            page,
                            RentIncludesBillsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    // Select yes for rent includes bills
                    updateRentIncludesBillsPage.submitIsIncluded()
                    val billsIncludedPage =
                        assertPageIs(
                            page,
                            BillsIncludedFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    // Select bills included and submit
                    billsIncludedPage.selectGasElectricityWater()
                    billsIncludedPage.form.submit()
                    var checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Change rent includes bills answer to no
                    checkYourAnswersPage.summaryList.rentIncludesBillsRow.clickFirstActionLinkAndWait()
                    updateRentIncludesBillsPage =
                        assertPageIs(
                            page,
                            RentIncludesBillsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    updateRentIncludesBillsPage.submitIsNotIncluded()
                    checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Confirm answers
                    assertThat(checkYourAnswersPage.summaryList.rentIncludesBillsRow).containsText("No")
                    assertThat(checkYourAnswersPage.summaryList.billsIncludedRow).isHidden()
                    checkYourAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check update is correct
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentIncludesBillsRow.value)
                        .containsText("No")
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.billsIncludedRow).isHidden()
                }

                @Test
                fun `Changing the bills included answer from the CYA page updates the property with the correct values`(page: Page) {
                    // Start update journey and reach the CYA page with bills included set
                    val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.beforePdjb939SummaryList.rentIncludesBillsRow.clickFirstActionLinkAndWait()
                    val updateRentIncludesBillsPage =
                        assertPageIs(
                            page,
                            RentIncludesBillsFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    updateRentIncludesBillsPage.submitIsIncluded()
                    val initialBillsIncludedPage =
                        assertPageIs(
                            page,
                            BillsIncludedFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )
                    initialBillsIncludedPage.selectGasElectricityWater()
                    initialBillsIncludedPage.form.submit()
                    var checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Click the change link on the bills included row
                    checkYourAnswersPage.summaryList.billsIncludedRow.clickFirstActionLinkAndWait()
                    val billsIncludedPage =
                        assertPageIs(
                            page,
                            BillsIncludedFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Change the selection and submit
                    billsIncludedPage.form.billsIncludedCheckboxes.checkCheckbox(BillsIncluded.COUNCIL_TAX.toString())
                    billsIncludedPage.form.submit()
                    checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentIncludesBillsAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Confirm new value shown on CYA and persisted to property details
                    val expectedBillsIncluded = "Gas, Electricity, Water, Council Tax"
                    assertThat(checkYourAnswersPage.summaryList.billsIncludedRow).containsText(expectedBillsIncluded)
                    checkYourAnswersPage.confirm()
                    val updatedPropertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)
                    assertThat(updatedPropertyDetailsPage.beforePdjb939SummaryList.billsIncludedRow)
                        .containsText(expectedBillsIncluded)
                }
            }

            @Nested
            inner class FurnishedStatusUpdates {
                @Test
                fun `A property can have just its furniture status updated`(page: Page) {
                    val newFurnishedStatusValue = "Partly furnished"
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    // Assert initial furnished status is not FurnishedStatus.PART_FURNISHED
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.furnishedStatusRow.value)
                        .not()
                        .containsText(newFurnishedStatusValue)
                    propertyDetailsPage.beforePdjb939SummaryList.furnishedStatusRow.clickFirstActionLinkAndWait()
                    val updateFurnishedStatusPage =
                        assertPageIs(
                            page,
                            FurnishedStatusFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update furnished status
                    val newFurnishedStatus = FurnishedStatus.PART_FURNISHED
                    assertThat(updateFurnishedStatusPage.form.fieldsetHeading)
                        .containsText("Update is the property furnished")
                    updateFurnishedStatusPage.submitFurnishedStatus(newFurnishedStatus)
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    // Check change has occurred
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.furnishedStatusRow.value)
                        .containsText(newFurnishedStatusValue)
                }
            }

            @Nested
            inner class RentFrequencyAndAmountUpdates {
                @Test
                fun `A property can have its rentFrequency and amount updated`(page: Page) {
                    val newRentFrequency = RentFrequency.WEEKLY
                    val newRentFrequencyDisplayName = "Weekly"
                    val newRentAmount = "200"
                    // Details page
                    var propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    // Assert initial rent frequency is not newRentFrequency
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentFrequencyRow.value)
                        .not()
                        .containsText(newRentFrequencyDisplayName)
                    // Assert initial rent amount is not newRentAmount
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentAmountRow.value)
                        .not()
                        .containsText(newRentAmount)
                    propertyDetailsPage.beforePdjb939SummaryList.rentFrequencyRow.clickFirstActionLinkAndWait()
                    val rentFrequencyPage =
                        assertPageIs(
                            page,
                            RentFrequencyFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Update rent frequency
                    assertThat(rentFrequencyPage.header).containsText("Update when you charge rent")
                    rentFrequencyPage.selectRentFrequency(newRentFrequency)
                    rentFrequencyPage.form.submit()
                    val rentAmountPage =
                        assertPageIs(page, RentAmountFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)

                    // Update rent amount
                    assertThat(rentAmountPage.header).containsText("Update your weekly rent")
                    rentAmountPage.submitRentAmount(newRentAmount)
                    val checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentFrequencyAndAmountAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check answers
                    assertThat(checkYourAnswersPage.summaryList.rentFrequencyRow).containsText(
                        newRentFrequencyDisplayName,
                    )
                    assertThat(checkYourAnswersPage.summaryList.rentAmountRow).containsText(newRentAmount)
                    checkYourAnswersPage.confirm()
                    propertyDetailsPage =
                        assertPageIs(page, PropertyDetailsPageLandlordView::class, occupiedPropertyUrlArguments)

                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentFrequencyRow).containsText(newRentFrequencyDisplayName)
                    assertThat(propertyDetailsPage.beforePdjb939SummaryList.rentAmountRow).containsText(newRentAmount)
                }

                @Test
                fun `Leading zeros are stripped from rent amount on the CYA page`(page: Page) {
                    // Details page
                    val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(occupiedPropertyOwnershipId)
                    propertyDetailsPage.beforePdjb939SummaryList.rentFrequencyRow.clickFirstActionLinkAndWait()
                    val rentFrequencyPage =
                        assertPageIs(
                            page,
                            RentFrequencyFormPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Submit any rent frequency
                    rentFrequencyPage.selectRentFrequency(RentFrequency.MONTHLY)
                    rentFrequencyPage.form.submit()
                    val rentAmountPage =
                        assertPageIs(page, RentAmountFormPagePropertyDetailsUpdate::class, occupiedPropertyUrlArguments)

                    // Submit rent amount with leading zeros
                    rentAmountPage.submitRentAmount("00500.50")
                    val checkYourAnswersPage =
                        assertPageIs(
                            page,
                            CheckRentFrequencyAndAmountAnswersPagePropertyDetailsUpdate::class,
                            occupiedPropertyUrlArguments,
                        )

                    // Check CYA page displays value without leading zeros
                    assertThat(checkYourAnswersPage.summaryList.rentAmountRow).containsText("£500.50")
                }
            }
        }
    }
}

package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor.captor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.communities.prsdb.webapp.clients.EpcRegisterClient
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.database.entity.SavedJourneyState
import uk.gov.communities.prsdb.webapp.database.repository.SavedJourneyStateRepository
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BackLink
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ErrorPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.BillsIncludedFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckElectricalCertUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasCertUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckJointLandlordsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceEmailFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceLookupAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceSelectAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ElectricalCertExpiryDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcInDateAtStartOfTenancyCheckPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertExpiredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertIssueDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasElectricalCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasEpcFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasSupplyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasMeesExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HmoAdditionalLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.IsEpcRequiredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LettingAgentEmailPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LicensingTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.MeesExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfHouseholdsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfPeopleFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OccupancyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OwnershipTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideTenancyDetailsLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RentAmountFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SelectiveLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockEpcData
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyRegistrationCheckAnswersSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @MockitoBean
    private lateinit var epcRegisterClient: EpcRegisterClient

    @MockitoSpyBean
    private lateinit var savedJourneyStateRepository: SavedJourneyStateRepository

    @Nested
    inner class Confirmation {
        @Test
        fun `navigating here with an incomplete form returns a 400 error page`(page: Page) {
            navigator.navigateToPropertyRegistrationConfirmationPage()
            val errorPage = assertPageIs(page, ErrorPage::class)
            BaseComponent.assertThat(errorPage.heading).containsText("Sorry, there is a problem with the service")
        }
    }

    @Nested
    inner class PropertyRegistrationStepCheckAnswers {
        @Test
        fun `after changing an answer, submitting a full section saves the state and returns the CYA page`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            var checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.summaryList.ownershipRow.actions.firstActionLink
                .clickAndWait()
            val ownershipPage = assertPageIs(page, OwnershipTypeFormPagePropertyRegistration::class)

            ownershipPage.submitOwnershipType(OwnershipType.LEASEHOLD)
            checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.summaryList.licensingRow.actions.firstActionLink
                .clickAndWait()
            val licensingTypePage = assertPageIs(page, LicensingTypeFormPagePropertyRegistration::class)

            licensingTypePage.submitLicensingType(LicensingType.HMO_ADDITIONAL_LICENCE)
            val licenceNumberPage = assertPageIs(page, HmoAdditionalLicenceFormPagePropertyRegistration::class)
            licenceNumberPage.submitLicenseNumber("licence number")
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            // Confirmation - verify record saved
            val savedJourneyStateCaptor = captor<SavedJourneyState>()
            verify(savedJourneyStateRepository, times(2)).save(savedJourneyStateCaptor.capture())
            val savedJourneyStateAfterOwnershipUpdate = savedJourneyStateCaptor.allValues[0]
            val savedJourneyStateAfterLicensingUpdate = savedJourneyStateCaptor.allValues[1]
            assertTrue(savedJourneyStateAfterOwnershipUpdate.serializedState.contains("ownershipType\":\"LEASEHOLD\""))
            assertTrue(savedJourneyStateAfterOwnershipUpdate.serializedState.contains("licensingType\":\"NO_LICENSING\""))
            assertTrue(savedJourneyStateAfterLicensingUpdate.serializedState.contains("licensingType\":\"HMO_ADDITIONAL_LICENCE\""))
            assertTrue(savedJourneyStateAfterLicensingUpdate.serializedState.contains("licenceNumber\":\"licence number\""))
        }

        @Test
        fun `the gas supply change link starts a CYA sub-journey that returns to the property registration CYA on submit`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            checkAnswersPage.complianceSummaryList.gasSupplyRow.clickFirstActionLinkAndWait()
            val hasGasSupplyPage = assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)
            hasGasSupplyPage.submitHasNoGasSupply()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the occupancy change link navigates to the occupancy page and changing from occupied to unoccupied returns to the CYA page`(
            page: Page,
        ) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()
            assertThat(checkAnswersPage.summaryList.occupancyQuestionRow.value).containsText("Yes")

            checkAnswersPage.summaryList.occupancyQuestionRow.actions.firstActionLink
                .clickAndWait()
            val occupancyPage = assertPageIs(page, OccupancyFormPagePropertyRegistration::class)
            occupancyPage.submitIsVacant()
            val updatedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(updatedCheckAnswersPage.summaryList.occupancyQuestionRow.value).containsText("No")
        }

        @Test
        fun `when landlord provides details, rented out section is shown and email row is hidden`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswersOccupied()
                        .withLandlordProvidesRentalDetails()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            BaseComponent.assertThat(checkAnswersPage.rentedOutHeading).isVisible()
            BaseComponent.assertThat(checkAnswersPage.lettingAgentDelegationSubheading).isVisible()
            assertThat(checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.value).containsText("I will provide these details")
            assertThat(checkAnswersPage.summaryList.lettingAgentEmailRow.key).hasCount(0)
            BaseComponent.assertThat(checkAnswersPage.lettingAgentDelegationBodyText).isHidden()
        }

        @Test
        fun `delegated occupied property CYA displays required sections and letting agent details`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswersOccupied()
                        .withLettingAgentProvidesRentalDetails()
                        .withSubmittedValue(
                            LettingAgentEmailStep.ROUTE_SEGMENT,
                            AllowLettingAgentEmailFormModel().apply { emailAddress = "letting.agent@example.com" },
                        ).withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            val headings = checkAnswersPage.sectionHeadings
            assertEquals(
                listOf(
                    "About your property",
                    "Property details",
                    "Ownership and landlords",
                    "Who the council should contact",
                    "Tell us if your property’s occupied",
                    "How your property’s rented out",
                    "Who will provide these details",
                ),
                headings,
            )
            BaseComponent.assertThat(checkAnswersPage.rentedOutHeading).isVisible()
            BaseComponent.assertThat(checkAnswersPage.lettingAgentDelegationSubheading).isVisible()
            BaseComponent.assertThat(checkAnswersPage.lettingAgentDelegationBodyText).isVisible()
            assertThat(checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.value).containsText("My letting agent or property manager")
            BaseComponent
                .assertThat(
                    checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.actions
                        .getActionLink("Change"),
                ).isVisible()
            assertThat(
                checkAnswersPage.summaryList.lettingAgentEmailRow.key,
            ).containsText("Letting agent or property manager’s email address")
            assertThat(checkAnswersPage.summaryList.lettingAgentEmailRow.value).containsText("letting.agent@example.com")
            BaseComponent
                .assertThat(
                    checkAnswersPage.summaryList.lettingAgentEmailRow.actions
                        .getActionLink("Change"),
                ).isVisible()
            checkAnswersPage.summaryList.lettingAgentEmailRow.clickFirstActionLinkAndWait()
            val emailPage = assertPageIs(page, LettingAgentEmailPagePropertyRegistration::class)

            emailPage.submitEmail("new.agent@example.com")

            val updatedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(updatedCheckAnswersPage.summaryList.lettingAgentEmailRow.value)
                .containsText("new.agent@example.com")
        }

        @Test
        fun `when delegating to a letting agent, the EPC section is hidden`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswersDelegatedToLettingAgent()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            BaseComponent.assertThat(checkAnswersPage.epcHeading).isHidden()
            assertThat(checkAnswersPage.complianceSummaryList.hasEpcRow.key).hasCount(0)
            assertThat(checkAnswersPage.complianceSummaryList.isEpcRequiredRow.key).hasCount(0)
        }

        @Test
        fun `when delegating to a letting agent after entering an EPC, the EPC section is hidden`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswersDelegatedToLettingAgent()
                        .withCompliantEpc()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            BaseComponent.assertThat(checkAnswersPage.epcHeading).isHidden()
            assertThat(checkAnswersPage.complianceSummaryList.hasEpcRow.key).hasCount(0)
            assertThat(page.locator("main").getByText("Your EPC")).hasCount(0)
        }

        @Test
        fun `rented out section appears after occupied and before licensing when landlord provides details`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswersOccupied()
                        .withLandlordProvidesRentalDetails()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            val headings = checkAnswersPage.sectionHeadings
            val occupancyIndex = headings.indexOf("Tell us if your property’s occupied")
            val rentedOutIndex = headings.indexOf("How your property’s rented out")
            val licensingIndex = headings.indexOf("Tell us if the property needs a license")

            assertTrue(occupancyIndex >= 0 && rentedOutIndex >= 0 && licensingIndex >= 0)
            assertTrue(rentedOutIndex > occupancyIndex, "Rented-out section should appear after occupied section")
            assertTrue(rentedOutIndex < licensingIndex, "Rented-out section should appear before licensing section")
        }

        @Test
        fun `CYA page uses the expected heading hierarchy`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            BaseComponent.assertThat(checkAnswersPage.heading).containsText("Check your answers for:")
            BaseComponent.assertThat(checkAnswersPage.aboutYourPropertyHeading).isVisible()
            BaseComponent.assertThat(checkAnswersPage.propertyDetailsHeading).isVisible()
        }

        @Test
        fun `CYA page renders EPC summary card title as h4 to preserve heading hierarchy`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageEpcExpiredInDateAtTenancyStart()

            BaseComponent.assertThat(checkAnswersPage.epcCard.title).isVisible()
            assertThat(page.locator("main h4.govuk-summary-card__title")).hasCount(1)
        }

        @Test
        fun `when property is unoccupied, letting agent delegation unoccupied panel is displayed`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            BaseComponent.assertThat(checkAnswersPage.rentedOutHeading).isVisible()
            BaseComponent.assertThat(checkAnswersPage.lettingAgentDelegationSubheading).isVisible()
            assertThat(checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.key).hasCount(0)
            assertThat(checkAnswersPage.summaryList.lettingAgentEmailRow.key).hasCount(0)
            BaseComponent.assertThat(checkAnswersPage.lettingAgentDelegationBodyText).isHidden()
            BaseComponent.assertThat(checkAnswersPage.lettingAgentDelegationUnoccupiedPanel).containsText(
                "When your property becomes occupied, you can choose for your letting agent or property manager to " +
                    "provide this section for you. They can also keep these details up to date.",
            )
        }

        @Test
        fun `the occupancy change link navigates to the occupancy page and changing from unoccupied to occupied returns to the CYA page`(
            page: Page,
        ) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageUnoccupiedWithTenancyDetails()
            assertThat(checkAnswersPage.summaryList.occupancyQuestionRow.value).containsText("No")

            checkAnswersPage.summaryList.occupancyQuestionRow.actions.firstActionLink
                .clickAndWait()
            val occupancyPage = assertPageIs(page, OccupancyFormPagePropertyRegistration::class)
            occupancyPage.submitIsOccupied()
            val updatedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(updatedCheckAnswersPage.summaryList.occupancyQuestionRow.value).containsText("Yes")
        }

        @Test
        fun `the electrical certificate change link navigates to the has electrical certificate page`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            checkAnswersPage.complianceSummaryList.electricalCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)
        }

        @Test
        fun `the EPC change link takes the user to the confirm epc step if epc is found by uprn`(page: Page) {
            whenever(epcRegisterClient.getByUprn(PropertyRegistrationJourneyTests.uprnForSelectedAddress))
                .thenReturn(MockEpcData.createEpcRegisterClientEpcFoundResponse())

            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersCompliantEpc(),
                )

            cyaPage.epcCard
                .getAction("Change")
                .link
                .clickAndWait()
            assertPageIs(page, ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration::class)
        }

        @Test
        fun `the EPC change link takes the user to the has epc step if epc is found by certificate number`(page: Page) {
            whenever(epcRegisterClient.getByUprn(PropertyRegistrationJourneyTests.uprnForSelectedAddress))
                .thenReturn(MockEpcData.epcRegisterClientEpcNotFoundResponse)

            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswersEpcFoundByCertificateNumber(),
                )
            cyaPage.epcCard
                .getAction("Change")
                .link
                .clickAndWait()
            assertPageIs(page, HasEpcFormPagePropertyRegistration::class)
        }

        @Test
        fun `the licensing number change link navigates to the licensing page`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswersWithSelectiveLicence()
                        .withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            checkAnswersPage.summaryList.licensingNumberRow.clickFirstActionLinkAndWait()
            val selectiveLicencePage = assertPageIs(page, SelectiveLicenceFormPagePropertyRegistration::class)
            selectiveLicencePage.submitLicenseNumber("SL-99999")
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the tenancy details change link navigates to the households page when tenancy details have been provided later`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageWithProvideTenancyDetailsLater()
            checkAnswersPage.summaryList.tenancyDetailsRow.clickFirstActionLinkAndWait()
            assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
        }

        @Test
        fun `selecting provide later again in the tenancy details CYA sub-journey returns to the property registration CYA`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageWithProvideTenancyDetailsLater()
            checkAnswersPage.summaryList.tenancyDetailsRow.clickFirstActionLinkAndWait()
            val householdsPage = assertPageIs(page, NumberOfHouseholdsFormPagePropertyRegistration::class)
            householdsPage.submitProvideThisLater()
            val confirmationPage = assertPageIs(page, ProvideTenancyDetailsLaterFormPagePropertyRegistration::class)
            confirmationPage.form.submit()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class CheckAnswersChangeLinkBackLinks {
        @Test
        fun `The back link on the check joint landlords page returns to the CYA page when reached from there`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswersOccupied()
                        .withCheckedJointLandlords(mutableListOf("email@address.com")),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.summaryList.jointLandlordsInvitationsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordsPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the gas certificate page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageGasCertUploaded()

            checkAnswersPage.complianceSummaryList.validGasCertRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)
            hasGasCertPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the EPC tenancy check page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageEpcExpiredInDateAtTenancyStart()

            checkAnswersPage.complianceSummaryList.epcTenancyCheckRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val tenancyCheckPage = assertPageIs(page, EpcInDateAtStartOfTenancyCheckPagePropertyRegistration::class)
            tenancyCheckPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the is EPC required and EPC exemption pages returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageNoEpc()

            checkAnswersPage.complianceSummaryList.isEpcRequiredRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val isEpcRequiredPage = assertPageIs(page, IsEpcRequiredFormPagePropertyRegistration::class)
            isEpcRequiredPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.complianceSummaryList.epcExemptionRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val epcExemptionPage = assertPageIs(page, EpcExemptionFormPagePropertyRegistration::class)
            epcExemptionPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the licensing number page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageWithSelectiveLicence()

            checkAnswersPage.summaryList.licensingNumberRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val selectiveLicencePage = assertPageIs(page, SelectiveLicenceFormPagePropertyRegistration::class)
            selectiveLicencePage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the gas certificate issue date page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageGasCertUploaded()

            checkAnswersPage.complianceSummaryList.gasCertIssueDateRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)
            gasCertIssueDatePage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `changing an expired gas certificate issue date reaches the expired certificate page before returning to CYA`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageGasCertUploaded()

            checkAnswersPage.complianceSummaryList.gasCertIssueDateRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val gasCertIssueDatePage = assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)
            gasCertIssueDatePage.submitDate(PropertyRegistrationJourneyTests.expiredGasSafetyCertIssueDate)
            assertPageIs(page, GasCertExpiredFormPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the gas certificate uploads page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageGasCertUploaded()

            checkAnswersPage.complianceSummaryList.gasCertUploadRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val gasCertUploadsPage = assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)
            gasCertUploadsPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the electrical certificate expiry date page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageElectricalCertUploaded()

            checkAnswersPage.complianceSummaryList.electricalCertExpiryDateRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val expiryDatePage = assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)
            expiryDatePage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the electrical certificate uploads page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageElectricalCertUploaded()

            checkAnswersPage.complianceSummaryList.electricalCertUploadRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val electricalCertUploadsPage = assertPageIs(page, CheckElectricalCertUploadsFormPagePropertyRegistration::class)
            electricalCertUploadsPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the registered energy efficiency exemption answer page returns to the CYA page when reached from there`(
            page: Page,
        ) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageEpcLowRatingWithExemption()

            checkAnswersPage.complianceSummaryList.hasMeesExemptionRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val hasMeesExemptionPage = assertPageIs(page, HasMeesExemptionFormPagePropertyRegistration::class)
            hasMeesExemptionPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the registered energy efficiency exemption reason page returns to the CYA page`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageEpcLowRatingWithExemption()

            checkAnswersPage.complianceSummaryList.meesExemptionRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val meesExemptionPage = assertPageIs(page, MeesExemptionFormPagePropertyRegistration::class)
            meesExemptionPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `changing the registered energy efficiency exemption answer to yes reaches the exemption reason page`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageEpcLowRatingWithExemption()

            checkAnswersPage.complianceSummaryList.hasMeesExemptionRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val hasMeesExemptionPage = assertPageIs(page, HasMeesExemptionFormPagePropertyRegistration::class)
            hasMeesExemptionPage.submitHasMeesExemption()
            assertPageIs(page, MeesExemptionFormPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the number of tenants page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()

            checkAnswersPage.summaryList.numberOfTenantsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val numberOfPeoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            numberOfPeoplePage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the rent amount page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()

            checkAnswersPage.summaryList.rentAmountRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val rentAmountPage = assertPageIs(page, RentAmountFormPagePropertyRegistration::class)
            rentAmountPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `The back link on the which bills are included page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()

            checkAnswersPage.summaryList.billsIncludedRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val billsIncludedPage = assertPageIs(page, BillsIncludedFormPagePropertyRegistration::class)
            billsIncludedPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class CorrespondenceCheckAnswers {
        @Test
        fun `council contact details are shown between ownership and occupancy`(page: Page) {
            val checkAnswersPage = goToCheckAnswersWithCorrespondence(page)

            assertEquals(
                listOf(
                    "About your property",
                    "Property details",
                    "Ownership and landlords",
                    "Who the council should contact",
                    "Tell us if your property’s occupied",
                ),
                checkAnswersPage.sectionHeadings.take(5),
            )
            assertThat(checkAnswersPage.correspondenceRowKeys).hasText(arrayOf("Email address", "Postal address"))
            assertThat(checkAnswersPage.summaryList.correspondenceEmailRow.value).hasText("alex.surname@example.com")
            assertThat(checkAnswersPage.summaryList.correspondencePostalAddressRow.value.locator("p"))
                .hasText(arrayOf("1 Fictional Road", "FA1 1AA"))
        }

        @Test
        fun `the email change link starts a CYA sub-journey that returns to the CYA with the new email`(page: Page) {
            var checkAnswersPage = goToCheckAnswersWithCorrespondence(page)

            checkAnswersPage.summaryList.correspondenceEmailRow.clickFirstActionLinkAndWait()
            val emailPage = assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
            emailPage.submitDifferentEmail("council.contact@example.com")

            checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.summaryList.correspondenceEmailRow.value).hasText("council.contact@example.com")
        }

        @Test
        fun `the postal address change link starts a CYA sub-journey that only changes the postal address`(page: Page) {
            var checkAnswersPage = goToCheckAnswersWithCorrespondence(page)

            checkAnswersPage.summaryList.correspondencePostalAddressRow.clickFirstActionLinkAndWait()
            val lookupPage = assertPageIs(page, CorrespondenceLookupAddressFormPagePropertyRegistration::class)
            lookupPage.submitPostcodeAndBuildingNameOrNumber("FA1 1AB", "2")
            val selectPage = assertPageIs(page, CorrespondenceSelectAddressFormPagePropertyRegistration::class)
            selectPage.selectAddressAndSubmit("2 Fake Way")

            checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.summaryList.correspondencePostalAddressRow.value.locator("p"))
                .hasText(arrayOf("2 Fake Way", "FA1 1AB"))
            assertThat(checkAnswersPage.summaryList.propertyAddressRow.value.locator("p"))
                .hasText(arrayOf("1 Street Address", "City", "AB1 2CD"))
        }

        @Test
        fun `The back link on the correspondence email page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = goToCheckAnswersWithCorrespondence(page)

            checkAnswersPage.summaryList.correspondenceEmailRow.clickFirstActionLinkAndWait()
            assertPageIs(page, CorrespondenceEmailFormPagePropertyRegistration::class)
            BackLink.default(page).clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        private fun goToCheckAnswersWithCorrespondence(page: Page): CheckAnswersPagePropertyRegistration {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms()
                        .withCompletedCorrespondence(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            return assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class ConfirmMissingComplianceStep {
        @Test
        fun `submitting with no option selected returns an error`() {
            val confirmPage = navigator.skipToPropertyRegistrationConfirmMissingCompliancePage()
            confirmPage.form.submit()
            assertThat(confirmPage.form.getErrorMessage()).containsText("Select whether you want to submit this registration")
        }

        @Test
        fun `selecting no, go back redirects to the check answers page`(page: Page) {
            val confirmPage = navigator.skipToPropertyRegistrationConfirmMissingCompliancePage()
            confirmPage.form.radios.selectValue("false")
            confirmPage.form.submit()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }
    }
}

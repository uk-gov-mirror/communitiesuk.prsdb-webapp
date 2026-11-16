package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentCaptor.captor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.communities.prsdb.webapp.clients.EpcRegisterClient
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.database.entity.SavedJourneyState
import uk.gov.communities.prsdb.webapp.database.repository.SavedJourneyStateRepository
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BackLink
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ErrorPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.AnyoneInOwnershipChainFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.BillsIncludedFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckEicUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckEicrUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasCertUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckJointLandlordsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckPropertyManagerFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmEpcDetailsRetrievedByUprnFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceEmailFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceLookupAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CorrespondenceSelectAddressFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EicIssueDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EicrExpiryDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcInDateAtStartOfTenancyCheckPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.FreeholdTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.FreeholdersInfoFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.FreeholdersListFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertExpiredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertIssueDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasElectricalCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasEpcFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasSupplyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasJointLandlordsFormBasePagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasMeesExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HmoAdditionalLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.IsEpcRequiredFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LettingAgentEmailPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LicensingTypeFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.MeesExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfHouseholdsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfPeopleFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OccupancyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OwnershipKindFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OwnsPropertyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.PropertyManagerDetailsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.PropertyManagerFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideTenancyDetailsLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RentAmountFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SelectiveLicenceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.SuperiorLandlordsListFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdType
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipKind
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.testHelpers.builders.AssociatedPartiesStateBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockEpcData
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyRegistrationCheckAnswersSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
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

    // TODO PDJB-2016: Remove this class once ASSOCIATED_PARTIES is released.
    @Nested
    inner class BeforePdjb955 {
        @Test
        fun `cya hides the freeholder section when the associated parties feature is disabled`(page: Page) {
            featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms()
                        .withOwnershipKind(OwnershipKind.LEASEHOLD_ONLY)
                        .withFreeholders(PropertyStateSessionBuilder.DEFAULT_FREEHOLDERS)
                        .withFreeholdersInfoComplete()
                        .withFreeholdersListComplete(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            BaseComponent.assertThat(checkAnswersPage.freeholdersHeading).hasCount(0)
            BaseComponent.assertThat(checkAnswersPage.freeholdersChangeLink).hasCount(0)
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

            checkAnswersPage.summaryList.freeholdTypeRow.actions.firstActionLink
                .clickAndWait()
            val freeholdTypePage = assertPageIs(page, FreeholdTypeFormPagePropertyRegistration::class)

            freeholdTypePage.submitFreeholdType(FreeholdType.COMMONHOLD)
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
            assertTrue(savedJourneyStateAfterOwnershipUpdate.serializedState.contains("freeholdType\":\"COMMONHOLD\""))
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
                    "People responsible for this property",
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
        fun `cya page uses the expected heading hierarchy`(page: Page) {
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
        fun `cya page renders EPC summary card title as h4 to preserve heading hierarchy`(page: Page) {
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
    inner class JointLandlordsCheckAnswers {
        @Test
        fun `joint landlord invitations are shown under the joint landlords heading`(page: Page) {
            val checkAnswersPage = goToJointLandlordsCheckAnswers(page, hasJointLandlords = true)

            BaseComponent.assertThat(checkAnswersPage.jointLandlordsHeading).isVisible()
            assertThat(checkAnswersPage.jointLandlordsSummaryList).containsText("Invitations")
            assertThat(checkAnswersPage.jointLandlordsSummaryList).containsText("email@address.com")
        }

        @Test
        fun `no joint landlords answer is shown under the joint landlords heading`(page: Page) {
            val checkAnswersPage = goToJointLandlordsCheckAnswers(page, hasJointLandlords = false)

            BaseComponent.assertThat(checkAnswersPage.jointLandlordsHeading).isVisible()
            assertThat(checkAnswersPage.summaryList.jointLandlordsAreThereRow.value)
                .hasText("No, I am the only landlord for this property")
        }
    }

    // TODO PDJB-2016: Remove this class when ASSOCIATED_PARTIES is released
    @Nested
    inner class BeforePdjb2146JointLandlordsCheckAnswers {
        @BeforeEach
        fun disableAssociatedPartiesFlag() {
            featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
        }

        @Test
        fun `joint landlord invitations use the legacy label without a joint landlords heading`(page: Page) {
            val checkAnswersPage = goToJointLandlordsCheckAnswers(page, hasJointLandlords = true)

            BaseComponent.assertThat(checkAnswersPage.jointLandlordsHeading).isHidden()
            assertThat(checkAnswersPage.summaryList.jointLandlordsInvitationsRowBeforePdjb2146.value)
                .containsText("email@address.com")
            assertThat(checkAnswersPage.summaryList.invitationsRow.key).hasCount(0)
        }

        @Test
        fun `no joint landlords answer is shown without a joint landlords heading`(page: Page) {
            val checkAnswersPage = goToJointLandlordsCheckAnswers(page, hasJointLandlords = false)

            BaseComponent.assertThat(checkAnswersPage.jointLandlordsHeading).isHidden()
            assertThat(checkAnswersPage.summaryList.jointLandlordsAreThereRow.value)
                .hasText("No, I am the only landlord for this property")
        }

        @Test
        fun `the back link on the check joint landlords page returns to the CYA page`(page: Page) {
            val checkAnswersPage = goToJointLandlordsCheckAnswers(page, hasJointLandlords = true)

            checkAnswersPage.summaryList.jointLandlordsInvitationsRowBeforePdjb2146.actions
                .getActionLink("Change")
                .clickAndWait()
            val checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordsPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }
    }

    private fun goToJointLandlordsCheckAnswers(
        page: Page,
        hasJointLandlords: Boolean,
    ): CheckAnswersPagePropertyRegistration {
        val state = PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswersOccupied()
        if (hasJointLandlords) {
            state.withCheckedJointLandlords(mutableListOf("email@address.com"))
        }
        val taskListPage = navigator.goToPropertyRegistrationTaskList(state)
        taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
        return assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
    }

    @Nested
    inner class CheckAnswersChangeLinkBackLinks {
        @Test
        fun `the back link on the has joint landlords page returns to CYA when there are no joint landlords`(page: Page) {
            val checkAnswersPage = goToJointLandlordsCheckAnswers(page, hasJointLandlords = false)

            checkAnswersPage.summaryList.jointLandlordsAreThereRow.actions
                .getActionLink("Change")
                .clickAndWait()
            assertPageIs(page, HasJointLandlordsFormBasePagePropertyRegistration::class)
            BackLink.default(page).clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the check joint landlords page returns to the CYA page when reached from there`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswersOccupied()
                        .withCheckedJointLandlords(mutableListOf("email@address.com")),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            checkAnswersPage.summaryList.invitationsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val checkJointLandlordsPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordsPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the gas certificate page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageGasCertUploaded()

            checkAnswersPage.complianceSummaryList.validGasCertRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val hasGasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)
            hasGasCertPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the EPC tenancy check page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageEpcExpiredInDateAtTenancyStart()

            checkAnswersPage.complianceSummaryList.epcTenancyCheckRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val tenancyCheckPage = assertPageIs(page, EpcInDateAtStartOfTenancyCheckPagePropertyRegistration::class)
            tenancyCheckPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the is EPC required and EPC exemption pages returns to the CYA page when reached from there`(page: Page) {
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
        fun `the back link on the licensing number page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageWithSelectiveLicence()

            checkAnswersPage.summaryList.licensingNumberRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val selectiveLicencePage = assertPageIs(page, SelectiveLicenceFormPagePropertyRegistration::class)
            selectiveLicencePage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the gas certificate issue date page returns to the CYA page when reached from there`(page: Page) {
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
        fun `the back link on the gas certificate uploads page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageGasCertUploaded()

            checkAnswersPage.complianceSummaryList.gasCertUploadRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val gasCertUploadsPage = assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)
            gasCertUploadsPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the EICR expiry date page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = skipToCheckAnswersPageWithElectricalCertUploaded(CertificateType.Eicr)

            checkAnswersPage.complianceSummaryList
                .electricalCertDateRow(CertificateType.Eicr)
                .actions
                .getActionLink("Change")
                .clickAndWait()
            val expiryDatePage = assertPageIs(page, EicrExpiryDateFormPagePropertyRegistration::class)
            expiryDatePage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the EIC issue date page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = skipToCheckAnswersPageWithElectricalCertUploaded(CertificateType.Eic)

            checkAnswersPage.complianceSummaryList
                .electricalCertDateRow(CertificateType.Eic)
                .actions
                .getActionLink("Change")
                .clickAndWait()
            val issueDatePage = assertPageIs(page, EicIssueDateFormPagePropertyRegistration::class)
            issueDatePage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CertificateType::class, names = ["Eic", "Eicr"])
        fun `the back link on the electrical certificate uploads page returns to the CYA page when reached from there`(
            certificateType: CertificateType,
            page: Page,
        ) {
            val checkAnswersPage = skipToCheckAnswersPageWithElectricalCertUploaded(certificateType)

            checkAnswersPage.complianceSummaryList.electricalCertUploadRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val electricalCertUploadsPage =
                when (certificateType) {
                    CertificateType.Eic -> assertPageIs(page, CheckEicUploadsFormPagePropertyRegistration::class)
                    CertificateType.Eicr -> assertPageIs(page, CheckEicrUploadsFormPagePropertyRegistration::class)
                    else -> throw IllegalArgumentException("Unexpected certificate type: $certificateType")
                }
            electricalCertUploadsPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        private fun skipToCheckAnswersPageWithElectricalCertUploaded(certificateType: CertificateType) =
            when (certificateType) {
                CertificateType.Eic -> navigator.skipToPropertyRegistrationCheckAnswersPageEicUploaded()
                CertificateType.Eicr -> navigator.skipToPropertyRegistrationCheckAnswersPageEicrUploaded()
                else -> throw IllegalArgumentException("Unexpected certificate type: $certificateType")
            }

        @Test
        fun `the back link on the registered energy efficiency exemption answer page returns to the CYA page when reached from there`(
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
        fun `the back link on the registered energy efficiency exemption reason page returns to the CYA page`(page: Page) {
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
        fun `the back link on the number of tenants page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()

            checkAnswersPage.summaryList.numberOfTenantsRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val numberOfPeoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            numberOfPeoplePage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the rent amount page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.skipToPropertyRegistrationCheckAnswersPageOccupied()

            checkAnswersPage.summaryList.rentAmountRow.actions
                .getActionLink("Change")
                .clickAndWait()
            val rentAmountPage = assertPageIs(page, RentAmountFormPagePropertyRegistration::class)
            rentAmountPage.backLink.clickAndWait()
            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the which bills are included page returns to the CYA page when reached from there`(page: Page) {
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
                    "People responsible for this property",
                    "Who the council should contact",
                    "Tell us if your property’s occupied",
                ),
                checkAnswersPage.sectionHeadings.take(5),
            )
            assertThat(checkAnswersPage.correspondenceRowKeys).hasText(arrayOf("Email address", "Postal address"))
            assertThat(checkAnswersPage.summaryList.correspondenceEmailRow.value).hasText("alex.surname@example.com")
            assertThat(
                checkAnswersPage.summaryList.correspondencePostalAddressRow.value
                    .locator("p"),
            ).hasText(arrayOf("1 Fictional Road", "FA1 1AA"))
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
            assertThat(
                checkAnswersPage.summaryList.correspondencePostalAddressRow.value
                    .locator("p"),
            ).hasText(arrayOf("2 Fake Way", "FA1 1AB"))
            assertThat(
                checkAnswersPage.summaryList.propertyAddressRow.value
                    .locator("p"),
            ).hasText(arrayOf("1 Street Address", "City", "AB1 2CD"))
        }

        @Test
        fun `the back link on the correspondence email page returns to the CYA page when reached from there`(page: Page) {
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
    inner class OwnershipCheckAnswers {
        @Test
        fun `the ownership heading and rows are shown`(page: Page) {
            val checkAnswersPage = goToCheckAnswers(page)

            BaseComponent.assertThat(checkAnswersPage.ownershipHeading).isVisible()
            assertThat(checkAnswersPage.summaryList.ownsPropertyRow.value).hasText("Yes")
            assertThat(checkAnswersPage.summaryList.ownershipKindRow.value).hasText("Freehold")
            assertThat(checkAnswersPage.summaryList.freeholdTypeRow.value).hasText("Sole ownership")
        }

        @Test
        fun `changing the ownership type pushes through the superior landlords question before returning to CYA`(page: Page) {
            var checkAnswersPage = goToCheckAnswers(page)

            checkAnswersPage.summaryList.ownershipKindRow.clickFirstActionLinkAndWait()
            val ownershipKindPage = assertPageIs(page, OwnershipKindFormPagePropertyRegistration::class)
            ownershipKindPage.submitOwnershipKind(OwnershipKind.FREEHOLD_AND_LEASEHOLD)
            val freeholdTypePage = assertPageIs(page, FreeholdTypeFormPagePropertyRegistration::class)
            freeholdTypePage.submitFreeholdType(FreeholdType.SOLE_OWNERSHIP)
            val anyoneInOwnershipChainPage = assertPageIs(page, AnyoneInOwnershipChainFormPagePropertyRegistration::class)
            anyoneInOwnershipChainPage.submitNo()

            checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            assertThat(checkAnswersPage.summaryList.ownershipKindRow.value).hasText("Freehold and leasehold")
            assertThat(checkAnswersPage.summaryList.anyoneInOwnershipChainRow.value).hasText("No")
        }

        @Test
        fun `changing the freeholder type to one with joint freeholders pushes through the freeholders task`(page: Page) {
            val checkAnswersPage = goToCheckAnswers(page)

            checkAnswersPage.summaryList.freeholdTypeRow.clickFirstActionLinkAndWait()
            val freeholdTypePage = assertPageIs(page, FreeholdTypeFormPagePropertyRegistration::class)
            freeholdTypePage.submitFreeholdType(FreeholdType.JOINT_TENANTS)

            assertPageIs(page, FreeholdersInfoFormPagePropertyRegistration::class)
        }

        @Test
        fun `changing do you own this property to no pushes through the freeholders task`(page: Page) {
            val checkAnswersPage = goToCheckAnswers(page)

            checkAnswersPage.summaryList.ownsPropertyRow.clickFirstActionLinkAndWait()
            val ownsPropertyPage = assertPageIs(page, OwnsPropertyFormPagePropertyRegistration::class)
            ownsPropertyPage.submitOwnsProperty(false)

            assertPageIs(page, FreeholdersInfoFormPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the do you own this property page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = goToCheckAnswers(page)

            checkAnswersPage.summaryList.ownsPropertyRow.clickFirstActionLinkAndWait()
            val ownsPropertyPage = assertPageIs(page, OwnsPropertyFormPagePropertyRegistration::class)
            ownsPropertyPage.backLink.clickAndWait()

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the ownership kind page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = goToCheckAnswers(page)

            checkAnswersPage.summaryList.ownershipKindRow.clickFirstActionLinkAndWait()
            val ownershipKindPage = assertPageIs(page, OwnershipKindFormPagePropertyRegistration::class)
            ownershipKindPage.backLink.clickAndWait()

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        @Test
        fun `the back link on the freehold type page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = goToCheckAnswers(page)

            checkAnswersPage.summaryList.freeholdTypeRow.clickFirstActionLinkAndWait()
            val freeholdTypePage = assertPageIs(page, FreeholdTypeFormPagePropertyRegistration::class)
            freeholdTypePage.backLink.clickAndWait()

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        private fun goToCheckAnswers(page: Page): CheckAnswersPagePropertyRegistration {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswers().withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            return assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class SuperiorLandlordsCheckAnswers {
        @Test
        fun `when nobody else is in the ownership chain, a summary row links to the ownership chain question`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms()
                        .withOwnershipKind(OwnershipKind.FREEHOLD_AND_LEASEHOLD)
                        .withAnyoneInOwnershipChain(false),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            BaseComponent.assertThat(checkAnswersPage.superiorLandlordsHeading).isVisible()
            assertThat(checkAnswersPage.summaryList.anyoneInOwnershipChainRow.value).hasText("No")
            BaseComponent.assertThat(checkAnswersPage.superiorLandlordsChangeLink).hasCount(0)

            checkAnswersPage.summaryList.anyoneInOwnershipChainRow.clickFirstActionLinkAndWait()
            assertPageIs(page, AnyoneInOwnershipChainFormPagePropertyRegistration::class)
        }

        @Test
        fun `when there are people in the ownership chain, they are shown as cards`(page: Page) {
            val checkAnswersPage = goToCheckAnswersWithSuperiorLandlords(page)

            BaseComponent.assertThat(checkAnswersPage.superiorLandlordsHeading).isVisible()
            BaseComponent.assertThat(checkAnswersPage.summaryList.anyoneInOwnershipChainRow).hasCount(0)
            listOf("Superior Landlord With Address", "Superior Landlord With Email", "Unknown person").forEach { title ->
                val card = checkAnswersPage.getSummaryCard(title, "h5")
                BaseComponent.assertThat(card.title).hasText(title)
            }
        }

        @Test
        fun `the add, change or remove link starts a CYA sub-journey at the superior landlords list`(page: Page) {
            var checkAnswersPage = goToCheckAnswersWithSuperiorLandlords(page)

            checkAnswersPage.superiorLandlordsChangeLink.clickAndWait()
            val superiorLandlordsListPage = assertPageIs(page, SuperiorLandlordsListFormPagePropertyRegistration::class)
            superiorLandlordsListPage.submit()

            checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            BaseComponent.assertThat(checkAnswersPage.superiorLandlordsHeading).isVisible()
        }

        @Test
        fun `the back link on the superior landlords list page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = goToCheckAnswersWithSuperiorLandlords(page)

            checkAnswersPage.superiorLandlordsChangeLink.clickAndWait()
            val superiorLandlordsListPage = assertPageIs(page, SuperiorLandlordsListFormPagePropertyRegistration::class)
            superiorLandlordsListPage.backLink.clickAndWait()

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        private fun goToCheckAnswersWithSuperiorLandlords(page: Page): CheckAnswersPagePropertyRegistration {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms()
                        .withOwnershipKind(OwnershipKind.FREEHOLD_AND_LEASEHOLD)
                        .withAnyoneInOwnershipChain()
                        .withSuperiorLandlords(PropertyStateSessionBuilder.DEFAULT_SUPERIOR_LANDLORDS)
                        .withSuperiorLandlordsListComplete(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            return assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class FreeholdersCheckAnswers {
        @Test
        fun `cya shows freeholder cards without per-card actions`() {
            val checkAnswersPage = navigator.goToPropertyRegistrationCheckAnswersPageWithFreeholders()

            BaseComponent.assertThat(checkAnswersPage.freeholdersHeading).isVisible()

            assertFreeholderCards(checkAnswersPage)
        }

        @Test
        fun `cya shows non-joint freeholder cards when the landlord does not own the property`() {
            val checkAnswersPage =
                navigator.goToPropertyRegistrationCheckAnswersPageWithFreeholders(
                    ownershipKind = null,
                    ownsProperty = false,
                )

            BaseComponent.assertThat(checkAnswersPage.freeholdersHeading).isVisible()
            BaseComponent.assertThat(checkAnswersPage.jointFreeholdersHeading).hasCount(0)
            BaseComponent.assertThat(checkAnswersPage.freeholdersChangeLink).isVisible()
            assertFreeholderCards(checkAnswersPage)
        }

        @Test
        fun `cya shows joint freeholder heading link and cards for joint ownership types`() {
            val checkAnswersPage =
                navigator.goToPropertyRegistrationCheckAnswersPageWithFreeholders(
                    ownershipKind = OwnershipKind.FREEHOLD_ONLY,
                    freeholdType = FreeholdType.JOINT_TENANTS,
                )

            BaseComponent.assertThat(checkAnswersPage.jointFreeholdersHeading).isVisible()
            BaseComponent.assertThat(checkAnswersPage.freeholdersHeading).hasCount(0)
            BaseComponent.assertThat(checkAnswersPage.jointFreeholdersChangeLink).isVisible()
            assertFreeholderCards(checkAnswersPage)
        }

        @Test
        fun `the add, change or remove link starts a CYA sub-journey at the freeholders list`(page: Page) {
            var checkAnswersPage = navigator.goToPropertyRegistrationCheckAnswersPageWithFreeholders()

            checkAnswersPage.freeholdersChangeLink.clickAndWait()
            val freeholdersListPage = assertPageIs(page, FreeholdersListFormPagePropertyRegistration::class)
            BaseComponent.assertThat(freeholdersListPage.heading).containsText("You’ve added 3 freeholders")
            freeholdersListPage.submit()

            checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            BaseComponent.assertThat(checkAnswersPage.freeholdersHeading).isVisible()
        }

        @Test
        fun `the back link on the freeholders list returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = navigator.goToPropertyRegistrationCheckAnswersPageWithFreeholders()

            checkAnswersPage.freeholdersChangeLink.clickAndWait()
            val freeholdersListPage = assertPageIs(page, FreeholdersListFormPagePropertyRegistration::class)
            freeholdersListPage.backLink.clickAndWait()

            val returnedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            BaseComponent.assertThat(returnedCheckAnswersPage.freeholdersHeading).isVisible()
            BaseComponent.assertThat(returnedCheckAnswersPage.freeholdersChangeLink).isVisible()
        }

        @Test
        fun `the back link on the freeholders list returns to CYA for a non-property owner`(page: Page) {
            val checkAnswersPage =
                navigator.goToPropertyRegistrationCheckAnswersPageWithFreeholders(
                    ownershipKind = null,
                    ownsProperty = false,
                )

            checkAnswersPage.freeholdersChangeLink.clickAndWait()
            val freeholdersListPage = assertPageIs(page, FreeholdersListFormPagePropertyRegistration::class)
            freeholdersListPage.backLink.clickAndWait()

            val returnedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            BaseComponent.assertThat(returnedCheckAnswersPage.freeholdersHeading).isVisible()
            BaseComponent.assertThat(returnedCheckAnswersPage.jointFreeholdersHeading).hasCount(0)
            BaseComponent.assertThat(returnedCheckAnswersPage.freeholdersChangeLink).isVisible()
        }

        @Test
        fun `the back link on the joint freeholders list returns to CYA`(page: Page) {
            val checkAnswersPage =
                navigator.goToPropertyRegistrationCheckAnswersPageWithFreeholders(
                    ownershipKind = OwnershipKind.FREEHOLD_AND_LEASEHOLD,
                    freeholdType = FreeholdType.TENANTS_IN_COMMON,
                )

            checkAnswersPage.jointFreeholdersChangeLink.clickAndWait()
            val freeholdersListPage = assertPageIs(page, FreeholdersListFormPagePropertyRegistration::class)
            BaseComponent.assertThat(freeholdersListPage.heading).containsText("You’ve added 3 joint freeholders")
            freeholdersListPage.backLink.clickAndWait()

            val returnedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            BaseComponent.assertThat(returnedCheckAnswersPage.jointFreeholdersHeading).isVisible()
            BaseComponent.assertThat(returnedCheckAnswersPage.jointFreeholdersChangeLink).isVisible()
        }

        @Test
        fun `the joint freeholders link opens the freeholders list with the joint heading`(page: Page) {
            val checkAnswersPage =
                navigator.goToPropertyRegistrationCheckAnswersPageWithFreeholders(
                    ownershipKind = OwnershipKind.FREEHOLD_ONLY,
                    freeholdType = FreeholdType.JOINT_TENANTS,
                )

            checkAnswersPage.jointFreeholdersChangeLink.clickAndWait()
            val freeholdersListPage = assertPageIs(page, FreeholdersListFormPagePropertyRegistration::class)
            BaseComponent.assertThat(freeholdersListPage.heading).containsText("You’ve added 3 joint freeholders")
        }

        @ParameterizedTest
        @EnumSource(value = FreeholdType::class, names = ["SOLE_OWNERSHIP", "COMMONHOLD"])
        fun `cya does not show freeholders for sole ownership or commonhold`(
            freeholdType: FreeholdType,
            page: Page,
        ) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms()
                        .withOwnershipKind(OwnershipKind.FREEHOLD_ONLY)
                        .withFreeholdType(freeholdType),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            BaseComponent.assertThat(checkAnswersPage.freeholdersHeading).hasCount(0)
            BaseComponent.assertThat(checkAnswersPage.jointFreeholdersHeading).hasCount(0)
            BaseComponent.assertThat(checkAnswersPage.freeholdersChangeLink).hasCount(0)
            BaseComponent.assertThat(checkAnswersPage.submitButton).isVisible()
        }

        private fun assertFreeholderCards(checkAnswersPage: CheckAnswersPagePropertyRegistration) {
            val addressCard = checkAnswersPage.getSummaryCard("Freeholder With Address", "h5")
            BaseComponent.assertThat(addressCard).containsText("1 Freehold Street")
            BaseComponent.assertThat(addressCard.getAction("Change")).hasCount(0)
            BaseComponent.assertThat(addressCard.getAction("Remove")).hasCount(0)

            val emailCard = checkAnswersPage.getSummaryCard("Freeholder With Email", "h5")
            BaseComponent.assertThat(emailCard).containsText("freeholder@example.com")
            BaseComponent.assertThat(emailCard.getAction("Change")).hasCount(0)
            BaseComponent.assertThat(emailCard.getAction("Remove")).hasCount(0)

            val unknownCard = checkAnswersPage.getSummaryCard("Unknown freeholder", "h5")
            BaseComponent.assertThat(unknownCard).containsText("You told us there was another freeholder")
            BaseComponent.assertThat(unknownCard.getAction("Change")).hasCount(0)
            BaseComponent.assertThat(unknownCard.getAction("Remove")).hasCount(0)
        }
    }

    @Nested
    inner class PropertyManagerCheckAnswers {
        @Test
        fun `when not using a property manager, a summary row links to the property manager question`(page: Page) {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswers().withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            BaseComponent.assertThat(checkAnswersPage.propertyManagerHeading).isVisible()
            assertThat(checkAnswersPage.summaryList.usingPropertyManagerRow.value)
                .hasText("No, I manage this property (or another joint landlord does)")
            BaseComponent.assertThat(checkAnswersPage.propertyManagerChangeLink).hasCount(0)

            checkAnswersPage.summaryList.usingPropertyManagerRow.clickFirstActionLinkAndWait()
            assertPageIs(page, PropertyManagerFormPagePropertyRegistration::class)
        }

        @Test
        fun `changing from not using to using a property manager goes through the details and check pages back to the CYA page`(
            page: Page,
        ) {
            // Arrange
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswers().withBedrooms(),
                )
            taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
            var checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

            // Act
            checkAnswersPage.summaryList.usingPropertyManagerRow.clickFirstActionLinkAndWait()
            assertPageIs(page, PropertyManagerFormPagePropertyRegistration::class).submitYes()
            assertPageIs(page, PropertyManagerDetailsFormPagePropertyRegistration::class)
                .submitDetails(name = "Other Managers Ltd", email = "other@example.com")
            assertPageIs(page, CheckPropertyManagerFormPagePropertyRegistration::class).form.submit()

            // Assert
            checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            BaseComponent.assertThat(checkAnswersPage.summaryList.usingPropertyManagerRow).hasCount(0)
            BaseComponent.assertThat(checkAnswersPage.getSummaryCard("Other Managers Ltd", "h5").title).hasText("Other Managers Ltd")
        }

        @Test
        fun `when using a property manager, they are shown as a card without actions`(page: Page) {
            val checkAnswersPage = goToCheckAnswersWithPropertyManager(page)

            BaseComponent.assertThat(checkAnswersPage.propertyManagerHeading).isVisible()
            BaseComponent.assertThat(checkAnswersPage.summaryList.usingPropertyManagerRow).hasCount(0)
            val card = checkAnswersPage.getSummaryCard(AssociatedPartiesStateBuilder.DEFAULT_PROPERTY_MANAGER_NAME, "h5")
            BaseComponent.assertThat(card.title).hasText(AssociatedPartiesStateBuilder.DEFAULT_PROPERTY_MANAGER_NAME)
            BaseComponent.assertThat(card.getAction("Change")).hasCount(0)
            BaseComponent.assertThat(card.getAction("Remove")).hasCount(0)
        }

        @Test
        fun `the add, change or remove link starts a CYA sub-journey at the check property manager page`(page: Page) {
            var checkAnswersPage = goToCheckAnswersWithPropertyManager(page)

            checkAnswersPage.propertyManagerChangeLink.clickAndWait()
            val checkPropertyManagerPage = assertPageIs(page, CheckPropertyManagerFormPagePropertyRegistration::class)
            checkPropertyManagerPage.form.submit()

            checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
            BaseComponent.assertThat(checkAnswersPage.propertyManagerHeading).isVisible()
        }

        @Test
        fun `The back link on the check property manager page returns to the CYA page when reached from there`(page: Page) {
            val checkAnswersPage = goToCheckAnswersWithPropertyManager(page)

            checkAnswersPage.propertyManagerChangeLink.clickAndWait()
            val checkPropertyManagerPage = assertPageIs(page, CheckPropertyManagerFormPagePropertyRegistration::class)
            checkPropertyManagerPage.backLink.clickAndWait()

            assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        }

        private fun goToCheckAnswersWithPropertyManager(page: Page): CheckAnswersPagePropertyRegistration {
            val taskListPage =
                navigator.goToPropertyRegistrationTaskList(
                    PropertyStateSessionBuilder
                        .beforePropertyRegistrationCheckAnswers()
                        .withBedrooms()
                        .withPropertyManager(),
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

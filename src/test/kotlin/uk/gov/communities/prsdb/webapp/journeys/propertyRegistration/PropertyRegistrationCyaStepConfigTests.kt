package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.PAYMENTS
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.WhoProvidesRentalDetails
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.UnrecoverableJourneyStateException
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.AnyoneInOwnershipChainStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BedroomsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckPropertyManagerStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CorrespondenceEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdersListStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasJointLandlordsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LicensingTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LocalCouncilStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupiedStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipKindStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnsPropertyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyManagerDetailsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyManagerStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyRegistrationCyaStepConfig
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideTenancyDetailsLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.SuperiorLandlordsListStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.WhoProvidesRentalDetailsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.CorrespondenceTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.FreeholdersDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.FreeholdersTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseholdsAndTenantsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.JointLandlordsPropertyRegistrationTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.LicensingTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipAndLandlordsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipAndLandlordsTaskBeforeAssociatedParties
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipStructureTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipTypeTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyManagerTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyRegistrationAddressTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.SuperiorLandlordsDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.SuperiorLandlordsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.TenancyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.WhoProvidesDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.ComplianceDetailsHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.FreeholderSummaryCardHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.LicensingDetailsHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.OccupancyDetailsHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.PropertyManagerSummaryCardHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.SuperiorLandlordSummaryCardHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.inviteJointLandlord.CheckJointLandlordsStep
import uk.gov.communities.prsdb.webapp.journeys.shared.inviteJointLandlord.InviteJointLandlordsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.CorrespondenceAddressTask
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.FreeholderDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.SuperiorLandlordDataModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AnyoneInOwnershipChainFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.CorrespondenceEmailFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.HasJointLandlordsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfBedroomsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OccupancyFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OwnershipTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyManagerDetailsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyManagerFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.WhoProvidesRentalDetailsFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryCardActionViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryCardViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.services.LocalCouncilService
import kotlin.test.assertIs

@ExtendWith(MockitoExtension::class)
class PropertyRegistrationCyaStepConfigTests {
    @Mock
    private lateinit var mockLocalCouncilService: LocalCouncilService

    @Mock
    private lateinit var mockLicensingDetailsHelper: LicensingDetailsHelper

    @Mock
    private lateinit var mockOccupancyDetailsHelper: OccupancyDetailsHelper

    @Mock
    private lateinit var mockComplianceDetailsHelper: ComplianceDetailsHelper

    @Mock
    private lateinit var mockMessageSource: MessageSource

    @Mock
    private lateinit var mockFeatureFlagManager: FeatureFlagManager

    @Mock
    private lateinit var mockState: PropertyRegistrationJourneyState

    @Mock
    private lateinit var mockCorrespondenceTask: CorrespondenceTask

    @Mock
    private lateinit var mockCorrespondenceEmailStep: CorrespondenceEmailStep

    @Mock
    private lateinit var mockCorrespondenceAddressTask: CorrespondenceAddressTask

    @Mock
    private lateinit var mockCorrespondenceLookupAddressStep: LookupAddressStep

    private val emailForm =
        CorrespondenceEmailFormModel().apply {
            correspondenceEmailOption = CorrespondenceEmailOption.ACCOUNT_EMAIL
            differentEmailAddress = "council.contact@example.com"
        }

    private val lookupPath = "${CorrespondenceAddressTask.ROUTE_SEGMENT}/${LookupAddressStep.ROUTE_SEGMENT}"

    @Mock
    private lateinit var mockPropertyDetailsTask: PropertyDetailsTask

    @Mock
    private lateinit var mockAddressTask: PropertyRegistrationAddressTask

    @Mock
    private lateinit var mockLookupAddressStep: LookupAddressStep

    @Mock
    private lateinit var mockLocalCouncilStep: LocalCouncilStep

    @Mock
    private lateinit var mockPropertyTypeStep: PropertyTypeStep

    @Mock
    private lateinit var mockPropertyTypeFormModel: PropertyTypeFormModel

    @Mock
    private lateinit var mockOwnershipAndLandlordsTaskBeforeAssociatedParties: OwnershipAndLandlordsTaskBeforeAssociatedParties

    @Mock
    private lateinit var mockOwnershipTypeStep: OwnershipTypeStep

    @Mock
    private lateinit var mockOwnershipTypeFormModel: OwnershipTypeFormModel

    @Mock
    private lateinit var mockJointLandlordsTask: JointLandlordsPropertyRegistrationTask

    @Mock
    private lateinit var mockHasJointLandlordsStep: HasJointLandlordsStep

    @Mock
    private lateinit var mockHasJointLandlordsFormModel: HasJointLandlordsFormModel

    @Mock
    private lateinit var mockBedroomsStep: BedroomsStep

    @Mock
    private lateinit var mockBedroomsFormModel: NumberOfBedroomsFormModel

    @Mock
    private lateinit var mockTenancyDetailsTask: TenancyDetailsTask

    @Mock
    private lateinit var mockOccupiedStep: OccupiedStep

    @Mock
    private lateinit var mockOccupancyFormModel: OccupancyFormModel

    @Mock
    private lateinit var mockHouseholdsAndTenantsTask: HouseholdsAndTenantsTask

    @Mock
    private lateinit var mockHouseholdStep: HouseholdStep

    @Mock
    private lateinit var mockProvideTenancyDetailsLaterStep: ProvideTenancyDetailsLaterStep

    @Mock
    private lateinit var mockWhoProvidesDetailsTask: WhoProvidesDetailsTask

    @Mock
    private lateinit var mockLicensingTask: LicensingTask

    @Mock
    private lateinit var mockLicensingTypeStep: LicensingTypeStep

    @Mock
    private lateinit var mockGasSafetyTask: GasSafetyTask

    @Mock
    private lateinit var mockElectricalSafetyTask: ElectricalSafetyTask

    @Mock
    private lateinit var mockEpcTask: EpcTask

    @Mock
    private lateinit var mockWhoProvidesRentalDetailsStep: WhoProvidesRentalDetailsStep

    @Mock
    private lateinit var mockWhoProvidesRentalDetailsFormModel: WhoProvidesRentalDetailsFormModel

    @Mock
    private lateinit var mockLettingAgentEmailFormModel: AllowLettingAgentEmailFormModel

    @Mock
    private lateinit var mockLettingAgentEmailStep: LettingAgentEmailStep

    @Mock
    private lateinit var mockOwnershipAndLandlordsTask: OwnershipAndLandlordsTask

    @Mock
    private lateinit var mockOwnershipStructureTask: OwnershipStructureTask

    @Mock
    private lateinit var mockOwnershipTypeTask: OwnershipTypeTask

    @Mock
    private lateinit var mockOwnsPropertyStep: OwnsPropertyStep

    @Mock
    private lateinit var mockOwnershipKindStep: OwnershipKindStep

    @Mock
    private lateinit var mockFreeholdTypeStep: FreeholdTypeStep

    @Mock
    private lateinit var mockFreeholdersTask: FreeholdersTask

    @Mock
    private lateinit var mockFreeholdersDetailsTask: FreeholdersDetailsTask

    @Mock
    private lateinit var mockFreeholdersListStep: FreeholdersListStep

    @Mock
    private lateinit var mockPropertyManagerTask: PropertyManagerTask

    @Mock
    private lateinit var mockPropertyManagerStep: PropertyManagerStep

    private lateinit var stepConfig: PropertyRegistrationCyaStepConfig

    @BeforeEach
    fun setUp() {
        stepConfig =
            PropertyRegistrationCyaStepConfig(
                mockLocalCouncilService,
                mockLicensingDetailsHelper,
                mockOccupancyDetailsHelper,
                mockComplianceDetailsHelper,
                mockMessageSource,
                mockFeatureFlagManager,
                SuperiorLandlordSummaryCardHelper(),
                FreeholderSummaryCardHelper(),
                PropertyManagerSummaryCardHelper(),
            )
        lenient().`when`(mockFeatureFlagManager.checkFeature(PAYMENTS)).thenReturn(false)
        lenient().`when`(mockState.propertyDetailsTask).thenReturn(mockPropertyDetailsTask)
        lenient().`when`(mockPropertyDetailsTask.addressTask).thenReturn(mockAddressTask)
        lenient().`when`(mockAddressTask.getAddress()).thenReturn(AddressDataModel("1 Test Street", localCouncilId = 1))
        lenient().`when`(mockAddressTask.lookupAddressStep).thenReturn(mockLookupAddressStep)
        lenient().`when`(mockAddressTask.localCouncilStep).thenReturn(mockLocalCouncilStep)
        lenient().`when`(mockLocalCouncilService.retrieveLocalCouncilById(1)).thenReturn(mock())
        lenient().`when`(mockPropertyDetailsTask.propertyTypeStep).thenReturn(mockPropertyTypeStep)
        lenient().`when`(mockPropertyTypeStep.formModel).thenReturn(mockPropertyTypeFormModel)
        lenient().`when`(mockPropertyDetailsTask.bedrooms).thenReturn(mockBedroomsStep)
        lenient().`when`(mockState.bedrooms).thenReturn(mockBedroomsStep)
        lenient().`when`(mockBedroomsStep.formModel).thenReturn(mockBedroomsFormModel)
        lenient()
            .`when`(
                mockState.ownershipAndLandlordsTaskBeforeAssociatedParties,
            ).thenReturn(mockOwnershipAndLandlordsTaskBeforeAssociatedParties)
        lenient().`when`(mockOwnershipAndLandlordsTaskBeforeAssociatedParties.ownershipTypeStep).thenReturn(mockOwnershipTypeStep)
        lenient().`when`(mockOwnershipAndLandlordsTaskBeforeAssociatedParties.jointLandlordsTask).thenReturn(mockJointLandlordsTask)
        lenient().`when`(mockOwnershipTypeStep.formModel).thenReturn(mockOwnershipTypeFormModel)
        lenient().`when`(mockJointLandlordsTask.hasJointLandlordsStep).thenReturn(mockHasJointLandlordsStep)
        lenient().`when`(mockHasJointLandlordsStep.formModel).thenReturn(mockHasJointLandlordsFormModel)
        lenient().`when`(mockHasJointLandlordsFormModel.hasJointLandlords).thenReturn(false)
        lenient().`when`(mockState.tenancyDetailsTask).thenReturn(mockTenancyDetailsTask)
        lenient().`when`(mockState.occupied).thenReturn(mockOccupiedStep)
        lenient().`when`(mockOccupiedStep.formModel).thenReturn(mockOccupancyFormModel)
        lenient().`when`(mockOccupancyFormModel.occupied).thenReturn(true)
        lenient().`when`(mockTenancyDetailsTask.householdsAndTenantsTask).thenReturn(mockHouseholdsAndTenantsTask)
        lenient().`when`(mockHouseholdsAndTenantsTask.households).thenReturn(mockHouseholdStep)
        lenient().`when`(mockHouseholdsAndTenantsTask.provideTenancyDetailsLaterStep).thenReturn(mockProvideTenancyDetailsLaterStep)
        lenient().`when`(mockState.getCyaJourneyId(any())).thenReturn("test-journey-id")
        lenient().`when`(mockComplianceDetailsHelper.getGasSafetyCyaContent(any(), any())).thenReturn(emptyMap())
        lenient().`when`(mockComplianceDetailsHelper.getElectricalSafetyCyaContent(any(), any())).thenReturn(emptyMap())
        lenient().`when`(mockComplianceDetailsHelper.getEpcCyaContent(any(), any())).thenReturn(emptyMap())
        lenient().`when`(mockLicensingDetailsHelper.getCheckYourAnswersSummaryList(any(), any())).thenReturn(emptyList())
        lenient()
            .`when`(mockOccupancyDetailsHelper.getOccupancySummaryList(any<PropertyRegistrationJourneyState>()))
            .thenReturn(emptyList())
        lenient()
            .`when`(mockOccupancyDetailsHelper.getCheckYourAnswersSummaryList(any<PropertyRegistrationJourneyState>(), any(), any()))
            .thenReturn(emptyList())
        lenient().`when`(mockState.whoProvidesDetailsTask).thenReturn(mockWhoProvidesDetailsTask)
        lenient().`when`(mockState.licensingTask).thenReturn(mockLicensingTask)
        lenient().`when`(mockLicensingTask.getLicensingType()).thenReturn(LicensingType.NO_LICENSING)
        lenient().`when`(mockLicensingTask.licensingTypeStep).thenReturn(mockLicensingTypeStep)
        lenient().`when`(mockState.gasSafetyTask).thenReturn(mockGasSafetyTask)
        lenient().`when`(mockState.electricalSafetyTask).thenReturn(mockElectricalSafetyTask)
        lenient().`when`(mockState.epcTask).thenReturn(mockEpcTask)
        lenient().`when`(mockWhoProvidesDetailsTask.whoProvidesRentalDetailsStep).thenReturn(mockWhoProvidesRentalDetailsStep)
        lenient().`when`(mockWhoProvidesRentalDetailsStep.formModel).thenReturn(mockWhoProvidesRentalDetailsFormModel)
        lenient().`when`(mockWhoProvidesRentalDetailsStep.formModelIfReachableOrNull).thenReturn(mockWhoProvidesRentalDetailsFormModel)
        lenient().`when`(mockWhoProvidesDetailsTask.lettingAgentEmailStep).thenReturn(mockLettingAgentEmailStep)
        lenient().`when`(mockLettingAgentEmailStep.formModel).thenReturn(mockLettingAgentEmailFormModel)
        lenient().`when`(mockState.correspondenceTask).thenReturn(mockCorrespondenceTask)
        lenient().`when`(mockCorrespondenceTask.correspondenceEmailStep).thenReturn(mockCorrespondenceEmailStep)
        lenient().`when`(mockCorrespondenceTask.addressTask).thenReturn(mockCorrespondenceAddressTask)
        lenient().`when`(mockCorrespondenceEmailStep.formModel).thenReturn(emailForm)
        lenient().`when`(mockCorrespondenceEmailStep.isStepReachable).thenReturn(true)
        lenient().`when`(mockCorrespondenceEmailStep.urlPath).thenReturn(CorrespondenceEmailStep.ROUTE_SEGMENT)
        lenient().`when`(mockState.loggedInLandlordEmailAtStartOfJourney).thenReturn("account@example.com")
        lenient().`when`(mockCorrespondenceAddressTask.lookupAddressStep).thenReturn(mockCorrespondenceLookupAddressStep)
        lenient().`when`(mockCorrespondenceLookupAddressStep.isStepReachable).thenReturn(true)
        lenient().`when`(mockCorrespondenceLookupAddressStep.urlPath).thenReturn(lookupPath)
        lenient().`when`(mockState.getCyaJourneyId(mockCorrespondenceEmailStep)).thenReturn("email-change")
        lenient().`when`(mockState.getCyaJourneyId(mockCorrespondenceLookupAddressStep)).thenReturn("postal-change")
        lenient().`when`(mockCorrespondenceAddressTask.getAddress()).thenReturn(
            AddressDataModel(
                "12 Test Road, Leeds, LS1 1AA",
                buildingNumber = "12",
                streetName = "Test Road",
                townName = "Leeds",
                postcode = "LS1 1AA",
            ),
        )
    }

    private fun setUpAssociatedPartiesOwnership(ownershipType: OwnershipType?) {
        lenient().`when`(mockFeatureFlagManager.checkFeature(ASSOCIATED_PARTIES)).thenReturn(true)
        lenient().`when`(mockState.ownershipAndLandlordsTask).thenReturn(mockOwnershipAndLandlordsTask)
        lenient().`when`(mockOwnershipAndLandlordsTask.jointLandlordsTask).thenReturn(mockJointLandlordsTask)
        lenient().`when`(mockOwnershipAndLandlordsTask.ownershipStructureTask).thenReturn(mockOwnershipStructureTask)
        lenient().`when`(mockOwnershipStructureTask.ownershipTypeTask).thenReturn(mockOwnershipTypeTask)
        lenient().`when`(mockOwnershipTypeTask.ownershipType).thenReturn(ownershipType)
        lenient().`when`(mockOwnershipTypeTask.ownsPropertyStep).thenReturn(mockOwnsPropertyStep)
        lenient().`when`(mockOwnershipTypeTask.ownershipKindStep).thenReturn(mockOwnershipKindStep)
        lenient().`when`(mockOwnershipTypeTask.freeholdTypeStep).thenReturn(mockFreeholdTypeStep)
        lenient().`when`(mockOwnsPropertyStep.isStepReachable).thenReturn(true)
        lenient().`when`(mockOwnsPropertyStep.urlPath).thenReturn(OwnsPropertyStep.ROUTE_SEGMENT)
        lenient().`when`(mockState.getCyaJourneyId(mockOwnsPropertyStep)).thenReturn("owns-change")
        lenient().`when`(mockOwnershipKindStep.isStepReachable).thenReturn(true)
        lenient().`when`(mockOwnershipKindStep.urlPath).thenReturn(OwnershipKindStep.ROUTE_SEGMENT)
        lenient().`when`(mockState.getCyaJourneyId(mockOwnershipKindStep)).thenReturn("kind-change")
        lenient().`when`(mockFreeholdTypeStep.isStepReachable).thenReturn(true)
        lenient().`when`(mockFreeholdTypeStep.urlPath).thenReturn(FreeholdTypeStep.ROUTE_SEGMENT)
        lenient().`when`(mockState.getCyaJourneyId(mockFreeholdTypeStep)).thenReturn("freehold-type-change")
        lenient().`when`(mockOwnershipStructureTask.freeholdersTask).thenReturn(mockFreeholdersTask)
        lenient().`when`(mockFreeholdersTask.freeholdersMap).thenReturn(mapOf(1 to FreeholderDataModel(detailsKnown = false)))
        lenient().`when`(mockFreeholdersTask.freeholdersDetailsTask).thenReturn(mockFreeholdersDetailsTask)
        lenient().`when`(mockFreeholdersDetailsTask.freeholdersListStep).thenReturn(mockFreeholdersListStep)
        lenient().`when`(mockFreeholdersListStep.isStepReachable).thenReturn(true)
        lenient().`when`(mockFreeholdersListStep.urlPath).thenReturn(FreeholdersListStep.ROUTE_SEGMENT)
        lenient().`when`(mockState.getCyaJourneyId(mockFreeholdersListStep)).thenReturn("freeholders-change")
        lenient().`when`(mockOwnershipAndLandlordsTask.propertyManagerTask).thenReturn(mockPropertyManagerTask)
        lenient().`when`(mockPropertyManagerTask.propertyManagerStep).thenReturn(mockPropertyManagerStep)
        lenient().`when`(mockPropertyManagerStep.formModelIfReachableOrNull)
            .thenReturn(PropertyManagerFormModel().apply { usingPropertyManager = false })
        lenient().`when`(mockPropertyManagerStep.isStepReachable).thenReturn(true)
        lenient().`when`(mockPropertyManagerStep.urlPath).thenReturn(PropertyManagerStep.ROUTE_SEGMENT)
    }

    @Nested
    inner class CorrespondenceContent {
        @Test
        fun `contact rows show the selected account email instead of a retained different email`() {
            val rows = correspondenceRows()

            assertEquals(
                listOf(
                    "forms.checkPropertyAnswers.correspondence.emailAddress",
                    "forms.checkPropertyAnswers.correspondence.postalAddress",
                ),
                rows.map { it.fieldHeading },
            )
            assertEquals("account@example.com", rows[0].fieldValue)
        }

        @Test
        fun `contact rows show the different email when selected`() {
            emailForm.correspondenceEmailOption = CorrespondenceEmailOption.DIFFERENT_EMAIL

            assertEquals("council.contact@example.com", correspondenceRows()[0].fieldValue)
            verify(mockState, never()).loggedInLandlordEmailAtStartOfJourney
        }

        @Test
        fun `contact postal address uses the same multiline format as the property address`() {
            assertEquals(listOf("12 Test Road", "Leeds", "LS1 1AA"), correspondenceRows()[1].fieldValue)
        }

        @Test
        fun `contact postal address preserves manually entered address lines and county`() {
            whenever(mockCorrespondenceAddressTask.getAddress()).thenReturn(
                AddressDataModel.fromManualAddressData(
                    addressLineOne = "12 Test Road",
                    addressLineTwo = "Test District",
                    townOrCity = "Leeds",
                    county = "West Yorkshire",
                    postcode = "LS1 1AA",
                ),
            )

            assertEquals(
                listOf("12 Test Road", "Test District", "Leeds", "West Yorkshire", "LS1 1AA"),
                correspondenceRows()[1].fieldValue,
            )
        }

        @Test
        fun `contact change links have independent child journey ids and the postal address route prefix`() {
            val rows = correspondenceRows()

            assertEquals("forms.links.change", rows[0].actions.single().text)
            assertEquals("forms.links.change", rows[1].actions.single().text)
            assertEquals(
                JourneyStateService.urlWithJourneyState(CorrespondenceEmailStep.ROUTE_SEGMENT, "email-change"),
                rows[0].actions.single().url,
            )
            assertEquals(
                JourneyStateService.urlWithJourneyState(lookupPath, "postal-change"),
                rows[1].actions.single().url,
            )
        }

        private fun correspondenceRows(): List<SummaryListRowViewModel> =
            assertIs<List<*>>(stepConfig.getStepSpecificContent(mockState)["correspondenceRows"])
                .map { assertIs<SummaryListRowViewModel>(it) }
    }

    // TODO PDJB-2016: Remove this class when ASSOCIATED_PARTIES is released
    @Nested
    inner class BeforePdjb2146JointLandlordsCheckAnswers {
        @Test
        fun `getStepSpecificContent hides joint landlord invitations when there are no joint landlords`() {
            whenever(mockFeatureFlagManager.checkFeature(ASSOCIATED_PARTIES)).thenReturn(false)
            val expectedOccupancyDetails = listOf(mock<SummaryListRowViewModel>())
            whenever(mockHasJointLandlordsFormModel.hasJointLandlords).thenReturn(false)
            whenever(mockOccupancyDetailsHelper.getOccupancySummaryList(mockState)).thenReturn(expectedOccupancyDetails)

            val content = stepConfig.getStepSpecificContent(mockState)

            val peopleResponsibleForThisPropertyRows = content["peopleResponsibleForThisPropertyRows"] as List<SummaryListRowViewModel>
            val occupancyDetails = content["occupancyDetails"] as List<SummaryListRowViewModel>
            assertEquals(
                listOf(
                    "propertyDetails.propertyRecord.ownership.ownershipType",
                    "forms.checkPropertyAnswers.jointLandlordsDetails.areThereJointLandlords",
                ),
                peopleResponsibleForThisPropertyRows.map(SummaryListRowViewModel::fieldHeading),
            )
            assertEquals(
                "forms.checkPropertyAnswers.jointLandlordsDetails.noJointLandlords",
                peopleResponsibleForThisPropertyRows.last().fieldValue,
            )
            assertNull(content["jointLandlordsRows"])
            assertEquals(expectedOccupancyDetails, occupancyDetails)
        }

        @Test
        fun `getStepSpecificContent adds invitations to ownership rows and retains occupancy when there are joint landlords`() {
            whenever(mockFeatureFlagManager.checkFeature(ASSOCIATED_PARTIES)).thenReturn(false)
            val mockInviteJointLandlordsTask = mock<InviteJointLandlordsTask>()
            val mockCheckJointLandlordsStep = mock<CheckJointLandlordsStep>()
            val expectedOccupancyDetails = listOf(mock<SummaryListRowViewModel>())
            whenever(mockHasJointLandlordsFormModel.hasJointLandlords).thenReturn(true)
            whenever(mockJointLandlordsTask.inviteJointLandlordsTask).thenReturn(mockInviteJointLandlordsTask)
            whenever(mockInviteJointLandlordsTask.invitedJointLandlords).thenReturn(listOf("joint.landlord@example.com"))
            whenever(mockInviteJointLandlordsTask.checkJointLandlordsStep).thenReturn(mockCheckJointLandlordsStep)
            whenever(mockOccupancyDetailsHelper.getOccupancySummaryList(mockState)).thenReturn(expectedOccupancyDetails)

            val content = stepConfig.getStepSpecificContent(mockState)

            val peopleResponsibleForThisPropertyRows = content["peopleResponsibleForThisPropertyRows"] as List<SummaryListRowViewModel>
            val occupancyDetails = content["occupancyDetails"] as List<SummaryListRowViewModel>
            assertEquals(
                listOf(
                    "propertyDetails.propertyRecord.ownership.ownershipType",
                    "forms.checkPropertyAnswers.jointLandlordsDetails.jointLandlordInvitations",
                ),
                peopleResponsibleForThisPropertyRows.map(SummaryListRowViewModel::fieldHeading),
            )
            assertEquals(listOf("joint.landlord@example.com"), peopleResponsibleForThisPropertyRows.last().fieldValue)
            assertNull(content["jointLandlordsRows"])
            assertEquals(expectedOccupancyDetails, occupancyDetails)
        }
    }

    @Nested
    inner class JointLandlordsContent {
        @Test
        fun `getStepSpecificContent shows the no joint landlords answer in the joint landlords section`() {
            whenever(mockHasJointLandlordsFormModel.hasJointLandlords).thenReturn(false)
            stubOwnershipAndLandlordsTaskWithoutOtherParties()

            val content = stepConfig.getStepSpecificContent(mockState)

            val jointLandlordsRows = content["jointLandlordsRows"] as List<SummaryListRowViewModel>
            assertEquals(
                listOf("forms.checkPropertyAnswers.jointLandlordsDetails.areThereJointLandlords"),
                jointLandlordsRows.map(SummaryListRowViewModel::fieldHeading),
            )
            assertEquals(
                "forms.checkPropertyAnswers.jointLandlordsDetails.noJointLandlords",
                jointLandlordsRows.single().fieldValue,
            )
        }

        @Test
        fun `getStepSpecificContent shows joint landlord invitations in the joint landlords section`() {
            val mockInviteJointLandlordsTask = mock<InviteJointLandlordsTask>()
            val mockCheckJointLandlordsStep = mock<CheckJointLandlordsStep>()
            whenever(mockHasJointLandlordsFormModel.hasJointLandlords).thenReturn(true)
            whenever(mockJointLandlordsTask.inviteJointLandlordsTask).thenReturn(mockInviteJointLandlordsTask)
            whenever(mockInviteJointLandlordsTask.invitedJointLandlords).thenReturn(listOf("joint.landlord@example.com"))
            whenever(mockInviteJointLandlordsTask.checkJointLandlordsStep).thenReturn(mockCheckJointLandlordsStep)
            stubOwnershipAndLandlordsTaskWithoutOtherParties()

            val content = stepConfig.getStepSpecificContent(mockState)

            val jointLandlordsRows = content["jointLandlordsRows"] as List<SummaryListRowViewModel>
            assertEquals(
                listOf("forms.checkPropertyAnswers.jointLandlordsDetails.invitations"),
                jointLandlordsRows.map(SummaryListRowViewModel::fieldHeading),
            )
            assertEquals(listOf("joint.landlord@example.com"), jointLandlordsRows.single().fieldValue)
        }

        private fun stubOwnershipAndLandlordsTaskWithoutOtherParties() {
            setUpAssociatedPartiesOwnership(OwnershipType.SOLE_FREEHOLD)
        }
    }

    @Nested
    inner class LicensingContent {
        @Test
        fun `getStepSpecificContent uses no licensing wording when no licence selected`() {
            whenever(mockLicensingTask.getLicensingType()).thenReturn(LicensingType.NO_LICENSING)

            val content = stepConfig.getStepSpecificContent(mockState)

            val licensingRows = content["rentedOutLicensingRows"] as List<SummaryListRowViewModel>
            assertEquals(
                "forms.checkPropertyAnswers.propertyDetails.noLicensingRequired",
                licensingRows.first().fieldValue,
            )
        }

        @Test
        fun `getStepSpecificContent uses unoccupied provide-later wording when unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)
            whenever(mockLicensingTask.getLicensingType()).thenReturn(LicensingType.PROVIDE_LATER)

            val content = stepConfig.getStepSpecificContent(mockState)

            val licensingRows = content["rentedOutLicensingRows"] as List<SummaryListRowViewModel>
            assertEquals(
                "forms.checkPropertyAnswers.propertyDetails.licensingProvideLaterUnoccupied",
                licensingRows.first().fieldValue,
            )
        }
    }

    @Nested
    inner class TenancyContent {
        @Test
        fun `getStepSpecificContent puts rentedOutTenancyRows from getCheckYourAnswersSummaryList`() {
            val expectedTenancyDetails = listOf<SummaryListRowViewModel>()
            whenever(
                mockOccupancyDetailsHelper.getCheckYourAnswersSummaryList(any<PropertyRegistrationJourneyState>(), any(), any()),
            ).thenReturn(expectedTenancyDetails)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(expectedTenancyDetails, content["rentedOutTenancyRows"])
        }

        @Test
        fun `getStepSpecificContent uses tenancy placeholder key and no tenancy rows when unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(
                "forms.checkPropertyAnswers.tenancyDetails.unoccupiedBodyText",
                content["tenancyUnoccupiedBodyTextKey"],
            )
            assertEquals(emptyList<SummaryListRowViewModel>(), content["rentedOutTenancyRows"])
        }
    }

    @Nested
    inner class NonDelegatedContent {
        @BeforeEach
        fun setupNonDelegatedProperty() {
            whenever(mockState.isDelegatedToLettingAgent()).thenReturn(false)
        }

        @Test
        fun `getStepSpecificContent includes lettingAgentDelegation when landlord provides details`() {
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LANDLORD)

            val content = stepConfig.getStepSpecificContent(mockState)
            val delegationSection = content["lettingAgentDelegation"] as? List<*>

            assertTrue(content.containsKey("lettingAgentDelegation"))
            assertEquals(1, delegationSection?.size, "Landlord path should only include who-will-provide row")
            assertEquals(false, content["lettingAgentDelegationBodyText"], "Body text should not be shown for landlord path")
        }

        @Test
        fun `getStepSpecificContent does not include lettingAgentDelegation when property is unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)
            whenever(mockWhoProvidesRentalDetailsStep.formModelIfReachableOrNull).thenReturn(null)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertTrue(!content.containsKey("lettingAgentDelegation") || content["lettingAgentDelegation"] == null)
        }

        @Test
        fun `getStepSpecificContent shows unoccupied inset when property is unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)
            whenever(mockWhoProvidesRentalDetailsStep.formModelIfReachableOrNull).thenReturn(null)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(true, content["showLettingAgentDelegationUnoccupiedPanel"])
        }

        @Test
        fun `getStepSpecificContent does not show unoccupied inset when property is occupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(true)
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LANDLORD)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(false, content["showLettingAgentDelegationUnoccupiedPanel"])
        }
    }

    @Nested
    inner class DelegatedContent {
        @BeforeEach
        fun setupDelegatedProperty() {
            whenever(mockState.isDelegatedToLettingAgent()).thenReturn(true)
        }

        @Test
        fun `getStepSpecificContent includes lettingAgentDelegation when delegated to agent`() {
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LETTING_AGENT)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertTrue(content.containsKey("lettingAgentDelegation"))
            val delegationSection = content["lettingAgentDelegation"] as? List<*>
            assertEquals(2, delegationSection?.size, "Should have 2 rows: who will provide and email placeholder")
            assertEquals(true, content["lettingAgentDelegationBodyText"], "Body text should be shown for letting agent path")
            assertEquals(
                true,
                content["hideDelegatedSections"],
                "Delegated path should hide the licensing, compliance and tenancy sections",
            )
            assertEquals(
                "forms.buttons.completeRegistration",
                content["submitButtonText"],
                "Delegated properties should use the complete-registration message key",
            )
        }

        @Test
        fun `getStepSpecificContent does not show unoccupied tenancy body text when delegated property is occupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(true)
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LETTING_AGENT)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertNull(content["tenancyUnoccupiedBodyTextKey"])
        }
    }

    @Nested
    inner class BaseContent {
        @Test
        fun `chooseTemplate returns CYA template`() {
            assertEquals(
                "forms/propertyRegistrationCheckAnswersForm",
                stepConfig.chooseTemplate(mockState),
            )
        }

        @Test
        fun `getStepSpecificContent puts occupancyDetails from getOccupancySummaryList`() {
            val expectedOccupancyDetails = listOf<SummaryListRowViewModel>()
            whenever(mockOccupancyDetailsHelper.getOccupancySummaryList(mockState)).thenReturn(expectedOccupancyDetails)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(expectedOccupancyDetails, content["occupancyDetails"])
        }

        @Test
        fun `getStepSpecificContent includes the registration warning text`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals("forms.checkPropertyAnswers.warning", content["warningTextKey"])
        }

        @Test
        fun `getStepSpecificContent includes flattened about-property and rented-out keys`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals("forms.checkPropertyAnswers.aboutYourProperty.heading", content["aboutPropertyHeadingKey"])
            assertEquals("forms.checkPropertyAnswers.rentedOut.heading", content["rentedOutHeadingKey"])
            assertTrue(content["rentedOutLicensingRows"] is List<*>)
            assertTrue(content["rentedOutTenancyRows"] is List<*>)
        }

        @Test
        fun `getStepSpecificContent uses Address heading for property details row`() {
            val content = stepConfig.getStepSpecificContent(mockState)
            val propertyDetailsRows = content["propertyDetails"] as List<SummaryListRowViewModel>

            assertTrue(
                propertyDetailsRows.any { it.fieldHeading == "propertyDetails.propertyRecord.propertyDetails.address" },
            )
        }

        @Test
        fun `getStepSpecificContent uses ownership question heading under ownership and landlords rows`() {
            val content = stepConfig.getStepSpecificContent(mockState)
            val propertyDetailsRows = content["propertyDetails"] as List<SummaryListRowViewModel>
            val peopleResponsibleForThisPropertyRows = content["peopleResponsibleForThisPropertyRows"] as List<SummaryListRowViewModel>

            assertTrue(
                peopleResponsibleForThisPropertyRows.any { it.fieldHeading == "propertyDetails.propertyRecord.ownership.ownershipType" },
            )
            assertTrue(
                propertyDetailsRows.none { it.fieldHeading == "propertyDetails.propertyRecord.ownership.ownershipType" },
            )
        }
    }

    @Nested
    inner class OwnershipContent {
        @Mock
        private lateinit var mockSuperiorLandlordsTask: SuperiorLandlordsTask

        @Mock
        private lateinit var mockAnyoneInOwnershipChainStep: AnyoneInOwnershipChainStep

        @BeforeEach
        fun setUpOwnership() {
            // SOLE_FREEHOLD needs no superior landlord info, so superior landlords content stays empty
            setUpAssociatedPartiesOwnership(OwnershipType.SOLE_FREEHOLD)
        }

        // TODO PDJB-2016: Remove this flag-off test when ASSOCIATED_PARTIES is enabled for all users.
        @Test
        fun `associated parties disabled has no ownership rows and keeps the old ownership type row`() {
            whenever(mockFeatureFlagManager.checkFeature(ASSOCIATED_PARTIES)).thenReturn(false)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertNull(content["ownershipRows"])
            val peopleRows = content["peopleResponsibleForThisPropertyRows"] as List<SummaryListRowViewModel>
            assertTrue(peopleRows.any { it.fieldHeading == "propertyDetails.propertyRecord.ownership.ownershipType" })
        }

        @Test
        fun `associated parties enabled removes the old ownership type row`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertNull(content["peopleResponsibleForThisPropertyRows"])
            val jointLandlordsRows = content["jointLandlordsRows"] as List<SummaryListRowViewModel>
            assertEquals(
                listOf("forms.checkPropertyAnswers.jointLandlordsDetails.areThereJointLandlords"),
                jointLandlordsRows.map(SummaryListRowViewModel::fieldHeading),
            )
        }

        @Test
        fun `not owning the property shows only the owns property row`() {
            whenever(mockOwnershipTypeTask.ownershipType).thenReturn(OwnershipType.NONE)
            // NONE requires superior landlord info, so answer the ownership chain question to avoid its exception
            stubAnyoneInOwnershipChainAnsweredNo()

            val rows = ownershipRows()

            assertEquals(listOf("propertyDetails.peopleResponsible.ownership.ownsProperty"), rows.map { it.fieldHeading })
            assertEquals("commonText.no", rows.single().getConvertedFieldValue())
            assertEquals(
                JourneyStateService.urlWithJourneyState(OwnsPropertyStep.ROUTE_SEGMENT, "owns-change"),
                rows.single().actions.single().url,
            )
        }

        @Test
        fun `leasehold only shows the owns property and ownership type rows`() {
            whenever(mockOwnershipTypeTask.ownershipType).thenReturn(OwnershipType.LEASEHOLD_ONLY)
            stubAnyoneInOwnershipChainAnsweredNo()

            val rows = ownershipRows()

            assertEquals(
                listOf(
                    "propertyDetails.peopleResponsible.ownership.ownsProperty",
                    "propertyDetails.peopleResponsible.ownership.ownershipType",
                ),
                rows.map { it.fieldHeading },
            )
            assertEquals(
                listOf("commonText.yes", "registerProperty.ownershipKind.checkbox.leasehold.label"),
                rows.map { it.getConvertedFieldValue() },
            )
            assertEquals(
                JourneyStateService.urlWithJourneyState(OwnershipKindStep.ROUTE_SEGMENT, "kind-change"),
                rows[1].actions.single().url,
            )
        }

        @Test
        fun `freehold only shows all three rows with change links to each question`() {
            val rows = ownershipRows()

            assertEquals(
                listOf(
                    "propertyDetails.peopleResponsible.ownership.ownsProperty",
                    "propertyDetails.peopleResponsible.ownership.ownershipType",
                    "propertyDetails.peopleResponsible.ownership.freeholderType",
                ),
                rows.map { it.fieldHeading },
            )
            assertEquals(
                listOf(
                    "commonText.yes",
                    "registerProperty.ownershipKind.checkbox.freehold.label",
                    "registerProperty.freeholdType.radios.option.soleOwnership.label",
                ),
                rows.map { it.getConvertedFieldValue() },
            )
            assertEquals(
                listOf(
                    JourneyStateService.urlWithJourneyState(OwnsPropertyStep.ROUTE_SEGMENT, "owns-change"),
                    JourneyStateService.urlWithJourneyState(OwnershipKindStep.ROUTE_SEGMENT, "kind-change"),
                    JourneyStateService.urlWithJourneyState(FreeholdTypeStep.ROUTE_SEGMENT, "freehold-type-change"),
                ),
                rows.map { it.actions.single().url },
            )
        }

        @Test
        fun `freehold and leasehold shows the combined ownership type value`() {
            whenever(mockOwnershipTypeTask.ownershipType).thenReturn(OwnershipType.SHARE_OF_TENANTS_IN_COMMON)
            stubAnyoneInOwnershipChainAnsweredNo()

            val rows = ownershipRows()

            assertEquals(
                listOf(
                    "commonText.yes",
                    "registerProperty.ownershipKind.value.freeholdAndLeasehold",
                    "registerProperty.freeholdType.radios.option.tenantsInCommon.label",
                ),
                rows.map { it.getConvertedFieldValue() },
            )
        }

        @Test
        fun `incomplete ownership type task throws an unrecoverable journey state exception`() {
            whenever(mockState.journeyId).thenReturn("journey-id")
            whenever(mockOwnershipTypeTask.ownershipType).thenReturn(null)

            assertThrows<UnrecoverableJourneyStateException> { stepConfig.getStepSpecificContent(mockState) }
        }

        private fun ownershipRows() = stepConfig.getStepSpecificContent(mockState)["ownershipRows"] as List<SummaryListRowViewModel>

        private fun stubAnyoneInOwnershipChainAnsweredNo() {
            whenever(mockOwnershipStructureTask.superiorLandlordsTask).thenReturn(mockSuperiorLandlordsTask)
            whenever(mockSuperiorLandlordsTask.anyoneInOwnershipChainStep).thenReturn(mockAnyoneInOwnershipChainStep)
            whenever(mockAnyoneInOwnershipChainStep.formModelIfReachableOrNull)
                .thenReturn(AnyoneInOwnershipChainFormModel(anyoneInOwnershipChain = false))
        }
    }

    @Nested
    inner class SuperiorLandlordsContent {
        @Mock
        private lateinit var mockSuperiorLandlordsTask: SuperiorLandlordsTask

        @Mock
        private lateinit var mockAnyoneInOwnershipChainStep: AnyoneInOwnershipChainStep

        @Mock
        private lateinit var mockSuperiorLandlordsDetailsTask: SuperiorLandlordsDetailsTask

        @Mock
        private lateinit var mockSuperiorLandlordsListStep: SuperiorLandlordsListStep

        @BeforeEach
        fun setUpSuperiorLandlords() {
            // LEASEHOLD_ONLY requires superior-landlord information, so this enables that content.
            setUpAssociatedPartiesOwnership(OwnershipType.LEASEHOLD_ONLY)
            lenient().`when`(mockOwnershipStructureTask.superiorLandlordsTask).thenReturn(mockSuperiorLandlordsTask)
            lenient().`when`(mockSuperiorLandlordsTask.anyoneInOwnershipChainStep).thenReturn(mockAnyoneInOwnershipChainStep)
            lenient().`when`(mockSuperiorLandlordsTask.superiorLandlordsDetailsTask).thenReturn(mockSuperiorLandlordsDetailsTask)
            lenient().`when`(mockSuperiorLandlordsDetailsTask.superiorLandlordsListStep).thenReturn(mockSuperiorLandlordsListStep)
            lenient().`when`(mockAnyoneInOwnershipChainStep.isStepReachable).thenReturn(true)
            lenient().`when`(mockAnyoneInOwnershipChainStep.urlPath).thenReturn(AnyoneInOwnershipChainStep.ROUTE_SEGMENT)
            lenient().`when`(mockState.getCyaJourneyId(mockAnyoneInOwnershipChainStep)).thenReturn("chain-change")
            lenient().`when`(mockSuperiorLandlordsListStep.isStepReachable).thenReturn(true)
            lenient().`when`(mockSuperiorLandlordsListStep.urlPath).thenReturn(SuperiorLandlordsListStep.ROUTE_SEGMENT)
            lenient().`when`(mockState.getCyaJourneyId(mockSuperiorLandlordsListStep)).thenReturn("list-change")
        }

        @Test
        fun `getStepSpecificContent uses the people responsible heading`() {
            whenever(mockAnyoneInOwnershipChainStep.formModelIfReachableOrNull)
                .thenReturn(AnyoneInOwnershipChainFormModel(anyoneInOwnershipChain = false))

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(
                "forms.checkPropertyAnswers.peopleResponsibleForThisProperty.heading",
                content["peopleResponsibleForThisPropertyHeadingKey"],
            )
        }

        @Test
        fun `ownership type that does not require superior landlord info has no superior landlords content`() {
            // Arrange, OwnershipType.SOLE_FREEHOLD is value that returns false for OwnershipType#requiresSuperiorLandlordInfo
            whenever(mockOwnershipTypeTask.ownershipType).thenReturn(OwnershipType.SOLE_FREEHOLD)

            // Act
            val content = stepConfig.getStepSpecificContent(mockState)

            // Assert
            assertNoSuperiorLandlordsContent(content)
        }

        @Test
        fun `missing ownership type throws an unrecoverable journey state exception`() {
            whenever(mockState.journeyId).thenReturn("journey-id")
            whenever(mockOwnershipTypeTask.ownershipType).thenReturn(null)

            assertThrows<UnrecoverableJourneyStateException> { stepConfig.getStepSpecificContent(mockState) }
        }

        @Test
        fun `missing anyone in ownership chain answer throws an unrecoverable journey state exception`() {
            // Arrange, the default setup is a journey that should've asked superior landlords
            whenever(mockState.journeyId).thenReturn("journey-id")
            whenever(mockAnyoneInOwnershipChainStep.formModelIfReachableOrNull).thenReturn(null)

            // Act & Assert
            assertThrows<UnrecoverableJourneyStateException> { stepConfig.getStepSpecificContent(mockState) }
        }

        @Test
        fun `answering yes with no superior landlords throws an unrecoverable journey state exception`() {
            // Arrange, the default setup is a journey that should've asked superior landlords
            whenever(mockState.journeyId).thenReturn("journey-id")
            whenever(mockAnyoneInOwnershipChainStep.formModelIfReachableOrNull)
                .thenReturn(AnyoneInOwnershipChainFormModel(anyoneInOwnershipChain = true))
            whenever(mockSuperiorLandlordsTask.superiorLandlordsMap).thenReturn(null)

            // Act & Assert
            assertThrows<UnrecoverableJourneyStateException> { stepConfig.getStepSpecificContent(mockState) }
        }

        @Test
        fun `answering no shows a single row with a change link to the ownership chain question`() {
            whenever(mockAnyoneInOwnershipChainStep.formModelIfReachableOrNull)
                .thenReturn(AnyoneInOwnershipChainFormModel(anyoneInOwnershipChain = false))

            val content = stepConfig.getStepSpecificContent(mockState)

            val row = (content["superiorLandlordsRows"] as List<SummaryListRowViewModel>).single()
            assertEquals("registerProperty.superiorLandlords.checkAnswers.anyoneInOwnershipChain", row.fieldHeading)
            assertEquals("commonText.no", row.fieldValue)
            assertEquals(
                JourneyStateService.urlWithJourneyState(AnyoneInOwnershipChainStep.ROUTE_SEGMENT, "chain-change"),
                row.actions.single().url,
            )
            assertNull(content["superiorLandlordsCards"])
            assertNull(content["superiorLandlordsChangeUrl"])
        }

        @Test
        fun `answering yes shows a link to the superior landlords list and cards without actions`() {
            whenever(mockAnyoneInOwnershipChainStep.formModelIfReachableOrNull)
                .thenReturn(AnyoneInOwnershipChainFormModel(anyoneInOwnershipChain = true))
            whenever(mockSuperiorLandlordsTask.superiorLandlordsMap).thenReturn(
                mapOf(
                    2 to SuperiorLandlordDataModel(detailsKnown = false),
                    1 to SuperiorLandlordDataModel(detailsKnown = true, name = "Billie Smith", email = "email@example.com"),
                ),
            )

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(
                JourneyStateService.urlWithJourneyState(SuperiorLandlordsListStep.ROUTE_SEGMENT, "list-change"),
                content["superiorLandlordsChangeUrl"],
            )
            val cards = content["superiorLandlordsCards"] as List<SummaryCardViewModel>
            assertEquals(
                listOf("Billie Smith", "registerProperty.superiorLandlords.list.unknownSuperiorLandlord.title"),
                cards.map { it.title },
            )
            cards.forEach { assertEquals(emptyList<SummaryCardActionViewModel>(), it.actions) }
            assertNull(content["superiorLandlordsRows"])
        }

        private fun assertNoSuperiorLandlordsContent(content: Map<String, Any?>) {
            assertNull(content["superiorLandlordsRows"])
            assertNull(content["superiorLandlordsCards"])
            assertNull(content["superiorLandlordsChangeUrl"])
        }
    }

    @Nested
    inner class FreeholdersContent {
        @Mock
        private lateinit var mockSuperiorLandlordsTask: SuperiorLandlordsTask

        @Mock
        private lateinit var mockAnyoneInOwnershipChainStep: AnyoneInOwnershipChainStep

        @BeforeEach
        fun setUpFreeholders() {
            setUpAssociatedPartiesOwnership(OwnershipType.LEASEHOLD_ONLY)
            lenient().`when`(mockOwnershipStructureTask.superiorLandlordsTask).thenReturn(mockSuperiorLandlordsTask)
            lenient().`when`(mockSuperiorLandlordsTask.anyoneInOwnershipChainStep).thenReturn(mockAnyoneInOwnershipChainStep)
            lenient().`when`(mockAnyoneInOwnershipChainStep.formModelIfReachableOrNull)
                .thenReturn(AnyoneInOwnershipChainFormModel(anyoneInOwnershipChain = false))
        }

        @Test
        fun `ownership type that does not require freeholder records has no freeholders content`() {
            // Arrange
            whenever(mockOwnershipTypeTask.ownershipType).thenReturn(OwnershipType.SOLE_FREEHOLD)

            // Act
            val content = stepConfig.getStepSpecificContent(mockState)

            // Assert
            assertNoFreeholdersContent(content)
        }

        @Test
        fun `missing ownership type throws an unrecoverable journey state exception`() {
            // Arrange
            whenever(mockState.journeyId).thenReturn("journey-id")
            whenever(mockOwnershipTypeTask.ownershipType).thenReturn(null)

            // Act & Assert
            assertThrows<UnrecoverableJourneyStateException> { stepConfig.getStepSpecificContent(mockState) }
        }

        @Test
        fun `missing freeholders throws an unrecoverable journey state exception`() {
            // Arrange
            whenever(mockState.journeyId).thenReturn("journey-id")
            whenever(mockFreeholdersTask.freeholdersMap).thenReturn(null)

            // Act & Assert
            assertThrows<UnrecoverableJourneyStateException> { stepConfig.getStepSpecificContent(mockState) }
        }

        @Test
        fun `empty freeholders throws an unrecoverable journey state exception`() {
            // Arrange
            whenever(mockState.journeyId).thenReturn("journey-id")
            whenever(mockFreeholdersTask.freeholdersMap).thenReturn(emptyMap())

            // Act & Assert
            assertThrows<UnrecoverableJourneyStateException> { stepConfig.getStepSpecificContent(mockState) }
        }

        @Test
        fun `non-joint ownership type uses the non-joint heading and change link keys`() {
            // Act
            val content = stepConfig.getStepSpecificContent(mockState)

            // Assert
            assertEquals("registerProperty.freeholders.checkAnswers.heading.nonJoint", content["freeholdersHeadingKey"])
            assertEquals("registerProperty.freeholders.checkAnswers.changeLink.nonJoint", content["freeholdersChangeLinkKey"])
        }

        @Test
        fun `joint ownership type uses the joint heading and change link keys`() {
            // Arrange
            whenever(mockOwnershipTypeTask.ownershipType).thenReturn(OwnershipType.JOINT_TENANTS)

            // Act
            val content = stepConfig.getStepSpecificContent(mockState)

            // Assert
            assertEquals("registerProperty.freeholders.checkAnswers.heading.joint", content["freeholdersHeadingKey"])
            assertEquals("registerProperty.freeholders.checkAnswers.changeLink.joint", content["freeholdersChangeLinkKey"])
        }

        @Test
        fun `freeholders content has a link to the freeholders list and cards without actions`() {
            // Arrange
            whenever(mockFreeholdersTask.freeholdersMap).thenReturn(
                mapOf(
                    2 to FreeholderDataModel(detailsKnown = false),
                    1 to FreeholderDataModel(detailsKnown = true, name = "Billie Smith", email = "email@example.com"),
                ),
            )

            // Act
            val content = stepConfig.getStepSpecificContent(mockState)

            // Assert
            assertEquals(
                JourneyStateService.urlWithJourneyState(FreeholdersListStep.ROUTE_SEGMENT, "freeholders-change"),
                content["freeholdersChangeUrl"],
            )
            val cards = content["freeholdersCards"] as List<SummaryCardViewModel>
            assertEquals(
                listOf("Billie Smith", "registerProperty.freeholders.list.unknownFreeholder.title"),
                cards.map { it.title },
            )
            cards.forEach { assertEquals(emptyList<SummaryCardActionViewModel>(), it.actions) }
        }

        private fun assertNoFreeholdersContent(content: Map<String, Any?>) {
            assertNull(content["freeholdersHeadingKey"])
            assertNull(content["freeholdersChangeLinkKey"])
            assertNull(content["freeholdersChangeUrl"])
            assertNull(content["freeholdersCards"])
        }
    }

    @Nested
    inner class PropertyManagerContent {
        @Mock
        private lateinit var mockPropertyManagerDetailsStep: PropertyManagerDetailsStep

        @Mock
        private lateinit var mockCheckPropertyManagerStep: CheckPropertyManagerStep

        @BeforeEach
        fun setUpPropertyManager() {
            // OwnershipType.SOLE_FREEHOLD returns false for OwnershipType#requiresSuperiorLandlordInfo
            setUpAssociatedPartiesOwnership(OwnershipType.SOLE_FREEHOLD)
            lenient().`when`(mockPropertyManagerTask.propertyManagerDetailsStep).thenReturn(mockPropertyManagerDetailsStep)
            lenient().`when`(mockPropertyManagerTask.checkPropertyManagerStep).thenReturn(mockCheckPropertyManagerStep)
            lenient().`when`(mockState.getCyaJourneyId(mockPropertyManagerStep)).thenReturn("property-manager-change")
            lenient().`when`(mockCheckPropertyManagerStep.isStepReachable).thenReturn(true)
            lenient().`when`(mockCheckPropertyManagerStep.urlPath).thenReturn(CheckPropertyManagerStep.ROUTE_SEGMENT)
            lenient().`when`(mockState.getCyaJourneyId(mockCheckPropertyManagerStep)).thenReturn("check-property-manager-change")
        }

        @Test
        fun `missing property manager answer throws an unrecoverable journey state exception`() {
            whenever(mockState.journeyId).thenReturn("journey-id")
            whenever(mockPropertyManagerStep.formModelIfReachableOrNull).thenReturn(null)

            assertThrows<UnrecoverableJourneyStateException> { stepConfig.getStepSpecificContent(mockState) }
        }

        @Test
        fun `not using a property manager shows a single row with a change link to the property manager question`() {
            // Arrange
            whenever(mockPropertyManagerStep.formModelIfReachableOrNull)
                .thenReturn(PropertyManagerFormModel().apply { usingPropertyManager = false })

            // Act
            val content = stepConfig.getStepSpecificContent(mockState)

            // Assert
            val row = (content["propertyManagerRows"] as List<SummaryListRowViewModel>).single()
            assertEquals("registerProperty.propertyManager.usingPropertyManager.fieldSetHeading", row.fieldHeading)
            assertEquals("registerProperty.propertyManager.usingPropertyManager.radios.no", row.fieldValue)
            assertEquals(
                JourneyStateService.urlWithJourneyState(PropertyManagerStep.ROUTE_SEGMENT, "property-manager-change"),
                row.actions.single().url,
            )
            assertNull(content["propertyManagerCards"])
            assertNull(content["propertyManagerChangeUrl"])
        }

        @Test
        fun `using a property manager shows a link to the check property manager page and a card without actions`() {
            // Arrange
            whenever(mockPropertyManagerStep.formModelIfReachableOrNull)
                .thenReturn(PropertyManagerFormModel().apply { usingPropertyManager = true })
            whenever(mockPropertyManagerDetailsStep.formModel).thenReturn(
                PropertyManagerDetailsFormModel().apply {
                    name = "Best Property Managers Ltd"
                    emailAddress = "manager@example.com"
                },
            )

            // Act
            val content = stepConfig.getStepSpecificContent(mockState)

            // Assert
            assertEquals(
                JourneyStateService.urlWithJourneyState(CheckPropertyManagerStep.ROUTE_SEGMENT, "check-property-manager-change"),
                content["propertyManagerChangeUrl"],
            )
            val card = (content["propertyManagerCards"] as List<SummaryCardViewModel>).single()
            assertEquals("Best Property Managers Ltd", card.title)
            assertEquals("manager@example.com", card.summaryList.single().fieldValue)
            assertEquals(emptyList<SummaryCardActionViewModel>(), card.actions)
            assertNull(content["propertyManagerRows"])
        }
    }

    // TODO PDJB-2016: Remove these flag-off tests when ASSOCIATED_PARTIES is enabled for all users and the flag is removed.
    @Nested
    inner class BeforeAssociatedPartiesContent {
        @BeforeEach
        fun setUpAssociatedPartiesDisabled() {
            whenever(mockFeatureFlagManager.checkFeature(ASSOCIATED_PARTIES)).thenReturn(false)
        }

        @Test
        fun `associated parties disabled uses the ownership and landlords heading and has no superior landlords content`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals("forms.checkPropertyAnswers.ownershipAndLandlords.heading", content["peopleResponsibleForThisPropertyHeadingKey"])
            assertNull(content["superiorLandlordsRows"])
            assertNull(content["superiorLandlordsCards"])
            assertNull(content["superiorLandlordsChangeUrl"])
        }

        @Test
        fun `associated parties disabled has no property manager content`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertNull(content["propertyManagerRows"])
            assertNull(content["propertyManagerCards"])
            assertNull(content["propertyManagerChangeUrl"])
        }

        @Test
        fun `associated parties disabled has no freeholders content`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertNull(content["freeholdersHeadingKey"])
            assertNull(content["freeholdersChangeLinkKey"])
            assertNull(content["freeholdersChangeUrl"])
            assertNull(content["freeholdersCards"])
        }

        // TODO PDJB-2016: Remove or update this assertion when ASSOCIATED_PARTIES is enabled
        @Test
        fun `associated parties feature flag is checked once for people responsible content`() {
            stepConfig.getStepSpecificContent(mockState)

            verify(mockFeatureFlagManager).checkFeature(ASSOCIATED_PARTIES)
        }
    }

    @Nested
    inner class SubmitButtonContent {
        @Test
        fun `getStepSpecificContent uses submit and pay button when payments is enabled`() {
            whenever(mockFeatureFlagManager.checkFeature(PAYMENTS)).thenReturn(true)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals("forms.buttons.submitAndPay", content["submitButtonText"])
        }

        @Test
        fun `getStepSpecificContent uses complete registration button when payments is disabled`() {
            whenever(mockFeatureFlagManager.checkFeature(PAYMENTS)).thenReturn(false)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals("forms.buttons.completeRegistration", content["submitButtonText"])
        }
    }
}

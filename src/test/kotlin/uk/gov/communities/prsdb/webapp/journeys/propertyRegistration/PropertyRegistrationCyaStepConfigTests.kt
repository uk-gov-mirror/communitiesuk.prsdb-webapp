package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.WhoProvidesRentalDetails
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BedroomsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CorrespondenceEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasJointLandlordsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LicensingTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LocalCouncilStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupiedStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyRegistrationCyaStepConfig
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideTenancyDetailsLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.WhoProvidesRentalDetailsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.CorrespondenceTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseholdsAndTenantsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.JointLandlordsPropertyRegistrationTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.LicensingTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipAndLandlordsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyRegistrationAddressTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.TenancyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.WhoProvidesDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.ComplianceDetailsHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.LicensingDetailsHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.OccupancyDetailsHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.inviteJointLandlord.CheckJointLandlordsStep
import uk.gov.communities.prsdb.webapp.journeys.shared.inviteJointLandlord.InviteJointLandlordsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.CorrespondenceAddressTask
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.CorrespondenceEmailFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.HasJointLandlordsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfBedroomsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OccupancyFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OwnershipTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.WhoProvidesRentalDetailsFormModel
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
    private lateinit var mockOwnershipAndLandlordsTask: OwnershipAndLandlordsTask

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
            )
        lenient().`when`(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
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
        lenient().`when`(mockState.ownershipAndLandlordsTask).thenReturn(mockOwnershipAndLandlordsTask)
        lenient().`when`(mockOwnershipAndLandlordsTask.ownershipTypeStep).thenReturn(mockOwnershipTypeStep)
        lenient().`when`(mockOwnershipTypeStep.formModel).thenReturn(mockOwnershipTypeFormModel)
        lenient().`when`(mockOwnershipAndLandlordsTask.jointLandlordsTask).thenReturn(mockJointLandlordsTask)
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
        lenient().`when`(mockOccupancyDetailsHelper.getRestructuredOccupancySummaryList(any())).thenReturn(emptyList())
        lenient().`when`(mockOccupancyDetailsHelper.getRestructuredCheckYourAnswersSummaryList(any(), any(), any())).thenReturn(emptyList())
        lenient().`when`(mockOccupancyDetailsHelper.getCheckYourAnswersSummaryList(any(), any())).thenReturn(emptyList())
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
    }

    @Nested
    inner class CorrespondenceContent {
        private val correspondenceTask = mock<CorrespondenceTask>()
        private val emailStep = mock<CorrespondenceEmailStep>()
        private val addressTask = mock<CorrespondenceAddressTask>()
        private val lookupStep = mock<LookupAddressStep>()
        private val emailForm =
            CorrespondenceEmailFormModel().apply {
                whichEmail = CorrespondenceEmailOption.ACCOUNT_EMAIL
                differentEmailAddress = "council.contact@example.com"
            }
        private val lookupPath = "${CorrespondenceAddressTask.ROUTE_SEGMENT}/${LookupAddressStep.ROUTE_SEGMENT}"

        @BeforeEach
        fun setUpCorrespondence() {
            lenient().`when`(mockFeatureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(true)
            lenient().`when`(mockFeatureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS)).thenReturn(true)
            lenient().`when`(mockState.correspondenceTask).thenReturn(correspondenceTask)
            lenient().`when`(correspondenceTask.correspondenceEmailStep).thenReturn(emailStep)
            lenient().`when`(correspondenceTask.addressTask).thenReturn(addressTask)
            lenient().`when`(emailStep.formModel).thenReturn(emailForm)
            lenient().`when`(emailStep.isStepReachable).thenReturn(true)
            lenient().`when`(emailStep.urlPath).thenReturn(CorrespondenceEmailStep.ROUTE_SEGMENT)
            lenient().`when`(mockState.loggedInLandlordEmail).thenReturn("account@example.com")
            lenient().`when`(addressTask.lookupAddressStep).thenReturn(lookupStep)
            lenient().`when`(lookupStep.isStepReachable).thenReturn(true)
            lenient().`when`(lookupStep.urlPath).thenReturn(lookupPath)
            lenient().`when`(mockState.getCyaJourneyId(emailStep)).thenReturn("email-change")
            lenient().`when`(mockState.getCyaJourneyId(lookupStep)).thenReturn("postal-change")
            lenient().`when`(addressTask.getAddress()).thenReturn(
                AddressDataModel(
                    "12 Test Road, Leeds, LS1 1AA",
                    buildingNumber = "12",
                    streetName = "Test Road",
                    townName = "Leeds",
                    postcode = "LS1 1AA",
                ),
            )
        }

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
            emailForm.whichEmail = CorrespondenceEmailOption.DIFFERENT_EMAIL

            assertEquals("council.contact@example.com", correspondenceRows()[0].fieldValue)
            verify(mockState, never()).loggedInLandlordEmail
        }

        @Test
        fun `contact postal address uses the same multiline format as the property address`() {
            assertEquals(listOf("12 Test Road", "Leeds", "LS1 1AA"), correspondenceRows()[1].fieldValue)
        }

        @Test
        fun `contact postal address preserves manually entered address lines and county`() {
            whenever(addressTask.getAddress()).thenReturn(
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

        @Test
        fun `contact rows are retained when property details are delegated to a letting agent`() {
            whenever(mockState.isDelegatedToLettingAgent(mockFeatureFlagManager)).thenReturn(true)
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LETTING_AGENT)

            assertEquals(2, correspondenceRows().size)
        }

        @Test
        fun `contact rows are retained when the property is unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)

            assertEquals(2, correspondenceRows().size)
        }

        @Test
        fun `contact rows are retained when letting agent delegation is disabled`() {
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(false)

            assertEquals(2, correspondenceRows().size)
        }

        @Test
        fun `disabled correspondence does not read contact state or create child journeys`() {
            whenever(mockFeatureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS)).thenReturn(false)

            assertEquals(emptyList<SummaryListRowViewModel>(), correspondenceRows())
            verify(mockState, never()).correspondenceTask
            verify(mockState, never()).getCyaJourneyId(emailStep)
            verify(mockState, never()).getCyaJourneyId(lookupStep)
        }

        @Test
        fun `legacy CYA does not read contact state even when correspondence is enabled`() {
            whenever(mockFeatureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(false)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertTrue(!content.containsKey("correspondenceRows"))
            verify(mockState, never()).correspondenceTask
            verify(mockState, never()).getCyaJourneyId(emailStep)
            verify(mockState, never()).getCyaJourneyId(lookupStep)
        }

        private fun correspondenceRows(): List<SummaryListRowViewModel> =
            assertIs<List<*>>(stepConfig.getStepSpecificContent(mockState)["correspondenceRows"])
                .map { assertIs<SummaryListRowViewModel>(it) }
    }

    @Nested
    inner class RestructuredContentWithoutLettingAgents {
        @BeforeEach
        fun enableRestructureAndSkippingFlagWithoutLettingAgents() {
            whenever(mockFeatureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(true)
            lenient().`when`(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(false)
        }

        @Test
        fun `chooseTemplate returns restructured CYA template`() {
            assertEquals(
                "forms/restructureAndSkipping/propertyRegistrationCheckAnswersForm",
                stepConfig.chooseTemplate(mockState),
            )
        }

        @Test
        fun `getStepSpecificContent puts occupancyDetails from getRestructuredOccupancySummaryList`() {
            val expectedOccupancyDetails = listOf<SummaryListRowViewModel>()
            whenever(mockOccupancyDetailsHelper.getRestructuredOccupancySummaryList(mockState)).thenReturn(expectedOccupancyDetails)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(expectedOccupancyDetails, content["occupancyDetails"])
        }

        @Test
        fun `getStepSpecificContent puts tenancyDetails from getRestructuredCheckYourAnswersSummaryList`() {
            val expectedTenancyDetails = listOf<SummaryListRowViewModel>()
            whenever(
                mockOccupancyDetailsHelper.getRestructuredCheckYourAnswersSummaryList(any(), any(), any()),
            ).thenReturn(expectedTenancyDetails)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(expectedTenancyDetails, content["tenancyDetails"])
        }

        @Test
        fun `getStepSpecificContent does not include lettingAgentDelegation`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertTrue(!content.containsKey("lettingAgentDelegation") || content["lettingAgentDelegation"] == null)
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
        fun `getStepSpecificContent uses complete registration button and restructured warning text`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals("forms.buttons.completeRegistration", content["submitButtonText"])
            assertEquals("forms.checkPropertyAnswers.warning", content["warningTextKey"])
        }

        @Test
        fun `getStepSpecificContent uses complete registration button when unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals("forms.buttons.completeRegistration", content["submitButtonText"])
        }

        @Test
        fun `getStepSpecificContent uses restructured no licensing wording when no licence selected`() {
            whenever(mockLicensingTask.getLicensingType()).thenReturn(LicensingType.NO_LICENSING)

            val content = stepConfig.getStepSpecificContent(mockState)

            val licensingRows = content["licensingDetails"] as List<SummaryListRowViewModel>
            assertEquals(
                "forms.checkPropertyAnswers.propertyDetails.noLicensing.restructureAndSkipping",
                licensingRows.first().fieldValue,
            )
        }

        @Test
        fun `getStepSpecificContent uses unoccupied provide-later wording and tenancy placeholder key when unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)
            whenever(mockLicensingTask.getLicensingType()).thenReturn(LicensingType.PROVIDE_LATER)

            val content = stepConfig.getStepSpecificContent(mockState)

            val licensingRows = content["licensingDetails"] as List<SummaryListRowViewModel>
            assertEquals(
                "forms.checkPropertyAnswers.propertyDetails.licensingProvideLaterUnoccupied",
                licensingRows.first().fieldValue,
            )
            assertEquals(
                "forms.checkPropertyAnswers.tenancyDetails.unoccupiedBodyText",
                content["tenancyUnoccupiedBodyTextKey"],
            )
            assertEquals(emptyList<SummaryListRowViewModel>(), content["rentedOutTenancyRows"])
        }

        @Test
        fun `getStepSpecificContent uses Address heading for restructured property details row`() {
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
            val ownershipAndLandlordsRows = content["ownershipAndLandlordsRows"] as List<SummaryListRowViewModel>

            assertTrue(
                ownershipAndLandlordsRows.any { it.fieldHeading == "propertyDetails.propertyRecord.ownership.ownershipType" },
            )
            assertTrue(
                propertyDetailsRows.none { it.fieldHeading == "propertyDetails.propertyRecord.ownership.ownershipType" },
            )
        }

        @Test
        fun `getStepSpecificContent hides joint landlord invitations when there are no joint landlords`() {
            val expectedOccupancyDetails = listOf(mock<SummaryListRowViewModel>())
            whenever(mockHasJointLandlordsFormModel.hasJointLandlords).thenReturn(false)
            whenever(mockOccupancyDetailsHelper.getRestructuredOccupancySummaryList(mockState)).thenReturn(expectedOccupancyDetails)

            val content = stepConfig.getStepSpecificContent(mockState)

            val ownershipAndLandlordsRows = content["ownershipAndLandlordsRows"] as List<SummaryListRowViewModel>
            val occupancyDetails = content["occupancyDetails"] as List<SummaryListRowViewModel>
            assertEquals(
                listOf(
                    "propertyDetails.propertyRecord.ownership.ownershipType",
                    "forms.checkPropertyAnswers.jointLandlordsDetails.areThereJointLandlords",
                ),
                ownershipAndLandlordsRows.map(SummaryListRowViewModel::fieldHeading),
            )
            assertEquals(
                "forms.checkPropertyAnswers.jointLandlordsDetails.noJointLandlords",
                ownershipAndLandlordsRows.last().fieldValue,
            )
            assertEquals(expectedOccupancyDetails, occupancyDetails)
        }

        @Test
        fun `getStepSpecificContent adds invitations to ownership rows and retains occupancy when there are joint landlords`() {
            val mockInviteJointLandlordsTask = mock<InviteJointLandlordsTask>()
            val mockCheckJointLandlordsStep = mock<CheckJointLandlordsStep>()
            val expectedOccupancyDetails = listOf(mock<SummaryListRowViewModel>())
            whenever(mockHasJointLandlordsFormModel.hasJointLandlords).thenReturn(true)
            whenever(mockJointLandlordsTask.inviteJointLandlordsTask).thenReturn(mockInviteJointLandlordsTask)
            whenever(mockInviteJointLandlordsTask.invitedJointLandlords).thenReturn(listOf("joint.landlord@example.com"))
            whenever(mockInviteJointLandlordsTask.checkJointLandlordsStep).thenReturn(mockCheckJointLandlordsStep)
            whenever(mockOccupancyDetailsHelper.getRestructuredOccupancySummaryList(mockState)).thenReturn(expectedOccupancyDetails)

            val content = stepConfig.getStepSpecificContent(mockState)

            val ownershipAndLandlordsRows = content["ownershipAndLandlordsRows"] as List<SummaryListRowViewModel>
            val occupancyDetails = content["occupancyDetails"] as List<SummaryListRowViewModel>
            assertEquals(
                listOf(
                    "propertyDetails.propertyRecord.ownership.ownershipType",
                    "forms.checkPropertyAnswers.jointLandlordsDetails.jointLandlordInvitations",
                ),
                ownershipAndLandlordsRows.map(SummaryListRowViewModel::fieldHeading),
            )
            assertEquals(listOf("joint.landlord@example.com"), ownershipAndLandlordsRows.last().fieldValue)
            assertEquals(expectedOccupancyDetails, occupancyDetails)
        }
    }

    @Nested
    inner class RestructureAndSkippingDisabled {
        @BeforeEach
        fun disableRestructureAndSkippingFlag() {
            whenever(mockFeatureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(false)
        }

        @Test
        fun `chooseTemplate returns legacy CYA template`() {
            assertEquals(
                "forms/restructureAndSkipping/propertyRegistrationCheckAnswersFormLegacy",
                stepConfig.chooseTemplate(mockState),
            )
        }

        @Test
        fun `getStepSpecificContent puts null for occupancyDetails`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertNull(content["occupancyDetails"])
        }

        @Test
        fun `getStepSpecificContent puts tenancyDetails from getCheckYourAnswersSummaryList`() {
            val expectedTenancyDetails = listOf<SummaryListRowViewModel>()
            whenever(mockOccupancyDetailsHelper.getCheckYourAnswersSummaryList(any(), any())).thenReturn(expectedTenancyDetails)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(expectedTenancyDetails, content["tenancyDetails"])
        }

        @Test
        fun `getStepSpecificContent uses complete registration button and legacy warning text`() {
            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals("forms.buttons.completeRegistration", content["submitButtonText"])
            assertEquals("forms.warning", content["warningTextKey"])
        }

        @Test
        fun `getStepSpecificContent uses Property address heading for legacy property details row`() {
            val content = stepConfig.getStepSpecificContent(mockState)
            val propertyDetailsRows = content["propertyDetails"] as List<SummaryListRowViewModel>

            assertTrue(
                propertyDetailsRows.any { it.fieldHeading == "forms.checkPropertyAnswers.propertyDetails.address" },
            )
        }

        @Test
        fun `getStepSpecificContent uses ownership type heading for legacy property details row`() {
            val content = stepConfig.getStepSpecificContent(mockState)
            val propertyDetailsRows = content["propertyDetails"] as List<SummaryListRowViewModel>

            assertTrue(
                propertyDetailsRows.any { it.fieldHeading == "forms.checkPropertyAnswers.propertyDetails.ownership" },
            )
        }

        @Test
        fun `getStepSpecificContent uses noJointLandlords wording for legacy jointLandlordsDetails row`() {
            whenever(mockHasJointLandlordsFormModel.hasJointLandlords).thenReturn(false)

            val content = stepConfig.getStepSpecificContent(mockState)
            val jointLandlordsDetailsRow = content["jointLandlordsDetails"] as SummaryListRowViewModel

            assertEquals(
                "forms.checkPropertyAnswers.jointLandlordsDetails.noJointLandlords",
                jointLandlordsDetailsRow.fieldValue,
            )
        }
    }

    @Nested
    inner class LettingAgentRestructuredContent {
        @BeforeEach
        fun enableLettingAgentFlags() {
            whenever(mockFeatureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(true)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockState.isDelegatedToLettingAgent(mockFeatureFlagManager)).thenReturn(false)
        }

        @Test
        fun `getStepSpecificContent includes lettingAgentDelegation when landlord provides details`() {
            whenever(mockState.isDelegatedToLettingAgent(mockFeatureFlagManager)).thenReturn(false)
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LANDLORD)

            val content = stepConfig.getStepSpecificContent(mockState)
            val delegationSection = content["lettingAgentDelegation"] as? List<*>

            assertTrue(content.containsKey("lettingAgentDelegation"))
            assertEquals(1, delegationSection?.size, "Landlord path should only include who-will-provide row")
            assertEquals(false, content["lettingAgentDelegationBodyText"], "Body text should not be shown for landlord path")
        }

        @Test
        fun `getStepSpecificContent includes lettingAgentDelegation when landlord provides details and property is unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)
            whenever(mockState.isDelegatedToLettingAgent(mockFeatureFlagManager)).thenReturn(false)
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LANDLORD)

            val content = stepConfig.getStepSpecificContent(mockState)
            val delegationSection = content["lettingAgentDelegation"] as? List<*>

            assertTrue(content.containsKey("lettingAgentDelegation"))
            assertEquals(1, delegationSection?.size, "Landlord path should only include who-will-provide row")
            assertEquals(false, content["lettingAgentDelegationBodyText"], "Body text should not be shown for landlord path")
            assertEquals(
                false,
                content["showLettingAgentDelegationUnoccupiedPanel"],
                "Unoccupied inset should not show once whoProvides has been answered",
            )
        }

        @Test
        fun `getStepSpecificContent includes lettingAgentDelegation when letting agent provides details`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(true)
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LETTING_AGENT)

            val content = stepConfig.getStepSpecificContent(mockState)
            val delegationSection = content["lettingAgentDelegation"] as? List<*>

            assertTrue(content.containsKey("lettingAgentDelegation"))
            assertEquals(2, delegationSection?.size, "Letting agent path should include who-will-provide and email placeholder rows")
            assertEquals(true, content["lettingAgentDelegationBodyText"], "Body text should be shown for letting agent path")
        }

        @Test
        fun `getStepSpecificContent includes lettingAgentDelegation when letting agent provides details and property is unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LETTING_AGENT)

            val content = stepConfig.getStepSpecificContent(mockState)
            val delegationSection = content["lettingAgentDelegation"] as? List<*>

            assertTrue(content.containsKey("lettingAgentDelegation"))
            assertEquals(2, delegationSection?.size, "Letting agent path should include who-will-provide and email placeholder rows")
            assertEquals(true, content["lettingAgentDelegationBodyText"], "Body text should be shown for letting agent path")
            assertEquals(
                false,
                content["showLettingAgentDelegationUnoccupiedPanel"],
                "Unoccupied inset should not show once whoProvides has been answered",
            )
        }

        @Test
        fun `getStepSpecificContent does not include lettingAgentDelegation when whoProvides step is unreachable`() {
            whenever(mockWhoProvidesRentalDetailsStep.formModelIfReachableOrNull).thenReturn(null)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertTrue(!content.containsKey("lettingAgentDelegation") || content["lettingAgentDelegation"] == null)
        }

        @Test
        fun `getStepSpecificContent shows unoccupied inset when property is unoccupied and whoProvides step is unreachable`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)
            whenever(mockWhoProvidesRentalDetailsStep.formModelIfReachableOrNull).thenReturn(null)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(true, content["showLettingAgentDelegationUnoccupiedPanel"])
        }

        @Test
        fun `getStepSpecificContent does not show unoccupied inset when property is occupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(true)
            whenever(mockWhoProvidesRentalDetailsStep.formModelIfReachableOrNull).thenReturn(null)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(false, content["showLettingAgentDelegationUnoccupiedPanel"])
        }

        @Test
        fun `getStepSpecificContent does not show unoccupied inset when whoProvides has already been answered`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LANDLORD)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(false, content["showLettingAgentDelegationUnoccupiedPanel"])
        }
    }

    @Nested
    inner class DelegatedRestructuredContent {
        @BeforeEach
        fun enableDelegationFlags() {
            whenever(mockFeatureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(true)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockState.isDelegatedToLettingAgent(mockFeatureFlagManager)).thenReturn(true)
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
        fun `getStepSpecificContent shows unoccupied tenancy body text when delegated property is unoccupied`() {
            whenever(mockOccupancyFormModel.occupied).thenReturn(false)
            whenever(mockWhoProvidesRentalDetailsFormModel.whoProvides).thenReturn(WhoProvidesRentalDetails.LETTING_AGENT)

            val content = stepConfig.getStepSpecificContent(mockState)

            assertEquals(
                "forms.checkPropertyAnswers.tenancyDetails.unoccupiedBodyText",
                content["tenancyUnoccupiedBodyTextKey"],
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
}

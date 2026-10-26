package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BedroomsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CorrespondenceEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.EpcExemptionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.EpcInDateAtStartOfTenancyCheckStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FurnishedStatusStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasElectricalCertMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasElectricalCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasEpcMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasEpcStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasJointLandlordsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LicensingTypeMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LicensingTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.MeesExemptionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupiedStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentAmountStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentFrequencyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.TenantsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.CorrespondenceTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseholdsAndTenantsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.JointLandlordsPropertyRegistrationTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.LicensingTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipAndLandlordsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyRegistrationAddressTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.RentFrequencyAndAmountTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.RentIncludesBillsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.WhoProvidesDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.CorrespondenceAddressTask
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.EpcDataModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.CorrespondenceEmailFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.EpcExemptionFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.EpcInDateAtStartOfTenancyCheckFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.FurnishedStatusFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.HasJointLandlordsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.MeesExemptionReasonFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NewNumberOfPeopleFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfBedroomsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfHouseholdsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OccupancyFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OwnershipTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.RentAmountFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.RentFrequencyFormModel
import uk.gov.communities.prsdb.webapp.services.EpcCertificateUrlProvider

@ExtendWith(MockitoExtension::class)
class PropertyRegistrationDataModelFactoryTests {
    @Mock
    private lateinit var mockEpcCertificateUrlProvider: EpcCertificateUrlProvider

    @Mock
    private lateinit var mockState: PropertyRegistrationJourneyState

    @Mock
    private lateinit var mockAddressTask: PropertyRegistrationAddressTask

    @Mock
    private lateinit var mockPropertyDetailsTask: PropertyDetailsTask

    @Mock
    private lateinit var mockOwnershipAndLandlordsTask: OwnershipAndLandlordsTask

    private lateinit var factory: PropertyRegistrationDataModelFactory

    @BeforeEach
    fun setUp() {
        factory =
            PropertyRegistrationDataModelFactory(
                epcCertificateUrlProvider = mockEpcCertificateUrlProvider,
            )
    }

    @ParameterizedTest
    @EnumSource(CorrespondenceEmailOption::class)
    fun `fromJourneyState returns the selected correspondence email and postal address`(choice: CorrespondenceEmailOption) {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        val correspondenceTask = mock<CorrespondenceTask>()
        val emailStep = mock<CorrespondenceEmailStep>()
        val addressTask = mock<CorrespondenceAddressTask>()
        val postalAddress = AddressDataModel.fromManualAddressData("12 Test Road", "Leeds", "LS1 1AA", county = "West Yorkshire")
        whenever(mockState.correspondenceTask).thenReturn(correspondenceTask)
        whenever(correspondenceTask.correspondenceEmailStep).thenReturn(emailStep)
        whenever(correspondenceTask.addressTask).thenReturn(addressTask)
        whenever(addressTask.getAddress()).thenReturn(postalAddress)
        whenever(emailStep.formModel).thenReturn(
            CorrespondenceEmailFormModel().apply {
                correspondenceEmailOption = choice
                differentEmailAddress = "edited.contact@example.com"
            },
        )
        if (choice == CorrespondenceEmailOption.ACCOUNT_EMAIL) {
            whenever(mockState.loggedInLandlordEmailAtStartOfJourney).thenReturn("account@example.com")
        }

        // Act
        val result = factory.fromJourneyState(mockState)

        // Assert
        assertEquals(
            if (choice == CorrespondenceEmailOption.ACCOUNT_EMAIL) "account@example.com" else "edited.contact@example.com",
            result.correspondenceEmail,
        )
        assertEquals(postalAddress, result.correspondenceAddressModel)
        if (choice == CorrespondenceEmailOption.DIFFERENT_EMAIL) {
            verify(mockState, never()).loggedInLandlordEmailAtStartOfJourney
        }
    }

    @Test
    fun `fromJourneyState returns all compliance fields from state`() {
        // Arrange
        val gasUploadIds = listOf(10L, 20L)
        val gasCertIssueDate = LocalDate(2024, 6, 15)

        val electricalUploadIds = listOf(30L)
        val electricalSafetyExpiryDate = LocalDate(2029, 3, 20)

        val certificateNumber = "1234-5678-9012-3456-7890"
        val epcUrl = "https://epc.example.com/$certificateNumber"
        val acceptedEpc =
            EpcDataModel(
                certificateNumber = certificateNumber,
                singleLineAddress = "1 Test St",
                energyRating = "B",
                expiryDate = LocalDate(2030, 1, 1),
            )
        val epcExemptionReason = EpcExemptionReason.PROTECTED_ARCHITECTURAL_OR_HISTORICAL_MERIT
        val meesExemptionReason = MeesExemptionReason.HIGH_COST

        setupStateForPropertyRegistration()
        setupStateForComplianceData(
            gasUploadIds = gasUploadIds,
            gasCertIssueDate = gasCertIssueDate,
            electricalUploadIds = electricalUploadIds,
            electricalCertExpiryDate = electricalSafetyExpiryDate,
            electricalCertType = CertificateType.Eicr,
            acceptedEpc = acceptedEpc,
            epcUrl = epcUrl,
            tenancyStartedBeforeEpcExpiry = true,
            epcExemptionReason = epcExemptionReason,
            meesExemptionReason = meesExemptionReason,
        )

        // Act
        val result = factory.fromJourneyState(mockState)

        // Assert
        assertEquals(true, result.hasGasSupply)
        assertEquals(gasCertIssueDate.toJavaLocalDate(), result.gasSafetyCertIssueDate)
        assertEquals(gasUploadIds, result.gasSafetyFileUploadIds)
        assertEquals(false, result.gasSafetyCertProvideLater)
        assertEquals(electricalUploadIds, result.electricalSafetyFileUploadIds)
        assertEquals(electricalSafetyExpiryDate.toJavaLocalDate(), result.electricalSafetyExpiryDate)
        assertEquals(CertificateType.Eicr, result.electricalCertType)
        assertEquals(false, result.electricalSafetyCertProvideLater)
        assertEquals(epcUrl, result.epcCertificateUrl)
        assertEquals(acceptedEpc.expiryDate.toJavaLocalDate(), result.epcExpiryDate)
        assertEquals(acceptedEpc.energyRating, result.epcEnergyRating)
        assertEquals(true, result.tenancyStartedBeforeEpcExpiry)
        assertEquals(epcExemptionReason, result.epcExemptionReason)
        assertEquals(meesExemptionReason, result.epcMeesExemptionReason)
        assertEquals(false, result.epcProvideLater)
        assertEquals(false, result.licenseProvideLater)
    }

    @Test
    fun `fromJourneyState returns licenseProvideLater as true when the user provides licensing later`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockState.licensingTask.licensingTypeStep.outcome).thenReturn(LicensingTypeMode.PROVIDE_LATER)
        whenever(mockState.licensingTask.getLicensingType()).thenReturn(LicensingType.PROVIDE_LATER)

        // Act
        val result = factory.fromJourneyState(mockState)

        // Assert
        assertEquals(true, result.licenseProvideLater)
        assertEquals(false, result.tenancyProvideLater)
    }

    @Test
    fun `fromJourneyState returns hasGasSupply as null and gasSafetyCertProvideLater as true when the user provides gas safety later`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockState.gasSafetyTask.gasSafetyDetailsTask.hasGasSupplyStep.outcome).thenReturn(HasGasSupplyMode.PROVIDE_LATER)

        // Act
        val result = factory.fromJourneyState(mockState)

        // Assert
        assertNull(result.hasGasSupply)
        assertEquals(true, result.gasSafetyCertProvideLater)
        assertEquals(false, result.tenancyProvideLater)
    }

    @Test
    fun `fromJourneyState returns hasGasSupply as false and gasSafetyCertProvideLater as false when the property has no gas supply`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockState.gasSafetyTask.gasSafetyDetailsTask.hasGasSupplyStep.outcome).thenReturn(HasGasSupplyMode.NO_SUPPLY)

        // Act
        val result = factory.fromJourneyState(mockState)

        // Assert
        assertEquals(false, result.hasGasSupply)
        assertEquals(false, result.gasSafetyCertProvideLater)
        assertEquals(false, result.tenancyProvideLater)
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    fun `fromJourneyState returns all provide-later fields as true when delegating to a letting agent`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockState.isDelegatedToLettingAgent()).thenReturn(true)
        whenever(mockState.gasSafetyTask.gasSafetyDetailsTask.hasGasSupplyStep.outcome).thenReturn(null)
        val mockWhoProvidesDetailsTask = mock<WhoProvidesDetailsTask>()
        val mockLettingAgentEmailStep = mock<LettingAgentEmailStep>()
        whenever(mockState.whoProvidesDetailsTask).thenReturn(mockWhoProvidesDetailsTask)
        whenever(mockWhoProvidesDetailsTask.lettingAgentEmailStep).thenReturn(mockLettingAgentEmailStep)
        whenever(mockLettingAgentEmailStep.formModel).thenReturn(
            AllowLettingAgentEmailFormModel().apply { emailAddress = "letting.agent@example.com" },
        )

        // Act
        val result = factory.fromJourneyState(mockState)

        // Assert
        assertEquals("letting.agent@example.com", result.lettingAgentEmail)
        assertNull(result.hasGasSupply)
        assertEquals(true, result.gasSafetyCertProvideLater)
        assertEquals(true, result.electricalSafetyCertProvideLater)
        assertEquals(true, result.epcProvideLater)
        assertEquals(true, result.licenseProvideLater)
        assertEquals(true, result.tenancyProvideLater)
        assertEquals(true, result.isDelegatedToLettingAgent)
    }

    @Test
    fun `fromJourneyState throws when hasGasSupply is null and registration is not delegated to a letting agent`() {
        // Arrange
        whenever(mockState.isDelegatedToLettingAgent()).thenReturn(false)

        val mockOccupiedStep = mock<OccupiedStep>()
        whenever(mockState.occupied).thenReturn(mockOccupiedStep)
        whenever(mockOccupiedStep.formModel).thenReturn(OccupancyFormModel().apply { occupied = false })

        val mockBedroomsStep = mock<BedroomsStep>()
        whenever(mockState.bedrooms).thenReturn(mockBedroomsStep)
        whenever(mockBedroomsStep.formModel).thenReturn(NumberOfBedroomsFormModel().apply { numberOfBedrooms = "3" })

        val mockRentIncludesBillsTask = mock<RentIncludesBillsTask>()
        whenever(mockState.rentIncludesBillsTask).thenReturn(mockRentIncludesBillsTask)
        whenever(mockRentIncludesBillsTask.getBillsIncludedOrNull()).thenReturn(null)

        whenever(mockState.propertyDetailsTask).thenReturn(mockPropertyDetailsTask)
        whenever(mockPropertyDetailsTask.addressTask).thenReturn(mockAddressTask)
        whenever(mockAddressTask.getAddress()).thenReturn(
            AddressDataModel(singleLineAddress = "1 Test St", uprn = 12345L, localCouncilId = 1),
        )
        val mockPropertyTypeStep = mock<PropertyTypeStep>()
        whenever(mockPropertyDetailsTask.propertyTypeStep).thenReturn(mockPropertyTypeStep)
        whenever(mockPropertyTypeStep.formModel).thenReturn(
            PropertyTypeFormModel().apply { propertyType = PropertyType.DETACHED_HOUSE },
        )

        val mockLicensingTask = mock<LicensingTask>()
        whenever(mockState.licensingTask).thenReturn(mockLicensingTask)
        whenever(mockLicensingTask.getLicensingType()).thenReturn(LicensingType.SELECTIVE_LICENCE)
        whenever(mockLicensingTask.getLicenceNumberOrNull()).thenReturn(null)

        val mockOwnershipTypeStep = mock<OwnershipTypeStep>()
        whenever(mockState.ownershipAndLandlordsTask).thenReturn(mockOwnershipAndLandlordsTask)
        whenever(mockOwnershipAndLandlordsTask.ownershipTypeStep).thenReturn(mockOwnershipTypeStep)
        whenever(mockOwnershipTypeStep.formModel).thenReturn(
            OwnershipTypeFormModel().apply { ownershipType = OwnershipType.FREEHOLD },
        )
        val mockJointLandlordsTask = mock<JointLandlordsPropertyRegistrationTask>()
        whenever(mockOwnershipAndLandlordsTask.jointLandlordsTask).thenReturn(mockJointLandlordsTask)
        val mockHasJointLandlordsStep = mock<HasJointLandlordsStep>()
        whenever(mockJointLandlordsTask.hasJointLandlordsStep).thenReturn(mockHasJointLandlordsStep)
        whenever(mockHasJointLandlordsStep.formModel).thenReturn(
            HasJointLandlordsFormModel().apply { hasJointLandlords = false },
        )
        whenever(mockJointLandlordsTask.inviteJointLandlordsTask).thenReturn(mock())

        val mockGasDetailsTask = mock<GasSafetyDetailsTask>()
        val mockHasGasSupplyStep = mock<HasGasSupplyStep>()
        whenever(mockGasDetailsTask.hasGasSupplyStep).thenReturn(mockHasGasSupplyStep)
        whenever(mockHasGasSupplyStep.outcome).thenReturn(null)
        val mockGasTask = mock<GasSafetyTask>()
        whenever(mockGasTask.gasSafetyDetailsTask).thenReturn(mockGasDetailsTask)
        whenever(mockState.gasSafetyTask).thenReturn(mockGasTask)

        // Act & Assert
        assertThrows<IllegalStateException> { factory.fromJourneyState(mockState) }
    }

    @Test
    fun `fromJourneyState returns nulls and empties when all compliance steps return no data`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()

        // Act
        val result = factory.fromJourneyState(mockState)

        // Assert
        assertNull(result.gasSafetyCertIssueDate)
        assertEquals(emptyList<Long>(), result.gasSafetyFileUploadIds)
        assertEquals(emptyList<Long>(), result.electricalSafetyFileUploadIds)
        assertNull(result.electricalSafetyExpiryDate)
        assertNull(result.electricalCertType)
        assertNull(result.epcCertificateUrl)
        assertNull(result.epcExpiryDate)
        assertNull(result.epcEnergyRating)
        assertNull(result.tenancyStartedBeforeEpcExpiry)
        assertNull(result.epcExemptionReason)
        assertNull(result.epcMeesExemptionReason)
        assertEquals(false, result.tenancyProvideLater)
    }

    @Test
    fun `fromJourneyState returns false tenancyProvideLater when property is occupied and tenancy details are provided`() {
        // Arrange
        setupStateForPropertyRegistration()
        whenever(mockState.occupied.formModel).thenReturn(OccupancyFormModel().apply { occupied = true })
        setupStateForOccupiedTenancyDetails()
        setupStateForComplianceDataWithNullValues()

        // Act
        val result = factory.fromJourneyState(mockState)

        // Assert
        assertEquals(true, result.isOccupied)
        assertNull(result.gasSafetyCertIssueDate)
        assertEquals(emptyList<Long>(), result.gasSafetyFileUploadIds)
        assertEquals(emptyList<Long>(), result.electricalSafetyFileUploadIds)
        assertNull(result.electricalSafetyExpiryDate)
        assertNull(result.electricalCertType)
        assertNull(result.epcCertificateUrl)
        assertNull(result.epcExpiryDate)
        assertNull(result.epcEnergyRating)
        assertNull(result.tenancyStartedBeforeEpcExpiry)
        assertNull(result.epcExemptionReason)
        assertNull(result.epcMeesExemptionReason)
        assertEquals(false, result.tenancyProvideLater)
    }

    @Test
    fun `fromJourneyState returns null tenancy fields and true tenancyProvideLater when tenancy is provide this later`() {
        // Arrange
        setupStateForPropertyRegistration()
        whenever(mockState.occupied.formModel).thenReturn(OccupancyFormModel().apply { occupied = true })
        whenever(mockState.provideTenancyDetailsLater).thenReturn(true)
        setupStateForComplianceDataWithNullValues()

        // Act
        val result = factory.fromJourneyState(mockState)

        // Assert
        assertEquals(0, result.numberOfHouseholds)
        assertEquals(0, result.numberOfPeople)
        assertEquals(3, result.numBedrooms)
        assertNull(result.billsIncludedList)
        assertNull(result.customBillsIncluded)
        assertNull(result.furnishedStatus)
        assertNull(result.rentFrequency)
        assertNull(result.customRentFrequency)
        assertNull(result.rentAmount)
        assertNull(result.gasSafetyCertIssueDate)
        assertEquals(emptyList<Long>(), result.gasSafetyFileUploadIds)
        assertEquals(emptyList<Long>(), result.electricalSafetyFileUploadIds)
        assertNull(result.electricalSafetyExpiryDate)
        assertNull(result.electricalCertType)
        assertNull(result.epcCertificateUrl)
        assertNull(result.epcExpiryDate)
        assertNull(result.epcEnergyRating)
        assertNull(result.tenancyStartedBeforeEpcExpiry)
        assertNull(result.epcExemptionReason)
        assertNull(result.epcMeesExemptionReason)
        assertEquals(true, result.tenancyProvideLater)
    }

    private fun setupStateForPropertyRegistration() {
        val mockOccupiedStep = mock<OccupiedStep>()
        val occupancyFormModel = OccupancyFormModel().apply { occupied = false }
        whenever(mockState.occupied).thenReturn(mockOccupiedStep)
        whenever(mockOccupiedStep.formModel).thenReturn(occupancyFormModel)

        val mockBedroomsStep = mock<BedroomsStep>()
        whenever(mockState.bedrooms).thenReturn(mockBedroomsStep)
        whenever(mockBedroomsStep.formModel).thenReturn(NumberOfBedroomsFormModel().apply { numberOfBedrooms = "3" })

        val mockRentIncludesBillsTask = mock<RentIncludesBillsTask>()
        whenever(mockState.rentIncludesBillsTask).thenReturn(mockRentIncludesBillsTask)
        whenever(mockRentIncludesBillsTask.getBillsIncludedOrNull()).thenReturn(null)

        whenever(mockState.propertyDetailsTask).thenReturn(mockPropertyDetailsTask)
        whenever(mockPropertyDetailsTask.addressTask).thenReturn(mockAddressTask)
        whenever(mockAddressTask.getAddress()).thenReturn(
            AddressDataModel(singleLineAddress = "1 Test St", uprn = 12345L, localCouncilId = 1),
        )

        val mockPropertyTypeStep = mock<PropertyTypeStep>()
        val propertyTypeFormModel = PropertyTypeFormModel().apply { propertyType = PropertyType.DETACHED_HOUSE }
        whenever(mockPropertyDetailsTask.propertyTypeStep).thenReturn(mockPropertyTypeStep)
        whenever(mockPropertyTypeStep.formModel).thenReturn(propertyTypeFormModel)

        val mockLicensingTypeStep = mock<LicensingTypeStep>()
        val mockLicensingTask = mock<LicensingTask>()
        whenever(mockState.licensingTask).thenReturn(mockLicensingTask)
        whenever(mockLicensingTask.licensingTypeStep).thenReturn(mockLicensingTypeStep)
        whenever(mockLicensingTypeStep.outcome).thenReturn(LicensingTypeMode.SELECTIVE_LICENCE)

        whenever(mockLicensingTask.getLicenceNumberOrNull()).thenReturn(null)
        whenever(mockLicensingTask.getLicensingType()).thenReturn(LicensingType.SELECTIVE_LICENCE)

        val mockOwnershipTypeStep = mock<OwnershipTypeStep>()
        val ownershipTypeFormModel = OwnershipTypeFormModel().apply { ownershipType = OwnershipType.FREEHOLD }
        whenever(mockState.ownershipAndLandlordsTask).thenReturn(mockOwnershipAndLandlordsTask)
        whenever(mockOwnershipAndLandlordsTask.ownershipTypeStep).thenReturn(mockOwnershipTypeStep)
        whenever(mockOwnershipTypeStep.formModel).thenReturn(ownershipTypeFormModel)

        val mockJointLandlordsTask = mock<JointLandlordsPropertyRegistrationTask>()
        whenever(mockOwnershipAndLandlordsTask.jointLandlordsTask).thenReturn(mockJointLandlordsTask)

        val mockHasJointLandlordsStep = mock<HasJointLandlordsStep>()
        val hasJointLandlordsFormModel = HasJointLandlordsFormModel().apply { hasJointLandlords = false }
        whenever(mockJointLandlordsTask.hasJointLandlordsStep).thenReturn(mockHasJointLandlordsStep)
        whenever(mockHasJointLandlordsStep.formModel).thenReturn(hasJointLandlordsFormModel)
        whenever(mockJointLandlordsTask.inviteJointLandlordsTask).thenReturn(mock())

        val mockCorrespondenceTask = mock<CorrespondenceTask>()
        val mockCorrespondenceEmailStep = mock<CorrespondenceEmailStep>()
        val mockCorrespondenceAddressTask = mock<CorrespondenceAddressTask>()
        lenient().`when`(mockState.correspondenceTask).thenReturn(mockCorrespondenceTask)
        lenient().`when`(mockCorrespondenceTask.correspondenceEmailStep).thenReturn(mockCorrespondenceEmailStep)
        lenient().`when`(mockCorrespondenceTask.addressTask).thenReturn(mockCorrespondenceAddressTask)
        lenient().`when`(mockCorrespondenceEmailStep.formModel).thenReturn(
            CorrespondenceEmailFormModel().apply {
                correspondenceEmailOption = CorrespondenceEmailOption.DIFFERENT_EMAIL
                differentEmailAddress = "contact@example.com"
            },
        )
        lenient().`when`(mockCorrespondenceAddressTask.getAddress()).thenReturn(
            AddressDataModel(singleLineAddress = "2 Contact St", uprn = 54321L, localCouncilId = 1),
        )
    }

    private fun setupStateForComplianceData(
        gasUploadIds: List<Long> = emptyList(),
        gasCertIssueDate: LocalDate? = null,
        electricalUploadIds: List<Long> = emptyList(),
        electricalCertExpiryDate: LocalDate? = null,
        electricalCertType: CertificateType? = null,
        acceptedEpc: EpcDataModel? = null,
        epcUrl: String? = null,
        tenancyStartedBeforeEpcExpiry: Boolean? = null,
        epcExemptionReason: EpcExemptionReason = EpcExemptionReason.PROTECTED_ARCHITECTURAL_OR_HISTORICAL_MERIT,
        meesExemptionReason: MeesExemptionReason = MeesExemptionReason.HIGH_COST,
    ) {
        val gasSafetyTask: GasSafetyDetailsTask = mock()
        val electricalSafetyDetailsTask: ElectricalSafetyDetailsTask = mock()

        whenever(gasSafetyTask.gasUploadIds).thenReturn(gasUploadIds)
        whenever(electricalSafetyDetailsTask.electricalUploadIds).thenReturn(electricalUploadIds)
        whenever(electricalSafetyDetailsTask.mapElectricalCertificateTypeToGlobalCertificateType()).thenReturn(electricalCertType)

        val mockHasGasSupplyStep = mock<HasGasSupplyStep>()
        whenever(gasSafetyTask.hasGasSupplyStep).thenReturn(mockHasGasSupplyStep)
        whenever(mockHasGasSupplyStep.outcome).thenReturn(HasGasSupplyMode.HAS_SUPPLY)

        val mockHasElectricalCertStep = mock<HasElectricalCertStep>()
        whenever(electricalSafetyDetailsTask.hasElectricalCertStep).thenReturn(mockHasElectricalCertStep)
        whenever(mockHasElectricalCertStep.outcome).thenReturn(HasElectricalCertMode.HAS_EIC)

        val mockHasEpcStep = mock<HasEpcStep>()
        val mockEpcTask: EpcTask = mock()
        val mockEpcDetailsTask: EpcDetailsTask = mock()
        whenever(mockEpcTask.epcDetailsTask).thenReturn(mockEpcDetailsTask)
        whenever(mockEpcDetailsTask.hasEpcStep).thenReturn(mockHasEpcStep)
        whenever(mockHasEpcStep.outcome).thenReturn(HasEpcMode.HAS_EPC)

        whenever(gasSafetyTask.getGasSafetyCertificateIssueDateIfReachable()).thenReturn(gasCertIssueDate)
        whenever(electricalSafetyDetailsTask.getElectricalCertificateExpiryDateIfReachable()).thenReturn(electricalCertExpiryDate)

        if (acceptedEpc != null) {
            whenever(mockEpcCertificateUrlProvider.getEpcCertificateUrl(acceptedEpc.certificateNumber)).thenReturn(epcUrl)
        }

        whenever(mockEpcDetailsTask.acceptedEpcIfStillAccepted).thenReturn(acceptedEpc)

        val mockTenancyStep = mock<EpcInDateAtStartOfTenancyCheckStep>()
        val mockEpcExemptionStep = mock<EpcExemptionStep>()
        val mockMeesExemptionStep = mock<MeesExemptionStep>()
        whenever(mockEpcDetailsTask.epcInDateAtStartOfTenancyCheckStep).thenReturn(mockTenancyStep)
        whenever(mockTenancyStep.formModelIfReachableOrNull).thenReturn(
            EpcInDateAtStartOfTenancyCheckFormModel().apply {
                tenancyStartedBeforeExpiry = tenancyStartedBeforeEpcExpiry
            },
        )
        whenever(mockEpcDetailsTask.epcExemptionStep).thenReturn(mockEpcExemptionStep)
        whenever(mockEpcExemptionStep.formModelIfReachableOrNull).thenReturn(
            EpcExemptionFormModel().apply {
                exemptionReason = epcExemptionReason
            },
        )
        whenever(mockEpcDetailsTask.meesExemptionStep).thenReturn(mockMeesExemptionStep)
        whenever(mockMeesExemptionStep.formModelIfReachableOrNull).thenReturn(
            MeesExemptionReasonFormModel().apply {
                exemptionReason = meesExemptionReason
            },
        )

        val mockGasTask: GasSafetyTask = mock()
        whenever(mockGasTask.gasSafetyDetailsTask).thenReturn(gasSafetyTask)
        whenever(mockState.gasSafetyTask).thenReturn(mockGasTask)
        val mockElectricalSafetyTask: ElectricalSafetyTask = mock()
        whenever(mockElectricalSafetyTask.electricalSafetyDetailsTask).thenReturn(electricalSafetyDetailsTask)
        whenever(mockState.electricalSafetyTask).thenReturn(mockElectricalSafetyTask)
        whenever(mockState.epcTask).thenReturn(mockEpcTask)
    }

    private fun setupStateForComplianceDataWithNullValues() {
        val gasSafetyDetailsTask: GasSafetyDetailsTask = mock()
        val electricalSafetyDetailsTask: ElectricalSafetyDetailsTask = mock()
        val mockEpcTask: EpcTask = mock()
        val mockEpcDetailsTask: EpcDetailsTask = mock()
        whenever(mockEpcTask.epcDetailsTask).thenReturn(mockEpcDetailsTask)

        whenever(gasSafetyDetailsTask.gasUploadIds).thenReturn(emptyList())
        whenever(electricalSafetyDetailsTask.electricalUploadIds).thenReturn(emptyList())
        whenever(electricalSafetyDetailsTask.mapElectricalCertificateTypeToGlobalCertificateType()).thenReturn(null)

        val mockHasGasSupplyStep = mock<HasGasSupplyStep>()
        val mockHasGasCertStep = mock<HasGasCertStep>()
        lenient().`when`(gasSafetyDetailsTask.hasGasSupplyStep).thenReturn(mockHasGasSupplyStep)
        lenient().`when`(mockHasGasSupplyStep.outcome).thenReturn(HasGasSupplyMode.HAS_SUPPLY)
        lenient().`when`(gasSafetyDetailsTask.hasGasCertStep).thenReturn(mockHasGasCertStep)
        lenient().`when`(mockHasGasCertStep.outcome).thenReturn(null)

        val mockHasElectricalCertStep = mock<HasElectricalCertStep>()
        whenever(electricalSafetyDetailsTask.hasElectricalCertStep).thenReturn(mockHasElectricalCertStep)
        whenever(mockHasElectricalCertStep.outcome).thenReturn(null)

        val mockHasEpcStep = mock<HasEpcStep>()
        whenever(mockEpcDetailsTask.hasEpcStep).thenReturn(mockHasEpcStep)
        whenever(mockHasEpcStep.outcome).thenReturn(null)

        whenever(gasSafetyDetailsTask.getGasSafetyCertificateIssueDateIfReachable()).thenReturn(null)
        whenever(electricalSafetyDetailsTask.getElectricalCertificateExpiryDateIfReachable()).thenReturn(null)
        whenever(mockEpcDetailsTask.acceptedEpcIfStillAccepted).thenReturn(null)

        val mockGasTask: GasSafetyTask = mock()
        whenever(mockGasTask.gasSafetyDetailsTask).thenReturn(gasSafetyDetailsTask)
        whenever(mockState.gasSafetyTask).thenReturn(mockGasTask)
        val mockElectricalSafetyTask: ElectricalSafetyTask = mock()
        whenever(mockElectricalSafetyTask.electricalSafetyDetailsTask).thenReturn(electricalSafetyDetailsTask)
        whenever(mockState.electricalSafetyTask).thenReturn(mockElectricalSafetyTask)
        whenever(mockState.epcTask).thenReturn(mockEpcTask)

        val mockTenancyStep = mock<EpcInDateAtStartOfTenancyCheckStep>()
        val mockEpcExemptionStep = mock<EpcExemptionStep>()
        val mockMeesExemptionStep = mock<MeesExemptionStep>()
        whenever(mockEpcDetailsTask.epcInDateAtStartOfTenancyCheckStep).thenReturn(mockTenancyStep)
        whenever(mockTenancyStep.formModelIfReachableOrNull).thenReturn(null)
        whenever(mockEpcDetailsTask.epcExemptionStep).thenReturn(mockEpcExemptionStep)
        whenever(mockEpcExemptionStep.formModelIfReachableOrNull).thenReturn(null)
        whenever(mockEpcDetailsTask.meesExemptionStep).thenReturn(mockMeesExemptionStep)
        whenever(mockMeesExemptionStep.formModelIfReachableOrNull).thenReturn(null)
    }

    private fun setupStateForOccupiedTenancyDetails() {
        val mockHouseholdsAndTenantsTask = mock<HouseholdsAndTenantsTask>()
        val mockHouseholdStep = mock<HouseholdStep>()
        val mockTenantsStep = mock<TenantsStep>()
        whenever(mockState.householdsAndTenantsTask).thenReturn(mockHouseholdsAndTenantsTask)
        whenever(mockHouseholdsAndTenantsTask.households).thenReturn(mockHouseholdStep)
        whenever(mockHouseholdsAndTenantsTask.tenants).thenReturn(mockTenantsStep)
        whenever(mockHouseholdStep.formModel).thenReturn(NumberOfHouseholdsFormModel().apply { numberOfHouseholds = "1" })
        whenever(mockTenantsStep.formModel).thenReturn(NewNumberOfPeopleFormModel().apply { numberOfPeople = "2" })

        val mockFurnishedStatusStep = mock<FurnishedStatusStep>()
        whenever(mockState.furnishedStatus).thenReturn(mockFurnishedStatusStep)
        whenever(mockFurnishedStatusStep.formModel).thenReturn(FurnishedStatusFormModel())

        val mockRentFrequencyAndAmountTask = mock<RentFrequencyAndAmountTask>()
        val mockRentFrequencyStep = mock<RentFrequencyStep>()
        val mockRentAmountStep = mock<RentAmountStep>()
        whenever(mockState.rentFrequencyAndAmountTask).thenReturn(mockRentFrequencyAndAmountTask)
        whenever(mockRentFrequencyAndAmountTask.rentFrequency).thenReturn(mockRentFrequencyStep)
        whenever(mockRentFrequencyAndAmountTask.rentAmount).thenReturn(mockRentAmountStep)
        whenever(mockRentFrequencyStep.formModel).thenReturn(RentFrequencyFormModel())
        whenever(mockRentAmountStep.formModel).thenReturn(RentAmountFormModel().apply { rentAmount = "100" })
    }
}

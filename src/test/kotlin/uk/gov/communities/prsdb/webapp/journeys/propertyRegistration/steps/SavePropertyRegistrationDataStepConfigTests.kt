package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import jakarta.persistence.EntityExistsException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasCertOutcome
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSupplyOutcome
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
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
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
import uk.gov.communities.prsdb.webapp.services.PropertyRegistrationService
import kotlin.test.assertNotEquals

@ExtendWith(MockitoExtension::class)
class SavePropertyRegistrationDataStepConfigTests {
    @Mock
    private lateinit var mockPropertyRegistrationService: PropertyRegistrationService

    @Mock
    private lateinit var mockEpcCertificateUrlProvider: EpcCertificateUrlProvider

    @Mock
    private lateinit var mockFeatureFlagManager: FeatureFlagManager

    @Mock
    private lateinit var mockState: PropertyRegistrationJourneyState

    @Mock
    private lateinit var mockAddressTask: PropertyRegistrationAddressTask

    @Mock
    private lateinit var mockPropertyDetailsTask: PropertyDetailsTask

    @Mock
    private lateinit var mockOwnershipAndLandlordsTask: OwnershipAndLandlordsTask

    private lateinit var stepConfig: SavePropertyRegistrationDataStepConfig

    @BeforeEach
    fun setUp() {
        stepConfig =
            SavePropertyRegistrationDataStepConfig(
                propertyRegistrationService = mockPropertyRegistrationService,
                epcCertificateUrlProvider = mockEpcCertificateUrlProvider,
                featureFlagManager = mockFeatureFlagManager,
            )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `mode returns COMPLETE`() {
        // Act
        val result = stepConfig.mode(mockState)

        // Assert
        assertEquals(Complete.COMPLETE, result)
    }

    @ParameterizedTest
    @EnumSource(CorrespondenceEmailOption::class)
    fun `registration passes the selected correspondence email and postal address`(choice: CorrespondenceEmailOption) {
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockFeatureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(true)
        whenever(mockFeatureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS)).thenReturn(true)
        val bedrooms = mock<BedroomsStep>()
        whenever(mockState.bedrooms).thenReturn(bedrooms)
        whenever(bedrooms.formModel).thenReturn(NumberOfBedroomsFormModel().apply { numberOfBedrooms = "4" })
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
                whichEmail = choice
                differentEmailAddress = "edited.contact@example.com"
            },
        )
        if (choice == CorrespondenceEmailOption.ACCOUNT_EMAIL) {
            whenever(mockState.loggedInLandlordEmail).thenReturn("account@example.com")
        }

        stepConfig.afterStepIsReached(mockState)

        verifyCorrespondenceDetails(
            if (choice == CorrespondenceEmailOption.ACCOUNT_EMAIL) "account@example.com" else "edited.contact@example.com",
            postalAddress,
        )
        if (choice == CorrespondenceEmailOption.DIFFERENT_EMAIL) {
            verify(mockState, never()).loggedInLandlordEmail
        }
    }

    @ParameterizedTest
    @CsvSource("false,false", "false,true", "true,false")
    fun `registration leaves correspondence defaults to the service when its journey is disabled`(
        restructured: Boolean,
        correspondenceEnabled: Boolean,
    ) {
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockFeatureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(restructured)
        lenient().`when`(mockFeatureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS)).thenReturn(correspondenceEnabled)
        if (restructured) {
            val bedrooms = mock<BedroomsStep>()
            whenever(mockState.bedrooms).thenReturn(bedrooms)
            whenever(bedrooms.formModel).thenReturn(NumberOfBedroomsFormModel().apply { numberOfBedrooms = "4" })
        }

        stepConfig.afterStepIsReached(mockState)

        verifyCorrespondenceDetails(null, null)
        verify(mockState, never()).correspondenceTask
    }

    private fun verifyCorrespondenceDetails(
        email: String?,
        address: AddressDataModel?,
    ) {
        verify(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = any(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = anyOrNull(),
            markedJointLandlord = any(),
            hasGasSupply = anyOrNull(),
            gasSafetyCertIssueDate = anyOrNull(),
            gasSafetyFileUploadIds = any(),
            gasSafetyCertProvideLater = anyOrNull(),
            electricalSafetyFileUploadIds = any(),
            electricalSafetyExpiryDate = anyOrNull(),
            electricalCertType = anyOrNull(),
            electricalSafetyCertProvideLater = anyOrNull(),
            epcCertificateUrl = anyOrNull(),
            epcExpiryDate = anyOrNull(),
            epcEnergyRating = anyOrNull(),
            tenancyStartedBeforeEpcExpiry = anyOrNull(),
            epcExemptionReason = anyOrNull(),
            epcMeesExemptionReason = anyOrNull(),
            epcProvideLater = anyOrNull(),
            licenseProvideLater = any(),
            tenancyProvideLater = anyOrNull(),
            isDelegatedToLettingAgent = any(),
            correspondenceEmail = eq(email),
            correspondenceAddressModel = eq(address),
        )
    }

    @Test
    fun `afterStepIsReached registers property and saves compliance data with all compliance fields from state`() {
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
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = any(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = anyOrNull(),
            markedJointLandlord = any(),
            hasGasSupply = eq(true),
            gasSafetyCertIssueDate = eq(gasCertIssueDate.toJavaLocalDate()),
            gasSafetyFileUploadIds = eq(gasUploadIds),
            gasSafetyCertProvideLater = eq(false),
            electricalSafetyFileUploadIds = eq(electricalUploadIds),
            electricalSafetyExpiryDate = eq(electricalSafetyExpiryDate.toJavaLocalDate()),
            electricalCertType = eq(CertificateType.Eicr),
            electricalSafetyCertProvideLater = eq(false),
            epcCertificateUrl = eq(epcUrl),
            epcExpiryDate = eq(acceptedEpc.expiryDate.toJavaLocalDate()),
            epcEnergyRating = eq(acceptedEpc.energyRating),
            tenancyStartedBeforeEpcExpiry = eq(true),
            epcExemptionReason = eq(epcExemptionReason),
            epcMeesExemptionReason = eq(meesExemptionReason),
            epcProvideLater = eq(false),
            licenseProvideLater = eq(false),
            tenancyProvideLater = any(),
            isDelegatedToLettingAgent = any(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
        )
    }

    @Test
    fun `afterStepIsReached passes licenseProvideLater as true when the user provides licensing later`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockState.licensingTask.licensingTypeStep.outcome).thenReturn(LicensingTypeMode.PROVIDE_LATER)
        whenever(mockState.licensingTask.getLicensingType()).thenReturn(LicensingType.PROVIDE_LATER)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = anyOrNull(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = anyOrNull(),
            markedJointLandlord = any(),
            hasGasSupply = anyOrNull(),
            gasSafetyCertIssueDate = anyOrNull(),
            gasSafetyFileUploadIds = any(),
            gasSafetyCertProvideLater = anyOrNull(),
            electricalSafetyFileUploadIds = any(),
            electricalSafetyExpiryDate = anyOrNull(),
            electricalCertType = anyOrNull(),
            electricalSafetyCertProvideLater = anyOrNull(),
            epcCertificateUrl = anyOrNull(),
            epcExpiryDate = anyOrNull(),
            epcEnergyRating = anyOrNull(),
            tenancyStartedBeforeEpcExpiry = anyOrNull(),
            epcExemptionReason = anyOrNull(),
            epcMeesExemptionReason = anyOrNull(),
            epcProvideLater = anyOrNull(),
            licenseProvideLater = eq(true),
            tenancyProvideLater = eq(false),
            isDelegatedToLettingAgent = any(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
        )
    }

    @Test
    fun `afterStepIsReached passes hasGasSupply and gasSafetyCertProvideLater as true when the user provides gas safety later`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockState.gasSafetyTask.gasSafetyDetailsTask.gasSupplyOutcome).thenReturn(GasSupplyOutcome.PROVIDE_LATER)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = anyOrNull(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = anyOrNull(),
            markedJointLandlord = any(),
            hasGasSupply = eq(true),
            gasSafetyCertIssueDate = anyOrNull(),
            gasSafetyFileUploadIds = any(),
            gasSafetyCertProvideLater = eq(true),
            electricalSafetyFileUploadIds = any(),
            electricalSafetyExpiryDate = anyOrNull(),
            electricalCertType = anyOrNull(),
            electricalSafetyCertProvideLater = anyOrNull(),
            epcCertificateUrl = anyOrNull(),
            epcExpiryDate = anyOrNull(),
            epcEnergyRating = anyOrNull(),
            tenancyStartedBeforeEpcExpiry = anyOrNull(),
            epcExemptionReason = anyOrNull(),
            epcMeesExemptionReason = anyOrNull(),
            epcProvideLater = anyOrNull(),
            licenseProvideLater = anyOrNull(),
            tenancyProvideLater = eq(false),
            isDelegatedToLettingAgent = any(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
        )
    }

    @Test
    fun `afterStepIsReached passes true when user chose provide later on legacy gas cert step`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockState.gasSafetyTask.gasSafetyDetailsTask.gasCertOutcome).thenReturn(GasCertOutcome.PROVIDE_LATER)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = anyOrNull(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = anyOrNull(),
            markedJointLandlord = any(),
            hasGasSupply = eq(true),
            gasSafetyCertIssueDate = anyOrNull(),
            gasSafetyFileUploadIds = any(),
            gasSafetyCertProvideLater = eq(true),
            electricalSafetyFileUploadIds = any(),
            electricalSafetyExpiryDate = anyOrNull(),
            electricalCertType = anyOrNull(),
            electricalSafetyCertProvideLater = anyOrNull(),
            epcCertificateUrl = anyOrNull(),
            epcExpiryDate = anyOrNull(),
            epcEnergyRating = anyOrNull(),
            tenancyStartedBeforeEpcExpiry = anyOrNull(),
            epcExemptionReason = anyOrNull(),
            epcMeesExemptionReason = anyOrNull(),
            epcProvideLater = anyOrNull(),
            licenseProvideLater = anyOrNull(),
            tenancyProvideLater = eq(false),
            isDelegatedToLettingAgent = any(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
        )
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    fun `afterStepIsReached passes all provide-later fields as true when delegating to a letting agent`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()
        whenever(mockState.isDelegatedToLettingAgent(any())).thenReturn(true)
        val mockWhoProvidesDetailsTask = mock<WhoProvidesDetailsTask>()
        val mockLettingAgentEmailStep = mock<LettingAgentEmailStep>()
        whenever(mockState.whoProvidesDetailsTask).thenReturn(mockWhoProvidesDetailsTask)
        whenever(mockWhoProvidesDetailsTask.lettingAgentEmailStep).thenReturn(mockLettingAgentEmailStep)
        whenever(mockLettingAgentEmailStep.formModel).thenReturn(
            AllowLettingAgentEmailFormModel().apply { emailAddress = "letting.agent@example.com" },
        )

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = anyOrNull(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = eq("letting.agent@example.com"),
            markedJointLandlord = any(),
            hasGasSupply = eq(true),
            gasSafetyCertIssueDate = anyOrNull(),
            gasSafetyFileUploadIds = any(),
            gasSafetyCertProvideLater = eq(true),
            electricalSafetyFileUploadIds = any(),
            electricalSafetyExpiryDate = anyOrNull(),
            electricalCertType = anyOrNull(),
            electricalSafetyCertProvideLater = eq(true),
            epcCertificateUrl = anyOrNull(),
            epcExpiryDate = anyOrNull(),
            epcEnergyRating = anyOrNull(),
            tenancyStartedBeforeEpcExpiry = anyOrNull(),
            epcExemptionReason = anyOrNull(),
            epcMeesExemptionReason = anyOrNull(),
            epcProvideLater = eq(true),
            licenseProvideLater = eq(true),
            tenancyProvideLater = eq(true),
            isDelegatedToLettingAgent = eq(true),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
        )
    }

    @Test
    fun `afterStepIsReached sets isAddressAlreadyRegistered when EntityExistsException`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceData()
        whenever(mockState.gasSafetyTask.gasSafetyDetailsTask.gasUploadIds).thenReturn(emptyList())
        whenever(mockState.electricalSafetyTask.electricalSafetyDetailsTask.electricalUploadIds).thenReturn(emptyList())
        whenever(
            mockState.electricalSafetyTask.electricalSafetyDetailsTask.mapElectricalCertificateTypeToGlobalCertificateType(),
        ).thenReturn(null)
        doThrow(EntityExistsException("Address already registered")).whenever(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = any(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = anyOrNull(),
            markedJointLandlord = any(),
            hasGasSupply = anyOrNull(),
            gasSafetyCertIssueDate = anyOrNull(),
            gasSafetyFileUploadIds = any(),
            gasSafetyCertProvideLater = anyOrNull(),
            electricalSafetyFileUploadIds = any(),
            electricalSafetyExpiryDate = anyOrNull(),
            electricalCertType = anyOrNull(),
            electricalSafetyCertProvideLater = anyOrNull(),
            epcCertificateUrl = anyOrNull(),
            epcExpiryDate = anyOrNull(),
            epcEnergyRating = anyOrNull(),
            tenancyStartedBeforeEpcExpiry = anyOrNull(),
            epcExemptionReason = anyOrNull(),
            epcMeesExemptionReason = anyOrNull(),
            epcProvideLater = anyOrNull(),
            licenseProvideLater = anyOrNull(),
            tenancyProvideLater = any(),
            isDelegatedToLettingAgent = any(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
        )

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockAddressTask).isAddressAlreadyRegistered = true
    }

    @Test
    fun `afterStepIsReached passes nulls and empties when all compliance steps return no data`() {
        // Arrange
        setupStateForPropertyRegistration()
        setupStateForComplianceDataWithNullValues()

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = any(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = anyOrNull(),
            markedJointLandlord = any(),
            hasGasSupply = anyOrNull(),
            gasSafetyCertIssueDate = isNull(),
            gasSafetyFileUploadIds = eq(emptyList()),
            gasSafetyCertProvideLater = anyOrNull(),
            electricalSafetyFileUploadIds = eq(emptyList()),
            electricalSafetyExpiryDate = isNull(),
            electricalCertType = isNull(),
            electricalSafetyCertProvideLater = anyOrNull(),
            epcCertificateUrl = isNull(),
            epcExpiryDate = isNull(),
            epcEnergyRating = isNull(),
            tenancyStartedBeforeEpcExpiry = isNull(),
            epcExemptionReason = isNull(),
            epcMeesExemptionReason = isNull(),
            epcProvideLater = anyOrNull(),
            licenseProvideLater = anyOrNull(),
            tenancyProvideLater = eq(false),
            isDelegatedToLettingAgent = any(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
        )
    }

    @Test
    fun `afterStepIsReached passes false tenancyProvideLater when property is occupied and tenancy details are provided`() {
        // Arrange
        setupStateForPropertyRegistration()
        whenever(mockState.occupied.formModel).thenReturn(OccupancyFormModel().apply { occupied = true })
        setupStateForOccupiedTenancyDetails()
        setupStateForComplianceDataWithNullValues()

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = any(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = eq(true),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = anyOrNull(),
            markedJointLandlord = any(),
            hasGasSupply = anyOrNull(),
            gasSafetyCertIssueDate = isNull(),
            gasSafetyFileUploadIds = eq(emptyList()),
            gasSafetyCertProvideLater = anyOrNull(),
            electricalSafetyFileUploadIds = eq(emptyList()),
            electricalSafetyExpiryDate = isNull(),
            electricalCertType = isNull(),
            electricalSafetyCertProvideLater = anyOrNull(),
            epcCertificateUrl = isNull(),
            epcExpiryDate = isNull(),
            epcEnergyRating = isNull(),
            tenancyStartedBeforeEpcExpiry = isNull(),
            epcExemptionReason = isNull(),
            epcMeesExemptionReason = isNull(),
            epcProvideLater = anyOrNull(),
            licenseProvideLater = anyOrNull(),
            tenancyProvideLater = eq(false),
            isDelegatedToLettingAgent = any(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
        )
    }

    @Test
    fun `afterStepIsReached passes null tenancy fields and true tenancyProvideLater when tenancy is provide this later`() {
        // Arrange
        setupStateForPropertyRegistration()
        whenever(mockState.occupied.formModel).thenReturn(OccupancyFormModel().apply { occupied = true })
        whenever(mockState.provideTenancyDetailsLater).thenReturn(true)
        setupStateForComplianceDataWithNullValues()

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(
            addressModel = any(),
            propertyType = any(),
            licenseType = any(),
            licenceNumber = any(),
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = eq(0),
            numberOfPeople = eq(0),
            numBedrooms = isNull(),
            billsIncludedList = isNull(),
            customBillsIncluded = isNull(),
            furnishedStatus = isNull(),
            rentFrequency = isNull(),
            customRentFrequency = isNull(),
            rentAmount = isNull(),
            customPropertyType = anyOrNull(),
            jointLandlordEmails = anyOrNull(),
            lettingAgentEmail = anyOrNull(),
            markedJointLandlord = any(),
            hasGasSupply = anyOrNull(),
            gasSafetyCertIssueDate = isNull(),
            gasSafetyFileUploadIds = eq(emptyList()),
            gasSafetyCertProvideLater = anyOrNull(),
            electricalSafetyFileUploadIds = eq(emptyList()),
            electricalSafetyExpiryDate = isNull(),
            electricalCertType = isNull(),
            electricalSafetyCertProvideLater = anyOrNull(),
            epcCertificateUrl = isNull(),
            epcExpiryDate = isNull(),
            epcEnergyRating = isNull(),
            tenancyStartedBeforeEpcExpiry = isNull(),
            epcExemptionReason = isNull(),
            epcMeesExemptionReason = isNull(),
            epcProvideLater = anyOrNull(),
            licenseProvideLater = anyOrNull(),
            tenancyProvideLater = eq(true),
            isDelegatedToLettingAgent = any(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
        )
    }

    @Test
    fun `resolveNextDestination deletes journey and returns default destination when address is not already registered`() {
        // Arrange
        val defaultDestination = Destination.ExternalUrl("redirect")
        whenever(mockState.propertyDetailsTask).thenReturn(mockPropertyDetailsTask)
        whenever(mockPropertyDetailsTask.addressTask).thenReturn(mockAddressTask)
        whenever(mockAddressTask.isAddressAlreadyRegistered).thenReturn(false)

        // Act
        val result = stepConfig.resolveNextDestination(mockState, defaultDestination)

        // Assert
        verify(mockState).deleteJourney()
        assertEquals(defaultDestination, result)
    }

    @Test
    fun `resolveNextDestination redirects to already registered step when address is already registered`() {
        // Arrange
        val defaultDestination = Destination.ExternalUrl("redirect")
        val mockAlreadyRegisteredStep = mock<AlreadyRegisteredStep>()
        whenever(mockAlreadyRegisteredStep.currentJourneyId).thenReturn("test-journey-id")
        whenever(mockState.propertyDetailsTask).thenReturn(mockPropertyDetailsTask)
        whenever(mockPropertyDetailsTask.addressTask).thenReturn(mockAddressTask)
        whenever(mockAddressTask.isAddressAlreadyRegistered).thenReturn(true)
        whenever(mockAddressTask.alreadyRegisteredStep).thenReturn(mockAlreadyRegisteredStep)

        // Act
        val result = stepConfig.resolveNextDestination(mockState, defaultDestination)

        // Assert
        verify(mockState, never()).deleteJourney()
        assertNotEquals(defaultDestination, result)
    }

    private fun setupStateForPropertyRegistration() {
        val mockOccupiedStep = mock<OccupiedStep>()
        val occupancyFormModel = OccupancyFormModel().apply { occupied = false }
        whenever(mockState.occupied).thenReturn(mockOccupiedStep)
        whenever(mockOccupiedStep.formModel).thenReturn(occupancyFormModel)

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

        whenever(gasSafetyTask.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
        whenever(gasSafetyTask.gasCertOutcome).thenReturn(GasCertOutcome.HAS_CERTIFICATE)

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

        lenient().`when`(gasSafetyDetailsTask.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
        lenient().`when`(gasSafetyDetailsTask.gasCertOutcome).thenReturn(null)

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

        val mockBedroomsStep = mock<BedroomsStep>()
        whenever(mockState.bedrooms).thenReturn(mockBedroomsStep)
        whenever(mockBedroomsStep.formModel).thenReturn(NumberOfBedroomsFormModel().apply { numberOfBedrooms = "1" })

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

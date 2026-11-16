package uk.gov.communities.prsdb.webapp.services

import jakarta.persistence.EntityExistsException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.communities.prsdb.webapp.constants.PAYMENTS
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PaymentStatus
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.entity.Landlord
import uk.gov.communities.prsdb.webapp.database.entity.License
import uk.gov.communities.prsdb.webapp.database.entity.PropertyManager
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.entity.RegistrationNumber
import uk.gov.communities.prsdb.webapp.database.repository.PaymentRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.helpers.RenewalDateHelper
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyManagerDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyRegistrationConfirmationEmail
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockJointLandlordData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockPaymentData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockPropertyRegistrationData
import java.net.URI
import java.time.LocalDate
import java.time.MonthDay
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PropertyRegistrationServiceTests {
    @Mock
    private lateinit var mockPropertyOwnershipRepository: PropertyOwnershipRepository

    @Mock
    private lateinit var mockUserToLandlordService: UserToLandlordService

    @Mock
    private lateinit var mockAddressService: AddressService

    @Mock
    private lateinit var mockLicenseService: LicenseService

    @Mock
    private lateinit var mockPropertyOwnershipService: PropertyOwnershipService

    @Mock
    private lateinit var mockPropertyManagerService: PropertyManagerService

    @Mock
    private lateinit var mockAbsoluteUrlProvider: AbsoluteUrlProvider

    @Mock
    private lateinit var mockConfirmationEmailSender: EmailNotificationService<PropertyRegistrationConfirmationEmail>

    @Mock
    private lateinit var mockPropertyRegistrationConfirmationService: PropertyRegistrationConfirmationService

    @Mock
    private lateinit var mockJointLandlordInvitationService: JointLandlordInvitationService

    @Mock
    private lateinit var mockPropertyComplianceService: PropertyComplianceService

    @Mock
    private lateinit var mockLettingAgentAccessService: LettingAgentAccessService

    @Mock
    private lateinit var mockDelegateToLettingAgentEmailService: DelegateToLettingAgentEmailService

    @Mock
    private lateinit var mockFeatureFlagManager: uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager

    @Mock
    private lateinit var mockCurrentUserService: CurrentUserService

    @Mock
    private lateinit var mockPaymentRepository: PaymentRepository

    @Mock
    private lateinit var mockActiveInactiveService: ActiveInactiveService

    @InjectMocks
    private lateinit var propertyRegistrationService: PropertyRegistrationService

    @BeforeEach
    fun setUp() {
        // TODO PDJB-1734: Remove the checkFeature(PAYMENTS) stub when the flag is removed, but keep the payment repository stub
        lenient().`when`(mockFeatureFlagManager.checkFeature(PAYMENTS)).thenReturn(true)
        lenient()
            .`when`(
                mockPaymentRepository.findFirstByAssociatedPropertyAndStatusOrderByPaymentCreatedAtDesc(
                    any(),
                    eq(PaymentStatus.SUCCEEDED),
                ),
            ).thenReturn(MockPaymentData.createPayment(status = PaymentStatus.SUCCEEDED))
    }

    @AfterEach
    fun tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `registerProperty throws an error if the given address is registered`() {
        val registeredAddress = AddressDataModel(singleLineAddress = "1 Example Road", uprn = 0L)
        val landlord = MockLandlordData.createIndividualLandlord()

        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipRepository.existsByIsActiveTrueAndAddress_Uprn(registeredAddress.uprn!!),
        ).thenReturn(true)

        val errorThrown =
            assertThrows<EntityExistsException> {
                propertyRegistrationService.registerProperty(
                    MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                        addressModel = registeredAddress,
                        propertyType = PropertyType.DETACHED_HOUSE,
                        licenseType = LicensingType.NO_LICENSING,
                        licenceNumber = "license number",
                        ownershipType = OwnershipType.SOLE_FREEHOLD,
                        isOccupied = true,
                        numberOfHouseholds = 1,
                        numberOfPeople = 1,
                        numBedrooms = null,
                        billsIncludedList = null,
                        customBillsIncluded = null,
                        furnishedStatus = null,
                        rentFrequency = RentFrequency.MONTHLY,
                        customRentFrequency = null,
                        rentAmount = 123.toBigDecimal(),
                        customPropertyType = null,
                    ),
                )
            }

        assertEquals("Address already registered", errorThrown.message)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["--03-15"])
    fun `registerProperty uses the landlord's anniversary, or today if they have none, for the renewal date and the landlord`(
        existingAnniversary: MonthDay?,
    ) {
        assertRegistrationRenewalDate(existingAnniversary)
    }

    @Test
    fun `registerProperty passes the paid renewal date unchanged and preserves the existing anniversary`() {
        assertRegistrationRenewalDate(MonthDay.of(3, 15), LocalDate.of(2030, 6, 12))
    }

    @Test
    fun `registerProperty passes the paid renewal date when the landlord has no anniversary`() {
        assertRegistrationRenewalDate(null, LocalDate.of(2030, 6, 12))
    }

    private fun assertRegistrationRenewalDate(
        existingAnniversary: MonthDay?,
        renewalDate: LocalDate? = null,
    ) {
        val landlord = MockLandlordData.createIndividualLandlord()
        existingAnniversary?.let { landlord.setAnniversaryIfAbsent(it) }
        val expectedAnniversary = existingAnniversary ?: MonthDay.now(DateTimeHelper.UK_ZONE)
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val expectedPropertyOwnership = MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord), address = address)

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = OwnershipType.SOLE_FREEHOLD,
                isOccupied = true,
                numberOfHouseholds = 1,
                numberOfPeople = 1,
                registeringLandlord = landlord,
                anniversary = expectedAnniversary,
                renewalDate = renewalDate ?: RenewalDateHelper.getRenewalDate(expectedAnniversary),
                propertyBuildType = PropertyType.DETACHED_HOUSE,
                customPropertyType = null,
                address = address,
                license = null,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                licenseProvideLater = false,
                tenancyProvideLater = false,
                correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
                correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData
                .createPropertyRegistrationDataModel(
                    addressModel = addressDataModel,
                    propertyType = PropertyType.DETACHED_HOUSE,
                    licenseType = LicensingType.NO_LICENSING,
                    licenceNumber = "",
                    ownershipType = OwnershipType.SOLE_FREEHOLD,
                    isOccupied = true,
                    numberOfHouseholds = 1,
                    numberOfPeople = 1,
                    numBedrooms = null,
                    billsIncludedList = null,
                    customBillsIncluded = null,
                    furnishedStatus = null,
                    rentFrequency = RentFrequency.MONTHLY,
                    customRentFrequency = null,
                    rentAmount = 123.toBigDecimal(),
                    customPropertyType = null,
                ).copy(renewalDate = renewalDate),
        )

        assertEquals(expectedAnniversary, landlord.anniversary)
    }

    @Test
    fun `registerProperty creates and returns the property ownership if all property fields are populated`() {
        // Arrange
        val correspondenceEmail = "chosen.contact@example.com"
        val correspondenceAddressModel = AddressDataModel.fromManualAddressData("12 Contact Road", "Leeds", "LS1 1AA")
        val ownershipType = OwnershipType.SOLE_FREEHOLD
        val isOccupied = true
        val numberOfHouseholds = 1
        val numberOfPeople = 2
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyType = PropertyType.OTHER
        val customPropertyType = "End terrace"
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val licenceType = LicensingType.SELECTIVE_LICENCE
        val licenceNumber = "L1234"
        val licence = License(licenceType, licenceNumber)
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)
        val numberOfBedrooms = 1
        val billsIncludedList = "Electricity, Water"
        val customBillsIncluded = "Internet"
        val furnishedStatus = FurnishedStatus.FURNISHED
        val rentFrequency = RentFrequency.OTHER
        val customRentFrequency = "Fortnightly"
        val rentAmount = 123.toBigDecimal()

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = numberOfHouseholds,
                currentNumTenants = numberOfPeople,
                isOccupied = isOccupied,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyType,
                customPropertyType = customPropertyType,
                address = address,
                license = licence,
                registrationNumber = registrationNumber,
                numberOfBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockLicenseService.createLicense(licenceType, licenceNumber)).thenReturn(licence)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                correspondenceEmail = correspondenceEmail,
                correspondenceAddressModel = correspondenceAddressModel,
                ownershipType = ownershipType,
                isOccupied = isOccupied,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                registeringLandlord = landlord,
                anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
                renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
                propertyBuildType = propertyType,
                customPropertyType = customPropertyType,
                address = address,
                license = licence,
                numBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
                licenseProvideLater = false,
                tenancyProvideLater = false,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        val propertyOwnership =
            propertyRegistrationService.registerProperty(
                MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                    addressModel = addressDataModel,
                    propertyType = propertyType,
                    licenseType = licenceType,
                    licenceNumber = licenceNumber,
                    correspondenceEmail = correspondenceEmail,
                    correspondenceAddressModel = correspondenceAddressModel,
                    ownershipType = ownershipType,
                    isOccupied = isOccupied,
                    numberOfHouseholds = numberOfHouseholds,
                    numberOfPeople = numberOfPeople,
                    numBedrooms = numberOfBedrooms,
                    billsIncludedList = billsIncludedList,
                    customBillsIncluded = customBillsIncluded,
                    furnishedStatus = furnishedStatus,
                    rentFrequency = rentFrequency,
                    customRentFrequency = customRentFrequency,
                    rentAmount = rentAmount,
                    customPropertyType = customPropertyType,
                ),
            )

        // Assert
        assertEquals(expectedPropertyOwnership, propertyOwnership)
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            correspondenceEmail = correspondenceEmail,
            correspondenceAddressModel = correspondenceAddressModel,
            ownershipType = ownershipType,
            isOccupied = isOccupied,
            numberOfHouseholds = numberOfHouseholds,
            numberOfPeople = numberOfPeople,
            registeringLandlord = landlord,
            anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
            renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
            propertyBuildType = propertyType,
            customPropertyType = customPropertyType,
            address = address,
            license = licence,
            numBedrooms = numberOfBedrooms,
            billsIncludedList = billsIncludedList,
            customBillsIncluded = customBillsIncluded,
            furnishedStatus = furnishedStatus,
            rentFrequency = rentFrequency,
            customRentFrequency = customRentFrequency,
            rentAmount = rentAmount,
            licenseProvideLater = false,
            tenancyProvideLater = false,
        )
        verify(mockPropertyComplianceService).saveRegistrationComplianceData(
            registrationNumberValue = registrationNumber.number,
            gasSafetyCertProvideLater = false,
            electricalSafetyCertProvideLater = false,
            epcProvideLater = false,
        )
    }

    @Test
    fun `registerProperty passes compliance data to saveRegistrationComplianceData`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)
        val gasSafetyCertIssueDate = LocalDate.of(2025, 6, 15)
        val eicIssueDate = LocalDate.of(2024, 3, 20)
        val epcUrl = "https://epc.example.com/cert/1234"
        val epcExpiryDate = LocalDate.of(2030, 1, 1)
        val epcEnergyRating = "B"
        val epcExemptionReason = EpcExemptionReason.PROTECTED_ARCHITECTURAL_OR_HISTORICAL_MERIT
        val meesExemptionReason = MeesExemptionReason.HIGH_COST

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                landlords = mutableSetOf(landlord),
                address = address,
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                registeringLandlord = any(),
                anniversary = any(),
                propertyBuildType = any(),
                address = any(),
                license = anyOrNull(),
                isActive = any(),
                numBedrooms = anyOrNull(),
                billsIncludedList = anyOrNull(),
                customBillsIncluded = anyOrNull(),
                furnishedStatus = anyOrNull(),
                rentFrequency = anyOrNull(),
                customRentFrequency = anyOrNull(),
                rentAmount = anyOrNull(),
                customPropertyType = anyOrNull(),
                markedJointLandlord = any(),
                licenseProvideLater = anyOrNull(),
                tenancyProvideLater = anyOrNull(),
                correspondenceEmail = anyOrNull(),
                correspondenceAddressModel = anyOrNull(),
                renewalDate = any(),
                freeholders = anyOrNull(),
                superiorLandlords = any(),
                propertyManager = anyOrNull(),
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = PropertyType.DETACHED_HOUSE,
                licenseType = LicensingType.NO_LICENSING,
                licenceNumber = "",
                ownershipType = OwnershipType.SOLE_FREEHOLD,
                isOccupied = true,
                numberOfHouseholds = 1,
                numberOfPeople = 1,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                customPropertyType = null,
                hasGasSupply = true,
                gasSafetyCertIssueDate = gasSafetyCertIssueDate,
                gasSafetyFileUploadIds = listOf(10L, 20L),
                electricalSafetyFileUploadIds = listOf(30L),
                eicrExpiryDate = null,
                eicIssueDate = eicIssueDate,
                electricalCertType = uk.gov.communities.prsdb.webapp.constants.enums.CertificateType.Eic,
                epcCertificateUrl = epcUrl,
                epcExpiryDate = epcExpiryDate,
                epcEnergyRating = epcEnergyRating,
                tenancyStartedBeforeEpcExpiry = true,
                epcExemptionReason = epcExemptionReason,
                epcMeesExemptionReason = meesExemptionReason,
            ),
        )

        // Assert
        verify(mockPropertyComplianceService).saveRegistrationComplianceData(
            registrationNumberValue = registrationNumber.number,
            hasGasSupply = true,
            gasSafetyCertIssueDate = gasSafetyCertIssueDate,
            gasSafetyFileUploadIds = listOf(10L, 20L),
            gasSafetyCertProvideLater = false,
            electricalSafetyFileUploadIds = listOf(30L),
            eicrExpiryDate = null,
            electricalCertType = uk.gov.communities.prsdb.webapp.constants.enums.CertificateType.Eic,
            electricalSafetyCertProvideLater = false,
            epcCertificateUrl = epcUrl,
            epcExpiryDate = epcExpiryDate,
            epcEnergyRating = epcEnergyRating,
            tenancyStartedBeforeEpcExpiry = true,
            epcExemptionReason = epcExemptionReason,
            epcMeesExemptionReason = meesExemptionReason,
            epcProvideLater = false,
            eicIssueDate = eicIssueDate,
        )
    }

    @Test
    fun `registerProperty sends confirmation email to current user and caches the registration number when it registers the property`() {
        // Arrange
        val landlord = MockLandlordData.createOrgLandlord()
        val currentUserEmail = "admin@example.com"
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 5678)

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                landlords = mutableSetOf(landlord),
                registrationNumber = registrationNumber,
            )

        whenever(mockCurrentUserService.getCurrentEmail()).thenReturn(currentUserEmail)
        whenever(mockAddressService.findOrCreateAddress(any())).thenReturn(expectedPropertyOwnership.address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockLicenseService.createLicense(any(), any())).thenReturn(expectedPropertyOwnership.license)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                registeringLandlord = any(),
                anniversary = any(),
                propertyBuildType = any(),
                address = any(),
                license = anyOrNull(),
                isActive = any(),
                numBedrooms = anyOrNull(),
                billsIncludedList = anyOrNull(),
                customBillsIncluded = anyOrNull(),
                furnishedStatus = anyOrNull(),
                rentFrequency = anyOrNull(),
                customRentFrequency = anyOrNull(),
                rentAmount = anyOrNull(),
                customPropertyType = anyOrNull(),
                markedJointLandlord = any(),
                licenseProvideLater = anyOrNull(),
                tenancyProvideLater = anyOrNull(),
                correspondenceEmail = anyOrNull(),
                correspondenceAddressModel = anyOrNull(),
                renewalDate = any(),
                freeholders = anyOrNull(),
                superiorLandlords = any(),
                propertyManager = anyOrNull(),
            ),
        ).thenReturn(expectedPropertyOwnership)

        val dashboardUri = URI("https:gov.uk")
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(dashboardUri)

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = AddressDataModel.fromAddress(expectedPropertyOwnership.address),
                propertyType = PropertyType.DETACHED_HOUSE,
                licenseType = LicensingType.SELECTIVE_LICENCE,
                licenceNumber = "Licence",
                ownershipType = OwnershipType.SOLE_FREEHOLD,
                isOccupied = true,
                numberOfHouseholds = 2,
                numberOfPeople = 1,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                customPropertyType = null,
            ),
        )

        // Assert
        verify(mockConfirmationEmailSender).sendEmail(
            eq(currentUserEmail),
            argThat<PropertyRegistrationConfirmationEmail> { email ->
                email.prn == RegistrationNumberDataModel.fromRegistrationNumber(registrationNumber).toString() &&
                    email.multiLineAddress == expectedPropertyOwnership.address.toMultiLineAddress() &&
                    email.prsdUrl == dashboardUri.toString() &&
                    email.isOccupied == (expectedPropertyOwnership.currentNumTenants > 0)
            },
        )

        verify(mockPropertyRegistrationConfirmationService).setLastPrnRegisteredThisSession(eq(registrationNumber.number))
    }

    @Test
    fun `registerProperty registers the property if there is no license`() {
        // Arrange
        val ownershipType = OwnershipType.SOLE_FREEHOLD
        val numberOfHouseholds = 1
        val numberOfPeople = 2
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyType = PropertyType.OTHER
        val customPropertyType = "End terrace"
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val licenceType = LicensingType.NO_LICENSING
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)
        val numberOfBedrooms = 1
        val billsIncludedList = "Electricity, Water"
        val customBillsIncluded = "Internet"
        val furnishedStatus = FurnishedStatus.FURNISHED
        val rentFrequency = RentFrequency.OTHER
        val customRentFrequency = "Fortnightly"
        val rentAmount = 123.toBigDecimal()

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = numberOfHouseholds,
                currentNumTenants = numberOfPeople,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyType,
                address = address,
                license = null,
                registrationNumber = registrationNumber,
                numberOfBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                registeringLandlord = landlord,
                anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
                renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
                propertyBuildType = propertyType,
                customPropertyType = customPropertyType,
                address = address,
                license = null,
                numBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
                licenseProvideLater = false,
                tenancyProvideLater = false,
                correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
                correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = propertyType,
                licenseType = licenceType,
                licenceNumber = "",
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                numBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
                customPropertyType = customPropertyType,
            ),
        )

        verify(mockLicenseService, never()).createLicense(any(), any())
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            ownershipType = ownershipType,
            isOccupied = true,
            numberOfHouseholds = numberOfHouseholds,
            numberOfPeople = numberOfPeople,
            registeringLandlord = landlord,
            anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
            renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
            propertyBuildType = propertyType,
            customPropertyType = customPropertyType,
            address = address,
            license = null,
            numBedrooms = numberOfBedrooms,
            billsIncludedList = billsIncludedList,
            customBillsIncluded = customBillsIncluded,
            furnishedStatus = furnishedStatus,
            rentFrequency = rentFrequency,
            customRentFrequency = customRentFrequency,
            rentAmount = rentAmount,
            licenseProvideLater = false,
            tenancyProvideLater = false,
            correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
            correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
        )
    }

    @Test
    fun `registerProperty does not create a license and sets licenseProvideLater when the user provides licensing later`() {
        // Arrange
        val ownershipType = OwnershipType.SOLE_FREEHOLD
        val numberOfHouseholds = 1
        val numberOfPeople = 2
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyType = PropertyType.DETACHED_HOUSE
        val customPropertyType = "End terrace"
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)
        val numberOfBedrooms = 1
        val billsIncludedList = "Electricity, Water"
        val customBillsIncluded = "Internet"
        val furnishedStatus = FurnishedStatus.FURNISHED
        val rentFrequency = RentFrequency.OTHER
        val customRentFrequency = "Fortnightly"
        val rentAmount = 123.toBigDecimal()

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = numberOfHouseholds,
                currentNumTenants = numberOfPeople,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyType,
                address = address,
                license = null,
                registrationNumber = registrationNumber,
                numberOfBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                registeringLandlord = landlord,
                anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
                renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
                propertyBuildType = propertyType,
                customPropertyType = customPropertyType,
                address = address,
                license = null,
                numBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
                licenseProvideLater = true,
                tenancyProvideLater = false,
                correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
                correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = propertyType,
                licenseType = LicensingType.PROVIDE_LATER,
                licenceNumber = "",
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                numBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
                customPropertyType = customPropertyType,
                licenseProvideLater = true,
            ),
        )

        // Assert
        verify(mockLicenseService, never()).createLicense(any(), any())
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            ownershipType = ownershipType,
            isOccupied = true,
            numberOfHouseholds = numberOfHouseholds,
            numberOfPeople = numberOfPeople,
            registeringLandlord = landlord,
            anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
            renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
            propertyBuildType = propertyType,
            customPropertyType = customPropertyType,
            address = address,
            license = null,
            numBedrooms = numberOfBedrooms,
            billsIncludedList = billsIncludedList,
            customBillsIncluded = customBillsIncluded,
            furnishedStatus = furnishedStatus,
            rentFrequency = rentFrequency,
            customRentFrequency = customRentFrequency,
            rentAmount = rentAmount,
            licenseProvideLater = true,
            tenancyProvideLater = false,
            correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
            correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
        )
    }

    @Test
    fun `registerProperty sends joint landlord invitation emails when joint landlord emails are provided`() {
        // Arrange
        val jointLandlordEmails = listOf("landlord1@example.com", "landlord2@example.com")
        val ownershipType = OwnershipType.SOLE_FREEHOLD
        val numberOfHouseholds = 1
        val numberOfPeople = 2
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyType = PropertyType.DETACHED_HOUSE
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val licenceType = LicensingType.SELECTIVE_LICENCE
        val licenceNumber = "Licence123"
        val license = License(licenceType, licenceNumber)
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = numberOfHouseholds,
                currentNumTenants = numberOfPeople,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyType,
                address = address,
                license = license,
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockLicenseService.createLicense(licenceType, licenceNumber)).thenReturn(license)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                registeringLandlord = landlord,
                anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
                renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
                propertyBuildType = propertyType,
                customPropertyType = null,
                address = address,
                license = license,
                isActive = true,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                licenseProvideLater = false,
                tenancyProvideLater = false,
                correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
                correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))
        val jointLandlordInvitations =
            jointLandlordEmails.map { email ->
                MockJointLandlordData.createJointLandlordInvitation(email = email, propertyOwnership = expectedPropertyOwnership)
            }
        whenever(mockJointLandlordInvitationService.createInvitations(jointLandlordEmails, expectedPropertyOwnership, landlord))
            .thenReturn(jointLandlordInvitations)

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = propertyType,
                licenseType = licenceType,
                licenceNumber = licenceNumber,
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                customPropertyType = null,
                jointLandlordEmails = jointLandlordEmails,
            ),
        )

        // Assert
        jointLandlordInvitations.forEach { invitation ->
            verify(mockJointLandlordInvitationService).sendInvitationEmail(invitation)
        }
        verify(mockJointLandlordInvitationService).sendInvitationConfirmationEmails(
            jointLandlordEmails,
            expectedPropertyOwnership,
            landlord,
        )
    }

    @Test
    fun `registerProperty does not send joint landlord invitation emails when no joint landlord emails provided`() {
        // Arrange
        val ownershipType = OwnershipType.SOLE_FREEHOLD
        val numberOfHouseholds = 1
        val numberOfPeople = 2
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyType = PropertyType.DETACHED_HOUSE
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val licenceType = LicensingType.NO_LICENSING
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = numberOfHouseholds,
                currentNumTenants = numberOfPeople,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyType,
                address = address,
                license = null,
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                registeringLandlord = landlord,
                anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
                renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
                propertyBuildType = propertyType,
                customPropertyType = null,
                address = address,
                license = null,
                isActive = true,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                licenseProvideLater = false,
                tenancyProvideLater = false,
                correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
                correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = propertyType,
                licenseType = licenceType,
                licenceNumber = "",
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                customPropertyType = null,
                jointLandlordEmails = null,
            ),
        )

        // Assert
        verifyNoInteractions(mockJointLandlordInvitationService)
    }

    @Test
    fun `registerProperty persists a letting agent invitation when a letting agent email is provided`() {
        // Arrange
        val lettingAgentEmail = "agent@example.com"
        val ownershipType = OwnershipType.SOLE_FREEHOLD
        val numberOfHouseholds = 1
        val numberOfPeople = 2
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyType = PropertyType.DETACHED_HOUSE
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val licenceType = LicensingType.NO_LICENSING
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = numberOfHouseholds,
                currentNumTenants = numberOfPeople,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyType,
                address = address,
                license = null,
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                registeringLandlord = landlord,
                anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
                renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
                propertyBuildType = propertyType,
                customPropertyType = null,
                address = address,
                license = null,
                isActive = true,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                licenseProvideLater = false,
                tenancyProvideLater = false,
                correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
                correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))
        val expectedToken = UUID.randomUUID()
        whenever(mockLettingAgentAccessService.createInvitation(expectedPropertyOwnership, lettingAgentEmail))
            .thenReturn(
                MockLettingAgentData.createLettingAgentAccess(
                    token = expectedToken,
                    invitedEmail = lettingAgentEmail,
                    propertyOwnership = expectedPropertyOwnership,
                ),
            )

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = propertyType,
                licenseType = licenceType,
                licenceNumber = "",
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                customPropertyType = null,
                lettingAgentEmail = lettingAgentEmail,
            ),
        )

        // Assert
        verify(mockLettingAgentAccessService).createInvitation(expectedPropertyOwnership, lettingAgentEmail)
        verify(mockDelegateToLettingAgentEmailService).sendDelegationEmailToLettingAgent(
            eq(expectedPropertyOwnership),
            any(),
            eq(lettingAgentEmail),
            any(),
            invitationToken = eq(expectedToken),
        )
    }

    @Test
    fun `registerProperty does not persist a letting agent invitation when no letting agent email is provided`() {
        // Arrange
        val ownershipType = OwnershipType.SOLE_FREEHOLD
        val numberOfHouseholds = 1
        val numberOfPeople = 2
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyType = PropertyType.DETACHED_HOUSE
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val licenceType = LicensingType.NO_LICENSING
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = numberOfHouseholds,
                currentNumTenants = numberOfPeople,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyType,
                address = address,
                license = null,
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                registeringLandlord = landlord,
                anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
                renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
                propertyBuildType = propertyType,
                customPropertyType = null,
                address = address,
                license = null,
                isActive = true,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                licenseProvideLater = false,
                tenancyProvideLater = false,
                correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
                correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = propertyType,
                licenseType = licenceType,
                licenceNumber = "",
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                customPropertyType = null,
            ),
        )

        // Assert
        verifyNoInteractions(mockLettingAgentAccessService)
    }

    @Test
    fun `registerProperty does not send joint landlord invitation emails when empty list provided`() {
        // Arrange
        val jointLandlordEmails = emptyList<String>()
        val ownershipType = OwnershipType.SOLE_FREEHOLD
        val numberOfHouseholds = 1
        val numberOfPeople = 2
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyType = PropertyType.DETACHED_HOUSE
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val licenceType = LicensingType.NO_LICENSING
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = numberOfHouseholds,
                currentNumTenants = numberOfPeople,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyType,
                address = address,
                license = null,
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                registeringLandlord = landlord,
                anniversary = MonthDay.now(DateTimeHelper.UK_ZONE),
                renewalDate = RenewalDateHelper.getRenewalDate(MonthDay.now(DateTimeHelper.UK_ZONE)),
                propertyBuildType = propertyType,
                customPropertyType = null,
                address = address,
                license = null,
                isActive = true,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                licenseProvideLater = false,
                tenancyProvideLater = false,
                correspondenceEmail = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_EMAIL,
                correspondenceAddressModel = MockPropertyRegistrationData.DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = propertyType,
                licenseType = licenceType,
                licenceNumber = "",
                ownershipType = ownershipType,
                isOccupied = true,
                numberOfHouseholds = numberOfHouseholds,
                numberOfPeople = numberOfPeople,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = RentFrequency.MONTHLY,
                customRentFrequency = null,
                rentAmount = 123.toBigDecimal(),
                customPropertyType = null,
                jointLandlordEmails = jointLandlordEmails,
            ),
        )

        // Assert
        verifyNoInteractions(mockJointLandlordInvitationService)
    }

    @Test
    fun `registerProperty passes markedJointLandlord to createPropertyOwnership`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                landlords = mutableSetOf(landlord),
                address = address,
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                registeringLandlord = any(),
                anniversary = any(),
                propertyBuildType = any(),
                address = any(),
                license = anyOrNull(),
                isActive = any(),
                numBedrooms = anyOrNull(),
                billsIncludedList = anyOrNull(),
                customBillsIncluded = anyOrNull(),
                furnishedStatus = anyOrNull(),
                rentFrequency = anyOrNull(),
                customRentFrequency = anyOrNull(),
                rentAmount = anyOrNull(),
                customPropertyType = anyOrNull(),
                markedJointLandlord = any(),
                licenseProvideLater = anyOrNull(),
                tenancyProvideLater = anyOrNull(),
                correspondenceEmail = anyOrNull(),
                correspondenceAddressModel = anyOrNull(),
                renewalDate = any(),
                freeholders = anyOrNull(),
                superiorLandlords = any(),
                propertyManager = anyOrNull(),
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = PropertyType.DETACHED_HOUSE,
                licenseType = LicensingType.NO_LICENSING,
                licenceNumber = "",
                ownershipType = OwnershipType.SOLE_FREEHOLD,
                isOccupied = false,
                numberOfHouseholds = 0,
                numberOfPeople = 0,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = null,
                customRentFrequency = null,
                rentAmount = null,
                customPropertyType = null,
                markedJointLandlord = true,
            ),
        )

        // Assert
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            registeringLandlord = any(),
            anniversary = any(),
            propertyBuildType = any(),
            address = any(),
            license = anyOrNull(),
            isActive = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            markedJointLandlord = eq(true),
            licenseProvideLater = anyOrNull(),
            tenancyProvideLater = anyOrNull(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
            renewalDate = any(),
            freeholders = anyOrNull(),
            superiorLandlords = any(),
            propertyManager = anyOrNull(),
        )
    }

    @Test
    fun `registerProperty creates and links a property manager when one was given`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val propertyManager = PropertyManager("Pat Manager", "pat@example.com")

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockPropertyManagerService.createPropertyManager("Pat Manager", "pat@example.com")).thenReturn(propertyManager)
        arrangeCreatePropertyOwnershipWithAnyArguments(MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord)))
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyManager = PropertyManagerDataModel("Pat Manager", "pat@example.com"),
            ),
        )

        // Assert
        verify(mockPropertyManagerService).createPropertyManager("Pat Manager", "pat@example.com")
        verifyPropertyManagerPassedToCreatePropertyOwnership(propertyManager)
    }

    @Test
    fun `registerProperty does not create a property manager when none was given`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        arrangeCreatePropertyOwnershipWithAnyArguments(MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord)))
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(addressModel = addressDataModel),
        )

        // Assert
        verifyNoInteractions(mockPropertyManagerService)
        verifyPropertyManagerPassedToCreatePropertyOwnership(null)
    }

    private fun arrangeCreatePropertyOwnershipWithAnyArguments(propertyOwnership: PropertyOwnership) {
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                registeringLandlord = any(),
                anniversary = any(),
                propertyBuildType = any(),
                address = any(),
                license = anyOrNull(),
                isActive = any(),
                numBedrooms = anyOrNull(),
                billsIncludedList = anyOrNull(),
                customBillsIncluded = anyOrNull(),
                furnishedStatus = anyOrNull(),
                rentFrequency = anyOrNull(),
                customRentFrequency = anyOrNull(),
                rentAmount = anyOrNull(),
                customPropertyType = anyOrNull(),
                markedJointLandlord = any(),
                licenseProvideLater = anyOrNull(),
                tenancyProvideLater = anyOrNull(),
                correspondenceEmail = anyOrNull(),
                correspondenceAddressModel = anyOrNull(),
                renewalDate = any(),
                freeholders = anyOrNull(),
                superiorLandlords = any(),
                propertyManager = anyOrNull(),
            ),
        ).thenReturn(propertyOwnership)
    }

    private fun verifyPropertyManagerPassedToCreatePropertyOwnership(propertyManager: PropertyManager?) {
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            registeringLandlord = any(),
            anniversary = any(),
            propertyBuildType = any(),
            address = any(),
            license = anyOrNull(),
            isActive = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            markedJointLandlord = any(),
            licenseProvideLater = anyOrNull(),
            tenancyProvideLater = anyOrNull(),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
            renewalDate = any(),
            freeholders = anyOrNull(),
            superiorLandlords = any(),
            propertyManager = if (propertyManager == null) isNull() else eq(propertyManager),
        )
    }

    @Test
    fun `registerProperty passes tenancyProvideLater to createPropertyOwnership`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                landlords = mutableSetOf(landlord),
                address = address,
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                registeringLandlord = any(),
                anniversary = any(),
                propertyBuildType = any(),
                address = any(),
                license = anyOrNull(),
                isActive = any(),
                numBedrooms = anyOrNull(),
                billsIncludedList = anyOrNull(),
                customBillsIncluded = anyOrNull(),
                furnishedStatus = anyOrNull(),
                rentFrequency = anyOrNull(),
                customRentFrequency = anyOrNull(),
                rentAmount = anyOrNull(),
                customPropertyType = anyOrNull(),
                markedJointLandlord = any(),
                licenseProvideLater = anyOrNull(),
                tenancyProvideLater = any(),
                correspondenceEmail = anyOrNull(),
                correspondenceAddressModel = anyOrNull(),
                renewalDate = any(),
                freeholders = anyOrNull(),
                superiorLandlords = any(),
                propertyManager = anyOrNull(),
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = addressDataModel,
                propertyType = PropertyType.DETACHED_HOUSE,
                licenseType = LicensingType.NO_LICENSING,
                licenceNumber = "",
                ownershipType = OwnershipType.SOLE_FREEHOLD,
                isOccupied = false,
                numberOfHouseholds = 0,
                numberOfPeople = 0,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = null,
                customRentFrequency = null,
                rentAmount = null,
                customPropertyType = null,
                tenancyProvideLater = true,
            ),
        )

        // Assert
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            registeringLandlord = any(),
            anniversary = any(),
            propertyBuildType = any(),
            address = any(),
            license = anyOrNull(),
            isActive = any(),
            numBedrooms = anyOrNull(),
            billsIncludedList = anyOrNull(),
            customBillsIncluded = anyOrNull(),
            furnishedStatus = anyOrNull(),
            rentFrequency = anyOrNull(),
            customRentFrequency = anyOrNull(),
            rentAmount = anyOrNull(),
            customPropertyType = anyOrNull(),
            markedJointLandlord = any(),
            licenseProvideLater = anyOrNull(),
            tenancyProvideLater = eq(true),
            correspondenceEmail = anyOrNull(),
            correspondenceAddressModel = anyOrNull(),
            renewalDate = any(),
            freeholders = anyOrNull(),
            superiorLandlords = any(),
            propertyManager = anyOrNull(),
        )
    }

    @Test
    fun `registerProperty sends confirmation email with isDelegatedToLettingAgent when delegated`() {
        val landlord = MockLandlordData.createIndividualLandlord()
        val currentUserEmail = "admin@example.com"
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 9999)
        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                landlords = mutableSetOf(landlord),
                registrationNumber = registrationNumber,
            )

        whenever(mockCurrentUserService.getCurrentEmail()).thenReturn(currentUserEmail)
        whenever(mockAddressService.findOrCreateAddress(any())).thenReturn(expectedPropertyOwnership.address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                registeringLandlord = any(),
                anniversary = any(),
                propertyBuildType = any(),
                address = any(),
                license = anyOrNull(),
                isActive = any(),
                numBedrooms = anyOrNull(),
                billsIncludedList = anyOrNull(),
                customBillsIncluded = anyOrNull(),
                furnishedStatus = anyOrNull(),
                rentFrequency = anyOrNull(),
                customRentFrequency = anyOrNull(),
                rentAmount = anyOrNull(),
                customPropertyType = anyOrNull(),
                markedJointLandlord = any(),
                licenseProvideLater = anyOrNull(),
                tenancyProvideLater = anyOrNull(),
                correspondenceEmail = anyOrNull(),
                correspondenceAddressModel = anyOrNull(),
                renewalDate = any(),
                freeholders = anyOrNull(),
                superiorLandlords = any(),
                propertyManager = anyOrNull(),
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https://gov.uk"))

        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = AddressDataModel.fromAddress(expectedPropertyOwnership.address),
                propertyType = PropertyType.DETACHED_HOUSE,
                licenseType = LicensingType.NO_LICENSING,
                licenceNumber = "",
                ownershipType = OwnershipType.SOLE_FREEHOLD,
                isOccupied = true,
                numberOfHouseholds = 1,
                numberOfPeople = 1,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = null,
                customRentFrequency = null,
                rentAmount = null,
                customPropertyType = null,
                isDelegatedToLettingAgent = true,
            ),
        )

        verify(mockConfirmationEmailSender).sendEmail(
            eq(currentUserEmail),
            argThat<PropertyRegistrationConfirmationEmail> { isDelegatedToLettingAgent },
        )
    }

    @Test
    fun `registerProperty sends confirmation email with provide-later flags for a non-delegated occupied property`() {
        val landlord = MockLandlordData.createIndividualLandlord()
        val currentUserEmail = "admin@example.com"
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 8888)
        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                landlords = mutableSetOf(landlord),
                isOccupied = true,
                registrationNumber = registrationNumber,
            )

        whenever(mockCurrentUserService.getCurrentEmail()).thenReturn(currentUserEmail)
        whenever(mockAddressService.findOrCreateAddress(any())).thenReturn(expectedPropertyOwnership.address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                registeringLandlord = any(),
                anniversary = any(),
                propertyBuildType = any(),
                address = any(),
                license = anyOrNull(),
                isActive = any(),
                numBedrooms = anyOrNull(),
                billsIncludedList = anyOrNull(),
                customBillsIncluded = anyOrNull(),
                furnishedStatus = anyOrNull(),
                rentFrequency = anyOrNull(),
                customRentFrequency = anyOrNull(),
                rentAmount = anyOrNull(),
                customPropertyType = anyOrNull(),
                markedJointLandlord = any(),
                licenseProvideLater = anyOrNull(),
                tenancyProvideLater = anyOrNull(),
                correspondenceEmail = anyOrNull(),
                correspondenceAddressModel = anyOrNull(),
                renewalDate = any(),
                freeholders = anyOrNull(),
                superiorLandlords = any(),
                propertyManager = anyOrNull(),
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https://gov.uk"))
        propertyRegistrationService.registerProperty(
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                addressModel = AddressDataModel.fromAddress(expectedPropertyOwnership.address),
                propertyType = PropertyType.DETACHED_HOUSE,
                licenseType = LicensingType.NO_LICENSING,
                licenceNumber = "",
                ownershipType = OwnershipType.SOLE_FREEHOLD,
                isOccupied = true,
                numberOfHouseholds = 1,
                numberOfPeople = 1,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = null,
                customRentFrequency = null,
                rentAmount = null,
                customPropertyType = null,
                licenseProvideLater = true,
                gasSafetyCertProvideLater = true,
                electricalSafetyCertProvideLater = true,
                epcProvideLater = true,
                tenancyProvideLater = true,
                isDelegatedToLettingAgent = false,
            ),
        )

        verify(mockConfirmationEmailSender).sendEmail(
            eq(currentUserEmail),
            argThat<PropertyRegistrationConfirmationEmail> {
                licenseProvideLater &&
                    gasSafetyCertProvideLater &&
                    electricalSafetyCertProvideLater &&
                    epcProvideLater &&
                    tenancyProvideLater &&
                    !isDelegatedToLettingAgent &&
                    toHashMap()["hasMissingDetails"] == "yes"
            },
        )
    }

    @Test
    fun `registerProperty creates the invitations but neither sets the last PRN registered this session nor sends emails before commit`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyOwnership = MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord))
        stubPropertyOwnershipCreation(landlord, propertyOwnership)
        whenever(mockLettingAgentAccessService.createInvitation(propertyOwnership, LETTING_AGENT_EMAIL))
            .thenReturn(createLettingAgentInvitation(propertyOwnership))
        whenever(mockJointLandlordInvitationService.createInvitations(JOINT_LANDLORD_EMAILS, propertyOwnership, landlord))
            .thenReturn(createJointLandlordInvitations(propertyOwnership))
        TransactionSynchronizationManager.initSynchronization()

        // Act
        propertyRegistrationService.registerProperty(registrationDataWithInvitations)

        // Assert
        verify(mockLettingAgentAccessService).createInvitation(propertyOwnership, LETTING_AGENT_EMAIL)
        verify(mockJointLandlordInvitationService).createInvitations(JOINT_LANDLORD_EMAILS, propertyOwnership, landlord)
        verifyNoMoreInteractions(mockJointLandlordInvitationService)
        verifyNoInteractions(
            mockConfirmationEmailSender,
            mockDelegateToLettingAgentEmailService,
            mockPropertyRegistrationConfirmationService,
        )
    }

    @Test
    fun `registerProperty sets the last PRN registered this session and sends every email once the transaction it is part of commits`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyOwnership = MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord))
        stubPropertyOwnershipCreation(landlord, propertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https://gov.uk"))
        val currentUserEmail = "admin@example.com"
        whenever(mockCurrentUserService.getCurrentEmail()).thenReturn(currentUserEmail)
        val lettingAgentInvitation = createLettingAgentInvitation(propertyOwnership)
        whenever(mockLettingAgentAccessService.createInvitation(propertyOwnership, LETTING_AGENT_EMAIL))
            .thenReturn(lettingAgentInvitation)
        val jointLandlordInvitations = createJointLandlordInvitations(propertyOwnership)
        whenever(mockJointLandlordInvitationService.createInvitations(JOINT_LANDLORD_EMAILS, propertyOwnership, landlord))
            .thenReturn(jointLandlordInvitations)
        TransactionSynchronizationManager.initSynchronization()

        // Act
        propertyRegistrationService.registerProperty(registrationDataWithInvitations)
        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }

        // Assert
        verify(mockPropertyRegistrationConfirmationService).setLastPrnRegisteredThisSession(propertyOwnership.registrationNumber.number)
        verify(mockConfirmationEmailSender).sendEmail(eq(currentUserEmail), any())
        verify(mockDelegateToLettingAgentEmailService).sendDelegationEmailToLettingAgent(
            eq(propertyOwnership),
            eq(landlord.name),
            eq(LETTING_AGENT_EMAIL),
            any(),
            invitationToken = eq(lettingAgentInvitation.token),
        )
        jointLandlordInvitations.forEach { invitation ->
            verify(mockJointLandlordInvitationService).sendInvitationEmail(invitation)
        }
        verify(mockJointLandlordInvitationService).sendInvitationConfirmationEmails(
            JOINT_LANDLORD_EMAILS,
            propertyOwnership,
            landlord,
        )
    }

    @Test
    fun `registerProperty still returns the property and sends the other emails when an email fails to send`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyOwnership = MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord))
        stubPropertyOwnershipCreation(landlord, propertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https://gov.uk"))
        whenever(mockLettingAgentAccessService.createInvitation(propertyOwnership, LETTING_AGENT_EMAIL))
            .thenReturn(createLettingAgentInvitation(propertyOwnership))
        val jointLandlordInvitations = createJointLandlordInvitations(propertyOwnership)
        whenever(mockJointLandlordInvitationService.createInvitations(JOINT_LANDLORD_EMAILS, propertyOwnership, landlord))
            .thenReturn(jointLandlordInvitations)
        whenever(mockConfirmationEmailSender.sendEmail(any(), any())).thenThrow(RuntimeException("Email failed to send"))
        whenever(mockJointLandlordInvitationService.sendInvitationEmail(any()))
            .thenThrow(RuntimeException("Email failed to send"))
            .thenAnswer { }

        // Act
        val registeredPropertyOwnership = propertyRegistrationService.registerProperty(registrationDataWithInvitations)

        // Assert
        assertEquals(propertyOwnership, registeredPropertyOwnership)
        verify(mockDelegateToLettingAgentEmailService).sendDelegationEmailToLettingAgent(
            any(),
            any(),
            any(),
            any(),
            invitationToken = any(),
        )
        verify(mockJointLandlordInvitationService).sendInvitationEmail(jointLandlordInvitations.last())
        verify(mockJointLandlordInvitationService).sendInvitationConfirmationEmails(
            JOINT_LANDLORD_EMAILS,
            propertyOwnership,
            landlord,
        )
    }

    // TODO PDJB-1734: Remove "when payments is enabled" from this test name when the PAYMENTS flag is removed
    @Test
    fun `registerProperty sends the confirmation email with the latest succeeded payment's details when payments is enabled`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyOwnership = MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord))
        stubPropertyOwnershipCreation(landlord, propertyOwnership)
        stubConfirmationEmailDependencies()
        val payment =
            MockPaymentData.createPayment(
                amountInPence = 2550,
                quoteDate = LocalDate.of(2026, 10, 5),
                status = PaymentStatus.SUCCEEDED,
            )
        whenever(
            mockPaymentRepository.findFirstByAssociatedPropertyAndStatusOrderByPaymentCreatedAtDesc(
                propertyOwnership,
                PaymentStatus.SUCCEEDED,
            ),
        ).thenReturn(payment)

        // Act
        propertyRegistrationService.registerProperty(MockPropertyRegistrationData.createPropertyRegistrationDataModel())

        // Assert
        val expectedPaymentDetails =
            PropertyRegistrationConfirmationEmail.PaymentDetails(
                amountInPence = 2550,
                paymentDate = LocalDate.of(2026, 10, 5),
                renewalDate = propertyOwnership.renewalDate,
            )
        verify(mockConfirmationEmailSender).sendEmail(
            eq(CURRENT_USER_EMAIL),
            argThat<PropertyRegistrationConfirmationEmail> { paymentDetails == expectedPaymentDetails },
        )
    }

    // TODO PDJB-1734: Remove "when payments is enabled" from this test name when the PAYMENTS flag is removed
    @Test
    fun `registerProperty does not send the confirmation email when payments is enabled and there is no succeeded payment`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyOwnership = MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord))
        stubPropertyOwnershipCreation(landlord, propertyOwnership)
        stubConfirmationEmailDependencies()
        whenever(
            mockPaymentRepository.findFirstByAssociatedPropertyAndStatusOrderByPaymentCreatedAtDesc(
                propertyOwnership,
                PaymentStatus.SUCCEEDED,
            ),
        ).thenReturn(null)

        // Act
        val registeredPropertyOwnership =
            propertyRegistrationService.registerProperty(MockPropertyRegistrationData.createPropertyRegistrationDataModel())

        // Assert
        assertEquals(propertyOwnership, registeredPropertyOwnership)
        verifyNoInteractions(mockConfirmationEmailSender)
    }

    // TODO PDJB-1734: Remove this test when the PAYMENTS flag is removed
    @Test
    fun `registerProperty sends the confirmation email without payment details when payments is disabled`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyOwnership = MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord))
        stubPropertyOwnershipCreation(landlord, propertyOwnership)
        stubConfirmationEmailDependencies()
        whenever(mockFeatureFlagManager.checkFeature(PAYMENTS)).thenReturn(false)

        // Act
        propertyRegistrationService.registerProperty(MockPropertyRegistrationData.createPropertyRegistrationDataModel())

        // Assert
        verify(mockConfirmationEmailSender).sendEmail(
            eq(CURRENT_USER_EMAIL),
            argThat<PropertyRegistrationConfirmationEmail> { paymentDetails == null },
        )
        verifyNoInteractions(mockPaymentRepository)
    }

    @Test
    fun `registerProperty reactivates the landlord via the active inactive service if they are inactive`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyOwnership = MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord))
        stubPropertyOwnershipCreation(landlord, propertyOwnership)
        stubConfirmationEmailDependencies()

        // Act
        propertyRegistrationService.registerProperty(MockPropertyRegistrationData.createPropertyRegistrationDataModel())

        // Assert
        verify(mockActiveInactiveService).reactivateLandlordIfInactive(landlord)
    }

    private fun stubPropertyOwnershipCreation(
        landlord: Landlord,
        propertyOwnership: PropertyOwnership,
    ) {
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockAddressService.findOrCreateAddress(any())).thenReturn(propertyOwnership.address)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                registeringLandlord = any(),
                anniversary = any(),
                propertyBuildType = any(),
                address = any(),
                license = anyOrNull(),
                isActive = any(),
                numBedrooms = anyOrNull(),
                billsIncludedList = anyOrNull(),
                customBillsIncluded = anyOrNull(),
                furnishedStatus = anyOrNull(),
                rentFrequency = anyOrNull(),
                customRentFrequency = anyOrNull(),
                rentAmount = anyOrNull(),
                customPropertyType = anyOrNull(),
                markedJointLandlord = any(),
                licenseProvideLater = anyOrNull(),
                tenancyProvideLater = anyOrNull(),
                correspondenceEmail = anyOrNull(),
                correspondenceAddressModel = anyOrNull(),
                renewalDate = any(),
                freeholders = anyOrNull(),
                superiorLandlords = any(),
                propertyManager = anyOrNull(),
            ),
        ).thenReturn(propertyOwnership)
    }

    private fun stubConfirmationEmailDependencies() {
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https://gov.uk"))
        whenever(mockCurrentUserService.getCurrentEmail()).thenReturn(CURRENT_USER_EMAIL)
    }

    private fun createLettingAgentInvitation(propertyOwnership: PropertyOwnership) =
        MockLettingAgentData.createLettingAgentAccess(invitedEmail = LETTING_AGENT_EMAIL, propertyOwnership = propertyOwnership)

    private fun createJointLandlordInvitations(propertyOwnership: PropertyOwnership) =
        JOINT_LANDLORD_EMAILS.map { email ->
            MockJointLandlordData.createJointLandlordInvitation(email = email, propertyOwnership = propertyOwnership)
        }

    companion object {
        private const val CURRENT_USER_EMAIL = "admin@example.com"
        private const val LETTING_AGENT_EMAIL = "agent@example.com"
        private val JOINT_LANDLORD_EMAILS = listOf("landlord1@example.com", "landlord2@example.com")
        private val registrationDataWithInvitations =
            MockPropertyRegistrationData.createPropertyRegistrationDataModel(
                jointLandlordEmails = JOINT_LANDLORD_EMAILS,
                lettingAgentEmail = LETTING_AGENT_EMAIL,
            )
    }
}

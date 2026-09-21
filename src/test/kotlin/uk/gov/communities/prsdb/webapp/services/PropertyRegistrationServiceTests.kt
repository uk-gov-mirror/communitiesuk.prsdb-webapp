package uk.gov.communities.prsdb.webapp.services

import jakarta.persistence.EntityExistsException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.entity.License
import uk.gov.communities.prsdb.webapp.database.entity.RegistrationNumber
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyRegistrationConfirmationEmail
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import java.net.URI
import java.time.Instant
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

    @InjectMocks
    private lateinit var propertyRegistrationService: PropertyRegistrationService

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
                    addressModel = registeredAddress,
                    propertyType = PropertyType.DETACHED_HOUSE,
                    licenseType = LicensingType.NO_LICENSING,
                    licenceNumber = "license number",
                    ownershipType = OwnershipType.FREEHOLD,
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
                )
            }

        assertEquals("Address already registered", errorThrown.message)
    }

    @Test
    fun `registerProperty delegates to setAnniversaryIfAbsent with the property's registration date`() {
        val landlord = spy(MockLandlordData.createIndividualLandlord())
        val addressDataModel = AddressDataModel("1 Example Road, EG1 2AB")
        val address = Address(addressDataModel)
        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                landlords = mutableSetOf(landlord),
                address = address,
                createdDate = Instant.parse("2024-05-10T09:00:00Z"),
            )

        whenever(mockAddressService.findOrCreateAddress(addressDataModel)).thenReturn(address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = OwnershipType.FREEHOLD,
                isOccupied = true,
                numberOfHouseholds = 1,
                numberOfPeople = 1,
                landlords = mutableSetOf(landlord),
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
                tenancyProvideLater = null,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        propertyRegistrationService.registerProperty(
            addressModel = addressDataModel,
            propertyType = PropertyType.DETACHED_HOUSE,
            licenseType = LicensingType.NO_LICENSING,
            licenceNumber = "",
            ownershipType = OwnershipType.FREEHOLD,
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
        )

        verify(landlord).setAnniversaryIfAbsent(MonthDay.of(5, 10))
    }

    @Test
    fun `registerProperty creates the property ownership if all property fields are populated`() {
        // Arrange
        val correspondenceEmail = "chosen.contact@example.com"
        val correspondenceAddressModel = AddressDataModel.fromManualAddressData("12 Contact Road", "Leeds", "LS1 1AA")
        val ownershipType = OwnershipType.FREEHOLD
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
                landlords = mutableSetOf(landlord),
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
                tenancyProvideLater = null,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
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
        )

        // Assert
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            correspondenceEmail = correspondenceEmail,
            correspondenceAddressModel = correspondenceAddressModel,
            ownershipType = ownershipType,
            isOccupied = isOccupied,
            numberOfHouseholds = numberOfHouseholds,
            numberOfPeople = numberOfPeople,
            landlords = mutableSetOf(landlord),
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
            tenancyProvideLater = null,
        )
        verify(mockPropertyComplianceService).saveRegistrationComplianceData(
            registrationNumberValue = registrationNumber.number,
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
        val electricalSafetyExpiryDate = LocalDate.of(2029, 3, 20)
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
                landlords = any(),
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
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            addressModel = addressDataModel,
            propertyType = PropertyType.DETACHED_HOUSE,
            licenseType = LicensingType.NO_LICENSING,
            licenceNumber = "",
            ownershipType = OwnershipType.FREEHOLD,
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
            electricalSafetyExpiryDate = electricalSafetyExpiryDate,
            electricalCertType = uk.gov.communities.prsdb.webapp.constants.enums.CertificateType.Eicr,
            epcCertificateUrl = epcUrl,
            epcExpiryDate = epcExpiryDate,
            epcEnergyRating = epcEnergyRating,
            tenancyStartedBeforeEpcExpiry = true,
            epcExemptionReason = epcExemptionReason,
            epcMeesExemptionReason = meesExemptionReason,
        )

        // Assert
        verify(mockPropertyComplianceService).saveRegistrationComplianceData(
            registrationNumberValue = registrationNumber.number,
            hasGasSupply = true,
            gasSafetyCertIssueDate = gasSafetyCertIssueDate,
            gasSafetyFileUploadIds = listOf(10L, 20L),
            electricalSafetyFileUploadIds = listOf(30L),
            electricalSafetyExpiryDate = electricalSafetyExpiryDate,
            electricalCertType = uk.gov.communities.prsdb.webapp.constants.enums.CertificateType.Eicr,
            epcCertificateUrl = epcUrl,
            epcExpiryDate = epcExpiryDate,
            epcEnergyRating = epcEnergyRating,
            tenancyStartedBeforeEpcExpiry = true,
            epcExemptionReason = epcExemptionReason,
            epcMeesExemptionReason = meesExemptionReason,
        )
    }

    @Test
    fun `registerProperty sends a confirmation email and caches the registration number when it registers the property`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 5678)

        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                landlords = mutableSetOf(landlord),
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(any())).thenReturn(expectedPropertyOwnership.address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockLicenseService.createLicense(any(), any())).thenReturn(expectedPropertyOwnership.license)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                landlords = any(),
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
            ),
        ).thenReturn(expectedPropertyOwnership)

        val dashboardUri = URI("https:gov.uk")
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(dashboardUri)

        // Act
        propertyRegistrationService.registerProperty(
            addressModel = AddressDataModel.fromAddress(expectedPropertyOwnership.address),
            propertyType = PropertyType.DETACHED_HOUSE,
            licenseType = LicensingType.SELECTIVE_LICENCE,
            licenceNumber = "Licence",
            ownershipType = OwnershipType.FREEHOLD,
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
        )

        // Assert
        verify(mockConfirmationEmailSender).sendEmail(
            eq(landlord.email),
            argThat<PropertyRegistrationConfirmationEmail> { email ->
                email.prn == RegistrationNumberDataModel.fromRegistrationNumber(registrationNumber).toString() &&
                    email.singleLineAddress == expectedPropertyOwnership.address.singleLineAddress &&
                    email.prsdUrl == dashboardUri.toString() &&
                    email.isOccupied == (expectedPropertyOwnership.currentNumTenants > 0)
            },
        )

        verify(mockPropertyRegistrationConfirmationService).setLastPrnRegisteredThisSession(eq(registrationNumber.number))
    }

    @Test
    fun `registerProperty registers the property if there is no license`() {
        // Arrange
        val ownershipType = OwnershipType.FREEHOLD
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
                landlords = mutableSetOf(landlord),
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
                tenancyProvideLater = null,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        propertyRegistrationService.registerProperty(
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
        )

        verify(mockLicenseService, never()).createLicense(any(), any())
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            ownershipType = ownershipType,
            isOccupied = true,
            numberOfHouseholds = numberOfHouseholds,
            numberOfPeople = numberOfPeople,
            landlords = mutableSetOf(landlord),
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
        )
    }

    @Test
    fun `registerProperty does not create a license and sets licenseProvideLater when the user provides licensing later`() {
        // Arrange
        val ownershipType = OwnershipType.FREEHOLD
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
                landlords = mutableSetOf(landlord),
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
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
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
        )

        // Assert
        verify(mockLicenseService, never()).createLicense(any(), any())
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            ownershipType = ownershipType,
            isOccupied = true,
            numberOfHouseholds = numberOfHouseholds,
            numberOfPeople = numberOfPeople,
            landlords = mutableSetOf(landlord),
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
            tenancyProvideLater = null,
        )
    }

    @Test
    fun `registerProperty sends joint landlord invitation emails when joint landlord emails are provided`() {
        // Arrange
        val jointLandlordEmails = listOf("landlord1@example.com", "landlord2@example.com")
        val ownershipType = OwnershipType.FREEHOLD
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
                landlords = mutableSetOf(landlord),
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
                tenancyProvideLater = null,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
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
        )

        // Assert
        verify(mockJointLandlordInvitationService).sendInvitationEmails(
            eq(jointLandlordEmails),
            eq(expectedPropertyOwnership),
            eq(landlord),
        )
    }

    @Test
    fun `registerProperty does not send joint landlord invitation emails when no joint landlord emails provided`() {
        // Arrange
        val ownershipType = OwnershipType.FREEHOLD
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
                landlords = mutableSetOf(landlord),
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
                tenancyProvideLater = null,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
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
        )

        // Assert
        org.mockito.Mockito.verifyNoInteractions(mockJointLandlordInvitationService)
    }

    @Test
    fun `registerProperty persists a letting agent invitation when a letting agent email is provided`() {
        // Arrange
        val lettingAgentEmail = "agent@example.com"
        val ownershipType = OwnershipType.FREEHOLD
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
                landlords = mutableSetOf(landlord),
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
                tenancyProvideLater = null,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))
        val expectedToken = UUID.randomUUID()
        whenever(mockLettingAgentAccessService.createInvitation(expectedPropertyOwnership, lettingAgentEmail))
            .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = expectedToken, propertyOwnership = expectedPropertyOwnership))

        // Act
        propertyRegistrationService.registerProperty(
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
        val ownershipType = OwnershipType.FREEHOLD
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
                landlords = mutableSetOf(landlord),
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
                tenancyProvideLater = null,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
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
        )

        // Assert
        org.mockito.Mockito.verifyNoInteractions(mockLettingAgentAccessService)
    }

    @Test
    fun `registerProperty does not send joint landlord invitation emails when empty list provided`() {
        // Arrange
        val jointLandlordEmails = emptyList<String>()
        val ownershipType = OwnershipType.FREEHOLD
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
                landlords = mutableSetOf(landlord),
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
                tenancyProvideLater = null,
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
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
        )

        // Assert
        org.mockito.Mockito.verifyNoInteractions(mockJointLandlordInvitationService)
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
                landlords = any(),
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
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            addressModel = addressDataModel,
            propertyType = PropertyType.DETACHED_HOUSE,
            licenseType = LicensingType.NO_LICENSING,
            licenceNumber = "",
            ownershipType = OwnershipType.FREEHOLD,
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
        )

        // Assert
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            landlords = any(),
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
                landlords = any(),
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
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https:gov.uk"))

        // Act
        propertyRegistrationService.registerProperty(
            addressModel = addressDataModel,
            propertyType = PropertyType.DETACHED_HOUSE,
            licenseType = LicensingType.NO_LICENSING,
            licenceNumber = "",
            ownershipType = OwnershipType.FREEHOLD,
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
        )

        // Assert
        verify(mockPropertyOwnershipService).createPropertyOwnership(
            ownershipType = any(),
            isOccupied = any(),
            numberOfHouseholds = any(),
            numberOfPeople = any(),
            landlords = any(),
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
        )
    }

    @Test
    fun `registerProperty sends confirmation email with isDelegatedToLettingAgent when delegated`() {
        val landlord = MockLandlordData.createIndividualLandlord()
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 9999)
        val expectedPropertyOwnership =
            MockLandlordData.createPropertyOwnership(
                landlords = mutableSetOf(landlord),
                registrationNumber = registrationNumber,
            )

        whenever(mockAddressService.findOrCreateAddress(any())).thenReturn(expectedPropertyOwnership.address)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(
            mockPropertyOwnershipService.createPropertyOwnership(
                ownershipType = any(),
                isOccupied = any(),
                numberOfHouseholds = any(),
                numberOfPeople = any(),
                landlords = any(),
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
            ),
        ).thenReturn(expectedPropertyOwnership)
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https://gov.uk"))

        propertyRegistrationService.registerProperty(
            addressModel = AddressDataModel.fromAddress(expectedPropertyOwnership.address),
            propertyType = PropertyType.DETACHED_HOUSE,
            licenseType = LicensingType.NO_LICENSING,
            licenceNumber = "",
            ownershipType = OwnershipType.FREEHOLD,
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
        )

        verify(mockConfirmationEmailSender).sendEmail(
            eq(landlord.email),
            argThat<PropertyRegistrationConfirmationEmail> { isDelegatedToLettingAgent },
        )
    }
}

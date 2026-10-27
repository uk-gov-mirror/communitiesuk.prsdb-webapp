package uk.gov.communities.prsdb.webapp.services

import jakarta.persistence.EntityNotFoundException
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor.captor
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_LATER_DEADLINE_DAYS
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.FileUploadStatus
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.database.entity.FileUpload
import uk.gov.communities.prsdb.webapp.database.entity.IndividualLandlord
import uk.gov.communities.prsdb.webapp.database.entity.PropertyCompliance
import uk.gov.communities.prsdb.webapp.database.repository.FileUploadRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyComplianceRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.exceptions.UpdateConflictException
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.models.dataModels.ComplianceStatusDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.ComplianceUpdateConfirmationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.LettingAgentComplianceUpdateNotificationEmail
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyComplianceBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockPropertyComplianceData
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.collections.listOf

@ExtendWith(MockitoExtension::class)
class PropertyComplianceServiceTests {
    @Mock
    private lateinit var mockPropertyComplianceRepository: PropertyComplianceRepository

    @Mock
    private lateinit var mockPropertyOwnershipRepository: PropertyOwnershipRepository

    @Mock
    private lateinit var mockVirusScanCallbackService: VirusScanCallbackService

    @Mock
    private lateinit var fileUploadRepository: FileUploadRepository

    @Mock
    private lateinit var mockComplianceUpdateConfirmationSender: EmailNotificationService<ComplianceUpdateConfirmationEmail>

    @Mock
    private lateinit var mockLettingAgentComplianceUpdateSender: EmailNotificationService<LettingAgentComplianceUpdateNotificationEmail>

    @Mock
    private lateinit var mockAbsoluteUrlProvider: AbsoluteUrlProvider

    @Mock
    private lateinit var mockUserToLandlordService: UserToLandlordService

    @Mock
    private lateinit var mockPropertyOwnershipService: PropertyOwnershipService

    @Mock
    private lateinit var mockCurrentUserService: CurrentUserService

    private lateinit var propertyComplianceService: PropertyComplianceService

    private val propertyOwnershipId = 1L
    private val initialLastModifiedDate = Instant.parse("2025-01-01T00:00:00Z")
    private val loggedInBaseUserId = "logged-in-base-user-id"
    private val mockLoggedInLandlord =
        MockLandlordData.createIndividualLandlord(
            baseUser = MockLandlordData.createPrsdbUser(loggedInBaseUserId),
        )
    private val mockPropertyOwnership =
        MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(mockLoggedInLandlord))
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)

    private fun createComplianceWithLastModifiedDate(lastModifiedDate: Instant = initialLastModifiedDate): PropertyCompliance {
        val compliance = MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = mockPropertyOwnership)
        ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
        ReflectionTestUtils.setField(compliance, "lastModifiedDate", lastModifiedDate)
        return compliance
    }

    @BeforeEach
    fun setup() {
        propertyComplianceService =
            PropertyComplianceService(
                mockPropertyComplianceRepository,
                mockPropertyOwnershipRepository,
                fileUploadRepository,
                mockVirusScanCallbackService,
                mockComplianceUpdateConfirmationSender,
                mockLettingAgentComplianceUpdateSender,
                mockAbsoluteUrlProvider,
                mockUserToLandlordService,
                mockPropertyOwnershipService,
                mockCurrentUserService,
            )
        lenient()
            .`when`(mockAbsoluteUrlProvider.buildLandlordDashboardUri())
            .thenReturn(URI("https://test.example.com"))
        lenient()
            .`when`(
                mockAbsoluteUrlProvider.buildComplianceInformationUri(any<Long>()),
            ).thenReturn(URI("https://test.example.com/compliance"))
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun setMockPrincipal(landlord: IndividualLandlord = mockLoggedInLandlord) {
        whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(landlord)
        lenient()
            .`when`(mockCurrentUserService.getRegisteredUserDetails())
            .thenReturn(CurrentUserService.RegisteredUserDetails(name = landlord.name, email = landlord.email))
    }

    @Test
    fun `getComplianceForPropertyOrNull retrieves the compliance record for the given property ownership ID`() {
        val expectedPropertyCompliance = MockPropertyComplianceData.createPropertyCompliance()
        whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(expectedPropertyCompliance.propertyOwnership.id))
            .thenReturn(expectedPropertyCompliance)

        val returnedPropertyCompliance =
            propertyComplianceService.getComplianceForPropertyOrNull(expectedPropertyCompliance.propertyOwnership.id)

        assertEquals(returnedPropertyCompliance, returnedPropertyCompliance)
    }

    @Test
    fun `getComplianceForPropertyOrNull returns null when no compliance record exists for the given property ownership ID`() {
        val propertyOwnershipId = 123L
        whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId)).thenReturn(null)

        val returnedPropertyCompliance = propertyComplianceService.getComplianceForPropertyOrNull(propertyOwnershipId)

        assertNull(returnedPropertyCompliance)
    }

    @Test
    fun `getNumberOfNonCompliantPropertiesForLandlord returns a count of the landlord's non-compliant occupied properties`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val nonCompliantProperties =
            listOf(
                PropertyComplianceBuilder.createWithMissingCerts(propertyIsOccupied = true),
                PropertyComplianceBuilder.createWithExpiredCerts(propertyIsOccupied = true),
                PropertyComplianceBuilder.createWithInDateCertsAndLowEpcRating(propertyIsOccupied = true),
            )
        val compliantProperties =
            listOf(
                PropertyComplianceBuilder.createWithInDateCerts(propertyIsOccupied = true),
                PropertyComplianceBuilder.createWithCertExemptions(propertyIsOccupied = true),
            )
        val compliances = nonCompliantProperties + compliantProperties

        whenever(
            mockPropertyComplianceRepository.findAllByPropertyOwnership_OwnershipLinks_Landlord_IdAndPropertyOwnership_IsActiveTrue(
                landlord.id,
            ),
        ).thenReturn(compliances)

        // Act
        val returnedCount = propertyComplianceService.getNumberOfNonCompliantPropertiesForLandlord(landlord)

        // Assert
        assertEquals(nonCompliantProperties.size, returnedCount)
    }

    @Test
    fun `getNumberOfNonCompliantPropertiesForLandlord only includes non-compliant unoccupied properties if they are expired`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val nonCompliantProperties =
            listOf(
                PropertyComplianceBuilder.createWithMissingCerts(propertyIsOccupied = false),
                PropertyComplianceBuilder.createWithExpiredCerts(propertyIsOccupied = false),
                PropertyComplianceBuilder.createWithInDateCertsAndLowEpcRating(propertyIsOccupied = false),
            )
        val compliantProperties =
            listOf(
                PropertyComplianceBuilder.createWithInDateCerts(propertyIsOccupied = false),
                PropertyComplianceBuilder.createWithCertExemptions(propertyIsOccupied = false),
            )
        val compliances = nonCompliantProperties + compliantProperties

        whenever(
            mockPropertyComplianceRepository.findAllByPropertyOwnership_OwnershipLinks_Landlord_IdAndPropertyOwnership_IsActiveTrue(
                landlord.id,
            ),
        ).thenReturn(compliances)

        // Act
        val returnedCount = propertyComplianceService.getNumberOfNonCompliantPropertiesForLandlord(landlord)

        // Assert
        assertEquals(1, returnedCount)
    }

    // TODO PDJB-2006: Delete the getNonCompliantPropertiesForLandlord tests when the PROPERTY_REGISTRATION_PHASE_THREE flag is removed
    @Test
    fun `getNonCompliantPropertiesForLandlord returns the landlord's non-compliant occupied properties`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val nonCompliantProperties =
            listOf(
                PropertyComplianceBuilder.createWithMissingCerts(propertyIsOccupied = true),
                PropertyComplianceBuilder.createWithExpiredCerts(propertyIsOccupied = true),
                PropertyComplianceBuilder.createWithInDateCertsAndLowEpcRating(propertyIsOccupied = true),
            )
        val compliantProperties =
            listOf(
                PropertyComplianceBuilder.createWithInDateCerts(propertyIsOccupied = true),
                PropertyComplianceBuilder.createWithCertExemptions(propertyIsOccupied = true),
            )
        val compliances = nonCompliantProperties + compliantProperties

        whenever(
            mockPropertyComplianceRepository.findAllByPropertyOwnership_OwnershipLinks_Landlord_IdAndPropertyOwnership_IsActiveTrue(
                landlord.id,
            ),
        ).thenReturn(compliances)

        val expectedNonCompliantProperties =
            nonCompliantProperties.map { ComplianceStatusDataModel.fromPropertyCompliance(it) }

        // Act
        val returnedNonCompliantProperties =
            propertyComplianceService.getNonCompliantPropertiesForLandlord(
                landlord,
                0,
            )

        // Assert
        assertEquals(expectedNonCompliantProperties, returnedNonCompliantProperties.content)
    }

    @Test
    fun `getNonCompliantPropertiesForLandlord returns the only expired non-compliant unoccupied properties`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val nonCompliantProperties =
            listOf(
                PropertyComplianceBuilder.createWithMissingCerts(propertyIsOccupied = false),
                PropertyComplianceBuilder.createWithExpiredCerts(propertyIsOccupied = false),
                PropertyComplianceBuilder.createWithInDateCertsAndLowEpcRating(propertyIsOccupied = false),
            )
        val compliantProperties =
            listOf(
                PropertyComplianceBuilder.createWithInDateCerts(propertyIsOccupied = false),
                PropertyComplianceBuilder.createWithCertExemptions(propertyIsOccupied = false),
            )
        val compliances = nonCompliantProperties + compliantProperties

        whenever(
            mockPropertyComplianceRepository.findAllByPropertyOwnership_OwnershipLinks_Landlord_IdAndPropertyOwnership_IsActiveTrue(
                landlord.id,
            ),
        ).thenReturn(compliances)

        val expectedNonCompliantProperties =
            listOf(ComplianceStatusDataModel.fromPropertyCompliance(nonCompliantProperties[1]))

        // Act
        val returnedNonCompliantProperties =
            propertyComplianceService.getNonCompliantPropertiesForLandlord(
                landlord,
                0,
            )

        // Assert
        assertEquals(expectedNonCompliantProperties, returnedNonCompliantProperties.content)
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `getNonCompliantPropertiesForLandlord anchors the provide-later deadline to the registration date when the property was occupied since registration`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val registrationDate = LocalDate.of(2025, 3, 1)
        val propertyOwnership =
            MockLandlordData.createOccupiedPropertyOwnership(
                createdDate = registrationDate.atStartOfDay(DateTimeHelper.UK_ZONE).toInstant(),
                lastOccupiedDate = registrationDate,
            )
        val compliance =
            PropertyComplianceBuilder()
                .withPropertyOwnership(propertyOwnership)
                .withHasGasSupply()
                .withGasSafetyCertProvideLater()
                .build()

        whenever(
            mockPropertyComplianceRepository.findAllByPropertyOwnership_OwnershipLinks_Landlord_IdAndPropertyOwnership_IsActiveTrue(
                landlord.id,
            ),
        ).thenReturn(listOf(compliance))

        // Act
        val returnedProperties = propertyComplianceService.getNonCompliantPropertiesForLandlord(landlord, 0)

        // Assert
        assertEquals(
            registrationDate.plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong()),
            returnedProperties.content.single().provideLaterDeadline,
        )
    }

    @Test
    fun `getNonCompliantPropertiesForLandlord leaves the provide-later deadline null when the property was occupied after registration`() {
        // Arrange
        val landlord = MockLandlordData.createIndividualLandlord()
        val registrationDate = LocalDate.of(2025, 3, 1)
        val propertyOwnership =
            MockLandlordData.createOccupiedPropertyOwnership(
                createdDate = registrationDate.atStartOfDay(DateTimeHelper.UK_ZONE).toInstant(),
                lastOccupiedDate = registrationDate.plusDays(30),
            )
        val compliance =
            PropertyComplianceBuilder()
                .withPropertyOwnership(propertyOwnership)
                .withHasGasSupply()
                .withGasSafetyCertProvideLater()
                .build()

        whenever(
            mockPropertyComplianceRepository.findAllByPropertyOwnership_OwnershipLinks_Landlord_IdAndPropertyOwnership_IsActiveTrue(
                landlord.id,
            ),
        ).thenReturn(listOf(compliance))

        // Act
        val returnedProperties = propertyComplianceService.getNonCompliantPropertiesForLandlord(landlord, 0)

        // Assert
        assertNull(returnedProperties.content.single().provideLaterDeadline)
    }

    private fun createOccupiedPropertyOwnership() =
        MockLandlordData.createPropertyOwnership(
            landlords = mutableSetOf(mockLoggedInLandlord),
            currentNumHouseholds = 1,
            currentNumTenants = 1,
            numberOfBedrooms = 2,
            furnishedStatus = FurnishedStatus.FURNISHED,
            rentFrequency = RentFrequency.MONTHLY,
            rentAmount = BigDecimal("1000"),
        )

    @Nested
    inner class SaveRegistrationComplianceData {
        private val mockPropertyOwnership = MockLandlordData.createPropertyOwnership()
        private val registrationNumberValue = 12345L

        @Test
        fun `creates new compliance record and sets all compliance fields correctly`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            val gasCertIssueDate = LocalDate.of(2024, 6, 15)
            val electricalSafetyExpiryDate = LocalDate.of(2029, 3, 20)
            val epcUrl = "https://epc.example.com/cert/1234"
            val epcExpiryDate = LocalDate.of(2030, 1, 1)
            val epcEnergyRating = "C"
            val epcExemptionReason = EpcExemptionReason.PROTECTED_ARCHITECTURAL_OR_HISTORICAL_MERIT
            val meesExemptionReason = MeesExemptionReason.HIGH_COST

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
                gasSafetyCertIssueDate = gasCertIssueDate,
                electricalSafetyExpiryDate = electricalSafetyExpiryDate,
                epcCertificateUrl = epcUrl,
                epcExpiryDate = epcExpiryDate,
                epcEnergyRating = epcEnergyRating,
                tenancyStartedBeforeEpcExpiry = true,
                epcExemptionReason = epcExemptionReason,
                epcMeesExemptionReason = meesExemptionReason,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            val saved = captor.value
            assertEquals(gasCertIssueDate, saved.gasSafetyCertIssueDate)
            assertEquals(electricalSafetyExpiryDate, saved.electricalSafetyExpiryDate)
            assertEquals(epcUrl, saved.epcUrl)
            assertEquals(epcExpiryDate, saved.epcExpiryDate)
            assertEquals(epcEnergyRating, saved.epcEnergyRating)
            assertEquals(true, saved.tenancyStartedBeforeEpcExpiry)
            assertEquals(epcExemptionReason, saved.epcExemptionReason)
            assertEquals(meesExemptionReason, saved.epcMeesExemptionReason)
        }

        @Test
        fun `sets gasSafetyCertProvideLater when provided`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
                gasSafetyCertProvideLater = true,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertEquals(true, captor.value.gasSafetyCertProvideLater)
        }

        @Test
        fun `sets electricalSafetyCertProvideLater when provided`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
                electricalSafetyCertProvideLater = true,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertEquals(true, captor.value.electricalSafetyCertProvideLater)
        }

        @Test
        fun `sets epcProvideLater when provided`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
                epcProvideLater = true,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertEquals(true, captor.value.epcProvideLater)
        }

        @Test
        fun `provideLater flags default to null when not specified`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            val saved = captor.value
            assertNull(saved.gasSafetyCertProvideLater)
            assertNull(saved.electricalSafetyCertProvideLater)
            assertNull(saved.epcProvideLater)
        }

        @Test
        fun `throws EntityNotFoundException when property ownership is not found`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(null)

            assertThrows<EntityNotFoundException> {
                propertyComplianceService.saveRegistrationComplianceData(
                    registrationNumberValue = registrationNumberValue,
                )
            }
        }

        @Test
        fun `sets hasGasSupply when hasGasSupply is false`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
                hasGasSupply = false,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertEquals(false, captor.value.hasGasSupply)
        }

        @Test
        fun `sets hasGasSupply when hasGasSupply is true`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
                hasGasSupply = true,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertEquals(true, captor.value.hasGasSupply)
        }

        @Test
        fun `sets up virus scan callbacks for file uploads`() {
            val gasUpload = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val electricalUpload = FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag2", "v2")

            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload)
            whenever(fileUploadRepository.getReferenceById(20L)).thenReturn(electricalUpload)

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
                gasSafetyFileUploadIds = listOf(10L),
                electricalSafetyFileUploadIds = listOf(20L),
                electricalCertType = CertificateType.Eicr,
            )

            verify(mockVirusScanCallbackService).updateCallbacksToOwner(
                10L,
                mockPropertyOwnership.id,
                CertificateType.GasSafetyCert,
            )
            verify(mockVirusScanCallbackService).updateCallbacksToOwner(
                20L,
                mockPropertyOwnership.id,
                CertificateType.Eicr,
            )
        }

        @Test
        fun `does not set up virus scan callbacks when no file uploads provided`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
            )

            verify(mockVirusScanCallbackService, never()).updateCallbacksToOwner(any<Long>(), any(), any())
        }

        @Test
        fun `throws IllegalArgumentException when electrical uploads are present but electricalCertType is null`() {
            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L))
                .thenReturn(FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag1", "v1"))

            assertThrows<IllegalArgumentException> {
                propertyComplianceService.saveRegistrationComplianceData(
                    registrationNumberValue = registrationNumberValue,
                    electricalSafetyFileUploadIds = listOf(10L),
                    electricalCertType = null,
                )
            }
        }

        @Test
        fun `attaches file uploads to compliance record`() {
            val gasUpload1 = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val gasUpload2 = FileUpload(FileUploadStatus.QUARANTINED, "gas-2", "pdf", "etag2", "v2")
            val electricalUpload1 = FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag3", "v3")

            whenever(mockPropertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue))
                .thenReturn(mockPropertyOwnership)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload1)
            whenever(fileUploadRepository.getReferenceById(20L)).thenReturn(gasUpload2)
            whenever(fileUploadRepository.getReferenceById(30L)).thenReturn(electricalUpload1)

            propertyComplianceService.saveRegistrationComplianceData(
                registrationNumberValue = registrationNumberValue,
                gasSafetyFileUploadIds = listOf(10L, 20L),
                electricalSafetyFileUploadIds = listOf(30L),
                electricalCertType = CertificateType.Eicr,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertEquals(listOf(gasUpload1, gasUpload2), captor.value.gasSafetyFileUploads)
            assertEquals(listOf(electricalUpload1), captor.value.electricalSafetyFileUploads)
        }
    }

    @Nested
    inner class UpdateGasSafety {
        @Test
        fun `updates gas safety fields on the compliance record`() {
            setMockPrincipal()
            val gasUpload1 = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val gasUpload2 = FileUpload(FileUploadStatus.QUARANTINED, "gas-2", "pdf", "etag2", "v2")
            val issueDate = LocalDate.of(2025, 6, 15)
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload1)
            whenever(fileUploadRepository.getReferenceById(20L)).thenReturn(gasUpload2)

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = listOf(10L, 20L),
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            val saved = captor.value
            assertEquals(true, saved.hasGasSupply)
            assertEquals(issueDate, saved.gasSafetyCertIssueDate)
            assertEquals(listOf(gasUpload1, gasUpload2), saved.gasSafetyFileUploads)
        }

        @Test
        fun `sets hasGasSupply when hasGasSupply is false`() {
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = false,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertEquals(false, captor.value.hasGasSupply)
        }

        @Test
        fun `resets gasSafetyCertProvideLater to null when gas safety is updated`() {
            setMockPrincipal()
            val compliance = createComplianceWithLastModifiedDate()
            compliance.gasSafetyCertProvideLater = true

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = LocalDate.of(2025, 6, 15),
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertNull(captor.value.gasSafetyCertProvideLater)
        }

        @Test
        fun `sets up virus scan callbacks for gas safety uploads`() {
            setMockPrincipal()
            val gasUpload = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload)

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = LocalDate.of(2025, 6, 15),
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockVirusScanCallbackService).updateCallbacksToOwner(
                10L,
                propertyOwnershipId,
                CertificateType.GasSafetyCert,
            )
        }

        @Test
        fun `does not set up virus scan callbacks for electrical safety`() {
            setMockPrincipal()
            val gasUpload = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload)

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = LocalDate.of(2025, 6, 15),
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockVirusScanCallbackService, never()).updateCallbacksToOwner(
                any<Long>(),
                any(),
                eq(CertificateType.Eicr),
            )
        }

        @Test
        fun `throws UpdateConflictException when lastModifiedDate does not match`() {
            val compliance = createComplianceWithLastModifiedDate(Instant.parse("2025-06-01T00:00:00Z"))

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)

            assertThrows<UpdateConflictException> {
                propertyComplianceService.updateGasSafety(
                    propertyOwnershipId = propertyOwnershipId,
                    initialLastModifiedDate = initialLastModifiedDate,
                    hasGasSupply = true,
                )
            }

            verify(mockPropertyComplianceRepository, never()).save(any<PropertyCompliance>())
        }

        @Test
        fun `throws EntityNotFoundException when no compliance record exists`() {
            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(null)

            assertThrows<EntityNotFoundException> {
                propertyComplianceService.updateGasSafety(
                    propertyOwnershipId = propertyOwnershipId,
                    initialLastModifiedDate = initialLastModifiedDate,
                    hasGasSupply = true,
                )
            }
        }

        @Test
        fun `does not set up virus scan callbacks when no file uploads provided`() {
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = false,
            )

            verify(mockVirusScanCallbackService, never()).updateCallbacksToOwner(any<Long>(), any(), any())
        }

        @Test
        fun `sends valid gas safety confirmation email when certificate is uploaded and not expired`() {
            setMockPrincipal()
            val gasUpload = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val compliance = createComplianceWithLastModifiedDate()
            val issueDate = LocalDate.now()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload)

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = mockPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(mockPropertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        expiryDate = issueDate.plusYears(1).format(dateFormatter),
                    ),
                ),
            )
        }

        @Test
        fun `sends the acting user's compliance confirmation to their current user details rather than the landlord's`() {
            val actingLandlord = mockLoggedInLandlord
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(actingLandlord))
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(actingLandlord)
            whenever(mockCurrentUserService.getRegisteredUserDetails())
                .thenReturn(
                    CurrentUserService.RegisteredUserDetails(name = "Sub User", email = "sub.user@example.com"),
                )
            val gasUpload = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val compliance = MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = propertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)
            val issueDate = LocalDate.now()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload)

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq("sub.user@example.com"),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = "Sub User",
                        multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        expiryDate = issueDate.plusYears(1).format(dateFormatter),
                    ),
                ),
            )
        }

        @Test
        fun `notifies other landlords with joint landlord email and does not send them the confirmation email`() {
            val otherLandlord =
                MockLandlordData.createIndividualLandlord(
                    baseUser = MockLandlordData.createPrsdbUser("other-base-user-id"),
                    name = "Other Landlord",
                    email = "other@example.com",
                )
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(mockLoggedInLandlord, otherLandlord))
            setMockPrincipal()
            val gasUpload = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val compliance = MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = propertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)
            val issueDate = LocalDate.now()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload)

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmailToLandlord(
                eq(otherLandlord),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = otherLandlord.name,
                        multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        expiryDate = issueDate.plusYears(1).format(dateFormatter),
                        isJointLandlord = true,
                    ),
                ),
            )
        }

        @Test
        fun `sends letting agent compliance notification to all landlords when a letting agent adds a certificate`() {
            val landlordOne = mockLoggedInLandlord
            val landlordTwo =
                MockLandlordData.createIndividualLandlord(
                    baseUser = MockLandlordData.createPrsdbUser("other-base-user-id"),
                    name = "Other Landlord",
                    email = "other@example.com",
                )
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlordOne, landlordTwo))
            val compliance = MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = propertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)
            val issueDate = LocalDate.now()

            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)
            whenever(mockPropertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnership.id)).thenReturn(true)
            whenever(mockAbsoluteUrlProvider.buildPropertyDetailsUri(propertyOwnership.id)).thenReturn(URI("http://property"))
            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId)).thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>())).thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L))
                .thenReturn(FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1"))

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockLettingAgentComplianceUpdateSender).sendEmailToLandlord(
                eq(landlordOne),
                eq(
                    LettingAgentComplianceUpdateNotificationEmail(
                        recipientName = landlordOne.name,
                        multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber).toString(),
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        expiryDate = issueDate.plusYears(1).format(dateFormatter),
                        propertyRecordUrl = "http://property",
                    ),
                ),
            )
            verify(mockLettingAgentComplianceUpdateSender).sendEmailToLandlord(eq(landlordTwo), any())
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmail(any(), any())
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmailToLandlord(any(), any())
        }

        @Test
        fun `sends a letting agent notification and an expiry email when a letting agent uploads an expired certificate`() {
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(mockLoggedInLandlord))
            val compliance = MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = propertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)
            val expiredIssueDate = LocalDate.now().minusYears(2)

            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)
            whenever(mockPropertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnership.id)).thenReturn(true)
            whenever(mockAbsoluteUrlProvider.buildPropertyDetailsUri(propertyOwnership.id)).thenReturn(URI("http://property"))
            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId)).thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>())).thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L))
                .thenReturn(FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1"))

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = expiredIssueDate,
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockLettingAgentComplianceUpdateSender).sendEmailToLandlord(
                eq(mockLoggedInLandlord),
                eq(
                    LettingAgentComplianceUpdateNotificationEmail(
                        recipientName = mockLoggedInLandlord.name,
                        multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber).toString(),
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        expiryDate = expiredIssueDate.plusYears(1).format(dateFormatter),
                        propertyRecordUrl = "http://property",
                    ),
                ),
            )
            verify(mockComplianceUpdateConfirmationSender).sendEmailToLandlord(
                eq(mockLoggedInLandlord),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_UNOCCUPIED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                    ),
                ),
            )
        }

        @Test
        fun `does not send an expiry email when a letting agent uploads a valid certificate`() {
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(mockLoggedInLandlord))
            val compliance = MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = propertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)

            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)
            whenever(mockPropertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnership.id)).thenReturn(true)
            whenever(mockAbsoluteUrlProvider.buildPropertyDetailsUri(propertyOwnership.id)).thenReturn(URI("http://property"))
            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId)).thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>())).thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L))
                .thenReturn(FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1"))

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = LocalDate.now(),
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockLettingAgentComplianceUpdateSender).sendEmailToLandlord(eq(mockLoggedInLandlord), any())
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmail(any(), any())
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmailToLandlord(any(), any())
        }

        @Test
        fun `notifies other landlords with the expiry joint landlord email when an updated certificate is expired`() {
            val otherLandlord =
                MockLandlordData.createIndividualLandlord(
                    baseUser = MockLandlordData.createPrsdbUser("other-base-user-id"),
                    name = "Other Landlord",
                    email = "other@example.com",
                )
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(mockLoggedInLandlord, otherLandlord))
            setMockPrincipal()
            val gasUpload = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val compliance = MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = propertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload)

            val issueDate = LocalDate.now().minusYears(2)
            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmailToLandlord(
                eq(otherLandlord),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = otherLandlord.name,
                        multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_UNOCCUPIED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        isJointLandlord = true,
                    ),
                ),
            )
        }

        @Test
        fun `sends expired unoccupied gas safety confirmation email when certificate is expired and property is unoccupied`() {
            setMockPrincipal()
            val gasUpload = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload)

            val issueDate = LocalDate.now().minusYears(2)
            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = mockPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(mockPropertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_UNOCCUPIED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                    ),
                ),
            )
        }

        @Test
        fun `sends expired occupied gas safety confirmation email when certificate is expired and property is occupied`() {
            setMockPrincipal()
            val gasUpload = FileUpload(FileUploadStatus.QUARANTINED, "gas-1", "pdf", "etag1", "v1")
            val occupiedPropertyOwnership = createOccupiedPropertyOwnership()
            val compliance =
                MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = occupiedPropertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(gasUpload)

            val issueDate = LocalDate.now().minusYears(2)
            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = listOf(10L),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = occupiedPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(
                                occupiedPropertyOwnership.registrationNumber,
                            ),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_OCCUPIED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        deadlineDate =
                            LocalDate
                                .now()
                                .plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong())
                                .format(dateFormatter),
                    ),
                ),
            )
        }

        @Test
        fun `sends expired unoccupied gas safety confirmation email when certificate is expired and no uploads are provided`() {
            setMockPrincipal()
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            val issueDate = LocalDate.now().minusYears(2)
            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = emptyList(),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = mockPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(mockPropertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_UNOCCUPIED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                    ),
                ),
            )
        }

        @Test
        fun `sends expired occupied gas safety confirmation email when certificate is expired and no uploads are provided`() {
            setMockPrincipal()
            val occupiedPropertyOwnership = createOccupiedPropertyOwnership()
            val compliance =
                MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = occupiedPropertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            val issueDate = LocalDate.now().minusYears(2)
            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = true,
                gasSafetyCertIssueDate = issueDate,
                gasSafetyCertUploadIds = emptyList(),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = occupiedPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(
                                occupiedPropertyOwnership.registrationNumber,
                            ),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_OCCUPIED,
                        certificateType = "gas safety certificate",
                        certificateTypeLabel = "Gas safety certificate",
                        deadlineDate =
                            LocalDate
                                .now()
                                .plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong())
                                .format(dateFormatter),
                    ),
                ),
            )
        }

        @Test
        fun `does not send confirmation email when no gas safety certificate issue date is provided`() {
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateGasSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                hasGasSupply = false,
            )

            verify(mockComplianceUpdateConfirmationSender, never()).sendEmail(any(), any())
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmailToLandlord(any(), any())
        }
    }

    @Nested
    inner class UpdateElectricalSafety {
        @Test
        fun `updates electrical safety fields on the compliance record`() {
            setMockPrincipal()
            val eicrUpload1 = FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag1", "v1")
            val eicrUpload2 = FileUpload(FileUploadStatus.QUARANTINED, "eicr-2", "pdf", "etag2", "v2")
            val expiryDate = LocalDate.of(2030, 3, 20)
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(eicrUpload1)
            whenever(fileUploadRepository.getReferenceById(20L)).thenReturn(eicrUpload2)

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = expiryDate,
                electricalSafetyCertUploadIds = listOf(10L, 20L),
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            val saved = captor.value
            assertEquals(expiryDate, saved.electricalSafetyExpiryDate)
            assertEquals(CertificateType.Eicr, saved.electricalCertType)
            assertEquals(listOf(eicrUpload1, eicrUpload2), saved.electricalSafetyFileUploads)
        }

        @Test
        fun `sets up virus scan callbacks for electrical safety uploads`() {
            setMockPrincipal()
            val eicrUpload = FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag1", "v1")
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(eicrUpload)

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = LocalDate.of(2030, 3, 20),
                electricalSafetyCertUploadIds = listOf(10L),
            )

            verify(mockVirusScanCallbackService).updateCallbacksToOwner(10L, propertyOwnershipId, CertificateType.Eicr)
        }

        @Test
        fun `resets electricalSafetyCertProvideLater to null when electrical safety is updated`() {
            setMockPrincipal()
            val compliance = createComplianceWithLastModifiedDate()
            compliance.electricalSafetyCertProvideLater = true

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = LocalDate.of(2030, 3, 20),
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertNull(captor.value.electricalSafetyCertProvideLater)
        }

        @Test
        fun `does not set up virus scan callbacks for gas safety`() {
            setMockPrincipal()
            val eicrUpload = FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag1", "v1")
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(eicrUpload)

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = LocalDate.of(2030, 3, 20),
                electricalSafetyCertUploadIds = listOf(10L),
            )

            verify(mockVirusScanCallbackService, never()).updateCallbacksToOwner(
                any<Long>(),
                any(),
                eq(CertificateType.GasSafetyCert),
            )
        }

        @Test
        fun `throws UpdateConflictException when lastModifiedDate does not match`() {
            val compliance = createComplianceWithLastModifiedDate(Instant.parse("2025-06-01T00:00:00Z"))

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)

            assertThrows<UpdateConflictException> {
                propertyComplianceService.updateElectricalSafety(
                    propertyOwnershipId = propertyOwnershipId,
                    initialLastModifiedDate = initialLastModifiedDate,
                    electricalCertType = CertificateType.Eicr,
                )
            }

            verify(mockPropertyComplianceRepository, never()).save(any<PropertyCompliance>())
        }

        @Test
        fun `throws EntityNotFoundException when no compliance record exists`() {
            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(null)

            assertThrows<EntityNotFoundException> {
                propertyComplianceService.updateElectricalSafety(
                    propertyOwnershipId = propertyOwnershipId,
                    initialLastModifiedDate = initialLastModifiedDate,
                    electricalCertType = CertificateType.Eicr,
                )
            }
        }

        @Test
        fun `does not set up virus scan callbacks when no file uploads provided`() {
            setMockPrincipal()
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = LocalDate.of(2030, 3, 20),
            )

            verify(mockVirusScanCallbackService, never()).updateCallbacksToOwner(any<Long>(), any(), any())
        }

        @Test
        fun `clears electrical safety fields when no cert type provided`() {
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            val saved = captor.value
            assertNull(saved.electricalSafetyExpiryDate)
            assertNull(saved.electricalCertType)
            assertTrue(saved.electricalSafetyFileUploads.isEmpty())
        }

        @Test
        fun `sends valid electrical safety confirmation email when certificate is uploaded and not expired`() {
            setMockPrincipal()
            val eicrUpload = FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag1", "v1")
            val compliance = createComplianceWithLastModifiedDate()
            val expiryDate = LocalDate.now().plusYears(1)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(eicrUpload)

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = expiryDate,
                electricalSafetyCertUploadIds = listOf(10L),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = mockPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(mockPropertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED,
                        certificateType = "electrical safety certificate",
                        certificateTypeLabel = "Electrical safety certificate (EICR)",
                        expiryDate = expiryDate.format(dateFormatter),
                    ),
                ),
            )
        }

        @Test
        fun `sends letting agent electrical notification with a plain certificate label when a letting agent adds the certificate`() {
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(mockLoggedInLandlord))
            val compliance = MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = propertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)
            val expiryDate = LocalDate.now().plusYears(1)

            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)
            whenever(mockPropertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnership.id)).thenReturn(true)
            whenever(mockAbsoluteUrlProvider.buildPropertyDetailsUri(propertyOwnership.id)).thenReturn(URI("http://property"))
            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId)).thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>())).thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L))
                .thenReturn(FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag1", "v1"))

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = expiryDate,
                electricalSafetyCertUploadIds = listOf(10L),
            )

            verify(mockLettingAgentComplianceUpdateSender).sendEmailToLandlord(
                eq(mockLoggedInLandlord),
                eq(
                    LettingAgentComplianceUpdateNotificationEmail(
                        recipientName = mockLoggedInLandlord.name,
                        multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber).toString(),
                        certificateType = "electrical safety certificate",
                        certificateTypeLabel = "Electrical safety certificate",
                        expiryDate = expiryDate.format(dateFormatter),
                        propertyRecordUrl = "http://property",
                    ),
                ),
            )
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmail(any(), any())
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmailToLandlord(any(), any())
        }

        @Test
        fun `sends expired unoccupied electrical safety confirmation email when certificate is expired and property is unoccupied`() {
            setMockPrincipal()
            val eicrUpload = FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag1", "v1")
            val compliance = createComplianceWithLastModifiedDate()
            val expiryDate = LocalDate.now().minusDays(1)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(eicrUpload)

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = expiryDate,
                electricalSafetyCertUploadIds = listOf(10L),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = mockPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(mockPropertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_UNOCCUPIED,
                        certificateType = "electrical safety certificate",
                        certificateTypeLabel = "Electrical safety certificate (EICR)",
                    ),
                ),
            )
        }

        @Test
        fun `sends expired occupied electrical safety confirmation email when certificate is expired and property is occupied`() {
            setMockPrincipal()
            val eicrUpload = FileUpload(FileUploadStatus.QUARANTINED, "eicr-1", "pdf", "etag1", "v1")
            val occupiedPropertyOwnership = createOccupiedPropertyOwnership()
            val compliance =
                MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = occupiedPropertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)
            val expiryDate = LocalDate.now().minusDays(1)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }
            whenever(fileUploadRepository.getReferenceById(10L)).thenReturn(eicrUpload)

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = expiryDate,
                electricalSafetyCertUploadIds = listOf(10L),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = occupiedPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(
                                occupiedPropertyOwnership.registrationNumber,
                            ),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_OCCUPIED,
                        certificateType = "electrical safety certificate",
                        certificateTypeLabel = "Electrical safety certificate (EICR)",
                        deadlineDate =
                            LocalDate
                                .now()
                                .plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong())
                                .format(dateFormatter),
                    ),
                ),
            )
        }

        @Test
        fun `sends expired unoccupied electrical safety confirmation email when certificate is expired and no uploads are provided`() {
            setMockPrincipal()
            val compliance = createComplianceWithLastModifiedDate()
            val expiryDate = LocalDate.now().minusDays(1)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = expiryDate,
                electricalSafetyCertUploadIds = emptyList(),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = mockPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(mockPropertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_UNOCCUPIED,
                        certificateType = "electrical safety certificate",
                        certificateTypeLabel = "Electrical safety certificate (EICR)",
                    ),
                ),
            )
        }

        @Test
        fun `sends expired occupied electrical safety confirmation email when certificate is expired and no uploads are provided`() {
            setMockPrincipal()
            val occupiedPropertyOwnership = createOccupiedPropertyOwnership()
            val compliance =
                MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = occupiedPropertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)
            val expiryDate = LocalDate.now().minusDays(1)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                electricalCertType = CertificateType.Eicr,
                electricalSafetyExpiryDate = expiryDate,
                electricalSafetyCertUploadIds = emptyList(),
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = occupiedPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(
                                occupiedPropertyOwnership.registrationNumber,
                            ),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_OCCUPIED,
                        certificateType = "electrical safety certificate",
                        certificateTypeLabel = "Electrical safety certificate (EICR)",
                        deadlineDate =
                            LocalDate
                                .now()
                                .plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong())
                                .format(dateFormatter),
                    ),
                ),
            )
        }

        @Test
        fun `does not send confirmation email when no electrical safety expiry date is provided`() {
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateElectricalSafety(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
            )

            verify(mockComplianceUpdateConfirmationSender, never()).sendEmail(any(), any())
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmailToLandlord(any(), any())
        }
    }

    @Nested
    inner class UpdateEpc {
        @Test
        fun `updates EPC fields, mees exemption and tenancy check on the compliance record`() {
            setMockPrincipal()
            val epcUrl = "https://example.com/epc/1234-5678-9012-3456-7890"
            val expiryDate = DateTimeHelper().getCurrentDateInUK().minus(5, DAY).toJavaLocalDate()
            val energyRating = "F"
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateEpc(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                epcCertificateUrl = epcUrl,
                epcExpiryDate = expiryDate,
                epcEnergyRating = energyRating,
                tenancyStartedBeforeEpcExpiry = true,
                epcMeesExemptionReason = MeesExemptionReason.HIGH_COST,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            val saved = captor.value
            assertEquals(epcUrl, saved.epcUrl)
            assertEquals(expiryDate, saved.epcExpiryDate)
            assertEquals(energyRating, saved.epcEnergyRating)
            assertEquals(true, saved.tenancyStartedBeforeEpcExpiry)
            assertNull(saved.epcExemptionReason)
            assertEquals(MeesExemptionReason.HIGH_COST, saved.epcMeesExemptionReason)
        }

        @Test
        fun `updates EPC fields with an Epc exemption reason`() {
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateEpc(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                epcExemptionReason = EpcExemptionReason.DUE_FOR_DEMOLITION,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            val saved = captor.value
            assertNull(saved.epcUrl)
            assertNull(saved.epcExpiryDate)
            assertNull(saved.epcEnergyRating)
            assertNull(saved.tenancyStartedBeforeEpcExpiry)
            assertEquals(EpcExemptionReason.DUE_FOR_DEMOLITION, saved.epcExemptionReason)
            assertNull(saved.epcMeesExemptionReason)
        }

        @Test
        fun `clears EPC fields when EPC is missing`() {
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateEpc(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            val saved = captor.value
            assertNull(saved.epcUrl)
            assertNull(saved.epcExpiryDate)
            assertNull(saved.epcEnergyRating)
            assertNull(saved.tenancyStartedBeforeEpcExpiry)
            assertNull(saved.epcExemptionReason)
            assertNull(saved.epcMeesExemptionReason)
        }

        @Test
        fun `resets epcProvideLater to null when EPC is updated`() {
            setMockPrincipal()
            val compliance = createComplianceWithLastModifiedDate()
            compliance.epcProvideLater = true

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateEpc(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                epcCertificateUrl = "https://example.com/epc/1234-5678-9012-3456-7890",
                epcExpiryDate = LocalDate.now().plusYears(5),
                epcEnergyRating = "C",
            )

            val captor = captor<PropertyCompliance>()
            verify(mockPropertyComplianceRepository).save(captor.capture())
            assertNull(captor.value.epcProvideLater)
        }

        @Test
        fun `throws UpdateConflictException when lastModifiedDate does not match`() {
            val compliance = createComplianceWithLastModifiedDate(Instant.parse("2025-06-01T00:00:00Z"))

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)

            assertThrows<UpdateConflictException> {
                propertyComplianceService.updateEpc(
                    propertyOwnershipId = propertyOwnershipId,
                    initialLastModifiedDate = initialLastModifiedDate,
                    epcCertificateUrl = "https://example.com/epc/1234",
                )
            }

            verify(mockPropertyComplianceRepository, never()).save(any<PropertyCompliance>())
        }

        @Test
        fun `throws EntityNotFoundException when no compliance record exists`() {
            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(null)

            assertThrows<EntityNotFoundException> {
                propertyComplianceService.updateEpc(
                    propertyOwnershipId = propertyOwnershipId,
                    initialLastModifiedDate = initialLastModifiedDate,
                    epcCertificateUrl = "https://example.com/epc/1234",
                )
            }
        }

        @Test
        fun `sends letting agent EPC notification when a letting agent adds the certificate`() {
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(mockLoggedInLandlord))
            val compliance = MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = propertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)
            val expiryDate = LocalDate.now().plusYears(5)

            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)
            whenever(mockPropertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnership.id)).thenReturn(true)
            whenever(mockAbsoluteUrlProvider.buildPropertyDetailsUri(propertyOwnership.id)).thenReturn(URI("http://property"))
            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId)).thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>())).thenAnswer { it.arguments[0] }

            propertyComplianceService.updateEpc(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                epcCertificateUrl = "https://example.com/epc/1234-5678-9012-3456-7890",
                epcExpiryDate = expiryDate,
                epcEnergyRating = "C",
            )

            verify(mockLettingAgentComplianceUpdateSender).sendEmailToLandlord(
                eq(mockLoggedInLandlord),
                eq(
                    LettingAgentComplianceUpdateNotificationEmail(
                        recipientName = mockLoggedInLandlord.name,
                        multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber).toString(),
                        certificateType = "energy performance certificate (EPC)",
                        certificateTypeLabel = "Energy performance certificate (EPC)",
                        expiryDate = expiryDate.format(dateFormatter),
                        propertyRecordUrl = "http://property",
                    ),
                ),
            )
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmail(any(), any())
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmailToLandlord(any(), any())
        }

        @Test
        fun `sends valid EPC confirmation email when EPC URL is provided and not expired`() {
            setMockPrincipal()
            val compliance = createComplianceWithLastModifiedDate()
            val expiryDate = LocalDate.now().plusYears(5)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateEpc(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                epcCertificateUrl = "https://example.com/epc/1234-5678-9012-3456-7890",
                epcExpiryDate = expiryDate,
                epcEnergyRating = "C",
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = mockPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(mockPropertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED,
                        certificateType = "energy performance certificate (EPC)",
                        certificateTypeLabel = "Energy performance certificate (EPC)",
                        expiryDate = expiryDate.format(dateFormatter),
                    ),
                ),
            )
        }

        @Test
        fun `sends expired unoccupied EPC confirmation email when EPC is expired and property is unoccupied`() {
            setMockPrincipal()
            val compliance = createComplianceWithLastModifiedDate()
            val expiryDate = LocalDate.now().minusDays(1)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateEpc(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                epcCertificateUrl = "https://example.com/epc/1234-5678-9012-3456-7890",
                epcExpiryDate = expiryDate,
                epcEnergyRating = "C",
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = mockPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(mockPropertyOwnership.registrationNumber),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_UNOCCUPIED,
                        certificateType = "energy performance certificate (EPC)",
                        certificateTypeLabel = "Energy performance certificate (EPC)",
                    ),
                ),
            )
        }

        @Test
        fun `sends expired occupied EPC confirmation email when EPC is expired and property is occupied`() {
            setMockPrincipal()
            val occupiedPropertyOwnership = createOccupiedPropertyOwnership()
            val compliance =
                MockPropertyComplianceData.createPropertyCompliance(propertyOwnership = occupiedPropertyOwnership)
            ReflectionTestUtils.setField(compliance, "createdDate", Instant.EPOCH)
            ReflectionTestUtils.setField(compliance, "lastModifiedDate", initialLastModifiedDate)
            val expiryDate = LocalDate.now().minusDays(1)

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateEpc(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
                epcCertificateUrl = "https://example.com/epc/1234-5678-9012-3456-7890",
                epcExpiryDate = expiryDate,
                epcEnergyRating = "C",
            )

            verify(mockComplianceUpdateConfirmationSender).sendEmail(
                eq(mockLoggedInLandlord.email),
                eq(
                    ComplianceUpdateConfirmationEmail(
                        landlordName = mockLoggedInLandlord.name,
                        multiLineAddress = occupiedPropertyOwnership.address.toMultiLineAddress(),
                        registrationNumber =
                            RegistrationNumberDataModel.fromRegistrationNumber(
                                occupiedPropertyOwnership.registrationNumber,
                            ),
                        dashboardUrl = URI("https://test.example.com"),
                        newCertificateUrl = URI("https://test.example.com/compliance"),
                        complianceUpdateType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_EPC_OCCUPIED,
                        certificateType = "energy performance certificate (EPC)",
                        certificateTypeLabel = "Energy performance certificate (EPC)",
                    ),
                ),
            )
        }

        @Test
        fun `does not send confirmation email when no EPC URL is provided`() {
            val compliance = createComplianceWithLastModifiedDate()

            whenever(mockPropertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId))
                .thenReturn(compliance)
            whenever(mockPropertyComplianceRepository.save(any<PropertyCompliance>()))
                .thenAnswer { it.arguments[0] }

            propertyComplianceService.updateEpc(
                propertyOwnershipId = propertyOwnershipId,
                initialLastModifiedDate = initialLastModifiedDate,
            )

            verify(mockComplianceUpdateConfirmationSender, never()).sendEmail(any(), any())
            verify(mockComplianceUpdateConfirmationSender, never()).sendEmailToLandlord(any(), any())
        }
    }
}

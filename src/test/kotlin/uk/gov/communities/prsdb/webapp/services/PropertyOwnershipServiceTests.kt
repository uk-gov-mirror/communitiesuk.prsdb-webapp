package uk.gov.communities.prsdb.webapp.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor.captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.internal.matchers.apachecommons.ReflectionEquals
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.QueryTimeoutException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.server.ResponseStatusException
import uk.gov.communities.prsdb.webapp.config.interceptors.BackLinkInterceptor.Companion.overrideBackLinkForUrl
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.REGISTERED_PROPERTIES_FRAGMENT
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.entity.IndividualLandlord
import uk.gov.communities.prsdb.webapp.database.entity.Landlord
import uk.gov.communities.prsdb.webapp.database.entity.License
import uk.gov.communities.prsdb.webapp.database.entity.LocalCouncil
import uk.gov.communities.prsdb.webapp.database.entity.PropertyCompliance
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.entity.RegistrationNumber
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.exceptions.RepositoryQueryTimeoutException
import uk.gov.communities.prsdb.webapp.exceptions.UpdateConflictException
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.searchResultModels.PropertySearchResultViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.RegisteredPropertyLandlordViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.RegisteredPropertyLocalCouncilViewModel
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLocalCouncilData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockPrsdbUserData
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class PropertyOwnershipServiceTests {
    @Mock
    private lateinit var mockPropertyOwnershipRepository: PropertyOwnershipRepository

    @Mock
    private lateinit var mockRegistrationNumberService: RegistrationNumberService

    @Mock
    private lateinit var mockLocalCouncilDataService: LocalCouncilDataService

    @Mock
    private lateinit var mockLicenseService: LicenseService

    @Mock
    private lateinit var mockBackUrlStorageService: BackUrlStorageService

    @Mock
    private lateinit var mockEmailService: JointLandlordOtherLandlordLeftEmailService

    @Mock
    private lateinit var mockUserToLandlordService: UserToLandlordService

    @Mock
    private lateinit var mockLettingAgentAccessService: LettingAgentAccessService

    @Mock
    private lateinit var mockLettingAgentAccessRepository: LettingAgentAccessRepository

    @Mock
    private lateinit var mockFeatureFlagManager: FeatureFlagManager

    @Mock
    private lateinit var mockAddressService: AddressService

    @InjectMocks
    private lateinit var propertyOwnershipService: PropertyOwnershipService

    @BeforeEach
    fun setUpAddressSnapshots() {
        lenient().`when`(mockAddressService.createAddressSnapshot(any())).thenAnswer {
            Address(it.getArgument<AddressDataModel>(0).copy(uprn = null))
        }
    }

    @Test
    fun `createPropertyOwnership creates a property ownership`() {
        // Arrange
        val ownershipType = OwnershipType.FREEHOLD
        val households = 1
        val tenants = 2
        val isOccupied = true
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyBuildType = PropertyType.OTHER
        val customPropertyType = "End terrace"
        val address = MockLandlordData.createAddress("11 Example Road, EG1 2AB")
        val license = License()
        val correspondenceAddress = MockLandlordData.createAddress("12 Contact Road, EG1 2AC")
        val correspondenceModel = AddressDataModel.fromAddress(correspondenceAddress)
        val correspondenceEmail = "chosen@example.com"
        whenever(mockAddressService.createAddressSnapshot(correspondenceModel)).thenReturn(correspondenceAddress)
        val numberOfBedrooms = 1
        val billsIncludedList = "Electricity, Water"
        val customBillsIncluded = "Internet"
        val furnishedStatus = FurnishedStatus.FURNISHED
        val rentFrequency = RentFrequency.OTHER
        val customRentFrequency = "Fortnightly"
        val rentAmount = 123.toBigDecimal()

        val expectedPropertyOwnership =
            PropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = households,
                currentNumTenants = tenants,
                isOccupied = isOccupied,
                registrationNumber = registrationNumber,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyBuildType,
                customPropertyType = customPropertyType,
                address = address,
                license = license,
                correspondenceEmail = correspondenceEmail,
                correspondenceAddress = correspondenceAddress,
                numBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
                lastOccupiedDate = LocalDate.now(),
            )

        whenever(mockRegistrationNumberService.createRegistrationNumber(RegistrationNumberType.PROPERTY)).thenReturn(
            registrationNumber,
        )
        whenever(mockPropertyOwnershipRepository.save(any<PropertyOwnership>())).thenReturn(
            expectedPropertyOwnership,
        )

        // Act
        propertyOwnershipService.createPropertyOwnership(
            ownershipType = ownershipType,
            isOccupied = isOccupied,
            numberOfHouseholds = households,
            numberOfPeople = tenants,
            landlords = mutableSetOf(landlord),
            propertyBuildType = propertyBuildType,
            customPropertyType = customPropertyType,
            address = address,
            license = license,
            correspondenceEmail = correspondenceEmail,
            correspondenceAddressModel = correspondenceModel,
            numBedrooms = numberOfBedrooms,
            billsIncludedList = billsIncludedList,
            customBillsIncluded = customBillsIncluded,
            furnishedStatus = furnishedStatus,
            rentFrequency = rentFrequency,
            customRentFrequency = customRentFrequency,
            rentAmount = rentAmount,
        )

        // Assert
        val propertyOwnershipCaptor = captor<PropertyOwnership>()
        verify(mockPropertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
        assertTrue(ReflectionEquals(expectedPropertyOwnership, "ownershipLinks").matches(propertyOwnershipCaptor.value))
        assertEquals(setOf(landlord), propertyOwnershipCaptor.value.landlords)
    }

    @Test
    fun `createPropertyOwnership can create a property ownership with no license`() {
        val ownershipType = OwnershipType.FREEHOLD
        val households = 1
        val tenants = 2
        val isOccupied = true
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyBuildType = PropertyType.OTHER
        val customPropertyType = "End terrace"
        val address = MockLandlordData.createAddress("11 Example Road, EG1 2AB")
        val numberOfBedrooms = 1
        val correspondenceAddress = Address(AddressDataModel.fromAddress(address).copy(uprn = null), address.localCouncil)
        whenever(mockAddressService.createAddressSnapshot(AddressDataModel.fromAddress(address))).thenReturn(correspondenceAddress)
        val billsIncludedList = "Electricity, Water"
        val customBillsIncluded = "Internet"
        val furnishedStatus = FurnishedStatus.FURNISHED
        val rentFrequency = RentFrequency.OTHER
        val customRentFrequency = "Fortnightly"
        val rentAmount = 123.toBigDecimal()

        val expectedPropertyOwnership =
            PropertyOwnership(
                ownershipType = ownershipType,
                currentNumHouseholds = households,
                currentNumTenants = tenants,
                isOccupied = isOccupied,
                registrationNumber = registrationNumber,
                landlords = mutableSetOf(landlord),
                propertyBuildType = propertyBuildType,
                customPropertyType = customPropertyType,
                address = address,
                license = null,
                correspondenceEmail = landlord.email,
                correspondenceAddress = correspondenceAddress,
                numBedrooms = numberOfBedrooms,
                billsIncludedList = billsIncludedList,
                customBillsIncluded = customBillsIncluded,
                furnishedStatus = furnishedStatus,
                rentFrequency = rentFrequency,
                customRentFrequency = customRentFrequency,
                rentAmount = rentAmount,
                lastOccupiedDate = LocalDate.now(),
            )

        whenever(mockRegistrationNumberService.createRegistrationNumber(RegistrationNumberType.PROPERTY)).thenReturn(
            registrationNumber,
        )
        whenever(mockPropertyOwnershipRepository.save(any<PropertyOwnership>())).thenReturn(
            expectedPropertyOwnership,
        )

        propertyOwnershipService.createPropertyOwnership(
            ownershipType = ownershipType,
            isOccupied = isOccupied,
            numberOfHouseholds = households,
            numberOfPeople = tenants,
            landlords = mutableSetOf(landlord),
            propertyBuildType = propertyBuildType,
            customPropertyType = customPropertyType,
            address = address,
            numBedrooms = numberOfBedrooms,
            billsIncludedList = billsIncludedList,
            customBillsIncluded = customBillsIncluded,
            furnishedStatus = furnishedStatus,
            rentFrequency = rentFrequency,
            customRentFrequency = customRentFrequency,
            rentAmount = rentAmount,
        )

        val propertyOwnershipCaptor = captor<PropertyOwnership>()
        verify(mockPropertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
        assertTrue(ReflectionEquals(expectedPropertyOwnership, "ownershipLinks").matches(propertyOwnershipCaptor.value))
        assertEquals(setOf(landlord), propertyOwnershipCaptor.value.landlords)
    }

    @Test
    fun `createPropertyOwnership sets lastOccupiedDate when property is occupied`() {
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyBuildType = PropertyType.OTHER
        val address = MockLandlordData.createAddress("11 Example Road, EG1 2AB")

        whenever(mockRegistrationNumberService.createRegistrationNumber(RegistrationNumberType.PROPERTY)).thenReturn(
            registrationNumber,
        )
        whenever(mockPropertyOwnershipRepository.save(any<PropertyOwnership>())).thenAnswer {
            it.arguments[0] as PropertyOwnership
        }

        propertyOwnershipService.createPropertyOwnership(
            ownershipType = OwnershipType.FREEHOLD,
            isOccupied = true,
            numberOfHouseholds = 1,
            numberOfPeople = 2,
            landlords = mutableSetOf(landlord),
            propertyBuildType = propertyBuildType,
            customPropertyType = "End terrace",
            address = address,
            numBedrooms = 1,
            billsIncludedList = "Electricity, Water",
            customBillsIncluded = "Internet",
            furnishedStatus = FurnishedStatus.FURNISHED,
            rentFrequency = RentFrequency.OTHER,
            customRentFrequency = "Fortnightly",
            rentAmount = 123.toBigDecimal(),
        )

        val propertyOwnershipCaptor = captor<PropertyOwnership>()
        verify(mockPropertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
        assertEquals(LocalDate.now(), propertyOwnershipCaptor.value.lastOccupiedDate)
    }

    @Test
    fun `createPropertyOwnership does not set lastOccupiedDate when property is unoccupied`() {
        val registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456)
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyBuildType = PropertyType.OTHER
        val address = MockLandlordData.createAddress("11 Example Road, EG1 2AB")

        whenever(mockRegistrationNumberService.createRegistrationNumber(RegistrationNumberType.PROPERTY)).thenReturn(
            registrationNumber,
        )
        whenever(mockPropertyOwnershipRepository.save(any<PropertyOwnership>())).thenAnswer {
            it.arguments[0] as PropertyOwnership
        }

        propertyOwnershipService.createPropertyOwnership(
            ownershipType = OwnershipType.FREEHOLD,
            isOccupied = false,
            numberOfHouseholds = 0,
            numberOfPeople = 0,
            landlords = mutableSetOf(landlord),
            propertyBuildType = propertyBuildType,
            customPropertyType = "End terrace",
            address = address,
            numBedrooms = null,
            billsIncludedList = null,
            customBillsIncluded = null,
            furnishedStatus = null,
            rentFrequency = null,
            customRentFrequency = null,
            rentAmount = null,
        )

        val propertyOwnershipCaptor = captor<PropertyOwnership>()
        verify(mockPropertyOwnershipRepository).save(propertyOwnershipCaptor.capture())
        assertEquals(null, propertyOwnershipCaptor.value.lastOccupiedDate)
    }

    @Nested
    inner class GetLandlordRegisteredPropertiesDetails {
        private val currentLandlord = MockLandlordData.createIndividualLandlord()
        private val registeredLandlords: MutableSet<Landlord> = mutableSetOf(currentLandlord)
        private val localCouncil = LocalCouncil(11, "DERBYSHIRE DALES DISTRICT COUNCIL", "1045")
        private val expectedPropertyLicence = "forms.checkPropertyAnswers.propertyDetails.noLicensing.old"
        private val expectedIsTenantedMessageKey = "commonText.no"
        private val expectedCurrentUrlKey = 101

        private val propertyOwnership1 =
            MockLandlordData.createPropertyOwnership(
                landlords = registeredLandlords,
                address = MockLandlordData.createAddress("11 Example Road, EG1 2AB", localCouncil),
                registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 1233456),
                license = null,
                currentNumTenants = 0,
            )

        private val propertyOwnership2 =
            MockLandlordData.createPropertyOwnership(
                landlords = registeredLandlords,
                address = MockLandlordData.createAddress("12 Example Road, EG1 2AB", localCouncil),
                registrationNumber = RegistrationNumber(RegistrationNumberType.PROPERTY, 654321),
                license = null,
                currentNumTenants = 0,
            )

        private val landlordsProperties: List<PropertyOwnership> = listOf(propertyOwnership1, propertyOwnership2)

        @Test
        fun `Returns a list of Landlords properties in correctly formatted data model from landlords BaseUser_Id`() {
            whenever(
                mockPropertyOwnershipRepository.findAllByOwnershipLinks_Landlord_IdAndIsActiveTrue(currentLandlord.id),
            ).thenReturn(landlordsProperties)

            whenever(mockBackUrlStorageService.storeCurrentUrlReturningKey(REGISTERED_PROPERTIES_FRAGMENT))
                .thenReturn(expectedCurrentUrlKey)

            val expectedResults: List<RegisteredPropertyLandlordViewModel> =
                listOf(
                    RegisteredPropertyLandlordViewModel(
                        address = propertyOwnership1.address.singleLineAddress,
                        registrationNumber =
                            RegistrationNumberDataModel
                                .fromRegistrationNumber(propertyOwnership1.registrationNumber)
                                .toString(),
                        recordLink =
                            PropertyDetailsController
                                .getPropertyDetailsPath(propertyOwnership1.id)
                                .overrideBackLinkForUrl(expectedCurrentUrlKey),
                    ),
                    RegisteredPropertyLandlordViewModel(
                        address = propertyOwnership2.address.singleLineAddress,
                        registrationNumber =
                            RegistrationNumberDataModel
                                .fromRegistrationNumber(propertyOwnership2.registrationNumber)
                                .toString(),
                        recordLink =
                            PropertyDetailsController
                                .getPropertyDetailsPath(propertyOwnership2.id)
                                .overrideBackLinkForUrl(expectedCurrentUrlKey),
                    ),
                )

            val result =
                propertyOwnershipService.getRegisteredPropertiesForLandlordUser(
                    currentLandlord,
                    currentUrlFragment = REGISTERED_PROPERTIES_FRAGMENT,
                )

            assertTrue(result.size == 2)
            assertEquals(expectedResults, result)
        }

        @Test
        fun `Returns a list of Landlords properties in correctly formatted data model from landlords Id`() {
            whenever(
                mockPropertyOwnershipRepository.findAllByOwnershipLinks_Landlord_IdAndIsActiveTrue(currentLandlord.id),
            ).thenReturn(landlordsProperties)

            whenever(mockBackUrlStorageService.storeCurrentUrlReturningKey(REGISTERED_PROPERTIES_FRAGMENT))
                .thenReturn(expectedCurrentUrlKey)

            val expectedResults: List<RegisteredPropertyLocalCouncilViewModel> =
                listOf(
                    RegisteredPropertyLocalCouncilViewModel(
                        address = propertyOwnership1.address.singleLineAddress,
                        registrationNumber =
                            RegistrationNumberDataModel
                                .fromRegistrationNumber(propertyOwnership1.registrationNumber)
                                .toString(),
                        localCouncilName = localCouncil.name,
                        licenseTypeMessageKey = expectedPropertyLicence,
                        isTenantedMessageKey = expectedIsTenantedMessageKey,
                        recordLink =
                            PropertyDetailsController
                                .getPropertyDetailsPath(propertyOwnership1.id, isLocalCouncilView = true)
                                .overrideBackLinkForUrl(expectedCurrentUrlKey),
                    ),
                    RegisteredPropertyLocalCouncilViewModel(
                        address = propertyOwnership2.address.singleLineAddress,
                        registrationNumber =
                            RegistrationNumberDataModel
                                .fromRegistrationNumber(propertyOwnership2.registrationNumber)
                                .toString(),
                        localCouncilName = localCouncil.name,
                        licenseTypeMessageKey = expectedPropertyLicence,
                        isTenantedMessageKey = expectedIsTenantedMessageKey,
                        recordLink =
                            PropertyDetailsController
                                .getPropertyDetailsPath(propertyOwnership2.id, isLocalCouncilView = true)
                                .overrideBackLinkForUrl(expectedCurrentUrlKey),
                    ),
                )

            val result =
                propertyOwnershipService.getRegisteredPropertiesForLandlord(
                    currentLandlord.id,
                    currentUrlFragment = REGISTERED_PROPERTIES_FRAGMENT,
                )

            assertTrue(result.size == 2)
            assertEquals(expectedResults, result)
        }
    }

    @Nested
    inner class GetPropertyOwnershipIfCurrentUserAuthorized {
        private fun setMockPrincipal(name: String) {
            val authentication = mock<Authentication>()
            whenever(authentication.name).thenReturn(name)
            val context = mock<SecurityContext>()
            whenever(context.authentication).thenReturn(authentication)
            SecurityContextHolder.setContext(context)
        }

        @Test
        fun `throws not found error if an active property ownership does not exist`() {
            val invalidId: Long = 1
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(invalidId)).thenReturn(null)

            val errorThrown =
                assertThrows<ResponseStatusException> {
                    propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(invalidId)
                }
            assertEquals(HttpStatus.NOT_FOUND, errorThrown.statusCode)
        }

        @Test
        fun `throws not found error if user is not a landlord or an lc user`() {
            val propertyOwnership = MockLandlordData.createPropertyOwnership()
            val principalName = "not-the-landlord"
            setMockPrincipal(principalName)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )
            whenever(mockLocalCouncilDataService.getIsLocalCouncilUser(principalName)).thenReturn(false)
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)

            val errorThrown =
                assertThrows<ResponseStatusException> {
                    propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(propertyOwnership.id)
                }
            assertEquals(HttpStatus.NOT_FOUND, errorThrown.statusCode)
        }

        @Test
        fun `returns property ownership when user is an lc user`() {
            val propertyOwnership = MockLandlordData.createPropertyOwnership()
            val localCouncilUser =
                MockLocalCouncilData.createLocalCouncilUser(
                    MockPrsdbUserData.createPrsdbUser("not-the-landlord"),
                    MockLocalCouncilData.createLocalCouncil(),
                )
            val principalName = localCouncilUser.baseUser.id
            setMockPrincipal(principalName)

            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )
            whenever(mockLocalCouncilDataService.getIsLocalCouncilUser(principalName)).thenReturn(true)

            val result =
                propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(propertyOwnership.id)

            assertEquals(result, propertyOwnership)
        }

        @Test
        fun `returns property ownership when user is only landlord`() {
            val propertyOwnership = MockLandlordData.createPropertyOwnership()
            val landlord = propertyOwnership.landlords.first() as IndividualLandlord
            val principalName = landlord.baseUser.id
            setMockPrincipal(principalName)

            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )
            whenever(mockLocalCouncilDataService.getIsLocalCouncilUser(principalName)).thenReturn(false)
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(landlord)

            val result =
                propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(propertyOwnership.id)

            assertEquals(result, propertyOwnership)
        }

        @Test
        fun `returns property ownership when user is a joint landlord`() {
            val jointLandlord =
                MockLandlordData.createIndividualLandlord(
                    baseUser = MockLandlordData.createPrsdbUser("joint-landlord"),
                )
            val propertyOwnership = MockLandlordData.createPropertyOwnership()
            propertyOwnership.addLandlord(jointLandlord)
            val principalName = jointLandlord.baseUser.id
            setMockPrincipal(principalName)

            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )
            whenever(mockLocalCouncilDataService.getIsLocalCouncilUser(principalName)).thenReturn(false)
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(jointLandlord)

            val result =
                propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(propertyOwnership.id)

            assertEquals(result, propertyOwnership)
        }
    }

    @Nested
    inner class GetLettingAgentAccess {
        @Test
        fun `getLettingAgentAccess returns null without loading the property when the feature is disabled`() {
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(false)

            assertNull(propertyOwnershipService.getLettingAgentAccess(1L))

            verify(mockPropertyOwnershipRepository, never()).findByIdAndIsActiveTrue(any())
            verifyNoInteractions(mockLettingAgentAccessRepository)
        }

        @Test
        fun `getLettingAgentAccess returns null when the property is unoccupied`() {
            val propertyOwnership = MockLandlordData.createUnoccupiedPropertyOwnership(id = 1L)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(1L)).thenReturn(propertyOwnership)

            assertNull(propertyOwnershipService.getLettingAgentAccess(1L))
        }

        @Test
        fun `getLettingAgentAccess returns null when the property is occupied but has no access record`() {
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1L)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(1L)).thenReturn(propertyOwnership)
            whenever(mockLettingAgentAccessRepository.findByPropertyOwnershipId(1L)).thenReturn(null)

            assertNull(propertyOwnershipService.getLettingAgentAccess(1L))
        }

        @Test
        fun `getLettingAgentAccess returns access for a delegated property when the feature is enabled`() {
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1L)
            val access = MockLettingAgentData.createLettingAgentAccess(propertyOwnership = propertyOwnership)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(1L)).thenReturn(propertyOwnership)
            whenever(mockLettingAgentAccessRepository.findByPropertyOwnershipId(1L)).thenReturn(access)

            assertEquals(access, propertyOwnershipService.getLettingAgentAccess(1L))
        }
    }

    @Nested
    inner class HasLettingAgent {
        @Test
        fun `hasLettingAgent returns false without loading the property when the feature is disabled`() {
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(false)

            assertFalse(propertyOwnershipService.hasLettingAgent(1L))

            verify(mockPropertyOwnershipRepository, never()).findByIdAndIsActiveTrue(any())
            verifyNoInteractions(mockLettingAgentAccessRepository)
        }

        @Test
        fun `hasLettingAgent returns false when the property is unoccupied`() {
            val propertyOwnership = MockLandlordData.createUnoccupiedPropertyOwnership(id = 1L)
            val access = MockLettingAgentData.createLettingAgentAccess(propertyOwnership = propertyOwnership)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(1L)).thenReturn(propertyOwnership)
            whenever(mockLettingAgentAccessRepository.findByPropertyOwnershipId(1L)).thenReturn(access)

            assertFalse(propertyOwnershipService.hasLettingAgent(1L))
        }

        @Test
        fun `hasLettingAgent returns false when the property is occupied but has no access record`() {
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1L)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(1L)).thenReturn(propertyOwnership)
            whenever(mockLettingAgentAccessRepository.findByPropertyOwnershipId(1L)).thenReturn(null)

            assertFalse(propertyOwnershipService.hasLettingAgent(1L))
        }

        @Test
        fun `hasLettingAgent returns true when the property is occupied and access exists`() {
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1L)
            val access = MockLettingAgentData.createLettingAgentAccess(propertyOwnership = propertyOwnership)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(1L)).thenReturn(propertyOwnership)
            whenever(mockLettingAgentAccessRepository.findByPropertyOwnershipId(1L)).thenReturn(access)

            assertTrue(propertyOwnershipService.hasLettingAgent(1L))
        }
    }

    @Nested
    inner class GetCurrentUserIsAuthorizedToEditRecord {
        @Test
        fun `returns true if the current user is a landlord for the property`() {
            val landlord = MockLandlordData.createIndividualLandlord(baseUser = MockLandlordData.createPrsdbUser("baseUserId"))
            val propertyOwnership = MockLandlordData.createPropertyOwnership(landlords = mutableSetOf(landlord))
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(landlord)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(propertyOwnership)

            val result = propertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnership.id)

            assertTrue(result)
        }

        @Test
        fun `returns false if the user is not a landlord and the letting agent feature flag is disabled`() {
            val propertyOwnershipId = 1L
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(false)

            val result = propertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnershipId)

            assertFalse(result)
        }

        @Test
        fun `returns false if the user is not a landlord and the property has no letting agent access`() {
            val propertyOwnershipId = 1L
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnershipId))
                .thenReturn(MockLandlordData.createOccupiedPropertyOwnership(id = propertyOwnershipId))
            whenever(mockLettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnershipId)).thenReturn(null)

            val result = propertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnershipId)

            assertFalse(result)
        }

        @Test
        fun `returns true if the user is not a landlord but the property's token is authorised in the session`() {
            val propertyOwnershipId = 1L
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = propertyOwnershipId)
            val access = MockLettingAgentData.createLettingAgentAccess(propertyOwnership = propertyOwnership)
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnershipId)).thenReturn(propertyOwnership)
            whenever(mockLettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnershipId)).thenReturn(access)
            whenever(mockLettingAgentAccessService.isTokenAuthorisedInSession(access.token.toString())).thenReturn(true)

            val result = propertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnershipId)

            assertTrue(result)
        }

        @Test
        fun `returns false if the user is not a landlord and the property's token is not authorised in the session`() {
            val propertyOwnershipId = 1L
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = propertyOwnershipId)
            val access = MockLettingAgentData.createLettingAgentAccess(propertyOwnership = propertyOwnership)
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)
            whenever(mockFeatureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnershipId)).thenReturn(propertyOwnership)
            whenever(mockLettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnershipId)).thenReturn(access)
            whenever(mockLettingAgentAccessService.isTokenAuthorisedInSession(access.token.toString())).thenReturn(false)

            val result = propertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnershipId)

            assertFalse(result)
        }
    }

    @Nested
    inner class IsCurrentUserLandlord {
        @Test
        fun `returns true when the current user is the only landlord`() {
            val landlord =
                MockLandlordData.createIndividualLandlord(
                    baseUser = MockLandlordData.createPrsdbUser("baseUserId"),
                )
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(
                    landlords = mutableSetOf(landlord),
                )
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(landlord)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(propertyOwnership)

            val result = propertyOwnershipService.isCurrentUserLandlord(propertyOwnership.id)

            assertTrue(result)
        }

        @Test
        fun `returns true when the current user is a joint landlord`() {
            val jointLandlord =
                MockLandlordData.createIndividualLandlord(
                    baseUser = MockLandlordData.createPrsdbUser("joint-landlord"),
                )
            val propertyOwnership = MockLandlordData.createPropertyOwnership()
            propertyOwnership.addLandlord(jointLandlord)
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(jointLandlord)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(propertyOwnership)

            val result = propertyOwnershipService.isCurrentUserLandlord(propertyOwnership.id)

            assertTrue(result)
        }

        @Test
        fun `returns false when the current user is not a landlord of the property`() {
            val otherLandlord =
                MockLandlordData.createIndividualLandlord(
                    baseUser = MockLandlordData.createPrsdbUser("other-user"),
                )
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(
                    landlords =
                        mutableSetOf(
                            MockLandlordData.createIndividualLandlord(
                                baseUser = MockLandlordData.createPrsdbUser("baseUserId"),
                            ),
                        ),
                )
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(otherLandlord)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(propertyOwnership)

            val result = propertyOwnershipService.isCurrentUserLandlord(propertyOwnership.id)

            assertFalse(result)
        }

        @Test
        fun `returns false when current user has no landlord record`() {
            val propertyOwnership = MockLandlordData.createPropertyOwnership()
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(null)

            val result = propertyOwnershipService.isCurrentUserLandlord(propertyOwnership.id)

            assertFalse(result)
        }

        @Test
        fun `throws not found error if the property ownership does not exist`() {
            val landlord = MockLandlordData.createIndividualLandlord()
            whenever(mockUserToLandlordService.getCurrentLandlordForUserOrNull()).thenReturn(landlord)

            val errorThrown =
                assertThrows<ResponseStatusException> {
                    propertyOwnershipService.isCurrentUserLandlord(1)
                }
            assertEquals(HttpStatus.NOT_FOUND, errorThrown.statusCode)
        }
    }

    @Test
    fun `searchForProperties returns a single matching property when the search term is a PRN`() {
        val searchPRN = RegistrationNumberDataModel(RegistrationNumberType.PROPERTY, 123)
        val lcBaseUserId = "id"
        val pageRequest = PageRequest.of(1, 10)
        val prnMatchingPropertyOwnership = listOf(MockLandlordData.createPropertyOwnership())
        val currentUrlKey = 13
        val expectedSearchResults =
            prnMatchingPropertyOwnership.map { PropertySearchResultViewModel.fromPropertyOwnership(it, currentUrlKey) }

        whenever(
            mockPropertyOwnershipRepository.searchMatchingPRN(searchPRN.number, lcBaseUserId, pageable = pageRequest),
        ).thenReturn(PageImpl(prnMatchingPropertyOwnership))

        whenever(mockBackUrlStorageService.storeCurrentUrlReturningKey()).thenReturn(currentUrlKey)

        val searchResults =
            propertyOwnershipService.searchForProperties(
                searchPRN.toString(),
                lcBaseUserId,
                requestedPageIndex = pageRequest.pageNumber,
                pageSize = pageRequest.pageSize,
            )

        assertEquals(expectedSearchResults, searchResults.content)
    }

    @Test
    fun `searchForProperties returns no results when the search term is a non-property registration number`() {
        val nonPropertyRegNum = RegistrationNumberDataModel(RegistrationNumberType.LANDLORD, 123)
        val lcBaseUserId = "id"
        val pageRequest = PageRequest.of(1, 10)

        whenever(
            mockPropertyOwnershipRepository.searchMatching(
                nonPropertyRegNum.toString(),
                lcBaseUserId,
                pageable = pageRequest,
            ),
        ).thenReturn(Page.empty())

        val searchResults =
            propertyOwnershipService.searchForProperties(
                nonPropertyRegNum.toString(),
                lcBaseUserId,
                requestedPageIndex = pageRequest.pageNumber,
                pageSize = pageRequest.pageSize,
            )

        verify(mockPropertyOwnershipRepository, never()).searchMatchingPRN(
            nonPropertyRegNum.number,
            lcBaseUserId,
            pageable = pageRequest,
        )
        assertEquals(emptyList<PropertySearchResultViewModel>(), searchResults.content)
    }

    @Test
    fun `searchForProperties returns a single matching property when the search term is a UPRN`() {
        val searchUPRN = "123"
        val lcBaseUserId = "id"
        val pageRequest = PageRequest.of(1, 10)
        val currentUrlKey = 23

        val uprnMatchingPropertyOwnership =
            listOf(MockLandlordData.createPropertyOwnership(address = MockLandlordData.createAddress(uprn = searchUPRN.toLong())))
        val expectedSearchResults =
            uprnMatchingPropertyOwnership.map { PropertySearchResultViewModel.fromPropertyOwnership(it, currentUrlKey) }

        whenever(
            mockPropertyOwnershipRepository.searchMatchingUPRN(
                searchUPRN.toLong(),
                lcBaseUserId,
                pageable = pageRequest,
            ),
        ).thenReturn(PageImpl(uprnMatchingPropertyOwnership))
        whenever(mockBackUrlStorageService.storeCurrentUrlReturningKey()).thenReturn(currentUrlKey)

        val searchResults =
            propertyOwnershipService.searchForProperties(
                searchUPRN,
                lcBaseUserId,
                requestedPageIndex = pageRequest.pageNumber,
                pageSize = pageRequest.pageSize,
            )

        assertEquals(expectedSearchResults, searchResults.content)
    }

    @Test
    fun `searchForProperties returns a collection of fuzzy matches when the search term is not a PRN or UPRN`() {
        val searchTerm = "road"
        val lcBaseUserId = "id"
        val urlKey = 7
        val pageRequest = PageRequest.of(1, 10)

        val fuzzyMatchingPropertyOwnerships =
            listOf(MockLandlordData.createPropertyOwnership(), MockLandlordData.createPropertyOwnership())
        val expectedSearchResults =
            fuzzyMatchingPropertyOwnerships.map { PropertySearchResultViewModel.fromPropertyOwnership(it, 7) }

        whenever(
            mockPropertyOwnershipRepository.searchMatching(searchTerm, lcBaseUserId, pageable = pageRequest),
        ).thenReturn(PageImpl(fuzzyMatchingPropertyOwnerships))
        whenever(mockBackUrlStorageService.storeCurrentUrlReturningKey()).thenReturn(urlKey)

        val searchResults =
            propertyOwnershipService.searchForProperties(
                searchTerm,
                lcBaseUserId,
                requestedPageIndex = pageRequest.pageNumber,
                pageSize = pageRequest.pageSize,
            )

        assertEquals(expectedSearchResults, searchResults.content)
    }

    @Test
    fun `searchForProperties returns the requested page of properties`() {
        val searchTerm = "searchTerm"
        val lcBaseUserId = "id"
        val pageSize = 25
        val matchingProperties = (1..40).map { MockLandlordData.createPropertyOwnership() }

        val pageIndex1 = 0
        val urlKey1 = 37
        val pageRequest1 = PageRequest.of(pageIndex1, pageSize)
        val matchingPropertiesPage1 = matchingProperties.subList(0, pageSize)
        val expectedPage1SearchResults =
            matchingPropertiesPage1.map { PropertySearchResultViewModel.fromPropertyOwnership(it, urlKey1) }

        val pageIndex2 = 1
        val urlKey2 = 41
        val pageRequest2 = PageRequest.of(pageIndex2, pageSize)
        val matchingPropertiesPage2 = matchingProperties.subList(pageSize, matchingProperties.size)
        val expectedPage2SearchResults =
            matchingPropertiesPage2.map { PropertySearchResultViewModel.fromPropertyOwnership(it, urlKey2) }

        whenever(mockPropertyOwnershipRepository.searchMatching(searchTerm, lcBaseUserId, pageable = pageRequest1))
            .thenReturn(PageImpl(matchingPropertiesPage1))
        whenever(mockPropertyOwnershipRepository.searchMatching(searchTerm, lcBaseUserId, pageable = pageRequest2))
            .thenReturn(PageImpl(matchingPropertiesPage2))

        whenever(mockBackUrlStorageService.storeCurrentUrlReturningKey()).thenReturn(urlKey1)
        val searchResults1 =
            propertyOwnershipService.searchForProperties(searchTerm, lcBaseUserId, requestedPageIndex = pageIndex1)

        whenever(mockBackUrlStorageService.storeCurrentUrlReturningKey()).thenReturn(urlKey2)
        val searchResults2 =
            propertyOwnershipService.searchForProperties(searchTerm, lcBaseUserId, requestedPageIndex = pageIndex2)

        assertEquals(expectedPage1SearchResults, searchResults1.content)
        assertEquals(expectedPage2SearchResults, searchResults2.content)
    }

    @Test
    fun `searchForProperties throws an exception when fuzzy searching times out`() {
        // Arrange
        val searchTerm = "searchTerm"
        val lcBaseUserId = "id"
        val pageRequest = PageRequest.of(1, 10)

        whenever(
            mockPropertyOwnershipRepository.searchMatching(searchTerm, lcBaseUserId, pageable = pageRequest),
        ).thenThrow(QueryTimeoutException("Query timed out"))

        // Act & Assert
        assertThrows<RepositoryQueryTimeoutException> {
            propertyOwnershipService.searchForProperties(
                searchTerm,
                lcBaseUserId,
                requestedPageIndex = pageRequest.pageNumber,
                pageSize = pageRequest.pageSize,
            )
        }
    }

    @Test
    fun `updateLicensing updates the property's license`() {
        // Arrange
        val propertyOwnership =
            MockLandlordData.createPropertyOwnership(
                id = 1,
                license = License(LicensingType.SELECTIVE_LICENCE, "licenceNumber"),
            )
        val newLicensingType = LicensingType.HMO_MANDATORY_LICENCE
        val newLicenceNumber = "newLicenceNumber"
        val updatedLicence = License(newLicensingType, newLicenceNumber)

        whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
            propertyOwnership,
        )
        whenever(
            mockLicenseService.updateLicence(propertyOwnership.license, newLicensingType, newLicenceNumber),
        ).thenReturn(updatedLicence)

        // Act
        propertyOwnershipService.updateLicensing(
            propertyOwnership.id,
            newLicensingType,
            newLicenceNumber,
            propertyOwnership.getMostRecentlyUpdated(),
        )

        // Assert
        assertEquals(updatedLicence, propertyOwnership.license)
    }

    @Test
    fun `updateLicensing clears the licenseProvideLater flag`() {
        // Arrange
        val propertyOwnership =
            MockLandlordData.createPropertyOwnership(
                id = 1,
                license = null,
                licenseProvideLater = true,
            )
        val newLicensingType = LicensingType.HMO_MANDATORY_LICENCE
        val newLicenceNumber = "newLicenceNumber"
        val updatedLicence = License(newLicensingType, newLicenceNumber)

        whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
            propertyOwnership,
        )
        whenever(
            mockLicenseService.updateLicence(propertyOwnership.license, newLicensingType, newLicenceNumber),
        ).thenReturn(updatedLicence)

        // Act
        propertyOwnershipService.updateLicensing(
            propertyOwnership.id,
            newLicensingType,
            newLicenceNumber,
            propertyOwnership.getMostRecentlyUpdated(),
        )

        // Assert
        assertEquals(false, propertyOwnership.licenseProvideLater)
    }

    @Test
    fun `updateLicensing throws UpdateConflictException if modified dates conflict`() {
        // Arrange
        val propertyOwnership =
            MockLandlordData.createPropertyOwnership(
                id = 1,
                license = License(LicensingType.SELECTIVE_LICENCE, "licenceNumber"),
            )

        whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
            propertyOwnership,
        )

        // Act & Assert
        val exception =
            assertThrows<UpdateConflictException> {
                propertyOwnershipService.updateLicensing(
                    propertyOwnership.id,
                    LicensingType.HMO_MANDATORY_LICENCE,
                    "newLicenceNumber",
                    initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated().minus(1, ChronoUnit.MINUTES),
                )
            }

        assertEquals(
            "The property ownership record has been updated since this update session started.",
            exception.message,
        )
    }

    @Test
    fun `updateOwnershipType updates the property's ownership type`() {
        // Arrange
        val propertyOwnership =
            MockLandlordData.createPropertyOwnership(
                id = 1,
                ownershipType = OwnershipType.FREEHOLD,
            )

        whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
            propertyOwnership,
        )

        // Act
        propertyOwnershipService.updateOwnershipType(
            propertyOwnership.id,
            OwnershipType.LEASEHOLD,
            propertyOwnership.getMostRecentlyUpdated(),
        )

        // Assert
        assertEquals(OwnershipType.LEASEHOLD, propertyOwnership.ownershipType)
    }

    @Test
    fun `updateOwnershipType throws UpdateConflictException if modified dates conflict`() {
        // Arrange
        val propertyOwnership =
            MockLandlordData.createPropertyOwnership(
                id = 1,
                ownershipType = OwnershipType.FREEHOLD,
            )

        whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
            propertyOwnership,
        )

        // Act & Assert
        val exception =
            assertThrows<UpdateConflictException> {
                propertyOwnershipService.updateOwnershipType(
                    propertyOwnership.id,
                    OwnershipType.LEASEHOLD,
                    initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated().minus(1, ChronoUnit.MINUTES),
                )
            }

        assertEquals(
            "The property ownership record has been updated since this update session started.",
            exception.message,
        )
    }

    @Nested
    inner class UpdateOccupancy {
        @Test
        fun `updateOccupancy updates the property's occupancy status`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createUnoccupiedPropertyOwnership(
                    id = 1,
                )
            val newOccupiedStatus = true
            val newNumberOfHouseholds = 1
            val newNumberOfTenants = 5
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateOccupancy(
                propertyOwnership.id,
                isOccupied = newOccupiedStatus,
                numberOfPeople = newNumberOfTenants,
                numberOfHouseholds = newNumberOfHouseholds,
                numBedrooms = propertyOwnership.numBedrooms,
                billsIncludedList = propertyOwnership.billsIncludedList,
                customBillsIncluded = propertyOwnership.customBillsIncluded,
                furnishedStatus = propertyOwnership.furnishedStatus,
                rentFrequency = propertyOwnership.rentFrequency,
                customRentFrequency = propertyOwnership.customRentFrequency,
                rentAmount = propertyOwnership.rentAmount,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(newOccupiedStatus, propertyOwnership.isOccupied)
            assertEquals(newNumberOfHouseholds, propertyOwnership.currentNumHouseholds)
            assertEquals(newNumberOfTenants, propertyOwnership.currentNumTenants)
        }

        @Test
        fun `updateOccupancy sets lastOccupiedDate when property transitions to occupied`() {
            // Arrange
            val propertyOwnership = MockLandlordData.createPropertyOwnership(id = 1)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateOccupancy(
                propertyOwnership.id,
                isOccupied = true,
                numberOfPeople = 2,
                numberOfHouseholds = 1,
                numBedrooms = 1,
                billsIncludedList = "Electricity, Water",
                customBillsIncluded = "Internet",
                furnishedStatus = FurnishedStatus.FURNISHED,
                rentFrequency = RentFrequency.OTHER,
                customRentFrequency = "Fortnightly",
                rentAmount = 123.toBigDecimal(),
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(LocalDate.now(), propertyOwnership.lastOccupiedDate)
        }

        @Test
        fun `updateOccupancy does not change lastOccupiedDate when property remains occupied`() {
            // Arrange
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1)
            val existingLastOccupiedDate = LocalDate.now().minusDays(10)
            ReflectionTestUtils.setField(propertyOwnership, "lastOccupiedDate", existingLastOccupiedDate)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateOccupancy(
                propertyOwnership.id,
                isOccupied = true,
                numberOfPeople = propertyOwnership.currentNumTenants + 1,
                numberOfHouseholds = propertyOwnership.currentNumHouseholds,
                numBedrooms = propertyOwnership.numBedrooms,
                billsIncludedList = propertyOwnership.billsIncludedList,
                customBillsIncluded = propertyOwnership.customBillsIncluded,
                furnishedStatus = propertyOwnership.furnishedStatus,
                rentFrequency = propertyOwnership.rentFrequency,
                customRentFrequency = propertyOwnership.customRentFrequency,
                rentAmount = propertyOwnership.rentAmount,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(existingLastOccupiedDate, propertyOwnership.lastOccupiedDate)
        }

        @Test
        fun `updateOccupancy does not set lastOccupiedDate when property transitions to unoccupied`() {
            // Arrange
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1)
            val existingLastOccupiedDate = LocalDate.now().minusDays(10)
            ReflectionTestUtils.setField(propertyOwnership, "lastOccupiedDate", existingLastOccupiedDate)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateOccupancy(
                propertyOwnership.id,
                isOccupied = false,
                numberOfPeople = 0,
                numberOfHouseholds = propertyOwnership.currentNumHouseholds,
                numBedrooms = propertyOwnership.numBedrooms,
                billsIncludedList = propertyOwnership.billsIncludedList,
                customBillsIncluded = propertyOwnership.customBillsIncluded,
                furnishedStatus = propertyOwnership.furnishedStatus,
                rentFrequency = propertyOwnership.rentFrequency,
                customRentFrequency = propertyOwnership.customRentFrequency,
                rentAmount = propertyOwnership.rentAmount,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(existingLastOccupiedDate, propertyOwnership.lastOccupiedDate)
        }

        @Test
        fun `updateOccupancy nulls tenancyStartedBeforeEpcExpiry when property transitions to unoccupied`() {
            // Arrange
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1)
            val propertyCompliance =
                PropertyCompliance(propertyOwnership = propertyOwnership, tenancyStartedBeforeEpcExpiry = true)
            ReflectionTestUtils.setField(propertyOwnership, "propertyCompliance", propertyCompliance)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateOccupancy(
                propertyOwnership.id,
                isOccupied = false,
                numberOfPeople = 0,
                numberOfHouseholds = 0,
                numBedrooms = null,
                billsIncludedList = null,
                customBillsIncluded = null,
                furnishedStatus = null,
                rentFrequency = null,
                customRentFrequency = null,
                rentAmount = null,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(null, propertyCompliance.tenancyStartedBeforeEpcExpiry)
        }

        @Test
        fun `updateOccupancy does not null tenancyStartedBeforeEpcExpiry when property remains occupied`() {
            // Arrange
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1)
            val propertyCompliance =
                PropertyCompliance(propertyOwnership = propertyOwnership, tenancyStartedBeforeEpcExpiry = true)
            ReflectionTestUtils.setField(propertyOwnership, "propertyCompliance", propertyCompliance)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateOccupancy(
                propertyOwnership.id,
                isOccupied = true,
                numberOfPeople = 3,
                numberOfHouseholds = propertyOwnership.currentNumHouseholds,
                numBedrooms = propertyOwnership.numBedrooms,
                billsIncludedList = propertyOwnership.billsIncludedList,
                customBillsIncluded = propertyOwnership.customBillsIncluded,
                furnishedStatus = propertyOwnership.furnishedStatus,
                rentFrequency = propertyOwnership.rentFrequency,
                customRentFrequency = propertyOwnership.customRentFrequency,
                rentAmount = propertyOwnership.rentAmount,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(true, propertyCompliance.tenancyStartedBeforeEpcExpiry)
        }

        @Test
        fun `updateOccupancy throws exception when initialLastModifiedDate does not match current lastModifiedDate`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership()
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act & Assert
            val exception =
                assertThrows<UpdateConflictException> {
                    propertyOwnershipService.updateOccupancy(
                        propertyOwnership.id,
                        isOccupied = true,
                        numberOfPeople = 6,
                        numberOfHouseholds = propertyOwnership.currentNumHouseholds,
                        numBedrooms = propertyOwnership.numBedrooms,
                        billsIncludedList = propertyOwnership.billsIncludedList,
                        customBillsIncluded = propertyOwnership.customBillsIncluded,
                        furnishedStatus = propertyOwnership.furnishedStatus,
                        rentFrequency = propertyOwnership.rentFrequency,
                        customRentFrequency = propertyOwnership.customRentFrequency,
                        rentAmount = propertyOwnership.rentAmount,
                        initialLastModifiedDate =
                            propertyOwnership
                                .getMostRecentlyUpdated()
                                .minus(1, ChronoUnit.MINUTES),
                    )
                }

            assertEquals(
                "The property ownership record has been updated since this update session started.",
                exception.message,
            )
        }
    }

    @Nested
    inner class UpdateIsOccupied {
        @Test
        fun `updateIsOccupied clears stale tenancy details when a property becomes unoccupied`() {
            // Arrange
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1)
            val originalNumBedrooms = propertyOwnership.numBedrooms
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateIsOccupied(
                propertyOwnership.id,
                isOccupied = false,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(false, propertyOwnership.isOccupied)
            assertEquals(0, propertyOwnership.currentNumHouseholds)
            assertEquals(0, propertyOwnership.currentNumTenants)
            assertEquals(originalNumBedrooms, propertyOwnership.numBedrooms)
            assertNull(propertyOwnership.billsIncludedList)
            assertNull(propertyOwnership.customBillsIncluded)
            assertNull(propertyOwnership.furnishedStatus)
            assertNull(propertyOwnership.rentFrequency)
            assertNull(propertyOwnership.customRentFrequency)
            assertNull(propertyOwnership.rentAmount)
            assertNull(propertyOwnership.tenancyProvideLater)
            verify(mockPropertyOwnershipRepository).save(propertyOwnership)
        }

        @Test
        @Suppress("ktlint:standard:max-line-length")
        fun `updateIsOccupied sets lastOccupiedDate and defaults to provide-later when a property becomes occupied`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership(id = 1).apply {
                    isOccupied = false
                }
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateIsOccupied(
                propertyOwnership.id,
                isOccupied = true,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(LocalDate.now(), propertyOwnership.lastOccupiedDate)
            assertEquals(true, propertyOwnership.tenancyProvideLater)
        }

        @Test
        fun `updateIsOccupied nulls tenancyStartedBeforeEpcExpiry when property transitions to unoccupied`() {
            // Arrange
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership(id = 1)
            val propertyCompliance =
                PropertyCompliance(propertyOwnership = propertyOwnership, tenancyStartedBeforeEpcExpiry = true)
            ReflectionTestUtils.setField(propertyOwnership, "propertyCompliance", propertyCompliance)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateIsOccupied(
                propertyOwnership.id,
                isOccupied = false,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(null, propertyCompliance.tenancyStartedBeforeEpcExpiry)
        }

        @Test
        fun `updateIsOccupied throws exception when initialLastModifiedDate does not match current lastModifiedDate`() {
            // Arrange
            val propertyOwnership = MockLandlordData.createOccupiedPropertyOwnership()
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act & Assert
            val exception =
                assertThrows<UpdateConflictException> {
                    propertyOwnershipService.updateIsOccupied(
                        propertyOwnership.id,
                        isOccupied = true,
                        initialLastModifiedDate =
                            propertyOwnership
                                .getMostRecentlyUpdated()
                                .minus(1, ChronoUnit.MINUTES),
                    )
                }

            assertEquals(
                "The property ownership record has been updated since this update session started.",
                exception.message,
            )
        }
    }

    @Nested
    inner class UpdateHouseholdsAndTenants {
        @Test
        fun `updateHouseholdsAndTenants updates the property's households and tenants values`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership(
                    id = 1,
                    currentNumHouseholds = 2,
                    currentNumTenants = 4,
                )
            val newNumberOfHouseholds = 3
            val newNumberOfTenants = 5
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateHouseholdsAndTenants(
                propertyOwnership.id,
                numberOfPeople = newNumberOfTenants,
                numberOfHouseholds = newNumberOfHouseholds,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(newNumberOfTenants, propertyOwnership.currentNumTenants)
            assertEquals(newNumberOfHouseholds, propertyOwnership.currentNumHouseholds)
        }

        @Test
        fun `updateHouseholdsAndTenants throws exception when initialLastModifiedDate does not match current lastModifiedDate`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership()
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act & Assert
            val exception =
                assertThrows<UpdateConflictException> {
                    propertyOwnershipService.updateHouseholdsAndTenants(
                        propertyOwnership.id,
                        numberOfPeople = 6,
                        numberOfHouseholds = 6,
                        initialLastModifiedDate =
                            propertyOwnership
                                .getMostRecentlyUpdated()
                                .minus(1, ChronoUnit.MINUTES),
                    )
                }

            assertEquals(
                "The property ownership record has been updated since this update session started.",
                exception.message,
            )
        }
    }

    @Nested
    inner class UpdateBedrooms {
        @Test
        fun `updateBedrooms updates the property's number of bedrooms`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership(
                    id = 1,
                    numberOfBedrooms = 2,
                )
            val newNumberOfBedrooms = 3
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateBedrooms(
                propertyOwnership.id,
                numberOfBedrooms = newNumberOfBedrooms,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(newNumberOfBedrooms, propertyOwnership.numBedrooms)
        }

        @Test
        fun `updateBedrooms throws exception when initialLastModifiedDate does not match current lastModifiedDate`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership()
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act & Assert
            val exception =
                assertThrows<UpdateConflictException> {
                    propertyOwnershipService.updateBedrooms(
                        propertyOwnership.id,
                        numberOfBedrooms = 4,
                        initialLastModifiedDate =
                            propertyOwnership
                                .getMostRecentlyUpdated()
                                .minus(1, ChronoUnit.MINUTES),
                    )
                }

            assertEquals(
                "The property ownership record has been updated since this update session started.",
                exception.message,
            )
        }
    }

    @Nested
    inner class UpdateRentIncludesBills {
        @Test
        fun `updateRentIncludesBills updates the property's bills included values`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership(
                    id = 1,
                    billsIncludedList = "ELECTRICITY,WATER,SOMETHING_ELSE",
                    customBillsIncluded = "Cat sitting",
                )
            val newBillsIncludedList = "GAS,BROADBAND,SOMETHING_ELSE"
            val newCustomBillsIncluded = "Dog grooming"
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateRentIncludesBills(
                propertyOwnership.id,
                billsIncludedList = newCustomBillsIncluded,
                customBillsIncluded = newBillsIncludedList,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(newCustomBillsIncluded, propertyOwnership.billsIncludedList)
            assertEquals(newBillsIncludedList, propertyOwnership.customBillsIncluded)
        }

        @Test
        fun `updateRentIncludesBills can update the property's bills included values to null`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership(
                    id = 1,
                    billsIncludedList = "ELECTRICITY,WATER,SOMETHING_ELSE",
                    customBillsIncluded = "Cat sitting",
                )
            val newBillsIncludedList = "GAS,BROADBAND,SOMETHING_ELSE"
            val newCustomBillsIncluded = "Dog grooming"
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateRentIncludesBills(
                propertyOwnership.id,
                billsIncludedList = newCustomBillsIncluded,
                customBillsIncluded = newBillsIncludedList,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(newCustomBillsIncluded, propertyOwnership.billsIncludedList)
            assertEquals(newBillsIncludedList, propertyOwnership.customBillsIncluded)
        }

        @Test
        fun `updateRentIncludesBills throws exception when initialLastModifiedDate does not match current lastModifiedDate`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership()
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act & Assert
            val exception =
                assertThrows<UpdateConflictException> {
                    propertyOwnershipService.updateRentIncludesBills(
                        propertyOwnership.id,
                        billsIncludedList = "GAS,BROADBAND,SOMETHING_ELSE",
                        customBillsIncluded = "Dog sitting",
                        initialLastModifiedDate =
                            propertyOwnership
                                .getMostRecentlyUpdated()
                                .minus(1, ChronoUnit.MINUTES),
                    )
                }

            assertEquals(
                "The property ownership record has been updated since this update session started.",
                exception.message,
            )
        }
    }

    @Nested
    inner class UpdateFurnishedStatus {
        @Test
        fun `updateFurnishedStatus updates the property's furnished status`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership(
                    id = 1,
                    furnishedStatus = FurnishedStatus.FURNISHED,
                )
            val newFurnishedStatus = FurnishedStatus.PART_FURNISHED
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateFurnishedStatus(
                propertyOwnership.id,
                furnishedStatus = newFurnishedStatus,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(newFurnishedStatus, propertyOwnership.furnishedStatus)
        }

        @Test
        fun `updateFurnishedStatus throws exception when initialLastModifiedDate does not match current lastModifiedDate`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership()
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act & Assert
            val exception =
                assertThrows<UpdateConflictException> {
                    propertyOwnershipService.updateFurnishedStatus(
                        propertyOwnership.id,
                        furnishedStatus = FurnishedStatus.UNFURNISHED,
                        initialLastModifiedDate =
                            propertyOwnership
                                .getMostRecentlyUpdated()
                                .minus(1, ChronoUnit.MINUTES),
                    )
                }

            assertEquals(
                "The property ownership record has been updated since this update session started.",
                exception.message,
            )
        }
    }

    @Nested
    inner class UpdateRentFrequencyAndAmount {
        @Test
        fun `updateRentFrequencyAndAmount updates the property's rent frequency and amount`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership(
                    id = 1,
                    rentFrequency = RentFrequency.MONTHLY,
                    customRentFrequency = null,
                    rentAmount = BigDecimal(100),
                )
            val newRentFrequency = RentFrequency.OTHER
            val newCustomRentFrequency = "Every 5 days"
            val newRentAmount = BigDecimal(50)
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.updateRentFrequencyAndAmount(
                propertyOwnership.id,
                rentFrequency = newRentFrequency,
                customRentFrequency = newCustomRentFrequency,
                rentAmount = newRentAmount,
                initialLastModifiedDate = propertyOwnership.getMostRecentlyUpdated(),
            )

            // Assert
            assertEquals(newRentFrequency, propertyOwnership.rentFrequency)
            assertEquals(newCustomRentFrequency, propertyOwnership.customRentFrequency)
            assertEquals(newRentAmount, propertyOwnership.rentAmount)
        }

        @Test
        fun `updateRentFrequencyAndAmount throws exception if initialLastModifiedDate does not match current lastModifiedDate`() {
            // Arrange
            val propertyOwnership =
                MockLandlordData.createOccupiedPropertyOwnership()
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act & Assert
            val exception =
                assertThrows<UpdateConflictException> {
                    propertyOwnershipService.updateRentFrequencyAndAmount(
                        propertyOwnership.id,
                        rentFrequency = RentFrequency.WEEKLY,
                        customRentFrequency = null,
                        rentAmount = BigDecimal(120),
                        initialLastModifiedDate =
                            propertyOwnership
                                .getMostRecentlyUpdated()
                                .minus(1, ChronoUnit.MINUTES),
                    )
                }

            assertEquals(
                "The property ownership record has been updated since this update session started.",
                exception.message,
            )
        }
    }

    @Test
    fun `deletePropertyOwnership calls delete on the propertyOwnershipRepository`() {
        val propertyOwnershipId = 1L

        propertyOwnershipService.deletePropertyOwnership(propertyOwnershipId)

        verify(mockPropertyOwnershipRepository).deleteById(propertyOwnershipId)
    }

    @Test
    fun `deletePropertyOwnerships deletes a list from the propertyOwnershipRepository`() {
        val propertyOwnerships =
            listOf(MockLandlordData.createPropertyOwnership(), MockLandlordData.createPropertyOwnership())

        propertyOwnershipService.deletePropertyOwnerships(propertyOwnerships)

        verify(mockPropertyOwnershipRepository).deleteAll(propertyOwnerships)
    }

    @Nested
    inner class GetNumberOfIncompleteCompliancesForLandlord {
        val landlord = MockLandlordData.createIndividualLandlord()

        @Test
        fun `returns the number of occupied properties without completed compliance`() {
            // Arrange
            val occupiedPropertyWithoutCompliance =
                MockLandlordData.createOccupiedPropertyOwnership(currentNumTenants = 1)
            val occupiedPropertyWithCompliance =
                MockLandlordData.createOccupiedPropertyOwnership(currentNumTenants = 1, id = 2)
            ReflectionTestUtils.setField(
                occupiedPropertyWithCompliance,
                "propertyCompliance",
                mock<PropertyCompliance>(),
            )
            val unoccupiedProperty = MockLandlordData.createPropertyOwnership(currentNumTenants = 0)

            whenever(
                mockPropertyOwnershipRepository.findAllByOwnershipLinks_Landlord_IdAndIsActiveTrue(landlord.id),
            ).thenReturn(listOf(unoccupiedProperty, occupiedPropertyWithCompliance, occupiedPropertyWithoutCompliance))

            // Act
            val numberOfIncompleteCompliances = propertyOwnershipService.getNumberOfIncompleteCompliancesForLandlord(landlord)

            // Assert
            assertEquals(1, numberOfIncompleteCompliances)
        }

        @Test
        fun `returns 0 if there are no incomplete compliances for a landlord`() {
            // Arrange
            whenever(
                mockPropertyOwnershipRepository.findAllByOwnershipLinks_Landlord_IdAndIsActiveTrue(landlord.id),
            ).thenReturn(emptyList())

            // Act
            val numberOfIncompleteCompliances = propertyOwnershipService.getNumberOfIncompleteCompliancesForLandlord(landlord)

            // Assert
            assertEquals(0, numberOfIncompleteCompliances)
        }
    }

    @Nested
    inner class GetPropertyCountForLandlord {
        @Test
        fun `returns the count from the repository`() {
            val landlord = MockLandlordData.createIndividualLandlord()
            whenever(mockPropertyOwnershipRepository.countByOwnershipLinks_Landlord_Id(landlord.id)).thenReturn(3)

            assertEquals(3L, propertyOwnershipService.getPropertyCountForLandlord(landlord))
        }
    }

    @Nested
    inner class AddLandlordToPropertyOwnership {
        @Test
        fun `addLandlordToPropertyOwnership adds the landlord to the property ownership`() {
            // Arrange
            val propertyOwnership = MockLandlordData.createPropertyOwnership(id = 1)
            val newLandlord = MockLandlordData.createIndividualLandlord()
            whenever(mockPropertyOwnershipRepository.findByIdAndIsActiveTrue(propertyOwnership.id)).thenReturn(
                propertyOwnership,
            )

            // Act
            propertyOwnershipService.addLandlordToPropertyOwnership(propertyOwnership.id, newLandlord)

            // Assert
            assertTrue(propertyOwnership.landlords.contains(newLandlord))
            verify(mockPropertyOwnershipRepository).save(propertyOwnership)
        }
    }

    @Nested
    inner class RemoveLandlord {
        @Test
        fun `removeLandlord removes the landlord from the property ownership`() {
            val landlord = MockLandlordData.createIndividualLandlord()
            val otherLandlord = MockLandlordData.createIndividualLandlord(name = "Other")
            val propertyOwnership =
                MockLandlordData.createPropertyOwnership(
                    landlords = mutableSetOf(landlord, otherLandlord),
                )

            propertyOwnershipService.removeLandlord(propertyOwnership, landlord)

            assertFalse(propertyOwnership.landlords.any { it == landlord })
            verify(mockEmailService).sendNotificationToRemainingLandlords(propertyOwnership, landlord)
        }
    }
}

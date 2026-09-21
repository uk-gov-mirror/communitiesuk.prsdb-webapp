package uk.gov.communities.prsdb.webapp.services

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentCaptor.captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.repository.AddressRepository
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLocalCouncilData
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class AddressServiceTests {
    @Mock
    private lateinit var mockAddressRepository: AddressRepository

    @Mock
    private lateinit var mockLocalCouncilService: LocalCouncilService

    @InjectMocks
    private lateinit var addressService: AddressService

    @Test
    fun `createAddressSnapshot saves a separate copy without the source UPRN`() {
        val model =
            AddressDataModel(
                "12 Test Road, Leeds, LS1 1AA",
                localCouncilId = 1,
                uprn = 123456L,
                buildingNumber = "12",
                streetName = "Test Road",
                townName = "Leeds",
                postcode = "LS1 1AA",
            )
        val council = MockLocalCouncilData.createLocalCouncil(id = 1)
        val savedAddress = MockLandlordData.createAddress()
        whenever(mockLocalCouncilService.retrieveLocalCouncilById(1)).thenReturn(council)
        whenever(mockAddressRepository.save(any())).thenReturn(savedAddress)

        val snapshot = addressService.createAddressSnapshot(model)

        val addressCaptor = captor<Address>()
        verify(mockAddressRepository).save(addressCaptor.capture())
        assertEquals(model.copy(uprn = null), AddressDataModel.fromAddress(addressCaptor.value))
        assertEquals(council, addressCaptor.value.localCouncil)
        assertEquals(savedAddress, snapshot)
        verify(mockAddressRepository, never()).findByIsActiveTrueAndUprn(any())
    }

    @Nested
    inner class FindOrCreateAddressTests {
        @Test
        fun `findOrCreateAddress returns the corresponding address when given an AddressDataModel with a UPRN`() {
            // Arrange
            val addressDataModel = AddressDataModel(singleLineAddress = "1 Example Road, EG1 2AB", uprn = 123456L)
            val address = Address(addressDataModel)
            whenever(mockAddressRepository.findByIsActiveTrueAndUprn(addressDataModel.uprn!!)).thenReturn(address)

            // Act
            val foundAddress = addressService.findOrCreateAddress(addressDataModel)

            // Assert
            assertEquals(address, foundAddress)
        }

        @Test
        fun `findOrCreateAddress throws an error when given an AddressDataModel with an invalid UPRN`() {
            // Arrange
            val addressDataModel = AddressDataModel(singleLineAddress = "1 Example Road, EG1 2AB", uprn = 123456L)
            whenever(mockAddressRepository.findByIsActiveTrueAndUprn(addressDataModel.uprn!!)).thenReturn(null)

            // Act & Assert
            assertThrows<EntityNotFoundException> { addressService.findOrCreateAddress(addressDataModel) }
        }

        @ParameterizedTest(name = "and {0}")
        @MethodSource("uk.gov.communities.prsdb.webapp.services.AddressServiceTests#provideAddressDataModels")
        fun `findOrCreateAddress creates an address when given an AddressDataModel with no UPRN`(addressDataModel: AddressDataModel) {
            // Arrange
            addressDataModel.localCouncilId?.let {
                whenever(mockLocalCouncilService.retrieveLocalCouncilById(it))
                    .thenReturn(MockLocalCouncilData.createLocalCouncil(id = it))
            }

            whenever(mockAddressRepository.save(any())).thenReturn(MockLandlordData.createAddress())

            // Act
            addressService.findOrCreateAddress(addressDataModel)

            // Assert
            val addressCaptor = captor<Address>()
            verify(mockAddressRepository).save(addressCaptor.capture())

            val createdAddress = AddressDataModel.fromAddress(addressCaptor.value)
            assertEquals(addressDataModel, createdAddress)
        }
    }

    @Nested
    inner class SearchForAddressesTests {
        @Test
        fun `searchForAddresses calls the address repository search method and returns the results as AddressDataModels`() {
            // Arrange
            val houseNameOrNumber = "1"
            val postcode = "EG1 2AB"
            val restrictToEngland = true
            val matchingAddresses =
                listOf(
                    MockLandlordData.createAddress(uprn = 1),
                    MockLandlordData.createAddress(uprn = 2),
                    MockLandlordData.createAddress(uprn = 3),
                )

            whenever(mockAddressRepository.search(houseNameOrNumber, postcode, restrictToEngland)).thenReturn(matchingAddresses)

            // Act
            val results = addressService.searchForAddresses(houseNameOrNumber, postcode, restrictToEngland)

            // Assert
            verify(mockAddressRepository).search(houseNameOrNumber, postcode, restrictToEngland)

            val expectedResults = matchingAddresses.map { AddressDataModel.fromAddress(it) }
            assertEquals(expectedResults, results)
        }
    }

    companion object {
        @JvmStatic
        private fun provideAddressDataModels() =
            listOf(
                named("no local council", AddressDataModel("1 Example Road, EG1 2AB", localCouncilId = null)),
                named("a local council", AddressDataModel("1 Example Road, EG1 2AB", localCouncilId = 1)),
            )
    }
}

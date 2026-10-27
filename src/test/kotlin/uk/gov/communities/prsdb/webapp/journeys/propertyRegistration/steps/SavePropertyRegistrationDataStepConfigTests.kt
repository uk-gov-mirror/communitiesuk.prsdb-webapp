package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import jakarta.persistence.EntityExistsException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.exceptions.CyaDataHasChangedException
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationDataModelFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyRegistrationAddressTask
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.services.PropertyRegistrationService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockPropertyRegistrationData
import java.time.Instant
import kotlin.test.assertNotEquals

@ExtendWith(MockitoExtension::class)
class SavePropertyRegistrationDataStepConfigTests {
    @Mock
    private lateinit var mockPropertyRegistrationService: PropertyRegistrationService

    @Mock
    private lateinit var mockPropertyRegistrationDataModelFactory: PropertyRegistrationDataModelFactory

    @Mock
    private lateinit var mockState: PropertyRegistrationJourneyState

    @Mock
    private lateinit var mockAddressTask: PropertyRegistrationAddressTask

    @Mock
    private lateinit var mockPropertyDetailsTask: PropertyDetailsTask

    private lateinit var stepConfig: SavePropertyRegistrationDataStepConfig

    private val registrationData = MockPropertyRegistrationData.createPropertyRegistrationDataModel()

    @BeforeEach
    fun setUp() {
        stepConfig =
            SavePropertyRegistrationDataStepConfig(
                propertyRegistrationService = mockPropertyRegistrationService,
                propertyRegistrationDataModelFactory = mockPropertyRegistrationDataModelFactory,
            )
    }

    @Test
    fun `mode returns COMPLETE`() {
        // Act
        val result = stepConfig.mode(mockState)

        // Assert
        assertEquals(Complete.COMPLETE, result)
    }

    @Test
    fun `afterStepIsReached registers the property using the registration data from the factory`() {
        // Arrange
        whenever(mockPropertyRegistrationDataModelFactory.fromJourneyState(mockState)).thenReturn(registrationData)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(registrationData)
    }

    @Test
    fun `afterStepIsReached throws and does not register the property when answers changed since they were confirmed`() {
        // Arrange
        whenever(mockState.answersLastChanged).thenReturn(Instant.now())
        whenever(mockState.answersConfirmedAt).thenReturn(Instant.now().minusSeconds(60))

        // Act & Assert
        assertThrows<CyaDataHasChangedException> { stepConfig.afterStepIsReached(mockState) }
        verify(mockPropertyRegistrationService, never()).registerProperty(any())
    }

    @Test
    fun `afterStepIsReached registers the property when answers are unchanged since they were confirmed`() {
        // Arrange
        val confirmedAt = Instant.now()
        whenever(mockState.answersLastChanged).thenReturn(confirmedAt)
        whenever(mockState.answersConfirmedAt).thenReturn(confirmedAt)
        whenever(mockPropertyRegistrationDataModelFactory.fromJourneyState(mockState)).thenReturn(registrationData)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockPropertyRegistrationService).registerProperty(registrationData)
    }

    @Test
    fun `afterStepIsReached sets isAddressAlreadyRegistered when EntityExistsException`() {
        // Arrange
        whenever(mockPropertyRegistrationDataModelFactory.fromJourneyState(mockState)).thenReturn(registrationData)
        whenever(mockPropertyRegistrationService.registerProperty(registrationData))
            .thenThrow(EntityExistsException("Address already registered"))
        whenever(mockState.propertyDetailsTask).thenReturn(mockPropertyDetailsTask)
        whenever(mockPropertyDetailsTask.addressTask).thenReturn(mockAddressTask)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockAddressTask).isAddressAlreadyRegistered = true
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
}

package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpSession
import tools.jackson.databind.ObjectMapper
import uk.gov.communities.prsdb.webapp.journeys.JourneyStatePersistenceService
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyRegistrationPaymentQuote
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyRegistrationPaymentQuoteStateTests {
    @Test
    fun `payment quote survives saving and restoring journey state in a new session`() {
        // Arrange
        val persistenceService = mock<JourneyStatePersistenceService>()
        val journeyStateService = JourneyStateService(MockHttpSession(), mock(), persistenceService)
        journeyStateService.initialiseJourneyWithId("journey-1")
        journeyStateService.setJourneyId("journey-1")
        val journey = createJourney(journeyStateService)
        val quote = createQuote(LocalDate.of(2027, 10, 10))
        journey.paymentQuote = quote
        whenever(persistenceService.saveJourneyStateData(any(), eq("journey-1"))).thenReturn(mock())

        // Act
        journey.save()
        val stateCaptor = argumentCaptor<Any>()
        verify(persistenceService).saveJourneyStateData(stateCaptor.capture(), eq("journey-1"))
        val objectMapper = ObjectMapper()
        val serializedState = objectMapper.writeValueAsString(stateCaptor.firstValue)
        val restoredState = objectMapper.readValue(serializedState, Any::class.java)
        whenever(persistenceService.retrieveJourneyStateData("journey-1")).thenReturn(restoredState)
        val restoredService = JourneyStateService(MockHttpSession(), mock(), persistenceService)
        restoredService.setJourneyId("journey-1")
        val restoredJourney = createJourney(restoredService)

        // Assert
        assertTrue(serializedState.contains("paymentQuote"))
        assertEquals(quote, restoredJourney.paymentQuote)
    }

    private fun createJourney(service: JourneyStateService) =
        PropertyRegistrationJourney(
            taskListStep = mock(),
            licensingTask = mock(),
            occupied = mock(),
            propertyDetailsTask = mock(),
            ownershipAndLandlordsTask = mock(),
            ownershipAndLandlordsTaskBeforeAssociatedParties = mock(),
            correspondenceTask = mock(),
            tenancyDetailsTask = mock(),
            whoProvidesDetailsTask = mock(),
            gasSafetyTask = mock(),
            electricalSafetyTask = mock(),
            epcTask = mock(),
            cyaStep = mock(),
            finishCyaStep = mock(),
            occupancyChangeRoutingStep = mock(),
            occupancyChangeInterruptionStep = mock(),
            hasMissingComplianceStep = mock(),
            confirmMissingComplianceStep = mock(),
            whoProvidesUpdateRoutingStep = mock(),
            confirmChangeToLettingAgentStep = mock(),
            savePropertyRegistrationDataStep = mock(),
            paymentSummaryStep = mock(),
            paymentReturnStep = mock(),
            paymentStatusCheckStep = mock(),
            paymentFailedStep = mock(),
            journeyStateService = service,
            stateFactory = mock(),
        )

    private fun createQuote(quoteDate: LocalDate) =
        PropertyRegistrationPaymentQuote(
            amountInPence = 1234,
            quoteDate = quoteDate,
            renewalDate = LocalDate.of(2028, 3, 1),
        )
}

package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.database.entity.SavedJourneyState
import uk.gov.communities.prsdb.webapp.exceptions.CyaDataHasChangedException
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyMetadata
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState

class FinishCyaJourneyConfigTests {
    private val config = FinishCyaJourneyConfig()
    private val mockState = mock<CheckYourAnswersJourneyState>()
    private val mockOriginalState = mock<CheckYourAnswersJourneyState>()

    private val baseJourneyId = "base-journey-id"
    private val timestamp = Instant.fromEpochMilliseconds(1000)
    private val cyaRouteSegment = "lookup-address"

    @BeforeEach
    fun setUp() {
        whenever(mockState.baseJourneyId).thenReturn(baseJourneyId)
        whenever(mockState.getBaseJourneyState()).thenReturn(mockOriginalState)
        whenever(mockState.checkingAnswersFor).thenReturn(cyaRouteSegment)
    }

    @Nested
    inner class AfterStepIsReached {
        @Test
        fun `copies CYA journey data to the base journey when journey data has not changed since the user started cya`() {
            val metadata = JourneyMetadata(baseJourneyId, timestamp)
            whenever(mockOriginalState.journeyMetadata).thenReturn(metadata)
            whenever(mockState.originalJourneyUpdated).thenReturn(timestamp)

            config.afterStepIsReached(mockState)

            verify(mockState).copyJourneyTo(baseJourneyId)
        }

        @Test
        fun `throws CyaDataHasChangedException when journey data has changed since the user started cya`() {
            val metadata = JourneyMetadata(baseJourneyId, timestamp)
            val differentTimestamp = Instant.fromEpochMilliseconds(2000)
            whenever(mockOriginalState.journeyMetadata).thenReturn(metadata)
            whenever(mockState.originalJourneyUpdated).thenReturn(differentTimestamp)

            assertThrows<CyaDataHasChangedException> {
                config.afterStepIsReached(mockState)
            }
        }

        @Test
        fun `does not copy journey data when journey data has changed since the user started cya`() {
            val metadata = JourneyMetadata(baseJourneyId, timestamp)
            val differentTimestamp = Instant.fromEpochMilliseconds(2000)
            whenever(mockOriginalState.journeyMetadata).thenReturn(metadata)
            whenever(mockState.originalJourneyUpdated).thenReturn(differentTimestamp)

            runCatching { config.afterStepIsReached(mockState) }

            verify(mockState, never()).copyJourneyTo(baseJourneyId)
        }

        @Test
        fun `ensures CYA fields are not present in the parent state that will be saved`() {
            val metadata = JourneyMetadata(baseJourneyId, timestamp)
            whenever(mockOriginalState.journeyMetadata).thenReturn(metadata)
            whenever(mockState.originalJourneyUpdated).thenReturn(timestamp)

            config.afterStepIsReached(mockState)

            verify(mockOriginalState).clearCyaFields()
        }

        @Test
        fun `marks answers changed on the base journey after copying and before clearing CYA fields`() {
            val metadata = JourneyMetadata(baseJourneyId, timestamp)
            whenever(mockOriginalState.journeyMetadata).thenReturn(metadata)
            whenever(mockState.originalJourneyUpdated).thenReturn(timestamp)

            config.afterStepIsReached(mockState)

            val inOrder = inOrder(mockState, mockOriginalState)
            inOrder.verify(mockState).copyJourneyTo(baseJourneyId)
            inOrder.verify(mockOriginalState).markAnswersChanged()
            inOrder.verify(mockOriginalState).clearCyaFields()
        }

        @Test
        fun `does not mark answers changed when journey data has changed since the user started cya`() {
            val metadata = JourneyMetadata(baseJourneyId, timestamp)
            whenever(mockOriginalState.journeyMetadata).thenReturn(metadata)
            whenever(mockState.originalJourneyUpdated).thenReturn(Instant.fromEpochMilliseconds(2000))

            runCatching { config.afterStepIsReached(mockState) }

            verify(mockOriginalState, never()).markAnswersChanged()
        }
    }

    @Nested
    inner class SaveState {
        @Test
        fun `saves the base journey state to the database`() {
            val mockSavedState = mock<SavedJourneyState>()
            whenever(mockOriginalState.save()).thenReturn(mockSavedState)

            config.saveState(mockState)

            verify(mockOriginalState).save()
        }
    }

    @Nested
    inner class ResolveNextDestination {
        @Test
        fun `returns the CYA page destination`() {
            val cyaDestination = Destination.StepRoute("cya-step", baseJourneyId)
            whenever(mockState.returnToCyaPageDestination).thenReturn(cyaDestination)
            whenever(mockOriginalState.cyaJourneys).thenReturn(mapOf(cyaRouteSegment to "cya-journey-id"))

            val result = config.resolveNextDestination(mockState, Destination.Nowhere())

            assertEquals(cyaDestination, result)
        }

        @Test
        fun `clears the checkingAnswersFor on the original state`() {
            whenever(mockState.returnToCyaPageDestination).thenReturn(Destination.Nowhere())
            whenever(mockOriginalState.cyaJourneys).thenReturn(mapOf(cyaRouteSegment to "cya-journey-id"))

            config.resolveNextDestination(mockState, Destination.Nowhere())

            verify(mockOriginalState).checkingAnswersFor = null
        }

        @Test
        fun `removes the CYA journey entry from the original state`() {
            whenever(mockState.returnToCyaPageDestination).thenReturn(Destination.Nowhere())
            whenever(mockOriginalState.cyaJourneys).thenReturn(mapOf(cyaRouteSegment to "cya-journey-id"))

            config.resolveNextDestination(mockState, Destination.Nowhere())

            verify(mockOriginalState, atLeastOnce()).cyaJourneys = emptyMap()
        }

        @Test
        fun `deletes the CYA journey`() {
            whenever(mockState.returnToCyaPageDestination).thenReturn(Destination.Nowhere())
            whenever(mockOriginalState.cyaJourneys).thenReturn(mapOf(cyaRouteSegment to "cya-journey-id"))

            config.resolveNextDestination(mockState, Destination.Nowhere())

            verify(mockState).deleteJourney()
        }
    }
}

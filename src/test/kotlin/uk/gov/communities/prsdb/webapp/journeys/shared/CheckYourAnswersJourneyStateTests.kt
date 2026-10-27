package uk.gov.communities.prsdb.webapp.journeys.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.journeys.DelegateKeyRegistry
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyMetadata
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FinishCyaJourneyStep
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState

class CheckYourAnswersJourneyStateTests {
    @Nested
    inner class IsCheckingAnswers {
        @Test
        fun `returns true when checkingAnswersFor is not null`() {
            val state = createTestState(checkingAnswersFor = "some-step")
            assertTrue(state.isCheckingAnswers)
        }

        @Test
        fun `returns false when checkingAnswersFor is null`() {
            val state = createTestState(checkingAnswersFor = null)
            assertFalse(state.isCheckingAnswers)
        }
    }

    @Nested
    inner class BaseJourneyId {
        @Test
        fun `returns baseJourneyId from journeyMetadata when it is set`() {
            val state = createTestState(journeyMetadata = JourneyMetadata.createNew("dataKey", baseJourneyId = "base-id"))
            assertEquals("base-id", state.baseJourneyId)
        }

        @Test
        fun `returns journeyId when journeyMetadata baseJourneyId is null`() {
            val state = createTestState(journeyId = "current-id", journeyMetadata = JourneyMetadata.createNew("dataKey"))
            assertEquals("current-id", state.baseJourneyId)
        }
    }

    @Nested
    inner class ReturnToCyaPageDestination {
        @Test
        fun `getter returns StepRoute when cyaRouteSegment is set`() {
            val state =
                createTestState(
                    cyaRouteSegment = "check-answers",
                    journeyMetadata = JourneyMetadata.createNew("dataKey", baseJourneyId = "base-id"),
                )
            val destination = state.returnToCyaPageDestination

            assertTrue(destination is Destination.StepRoute)
            with(destination as Destination.StepRoute) {
                assertEquals("check-answers", routeSegment)
                assertEquals("base-id", journeyId)
            }
        }

        @Test
        fun `getter returns Nowhere when cyaRouteSegment is null`() {
            val state = createTestState(cyaRouteSegment = null)
            assertTrue(state.returnToCyaPageDestination is Destination.Nowhere)
        }

        @Test
        fun `setter extracts routeSegment from a StepRoute destination`() {
            val state = createTestState()
            state.returnToCyaPageDestination = Destination.StepRoute("my-route", "some-journey-id")
            assertEquals("my-route", state.cyaUrlPath)
        }

        @Test
        fun `setter extracts routeSegment from a VisitableStep destination`() {
            val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
            whenever(mockStep.urlPath).thenReturn("step-route")

            val state = createTestState()
            state.returnToCyaPageDestination = Destination.VisitableStep(mockStep, "some-journey-id")
            assertEquals("step-route", state.cyaUrlPath)
        }

        @Test
        fun `setter sets cyaRouteSegment to null for other destination types`() {
            val state = createTestState(cyaRouteSegment = "existing-route")
            state.returnToCyaPageDestination = Destination.Nowhere()
            assertEquals(null, state.cyaUrlPath)
        }
    }

    @Nested
    inner class GetCyaJourneyId {
        @Test
        fun `returns existing journey ID if one already exists for the step`() {
            val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
            whenever(mockStep.urlPath).thenReturn("existing-step")

            val state = createTestState(cyaJourneys = mutableMapOf("existing-step" to "existing-cya-journey-id"))
            val result = state.getCyaJourneyId(mockStep)

            assertEquals("existing-cya-journey-id", result)
        }

        @Test
        fun `creates a new journey ID if one does not exist for the step`() {
            val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
            whenever(mockStep.urlPath).thenReturn("new-step")

            val mockCyaStep = mock<JourneyStep.RequestableStep<*, *, *>>()

            val state = createTestState(cyaJourneys = mutableMapOf(), cyaStep = mockCyaStep)
            val result = state.getCyaJourneyId(mockStep)

            assertNotNull(result)
            assertTrue(state.cyaJourneys.containsKey("new-step"))
        }

        @Test
        fun `sets checkingAnswersFor on the child journey state`() {
            val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
            whenever(mockStep.urlPath).thenReturn("new-step")

            val mockCyaStep = mock<JourneyStep.RequestableStep<*, *, *>>()
            whenever(mockCyaStep.urlPath).thenReturn("check-answers")

            val state = createTestState(cyaJourneys = mutableMapOf(), cyaStep = mockCyaStep)
            state.getCyaJourneyId(mockStep)

            assertEquals("new-step", state.childState?.checkingAnswersFor)
        }

        @Test
        fun `sets returnToCyaPageDestination on the child journey state`() {
            val mockStep = mock<JourneyStep.RequestableStep<*, *, *>>()
            whenever(mockStep.urlPath).thenReturn("new-step")

            val mockCyaStep = mock<JourneyStep.RequestableStep<*, *, *>>()
            whenever(mockCyaStep.urlPath).thenReturn("check-answers")

            val originalJourneyId = "base-id"

            val state =
                createTestState(
                    journeyId = originalJourneyId,
                    cyaJourneys = mutableMapOf(),
                    cyaStep = mockCyaStep,
                    journeyMetadata = JourneyMetadata.createNew("dataKey", baseJourneyId = originalJourneyId),
                )
            state.getCyaJourneyId(mockStep)

            val childDestination = state.childState?.returnToCyaPageDestination
            assertTrue(childDestination is Destination.StepRoute)
            with(childDestination as Destination.StepRoute) {
                assertEquals(mockCyaStep.urlPath, routeSegment)
                assertEquals(originalJourneyId, journeyId)
            }
        }
    }

    private fun createTestState(
        journeyId: String = "test-journey-id",
        journeyMetadata: JourneyMetadata = JourneyMetadata.createNew("dataKey"),
        checkingAnswersFor: String? = null,
        cyaRouteSegment: String? = null,
        cyaJourneys: MutableMap<String, String> = mutableMapOf(),
        cyaStep: JourneyStep.RequestableStep<*, *, *> = mock(),
    ): TestCheckYourAnswersJourneyState =
        TestCheckYourAnswersJourneyState(
            testJourneyId = journeyId,
            testJourneyMetadata = journeyMetadata,
            initialCheckingAnswersFor = checkingAnswersFor,
            initialCyaRouteSegment = cyaRouteSegment,
            initialCyaJourneys = cyaJourneys,
            testCyaStep = cyaStep,
        )

    class TestCheckYourAnswersJourneyState(
        private val testJourneyId: String = "test-journey-id",
        private val testJourneyMetadata: JourneyMetadata = JourneyMetadata.createNew("dataKey"),
        initialCheckingAnswersFor: String? = null,
        initialCyaRouteSegment: String? = null,
        initialCyaJourneys: MutableMap<String, String> = mutableMapOf(),
        private val testCyaStep: JourneyStep.RequestableStep<*, *, *> = mock(),
    ) : CheckYourAnswersJourneyState {
        override val finishCyaStep: FinishCyaJourneyStep = mock()
        override val cyaStep: JourneyStep.RequestableStep<*, *, *> = testCyaStep
        override var originalJourneyUpdated: Instant? = Clock.System.now()
        override var cyaJourneys: Map<String, String> = initialCyaJourneys
        override var cyaUrlPath: String? = initialCyaRouteSegment
        override var checkingAnswersFor: String? = initialCheckingAnswersFor

        override val journeyId: String = testJourneyId
        override val journeyMetadata: JourneyMetadata = testJourneyMetadata
        override val stateFactory: ObjectFactory<TestCheckYourAnswersJourneyState> =
            mock<ObjectFactory<TestCheckYourAnswersJourneyState>>().apply {
                whenever(getObject()).thenAnswer {
                    val child =
                        TestCheckYourAnswersJourneyState(
                            testJourneyMetadata = JourneyMetadata.createNew("dataKey", baseJourneyId = journeyId),
                            testCyaStep = testCyaStep,
                        )
                    childState = child
                    child
                }
            }

        var childState: TestCheckYourAnswersJourneyState? = null

        override fun getBaseJourneyState(): CheckYourAnswersJourneyState = this

        override fun createChildJourneyState(childJourneyId: String): CheckYourAnswersJourneyState {
            val child =
                TestCheckYourAnswersJourneyState(
                    testJourneyId = childJourneyId,
                    testJourneyMetadata = JourneyMetadata.createNew("dataKey", baseJourneyId = journeyId),
                    testCyaStep = testCyaStep,
                )
            childState = child
            return child
        }

        override fun generateJourneyId(seed: Any?): String = "generated-${seed.hashCode()}"

        override fun getStepData(key: String) = null

        override fun addStepData(
            key: String,
            value: Map<String, Any?>,
        ) {}

        override fun clearStepData(key: String) {}

        override val answersLastChanged: Instant? = null
        override var answersConfirmedAt: Instant? = null

        override fun markAnswersChanged() {}

        override fun getSubmittedStepData(): Map<String, Any?> = emptyMap()

        override fun deleteJourney() {}

        override fun initializeState(seed: Any?): String = "state-id"

        override fun initializeOrRestoreState(seed: Any?): String = "state-id"

        override fun save() = mock<uk.gov.communities.prsdb.webapp.database.entity.SavedJourneyState>()

        override fun setJourneyId(newJourneyId: String) {}

        override fun copyJourneyTo(newJourneyId: String) {}

        override fun bindKeyRegistry(registry: DelegateKeyRegistry) {}
    }
}

package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.Arguments.arguments
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.constants.enums.WhoProvidesRentalDetails
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.NoParents
import uk.gov.communities.prsdb.webapp.journeys.Parentage
import uk.gov.communities.prsdb.webapp.journeys.SubjourneyExitStep
import uk.gov.communities.prsdb.webapp.journeys.SubjourneyExitStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Task
import uk.gov.communities.prsdb.webapp.journeys.builders.TaskInitialiser
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStepConfig
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupiedStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupiedStepConfig
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.WhoProvidesRentalDetailsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.WhoProvidesRentalDetailsStepConfig
import uk.gov.communities.prsdb.webapp.journeys.shared.YesOrNo
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.AlwaysTrueValidator

class WhoProvidesDetailsTaskTests {
    @Test
    fun `landlord choice routes to exit step and completes the task`() {
        val task =
            buildTask(
                provider = WhoProvidesRentalDetails.LANDLORD,
                epcComplete = true,
                occupied = YesOrNo.YES,
            )

        val destination = task.whoProvidesRentalDetailsStep.getNextDestination()

        assertTrue(destination is Destination.NavigationalStep)
        assertSame(task.exitStep, (destination as Destination.NavigationalStep).step)
        assertTrue(task.isComplete().allowsChild())
    }

    @Test
    fun `letting agent choice routes to allow letting agent step and leaves task incomplete`() {
        val task =
            buildTask(
                provider = WhoProvidesRentalDetails.LETTING_AGENT,
                epcComplete = true,
                occupied = YesOrNo.YES,
            )

        val destination = task.whoProvidesRentalDetailsStep.getNextDestination()

        assertTrue(destination is Destination.VisitableStep)
        assertSame(task.lettingAgentEmailStep, (destination as Destination.VisitableStep).step)
        assertTrue(task.lettingAgentEmailStep.isStepReachable)
        assertFalse(task.isComplete().allowsChild())
    }

    private fun buildTask(
        provider: WhoProvidesRentalDetails,
        epcComplete: Boolean,
        occupied: YesOrNo?,
    ): WhoProvidesDetailsTask {
        val journeyStateService = mock<JourneyStateService>()
        whenever(journeyStateService.journeyId).thenReturn("journey-id")
        whenever(journeyStateService.getSubmittedStepData()).thenReturn(
            mapOf(
                WhoProvidesRentalDetailsStep.ROUTE_SEGMENT to
                    mapOf(
                        "whoProvides" to provider.name,
                    ),
            ),
        )

        val task =
            WhoProvidesDetailsTask(
                journeyStateService = journeyStateService,
                whoProvidesRentalDetailsStep = WhoProvidesRentalDetailsStep(whoProvidesRentalDetailsStepStepConfig()),
                lettingAgentEmailStep = LettingAgentEmailStep(lettingAgentEmailStepStepConfig()),
            )

        val epcTask = mock<Task<*, *>>()
        val epcExitStep = initializedTaskExitStep(epcComplete)
        whenever(epcTask.exitStep).thenReturn(epcExitStep)

        val occupiedStep = initializedOccupiedStep(occupied)

        task.bindDependencies(
            object : WhoProvidesDetailsDependencies {
                override var cachedWhoProvidesRentalDetails: WhoProvidesRentalDetails? = null
                override val loggedInLandlordEmail: String? = "original.landlord@example.com"
            },
        )

        TaskInitialiser(task, task)
            .apply {
                initialStep()
                nextDestination { Destination.Nowhere() }
                unreachableStepDestinationIfNotSet { Destination.Nowhere() }
            }.build()

        return task
    }

    private fun initializedTaskExitStep(isComplete: Boolean): SubjourneyExitStep {
        val state = mock<JourneyState>()
        whenever(state.journeyId).thenReturn("journey-id")

        return SubjourneyExitStep(SubjourneyExitStepConfig()).apply {
            initialize(
                path = null,
                state = state,
                backDestinationOverride = null,
                redirectDestinationProvider = { Destination.Nowhere() },
                parentage = if (isComplete) NoParents() else unreachableParentage(),
                unreachableStepDestinationProvider = { Destination.Nowhere() },
                shouldSaveOnCompletion = false,
            )
        }
    }

    private fun initializedOccupiedStep(occupied: YesOrNo?): OccupiedStep {
        val state = mock<uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.OccupationState>()
        whenever(state.journeyId).thenReturn("journey-id")
        whenever(state.getStepData(OccupiedStep.ROUTE_SEGMENT)).thenReturn(
            when (occupied) {
                YesOrNo.YES -> mapOf("occupied" to true)
                YesOrNo.NO -> mapOf("occupied" to false)
                null -> null
            },
        )

        return OccupiedStep(OccupiedStepConfig().apply { validator = AlwaysTrueValidator() }).apply {
            initialize(
                path = OccupiedStep.ROUTE_SEGMENT,
                state = state,
                backDestinationOverride = null,
                redirectDestinationProvider = { Destination.Nowhere() },
                parentage = NoParents(),
                unreachableStepDestinationProvider = { Destination.Nowhere() },
                shouldSaveOnCompletion = false,
            )
        }
    }

    private fun unreachableParentage(): Parentage =
        mock<Parentage>().apply {
            whenever(allowsChild()).thenReturn(false)
            whenever(ancestry).thenReturn(emptyList())
            whenever(allowingParentSteps).thenReturn(emptyList())
            whenever(potentialParents).thenReturn(emptyList())
        }

    private fun whoProvidesRentalDetailsStepStepConfig() =
        WhoProvidesRentalDetailsStepConfig().apply {
            validator = AlwaysTrueValidator()
        }

    private fun lettingAgentEmailStepStepConfig() =
        LettingAgentEmailStepConfig().apply {
            validator = AlwaysTrueValidator()
        }

    companion object {
        @JvmStatic
        private fun provideReachabilityScenarios() =
            arrayOf(
                arguments(true, YesOrNo.YES, true),
                arguments(true, YesOrNo.NO, false),
                arguments(false, YesOrNo.YES, false),
                arguments(false, YesOrNo.NO, false),
                arguments(true, null, false),
            )
    }
}

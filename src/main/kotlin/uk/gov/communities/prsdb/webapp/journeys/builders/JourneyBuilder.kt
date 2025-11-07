package uk.gov.communities.prsdb.webapp.journeys.builders

import uk.gov.communities.prsdb.webapp.exceptions.JourneyInitialisationException
import uk.gov.communities.prsdb.webapp.journeys.AbstractStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.StepInitialisationStage
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.Task

class JourneyBuilder<TState : JourneyState>(
    // The state is referred to here as the "journey" so that in the DSL steps can be referenced as `journey.stepName`
    val journey: TState,
) {
    fun getStepInitialisers() = stepsUnderConstruction.toList()

    private val stepsUnderConstruction: MutableList<StepInitialiser<*, TState, *>> = mutableListOf()
    private var unreachableStepDestination: (() -> Destination)? = null

    fun build(): Map<String, StepLifecycleOrchestrator> =
        buildMap {
            stepsUnderConstruction.forEach { step ->
                val journeyStep = step.build(journey, unreachableStepDestination)
                checkForUninitialisedParents(step)
                when (journeyStep) {
                    is JourneyStep.VisitableStep<*, *, TState> -> put(journeyStep.routeSegment, StepLifecycleOrchestrator(journeyStep))
                    is JourneyStep.NotionalStep<*, *, TState> -> {}
                }
            }
        }

    fun <TMode : Enum<TMode>, TStep : AbstractStepConfig<TMode, *, TState>> step(
        segment: String,
        uninitialisedStep: JourneyStep.VisitableStep<TMode, *, TState>,
        init: StepInitialiser<TStep, TState, TMode>.() -> Unit,
    ) {
        val stepInitialiser = StepInitialiser<TStep, TState, TMode>(segment, uninitialisedStep)
        stepInitialiser.init()
        stepsUnderConstruction.add(stepInitialiser)
    }

    fun <TMode : Enum<TMode>, TStep : AbstractStepConfig<TMode, *, TState>> notionalStep(
        uninitialisedStep: JourneyStep.NotionalStep<TMode, *, TState>,
        init: StepInitialiser<TStep, TState, TMode>.() -> Unit,
    ) {
        val stepInitialiser = StepInitialiser<TStep, TState, TMode>(null, uninitialisedStep)
        stepInitialiser.init()
        stepsUnderConstruction.add(stepInitialiser)
    }

    fun <TMode : Enum<TMode>> task(
        uninitialisedTask: Task<TMode, TState>,
        init: TaskInitialiser<TMode, TState>.() -> Unit,
    ) {
        val taskInitialiser = TaskInitialiser(uninitialisedTask)
        taskInitialiser.init()
        val taskSteps = taskInitialiser.mapToStepInitialisers(journey)
        stepsUnderConstruction.addAll(taskSteps)
    }

    fun unreachableStepUrl(getUrl: () -> String) {
        if (unreachableStepDestination != null) {
            throw JourneyInitialisationException("unreachableStepDestination has already been set")
        }
        unreachableStepDestination = { Destination.ExternalUrl(getUrl()) }
    }

    fun unreachableStepStep(getStep: () -> JourneyStep<*, *, *>) {
        if (unreachableStepDestination != null) {
            throw JourneyInitialisationException("unreachableStepDestination has already been set")
        }
        unreachableStepDestination = { Destination(getStep()) }
    }

    fun unreachableStepDestination(getDestination: () -> Destination) {
        if (unreachableStepDestination != null) {
            throw JourneyInitialisationException("unreachableStepDestination has already been set")
        }
        unreachableStepDestination = getDestination
    }

    private fun checkForUninitialisedParents(stepInitialiser: StepInitialiser<*, *, *>) {
        val uninitialisedParents =
            stepInitialiser.potentialParents.filter {
                it.initialisationStage != StepInitialisationStage.FULLY_INITIALISED
            }
        if (uninitialisedParents.any()) {
            val parentNames = uninitialisedParents.joinToString { "\n- $it" }
            throw JourneyInitialisationException(
                "Step ${stepInitialiser.segment} has uninitialised potential parents on initialisation: $parentNames\n" +
                    "This could imply a dependency loop, or that these two steps are declared in the wrong order.",
            )
        }
    }

    companion object {
        fun <TState : JourneyState> journey(
            state: TState,
            init: JourneyBuilder<TState>.() -> Unit,
        ): Map<String, StepLifecycleOrchestrator> {
            val builder = JourneyBuilder(state)
            builder.init()
            return builder.build()
        }

        fun <TState : JourneyState> subJourney(
            state: TState,
            init: JourneyBuilder<TState>.() -> Unit = {},
        ): List<StepInitialiser<*, TState, *>> {
            val builder = JourneyBuilder(state)
            builder.init()
            return builder.getStepInitialisers()
        }
    }
}

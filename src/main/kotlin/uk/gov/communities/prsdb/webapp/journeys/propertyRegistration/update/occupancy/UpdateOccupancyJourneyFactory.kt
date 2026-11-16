package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.occupancy

import kotlinx.datetime.Instant
import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractPropertyOwnershipUpdateJourneyState
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.OrParents
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder.Companion.journey
import uk.gov.communities.prsdb.webapp.journeys.hasOutcome
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.OccupationState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FinishCyaJourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OccupiedStep
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@PrsdbWebService
class UpdateOccupancyJourneyFactory(
    private val stateFactory: ObjectFactory<UpdateOccupancyJourney>,
    private val propertyOwnershipService: PropertyOwnershipService,
) {
    final fun createJourneySteps(propertyId: Long): Map<String, StepLifecycleOrchestrator> {
        val state = stateFactory.getObject()

        if (!state.isStateInitialized) {
            val propertyOwnership = propertyOwnershipService.getPropertyOwnership(propertyId)
            state.propertyId = propertyId
            state.lastModifiedDate = propertyOwnership.getMostRecentlyUpdated().toString()
            state.propertyIsOccupied = propertyOwnership.isOccupied
            state.isStateInitialized = true
        }

        if (state.propertyId != propertyId) {
            throw PrsdbWebException("Journey state propertyId ${state.propertyId} does not match provided propertyId $propertyId")
        }

        val checkingAnswersFor = state.checkingAnswersFor
        return journeyMap(state, checkingAnswersFor, propertyId)
    }

    private fun isDelegatedToLettingAgent(propertyId: Long): Boolean = propertyOwnershipService.hasLettingAgent(propertyId)

    private fun journeyMap(
        state: UpdateOccupancyJourney,
        checkingAnswersFor: String?,
        propertyId: Long,
    ): Map<String, StepLifecycleOrchestrator> =
        if (checkingAnswersFor == null) {
            mainJourneyMap(state, propertyId)
        } else {
            checkYourAnswersJourneyMap(state, checkingAnswersFor, propertyId)
        }

    private fun mainJourneyMap(
        state: UpdateOccupancyJourney,
        propertyId: Long,
    ): Map<String, StepLifecycleOrchestrator> {
        val propertyDetailsRoute = PropertyDetailsController.getPropertyDetailsPath(propertyId)

        return journey(state) {
            unreachableStepUrl { propertyDetailsRoute }
            step(journey.occupied) {
                routeSegment(OccupiedStep.ROUTE_SEGMENT)
                initialStep()
                backUrl { propertyDetailsRoute }
                nextStep { journey.occupancyUpdateRoutingStep }
                withAdditionalContentProperties {
                    mapOf("submitButtonText" to "forms.buttons.saveAndContinue")
                }
            }
            step<OccupancyUpdateRouteMode, OccupancyUpdateRoutingStepConfig>(journey.occupancyUpdateRoutingStep) {
                stepSpecificInitialisation {
                    usingCurrentDelegation { isDelegatedToLettingAgent(propertyId) }
                }
                parents { journey.occupied.isComplete() }
                nextDestination { mode ->
                    when (mode) {
                        OccupancyUpdateRouteMode.NO_INTERRUPTION -> Destination(journey.checkYourAnswersStep)
                        OccupancyUpdateRouteMode.SHOW_INTERRUPTION -> Destination(journey.lettingAgentInterruptionStep)
                    }
                }
            }
            step(journey.lettingAgentInterruptionStep) {
                routeSegment(OccupancyLettingAgentInterruptionStep.ROUTE_SEGMENT)
                parents { journey.occupancyUpdateRoutingStep.hasOutcome(OccupancyUpdateRouteMode.SHOW_INTERRUPTION) }
                nextStep { journey.checkYourAnswersStep }
            }
            step(journey.checkYourAnswersStep) {
                routeSegment(UpdateOccupancyCheckYourAnswersStep.ROUTE_SEGMENT)
                parents {
                    OrParents(
                        journey.occupancyUpdateRoutingStep.hasOutcome(OccupancyUpdateRouteMode.NO_INTERRUPTION),
                        journey.lettingAgentInterruptionStep.isComplete(),
                    )
                }
                nextStep { journey.completeOccupancyUpdateStep }
            }
            step(journey.completeOccupancyUpdateStep) {
                parents { journey.checkYourAnswersStep.isComplete() }
                nextUrl { propertyDetailsRoute }
            }
        }
    }

    private fun checkYourAnswersJourneyMap(
        state: UpdateOccupancyJourney,
        checkingAnswersFor: String,
        propertyId: Long,
    ): Map<String, StepLifecycleOrchestrator> {
        val propertyDetailsRoute = PropertyDetailsController.getPropertyDetailsPath(propertyId)

        return journey(state) {
            unreachableStepUrl { propertyDetailsRoute }
            configureFirst { backDestination { journey.returnToCyaPageDestination } }
            when (checkingAnswersFor) {
                OccupiedStep.ROUTE_SEGMENT -> {
                    step(journey.occupied) {
                        initialStep()
                        routeSegment(OccupiedStep.ROUTE_SEGMENT)
                        nextStep { journey.occupancyUpdateRoutingStep }
                        withAdditionalContentProperties {
                            mapOf("submitButtonText" to "forms.buttons.saveAndContinue")
                        }
                    }
                    step<OccupancyUpdateRouteMode, OccupancyUpdateRoutingStepConfig>(journey.occupancyUpdateRoutingStep) {
                        stepSpecificInitialisation {
                            usingCurrentDelegation { isDelegatedToLettingAgent(propertyId) }
                        }
                        parents { journey.occupied.isComplete() }
                        nextDestination { mode ->
                            when (mode) {
                                OccupancyUpdateRouteMode.NO_INTERRUPTION -> Destination(journey.finishCyaStep)
                                OccupancyUpdateRouteMode.SHOW_INTERRUPTION -> Destination(journey.lettingAgentInterruptionStep)
                            }
                        }
                    }
                    step(journey.lettingAgentInterruptionStep) {
                        routeSegment(OccupancyLettingAgentInterruptionStep.ROUTE_SEGMENT)
                        parents { journey.occupancyUpdateRoutingStep.hasOutcome(OccupancyUpdateRouteMode.SHOW_INTERRUPTION) }
                        nextStep { journey.finishCyaStep }
                    }
                }

                else -> {
                    throw IllegalStateException("Unknown step being checked: $checkingAnswersFor")
                }
            }
            step(journey.finishCyaStep) {
                initialStep()
                nextDestination { Destination.Nowhere() }
            }
        }
    }

    fun initializeJourneyState(
        seed: Any?,
        currentLastModifiedDate: java.time.Instant,
    ): String = stateFactory.getObject().initialiseOrRestoreStateReinitialisingIfOutdated(seed, currentLastModifiedDate)
}

@JourneyFrameworkComponent
class UpdateOccupancyJourney(
    // Occupancy task
    override val occupied: OccupiedStep,
    // Finish check-your-answers step
    override val finishCyaStep: FinishCyaJourneyStep,
    // Completion step for the single-page update
    override val completeOccupancyUpdateStep: CompleteOccupancyUpdateStep,
    // Routes past the interruption unless the property is being unoccupied while delegated to a letting agent
    override val occupancyUpdateRoutingStep: OccupancyUpdateRoutingStep,
    // Interruption shown when unoccupying a property that is delegated to a letting agent
    override val lettingAgentInterruptionStep: OccupancyLettingAgentInterruptionStep,
    // Check-your-answers step for the update
    override val checkYourAnswersStep: UpdateOccupancyCheckYourAnswersStep,
    journeyStateService: JourneyStateService,
    journeyName: String = "occupancy",
    override val stateFactory: ObjectFactory<UpdateOccupancyJourneyState>,
) : AbstractPropertyOwnershipUpdateJourneyState(journeyStateService, journeyName),
    UpdateOccupancyJourneyState {
    override val cyaStep: JourneyStep.RequestableStep<*, *, *>
        get() = checkYourAnswersStep

    override var propertyId: Long by delegateProvider.requiredDelegate("propertyId")

    override var originalJourneyUpdated: Instant? by delegateProvider.nullableDelegate("originalJourneyUpdated")
    override var checkingAnswersFor: String? by delegateProvider.nullableDelegate("checkingAnswersFor")
    override var cyaJourneys: Map<String, String> = mapOf()

    override var cyaUrlPath: String? by delegateProvider.nullableDelegate("cyaRouteSegment")

    override var lastModifiedDate: String by delegateProvider.requiredImmutableDelegate(LAST_MODIFIED_DATE_KEY)

    override var propertyIsOccupied: Boolean by delegateProvider.requiredImmutableDelegate("wasOccupied")

    override var cachedOccupied: Boolean? by delegateProvider.nullableDelegate("cachedOccupied")
}

interface UpdateOccupancyJourneyState :
    OccupationState,
    CheckYourAnswersJourneyState {
    val completeOccupancyUpdateStep: CompleteOccupancyUpdateStep
    val occupancyUpdateRoutingStep: OccupancyUpdateRoutingStep
    val lettingAgentInterruptionStep: OccupancyLettingAgentInterruptionStep
    val checkYourAnswersStep: UpdateOccupancyCheckYourAnswersStep
    val propertyId: Long
    val lastModifiedDate: String
    val propertyIsOccupied: Boolean
}

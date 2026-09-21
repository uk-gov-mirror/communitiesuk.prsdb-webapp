package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.rentFrequencyAndAmount

import kotlinx.datetime.Instant
import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractPropertyOwnershipUpdateJourneyState
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder.Companion.journey
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FinishCyaJourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentAmountStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.RentFrequencyAndAmountTask
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState.Companion.checkAnswerTask
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@PrsdbWebService
class UpdateRentFrequencyAndAmountJourneyFactory(
    private val stateFactory: ObjectFactory<UpdateRentFrequencyAndAmountJourney>,
    private val propertyOwnershipService: PropertyOwnershipService,
) {
    final fun createJourneySteps(
        propertyId: Long,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> {
        val state = stateFactory.getObject()

        if (!state.isStateInitialized) {
            state.propertyId = propertyId
            state.lastModifiedDate = propertyOwnershipService.getLastModifiedDate(propertyId).toString()
            state.isStateInitialized = true
        }

        if (state.propertyId != propertyId) {
            throw PrsdbWebException("Journey state propertyId ${state.propertyId} does not match provided propertyId $propertyId")
        }

        val checkingAnswersFor = state.checkingAnswersFor
        return if (checkingAnswersFor == null) {
            mainJourneyMap(state, returnUrl)
        } else {
            checkYourAnswersJourneyMap(state, returnUrl)
        }
    }

    private fun mainJourneyMap(
        state: UpdateRentFrequencyAndAmountJourney,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepUrl { returnUrl }
            task(journey.rentFrequencyAndAmountTask) {
                initialStep()
                backUrl { returnUrl }
                nextStep { journey.cyaStep }
                withAdditionalContentProperty {
                    "title" to "propertyDetails.update.title"
                }
            }
            step(journey.cyaStep) {
                routeSegment(UpdateRentFrequencyAndAmountCyaStep.ROUTE_SEGMENT)
                parents { journey.rentFrequencyAndAmountTask.isComplete() }
                nextDestination {
                    Destination
                        .ExternalUrl(returnUrl)
                        .withFlashAttribute("updateSuccessBanner", "propertyDetails.updateSuccessBanner.tenancyDetails")
                }
            }
            configureStep(journey.rentFrequencyAndAmountTask.rentFrequency) {
                withAdditionalContentProperty {
                    "heading" to "forms.update.rentFrequency.heading"
                }
                withAdditionalContentProperty {
                    "submitButtonText" to "forms.buttons.continue"
                }
            }
            configureStep(journey.rentFrequencyAndAmountTask.rentAmount) {
                withAdditionalContentProperty {
                    "heading" to state.rentFrequencyAndAmountTask.getUpdateRentAmountHeading()
                }
                withAdditionalContentProperty {
                    "submitButtonText" to "forms.buttons.continue"
                }
            }
        }

    private fun checkYourAnswersJourneyMap(
        state: UpdateRentFrequencyAndAmountJourney,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            configureFirst { backDestination { journey.returnToCyaPageDestination } }
            unreachableStepUrl { returnUrl }
            configure {
                withAdditionalContentProperty { "title" to "propertyDetails.update.title" }
            }
            when (state.checkingAnswersFor) {
                RentAmountStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.rentFrequencyAndAmountTask)
                    configureStep(journey.rentFrequencyAndAmountTask.rentAmount) {
                        backDestination { journey.returnToCyaPageDestination }
                        withAdditionalContentProperty {
                            "heading" to state.rentFrequencyAndAmountTask.getUpdateRentAmountHeading()
                        }
                        withAdditionalContentProperty {
                            "submitButtonText" to "forms.buttons.continue"
                        }
                    }
                }

                else -> {
                    checkAnswerTask(journey.rentFrequencyAndAmountTask)
                    configureStep(journey.rentFrequencyAndAmountTask.rentFrequency) {
                        withAdditionalContentProperty {
                            "heading" to "forms.update.rentFrequency.heading"
                        }
                        withAdditionalContentProperty {
                            "submitButtonText" to "forms.buttons.continue"
                        }
                    }
                    configureStep(journey.rentFrequencyAndAmountTask.rentAmount) {
                        withAdditionalContentProperty {
                            "heading" to state.rentFrequencyAndAmountTask.getUpdateRentAmountHeading()
                        }
                        withAdditionalContentProperty {
                            "submitButtonText" to "forms.buttons.continue"
                        }
                    }
                }
            }
            step(journey.finishCyaStep) {
                parents { journey.rentFrequencyAndAmountTask.isComplete() }
                nextDestination { Destination.Nowhere() }
            }
        }

    fun initialiseJourneyState(
        seed: Any?,
        currentLastModifiedDate: java.time.Instant,
    ): String = stateFactory.getObject().initialiseOrRestoreStateReinitialisingIfOutdated(seed, currentLastModifiedDate)
}

@JourneyFrameworkComponent
class UpdateRentFrequencyAndAmountJourney(
    // RentFrequencyAndAmount task
    override val rentFrequencyAndAmountTask: RentFrequencyAndAmountTask,
    // Check your answers step
    override val cyaStep: UpdateRentFrequencyAndAmountCyaStep,
    journeyStateService: JourneyStateService,
    journeyName: String = "rent frequency and amount",
    override val finishCyaStep: FinishCyaJourneyStep,
    override val stateFactory: ObjectFactory<UpdateRentFrequencyAndAmountJourneyState>,
) : AbstractPropertyOwnershipUpdateJourneyState(journeyStateService, journeyName),
    UpdateRentFrequencyAndAmountJourneyState {
    override var propertyId: Long by delegateProvider.requiredImmutableDelegate("propertyId")
    override var lastModifiedDate: String by delegateProvider.requiredImmutableDelegate(LAST_MODIFIED_DATE_KEY)
    override var cyaJourneys: Map<String, String> = mapOf()
    override var checkingAnswersFor: String? by delegateProvider.nullableDelegate("checkingAnswersFor")
    override var cyaUrlPath: String? by delegateProvider.nullableDelegate("cyaRouteSegment")
    override var originalJourneyUpdated: Instant? by delegateProvider.nullableDelegate("originalJourneyUpdated")
}

interface UpdateRentFrequencyAndAmountJourneyState : CheckYourAnswersJourneyState {
    val rentFrequencyAndAmountTask: RentFrequencyAndAmountTask
    override val cyaStep: UpdateRentFrequencyAndAmountCyaStep
    val propertyId: Long
    val lastModifiedDate: String
}

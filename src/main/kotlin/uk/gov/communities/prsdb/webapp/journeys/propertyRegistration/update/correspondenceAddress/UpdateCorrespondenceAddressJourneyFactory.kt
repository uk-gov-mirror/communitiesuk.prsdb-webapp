package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.correspondenceAddress

import kotlinx.datetime.Instant
import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractPropertyOwnershipUpdateJourneyState
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder.Companion.journey
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FinishCyaJourneyStep
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState.Companion.checkAnswerTask
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.CorrespondenceAddressTask
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@PrsdbWebService
class UpdateCorrespondenceAddressJourneyFactory(
    private val stateFactory: ObjectFactory<UpdateCorrespondenceAddressJourney>,
    private val propertyOwnershipService: PropertyOwnershipService,
) {
    fun initialiseJourneyState(
        seed: Any?,
        currentLastModifiedDate: java.time.Instant,
    ): String = stateFactory.getObject().initialiseOrRestoreStateReinitialisingIfOutdated(seed, currentLastModifiedDate)

    final fun createJourneySteps(propertyId: Long): Map<String, StepLifecycleOrchestrator> {
        val state = stateFactory.getObject()

        if (!state.isStateInitialized) {
            state.propertyId = propertyId
            state.lastModifiedDate = propertyOwnershipService.getLastModifiedDate(propertyId).toString()
            state.isStateInitialized = true
        }

        if (state.propertyId != propertyId) {
            throw PrsdbWebException("Journey state propertyId ${state.propertyId} does not match provided propertyId $propertyId")
        }

        val returnUrl = PropertyDetailsController.getPropertyDetailsPath(propertyId)
        val checkingAnswersFor = state.checkingAnswersFor
        return if (checkingAnswersFor == null) {
            mainJourneyMap(state, returnUrl)
        } else {
            checkYourAnswersJourneyMap(state, checkingAnswersFor, returnUrl)
        }
    }

    private fun mainJourneyMap(
        state: UpdateCorrespondenceAddressJourney,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepUrl { returnUrl }
            configure {
                withAdditionalContentProperty { "title" to "propertyDetails.update.title" }
            }
            task(journey.addressTask) {
                initialStep()
                backUrl { returnUrl }
                nextStep { journey.cyaStep }
            }
            step(journey.cyaStep) {
                routeSegment(UpdateCorrespondenceAddressCyaStep.ROUTE_SEGMENT)
                parents { journey.addressTask.isComplete() }
                nextDestination { Destination.ExternalUrl(returnUrl) }
            }
        }

    private fun checkYourAnswersJourneyMap(
        state: UpdateCorrespondenceAddressJourney,
        checkingAnswersFor: String,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepUrl { returnUrl }
            configure {
                withAdditionalContentProperty { "title" to "propertyDetails.update.title" }
            }
            configureFirst { backDestination { journey.returnToCyaPageDestination } }
            when (checkingAnswersFor) {
                LookupAddressStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.addressTask)
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

@JourneyFrameworkComponent
class UpdateCorrespondenceAddressJourney(
    override val addressTask: CorrespondenceAddressTask,
    override val cyaStep: UpdateCorrespondenceAddressCyaStep,
    override val finishCyaStep: FinishCyaJourneyStep,
    journeyStateService: JourneyStateService,
    override val stateFactory: ObjectFactory<UpdateCorrespondenceAddressJourney>,
) : AbstractPropertyOwnershipUpdateJourneyState(journeyStateService, "correspondence address"),
    UpdateCorrespondenceAddressJourneyState {
    override var propertyId: Long by delegateProvider.requiredImmutableDelegate("propertyId")
    override var lastModifiedDate: String by delegateProvider.requiredImmutableDelegate(LAST_MODIFIED_DATE_KEY)
    override var originalJourneyUpdated: Instant? by delegateProvider.nullableDelegate("originalJourneyUpdated")
    override var cyaJourneys: Map<String, String> = mapOf()
    override var checkingAnswersFor: String? by delegateProvider.nullableDelegate("checkingAnswersFor")
    override var cyaUrlPath: String? by delegateProvider.nullableDelegate("cyaRouteSegment")
}

interface UpdateCorrespondenceAddressJourneyState : CheckYourAnswersJourneyState {
    val addressTask: CorrespondenceAddressTask
    override val cyaStep: UpdateCorrespondenceAddressCyaStep
    val propertyId: Long
    val lastModifiedDate: String
}

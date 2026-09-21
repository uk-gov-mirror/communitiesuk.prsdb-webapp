package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.furnishedStatus

import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractPropertyOwnershipUpdateJourneyState
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder.Companion.journey
import uk.gov.communities.prsdb.webapp.journeys.hasOutcome
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.FurnishedStatusState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FurnishedStatusStep
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@PrsdbWebService
class UpdateFurnishedStatusJourneyFactory(
    private val stateFactory: ObjectFactory<UpdateFurnishedStatusJourney>,
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

        return journey(state) {
            unreachableStepUrl { returnUrl }
            step(journey.furnishedStatus) {
                routeSegment(FurnishedStatusStep.ROUTE_SEGMENT)
                backUrl { returnUrl }
                nextStep { journey.completeFurnishedStatusUpdateStep }
                initialStep()
                withAdditionalContentProperties {
                    mapOf(
                        "title" to "propertyDetails.update.title",
                        "fieldSetHeading" to "forms.update.furnishedStatus.fieldSetHeading",
                        "submitButtonText" to "forms.buttons.confirmAndSubmitUpdate",
                        "submitButton" to "transactionSubmitButton",
                        "showWarning" to true,
                    )
                }
            }
            step(journey.completeFurnishedStatusUpdateStep) {
                parents { journey.furnishedStatus.hasOutcome(Complete.COMPLETE) }
                nextDestination {
                    Destination
                        .ExternalUrl(returnUrl)
                        .withFlashAttribute("updateSuccessBanner", "propertyDetails.updateSuccessBanner.tenancyDetails")
                }
            }
        }
    }

    fun initializeJourneyState(
        seed: Any?,
        currentLastModifiedDate: java.time.Instant,
    ): String = stateFactory.getObject().initialiseOrRestoreStateReinitialisingIfOutdated(seed, currentLastModifiedDate)
}

@JourneyFrameworkComponent
class UpdateFurnishedStatusJourney(
    override val furnishedStatus: FurnishedStatusStep,
    override val completeFurnishedStatusUpdateStep: CompleteFurnishedStatusUpdateStep,
    journeyStateService: JourneyStateService,
    journeyName: String = "furnished status",
) : AbstractPropertyOwnershipUpdateJourneyState(journeyStateService, journeyName),
    UpdateFurnishedStatusJourneyState {
    override var propertyId: Long by delegateProvider.requiredImmutableDelegate("propertyId")
    override var lastModifiedDate: String by delegateProvider.requiredImmutableDelegate(LAST_MODIFIED_DATE_KEY)
}

interface UpdateFurnishedStatusJourneyState :
    JourneyState,
    FurnishedStatusState {
    val completeFurnishedStatusUpdateStep: CompleteFurnishedStatusUpdateStep
    val propertyId: Long
    val lastModifiedDate: String
}

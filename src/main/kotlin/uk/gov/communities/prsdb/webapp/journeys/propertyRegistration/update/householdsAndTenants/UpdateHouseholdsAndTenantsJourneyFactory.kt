package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.householdsAndTenants

import kotlinx.datetime.Instant
import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractPropertyOwnershipUpdateJourneyState
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder.Companion.journey
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FinishCyaJourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.TenantsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseHoldsAndTenantsDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseholdsAndTenantsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState.Companion.checkAnswerTask
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@PrsdbWebService
class UpdateHouseholdsAndTenantsJourneyFactory(
    private val stateFactory: ObjectFactory<UpdateHouseholdsAndTenantsJourney>,
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
        state: UpdateHouseholdsAndTenantsJourney,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepUrl { returnUrl }
            task(journey.householdsAndTenantsTask) {
                initialStep()
                backUrl { returnUrl }
                nextStep { journey.cyaStep }
                withDependencies { HouseHoldsAndTenantsDependencies(false) }
                withAdditionalContentProperty {
                    "title" to "propertyDetails.update.title"
                }
            }
            step(journey.cyaStep) {
                routeSegment(UpdateHouseholdsAndTenantsCyaStep.ROUTE_SEGMENT)
                parents { journey.householdsAndTenantsTask.isComplete() }
                nextDestination {
                    Destination
                        .ExternalUrl(returnUrl)
                        .withFlashAttribute("updateSuccessBanner", "propertyDetails.updateSuccessBanner.tenancyDetails")
                }
            }
            replaceHeadingsAndButtons()
        }

    private fun checkYourAnswersJourneyMap(
        state: UpdateHouseholdsAndTenantsJourney,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepUrl { returnUrl }
            configure {
                withAdditionalContentProperty {
                    "title" to "propertyDetails.update.title"
                }
            }
            configureFirst { backDestination { journey.returnToCyaPageDestination } }
            checkAnswerTask(journey.householdsAndTenantsTask, { HouseHoldsAndTenantsDependencies(false) })
            if (state.checkingAnswersFor == TenantsStep.ROUTE_SEGMENT) {
                configureStep(journey.householdsAndTenantsTask.tenants) {
                    backDestination { journey.returnToCyaPageDestination }
                }
            }
            step(journey.finishCyaStep) {
                parents { journey.householdsAndTenantsTask.isComplete() }
                nextDestination { Destination.Nowhere() }
            }
            replaceHeadingsAndButtons()
        }

    private fun JourneyBuilder<UpdateHouseholdsAndTenantsJourney>.replaceHeadingsAndButtons() {
        configureStep(journey.householdsAndTenantsTask.households) {
            withAdditionalContentProperty {
                "fieldSetHeading" to "forms.update.numberOfHouseholds.fieldSetHeading"
            }
            withAdditionalContentProperty {
                "submitButtonText" to "forms.buttons.continue"
            }
        }
        configureStep(journey.householdsAndTenantsTask.tenants) {
            withAdditionalContentProperty {
                "fieldSetHeading" to "forms.update.numberOfPeople.fieldSetHeading"
            }
            withAdditionalContentProperty {
                "submitButtonText" to "forms.buttons.continue"
            }
        }
    }

    fun initialiseJourneyState(
        seed: Any?,
        currentLastModifiedDate: java.time.Instant,
    ): String = stateFactory.getObject().initialiseOrRestoreStateReinitialisingIfOutdated(seed, currentLastModifiedDate)
}

@JourneyFrameworkComponent
class UpdateHouseholdsAndTenantsJourney(
    // HouseholdsAndTenants task
    override val householdsAndTenantsTask: HouseholdsAndTenantsTask,
    // Check your answers step
    override val cyaStep: UpdateHouseholdsAndTenantsCyaStep,
    journeyStateService: JourneyStateService,
    override val finishCyaStep: FinishCyaJourneyStep,
    override val stateFactory: ObjectFactory<UpdateHouseholdsAndTenantsJourney>,
) : AbstractPropertyOwnershipUpdateJourneyState(journeyStateService, "households and tenants"),
    UpdateHouseholdsAndTenantsJourneyState {
    override var propertyId: Long by delegateProvider.requiredImmutableDelegate("propertyId")
    override var lastModifiedDate: String by delegateProvider.requiredImmutableDelegate(LAST_MODIFIED_DATE_KEY)
    override var cyaJourneys: Map<String, String> = mapOf()
    override var checkingAnswersFor: String? by delegateProvider.nullableDelegate("checkingAnswersFor")

    override var originalJourneyUpdated: Instant? by delegateProvider.nullableDelegate("originalJourneyUpdated")
    override var cyaUrlPath: String? by delegateProvider.nullableDelegate("cyaRouteSegment")
}

interface UpdateHouseholdsAndTenantsJourneyState : CheckYourAnswersJourneyState {
    val householdsAndTenantsTask: HouseholdsAndTenantsTask
    override val cyaStep: UpdateHouseholdsAndTenantsCyaStep
    val propertyId: Long
    val lastModifiedDate: String
}

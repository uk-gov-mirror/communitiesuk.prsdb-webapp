package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.tenancyDetails

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
import uk.gov.communities.prsdb.webapp.journeys.hasOutcome
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.TenancyDetailsState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BillsIncludedStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FinishCyaJourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FurnishedStatusStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentAmountStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentFrequencyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentIncludesBillsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.TenantsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseHoldsAndTenantsDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseholdsAndTenantsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.RentFrequencyAndAmountTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.RentIncludesBillsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState.Companion.checkAnswerStep
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState.Companion.checkAnswerTask
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@PrsdbWebService
class UpdateTenancyDetailsJourneyFactory(
    private val stateFactory: ObjectFactory<UpdateTenancyDetailsJourney>,
    private val propertyOwnershipService: PropertyOwnershipService,
) {
    fun initialiseJourneyState(
        seed: Any?,
        currentLastModifiedDate: java.time.Instant,
    ): String = stateFactory.getObject().initialiseOrRestoreStateReinitialisingIfOutdated(seed, currentLastModifiedDate)

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
            checkYourAnswersJourneyMap(state, checkingAnswersFor, returnUrl)
        }
    }

    private fun mainJourneyMap(
        state: UpdateTenancyDetailsJourney,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepUrl { returnUrl }
            configure {
                withAdditionalContentProperty { "title" to "propertyDetails.update.title" }
            }
            task(journey.householdsAndTenantsTask) {
                initialStep()
                backUrl { returnUrl }
                withDependencies { HouseHoldsAndTenantsDependencies(false) }
                nextStep { journey.rentIncludesBillsTask.firstStep }
            }
            task(journey.rentIncludesBillsTask) {
                parents { journey.householdsAndTenantsTask.isComplete() }
                nextStep { journey.furnishedStatus }
            }
            step(journey.furnishedStatus) {
                routeSegment(FurnishedStatusStep.ROUTE_SEGMENT)
                parents { journey.rentIncludesBillsTask.isComplete() }
                nextStep { journey.rentFrequencyAndAmountTask.firstStep }
            }
            task(journey.rentFrequencyAndAmountTask) {
                parents { journey.furnishedStatus.hasOutcome(Complete.COMPLETE) }
                nextStep { journey.cyaStep }
            }
            step(journey.cyaStep) {
                routeSegment(UpdateTenancyDetailsCyaStep.ROUTE_SEGMENT)
                parents { journey.rentFrequencyAndAmountTask.isComplete() }
                nextDestination {
                    Destination
                        .ExternalUrl(returnUrl)
                        .withFlashAttribute("updateSuccessBanner", "propertyDetails.updateSuccessBanner.tenancyDetails")
                }
            }
            replaceHeadingsAndButtons(state)
        }

    private fun checkYourAnswersJourneyMap(
        state: UpdateTenancyDetailsJourney,
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
                HouseholdStep.ROUTE_SEGMENT, TenantsStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.householdsAndTenantsTask)
                }

                RentIncludesBillsStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.rentIncludesBillsTask)
                }

                BillsIncludedStep.ROUTE_SEGMENT -> {
                    fromTask(journey.rentIncludesBillsTask) {
                        checkAnswerStep(task.billsIncluded, BillsIncludedStep.ROUTE_SEGMENT)
                    }
                }

                FurnishedStatusStep.ROUTE_SEGMENT -> {
                    checkAnswerStep(journey.furnishedStatus, FurnishedStatusStep.ROUTE_SEGMENT)
                }

                RentFrequencyStep.ROUTE_SEGMENT, RentAmountStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.rentFrequencyAndAmountTask)
                }

                else -> {
                    throw IllegalStateException("Unknown step being checked: $checkingAnswersFor")
                }
            }
            replaceHeadingsAndButtons(state)
            step(journey.finishCyaStep) {
                initialStep()
                nextDestination { Destination.Nowhere() }
            }
        }

    private fun JourneyBuilder<UpdateTenancyDetailsJourney>.replaceHeadingsAndButtons(state: UpdateTenancyDetailsJourney) {
        configureStep(journey.householdsAndTenantsTask.households) {
            withAdditionalContentProperty { "fieldSetHeading" to "forms.update.numberOfHouseholds.fieldSetHeading" }
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
        configureStep(journey.householdsAndTenantsTask.tenants) {
            withAdditionalContentProperty { "fieldSetHeading" to "forms.update.numberOfPeople.fieldSetHeading" }
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
        configureStep(journey.rentIncludesBillsTask.rentIncludesBills) {
            withAdditionalContentProperty { "fieldSetHeading" to "forms.update.rentIncludesBills.fieldSetHeading" }
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
        configureStep(journey.rentIncludesBillsTask.billsIncluded) {
            withAdditionalContentProperty { "fieldSetHeading" to "forms.update.billsIncluded.fieldSetHeading" }
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
        configureStep(journey.furnishedStatus) {
            withAdditionalContentProperty { "fieldSetHeading" to "forms.update.furnishedStatus.fieldSetHeading" }
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
        configureStep(journey.rentFrequencyAndAmountTask.rentFrequency) {
            withAdditionalContentProperty { "heading" to "forms.update.rentFrequency.heading" }
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
        configureStep(journey.rentFrequencyAndAmountTask.rentAmount) {
            withAdditionalContentProperty { "heading" to state.rentFrequencyAndAmountTask.getUpdateRentAmountHeading() }
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
    }
}

@JourneyFrameworkComponent
class UpdateTenancyDetailsJourney(
    override val householdsAndTenantsTask: HouseholdsAndTenantsTask,
    override val rentIncludesBillsTask: RentIncludesBillsTask,
    override val furnishedStatus: FurnishedStatusStep,
    override val rentFrequencyAndAmountTask: RentFrequencyAndAmountTask,
    override val cyaStep: UpdateTenancyDetailsCyaStep,
    override val finishCyaStep: FinishCyaJourneyStep,
    journeyStateService: JourneyStateService,
    override val stateFactory: ObjectFactory<UpdateTenancyDetailsJourney>,
) : AbstractPropertyOwnershipUpdateJourneyState(journeyStateService, "tenancy details"),
    UpdateTenancyDetailsJourneyState {
    override var propertyId: Long by delegateProvider.requiredImmutableDelegate("propertyId")
    override var originalJourneyUpdated: Instant? by delegateProvider.nullableDelegate("originalJourneyUpdated")
    override var checkingAnswersFor: String? by delegateProvider.nullableDelegate("checkingAnswersFor")
    override var cyaJourneys: Map<String, String> = mapOf()
    override var cyaUrlPath: String? by delegateProvider.nullableDelegate("cyaRouteSegment")
    override var lastModifiedDate: String by delegateProvider.requiredImmutableDelegate(LAST_MODIFIED_DATE_KEY)
}

interface UpdateTenancyDetailsJourneyState :
    TenancyDetailsState,
    CheckYourAnswersJourneyState {
    override val cyaStep: UpdateTenancyDetailsCyaStep
    val propertyId: Long
    val lastModifiedDate: String
}

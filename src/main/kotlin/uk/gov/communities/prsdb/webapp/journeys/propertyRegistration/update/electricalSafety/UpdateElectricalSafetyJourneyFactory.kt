package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.electricalSafety

import kotlinx.datetime.Instant
import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractPropertyOwnershipUpdateJourneyState
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder.Companion.journey
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ElectricalCertExpiryDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FinishCyaJourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState.Companion.checkAnswerTask
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@PrsdbWebService
class UpdateElectricalSafetyJourneyFactory(
    private val stateFactory: ObjectFactory<UpdateElectricalSafetyJourney>,
    private val propertyOwnershipService: PropertyOwnershipService,
) {
    final fun createJourneySteps(
        propertyId: Long,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> {
        val state = stateFactory.getObject()

        if (!state.isStateInitialized) {
            val propertyOwnership = propertyOwnershipService.getPropertyOwnership(propertyId)
            val propertyCompliance =
                propertyOwnership.propertyCompliance
                    ?: throw PrsdbWebException("Property ownership $propertyId does not have a compliance record")

            state.propertyId = propertyId
            state.lastModifiedDate = propertyCompliance.getMostRecentlyUpdated().toString()
            state.isOccupied = propertyOwnership.isOccupied
            state.previousUploadIds = propertyCompliance.electricalSafetyFileUploads.map { it.id }
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
        state: UpdateElectricalSafetyJourney,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepUrl { returnUrl }
            task(journey.electricalSafetyDetailsTask) {
                withDependencies { journey }
                initialStep()
                backUrl { returnUrl }
                nextStep { journey.updateCheckElectricalSafetyAnswersStep }
                withAdditionalContentProperties {
                    mapOf(
                        "title" to "propertyDetails.update.title",
                        "sectionHeaderInfo" to null,
                    )
                }
            }
            configureStep(journey.electricalSafetyDetailsTask.checkElectricalCertUploadsStep) {
                backStep { journey.electricalSafetyDetailsTask.electricalCertExpiryDateStep }
            }
            step(journey.updateCheckElectricalSafetyAnswersStep) {
                routeSegment(UpdateCheckElectricalSafetyAnswersStep.ROUTE_SEGMENT)
                parents { journey.electricalSafetyDetailsTask.isComplete() }
                nextStep { journey.completeElectricalSafetyUpdateStep }
                withAdditionalContentProperties {
                    mapOf(
                        "title" to "propertyDetails.update.title",
                    )
                }
            }
            step(journey.completeElectricalSafetyUpdateStep) {
                parents { journey.updateCheckElectricalSafetyAnswersStep.isComplete() }
                nextDestination {
                    Destination
                        .ExternalUrl(returnUrl)
                        .withFlashAttribute("updateSuccessBanner", "propertyDetails.updateSuccessBanner.compliance")
                }
            }
            replaceButtons()
        }

    private fun checkYourAnswersJourneyMap(
        state: UpdateElectricalSafetyJourney,
        returnUrl: String,
    ): Map<String, StepLifecycleOrchestrator> =
        journey(state) {
            unreachableStepUrl { returnUrl }
            configure {
                withAdditionalContentProperties {
                    mapOf(
                        "title" to "propertyDetails.update.title",
                        "sectionHeaderInfo" to null,
                    )
                }
            }
            configureFirst { backDestination { journey.returnToCyaPageDestination } }
            when (state.checkingAnswersFor) {
                ElectricalCertExpiryDateStep.ROUTE_SEGMENT -> {
                    checkAnswerTask(journey.electricalSafetyDetailsTask, { journey })
                    configureStep(journey.electricalSafetyDetailsTask.electricalCertExpiryDateStep) {
                        backDestination { journey.returnToCyaPageDestination }
                    }
                }

                else -> {
                    checkAnswerTask(journey.electricalSafetyDetailsTask, { journey })
                }
            }
            configureStep(journey.electricalSafetyDetailsTask.checkElectricalCertUploadsStep) {
                backDestination { journey.returnToCyaPageDestination }
            }

            step(journey.finishCyaStep) {
                initialStep()
                nextDestination { Destination.Nowhere() }
            }
            replaceButtons()
        }

    private fun JourneyBuilder<UpdateElectricalSafetyJourney>.replaceButtons() {
        configureStep(journey.electricalSafetyDetailsTask.hasElectricalCertStep) {
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
        configureStep(journey.electricalSafetyDetailsTask.electricalCertExpiryDateStep) {
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
        configureStep(journey.electricalSafetyDetailsTask.checkElectricalCertUploadsStep) {
            withAdditionalContentProperty { "submitButtonText" to "forms.buttons.continue" }
        }
        configureStep(journey.electricalSafetyDetailsTask.electricalCertExpiredStep) {
            withAdditionalContentProperty {
                "submitButtonText" to
                    if (journey.isOccupied) "forms.buttons.continueWithoutElectricalSafety" else "forms.buttons.continue"
            }
        }
    }

    fun initialiseJourneyState(
        seed: Any?,
        currentLastModifiedDate: java.time.Instant,
    ): String = stateFactory.getObject().initialiseOrRestoreStateReinitialisingIfOutdated(seed, currentLastModifiedDate)
}

@JourneyFrameworkComponent
class UpdateElectricalSafetyJourney(
    journeyStateService: JourneyStateService,
    journeyName: String = "electricalSafety",
    val updateCheckElectricalSafetyAnswersStep: UpdateCheckElectricalSafetyAnswersStep,
    override val completeElectricalSafetyUpdateStep: CompleteElectricalSafetyUpdateStep,
    override val electricalSafetyDetailsTask: ElectricalSafetyDetailsTask,
    override val finishCyaStep: FinishCyaJourneyStep,
    override val stateFactory: ObjectFactory<UpdateElectricalSafetyJourneyState>,
) : AbstractPropertyOwnershipUpdateJourneyState(journeyStateService, journeyName),
    UpdateElectricalSafetyJourneyState {
    override var propertyId: Long by delegateProvider.requiredImmutableDelegate("propertyId")
    override var lastModifiedDate: String by delegateProvider.requiredImmutableDelegate(LAST_MODIFIED_DATE_KEY)
    override var previousUploadIds: List<Long> by delegateProvider.requiredImmutableDelegate("previousUploads")

    override var originalJourneyUpdated: Instant? by delegateProvider.nullableDelegate("originalJourneyUpdated")
    override var checkingAnswersFor: String? by delegateProvider.nullableDelegate("checkingAnswersFor")
    override var cyaJourneys: Map<String, String> = mapOf()
    override var cyaUrlPath: String? by delegateProvider.nullableDelegate("cyaRouteSegment")

    override val cyaStep get() = updateCheckElectricalSafetyAnswersStep

    override var isOccupied: Boolean by delegateProvider.requiredImmutableDelegate("isOccupied")
    override val allowProvideCertificateLaterRoute: Boolean = false
    override val propertyOwnershipId: Long? get() = propertyId
}

interface UpdateElectricalSafetyJourneyState :
    JourneyState,
    ElectricalSafetyDependencies,
    CheckYourAnswersJourneyState {
    val electricalSafetyDetailsTask: ElectricalSafetyDetailsTask
    val propertyId: Long
    val lastModifiedDate: String
    val previousUploadIds: List<Long>
    val completeElectricalSafetyUpdateStep: CompleteElectricalSafetyUpdateStep
}

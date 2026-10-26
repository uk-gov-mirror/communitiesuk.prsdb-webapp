package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.OrParents
import uk.gov.communities.prsdb.webapp.journeys.Task
import uk.gov.communities.prsdb.webapp.journeys.hasOutcome
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.CertificateUpload
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSafetyDetailState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasCertUploadsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertExpiredStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertIssueDateMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertIssueDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertMissingStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasAnyInCollectionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasAnyInCollectionStepConfig
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideGasCertLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RemoveGasCertUploadStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.UploadGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.shared.AnyMembers
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState

@JourneyFrameworkComponent("propertyRegistrationGasSafetyDetailsTask")
class GasSafetyDetailsTask(
    override val hasUploadedCert: HasAnyInCollectionStep,
    override val hasGasSupplyStep: HasGasSupplyStep,
    override val hasGasCertStep: HasGasCertStep,
    override val gasCertIssueDateStep: GasCertIssueDateStep,
    override val uploadGasCertStep: UploadGasCertStep,
    override val checkGasCertUploadsStep: CheckGasCertUploadsStep,
    override val removeGasCertUploadStep: RemoveGasCertUploadStep,
    override val gasCertExpiredStep: GasCertExpiredStep,
    override val gasCertMissingStep: GasCertMissingStep,
    override val provideGasCertLaterStep: ProvideGasCertLaterStep,
    journeyStateService: JourneyStateService,
) : Task<GasSafetyDetailState, GasSafetyDependencies>(journeyStateService),
    GasSafetyDetailState {
    override val taskState: GasSafetyDetailState
        get() = this

    override val isOccupied: Boolean
        get() = dependencies.isOccupied
    override val allowProvideCertificateLaterRoute: Boolean
        get() = dependencies.allowProvideCertificateLaterRoute
    override val propertyOwnershipId: Long?
        get() = dependencies.propertyOwnershipId

    override var gasUploadMap: Map<Int, CertificateUpload> by delegateProvider.requiredDelegate("gasUploadMap", mapOf())
    override var highestAssignedGasMemberId: Int? by delegateProvider.nullableDelegate("highestGasUploadMemberId")

    override fun makeSubJourney(state: GasSafetyDetailState) =
        subJourney(state) {
            step(journey.hasGasSupplyStep) {
                routeSegment(HasGasSupplyStep.ROUTE_SEGMENT)
                nextStep { mode ->
                    when (mode) {
                        HasGasSupplyMode.HAS_SUPPLY -> journey.hasGasCertStep
                        HasGasSupplyMode.NO_SUPPLY -> exitStep
                        HasGasSupplyMode.PROVIDE_LATER -> journey.provideGasCertLaterStep
                    }
                }
                savable()
            }
            step(journey.hasGasCertStep) {
                routeSegment(HasGasCertStep.ROUTE_SEGMENT)
                parents { journey.hasGasSupplyStep.hasOutcome(HasGasSupplyMode.HAS_SUPPLY) }
                nextStep { mode ->
                    when (mode) {
                        HasGasCertMode.YES -> journey.gasCertIssueDateStep
                        HasGasCertMode.NO -> journey.gasCertMissingStep
                    }
                }
                savable()
            }

            step(journey.gasCertIssueDateStep) {
                routeSegment(GasCertIssueDateStep.ROUTE_SEGMENT)
                parents { journey.hasGasCertStep.hasOutcome(HasGasCertMode.YES) }
                nextStep { mode ->
                    when (mode) {
                        GasCertIssueDateMode.GAS_SAFETY_CERTIFICATE_IN_DATE -> journey.hasUploadedCert
                        GasCertIssueDateMode.GAS_SAFETY_CERTIFICATE_OUTDATED -> journey.gasCertExpiredStep
                    }
                }
                savable()
            }
            step<AnyMembers, HasAnyInCollectionStepConfig>(journey.hasUploadedCert) {
                parents { journey.gasCertIssueDateStep.hasOutcome(GasCertIssueDateMode.GAS_SAFETY_CERTIFICATE_IN_DATE) }
                nextStep { mode ->
                    when (mode) {
                        AnyMembers.NO_MEMBERS -> journey.uploadGasCertStep
                        AnyMembers.SOME_MEMBERS -> journey.checkGasCertUploadsStep
                    }
                }
                stepSpecificInitialisation { collectionMap = journey.gasUploadMap }
            }
            step(journey.uploadGasCertStep) {
                routeSegment(UploadGasCertStep.ROUTE_SEGMENT)
                parents { journey.gasCertIssueDateStep.hasOutcome(GasCertIssueDateMode.GAS_SAFETY_CERTIFICATE_IN_DATE) }
                nextStep { journey.checkGasCertUploadsStep }
                savable()
            }
            step(journey.checkGasCertUploadsStep) {
                routeSegment(CheckGasCertUploadsStep.ROUTE_SEGMENT)
                parents { journey.uploadGasCertStep.isComplete() }
                nextStep { exitStep }
                backDestination {
                    val cyaState = dependencies as? CheckYourAnswersJourneyState
                    if (cyaState?.isCheckingAnswers == true) {
                        cyaState.returnToCyaPageDestination
                    } else {
                        Destination(journey.gasCertIssueDateStep)
                    }
                }
                savable()
            }
            step(journey.removeGasCertUploadStep) {
                parents {
                    journey.hasUploadedCert.hasOutcome(AnyMembers.SOME_MEMBERS)
                }
                backStep { journey.checkGasCertUploadsStep }
                nextStep { mode ->
                    when (mode) {
                        AnyMembers.SOME_MEMBERS -> journey.checkGasCertUploadsStep
                        AnyMembers.NO_MEMBERS -> journey.uploadGasCertStep
                    }
                }
                routeSegment(RemoveGasCertUploadStep.ROUTE_SEGMENT)
                savable()
            }
            step(journey.gasCertExpiredStep) {
                routeSegment(GasCertExpiredStep.ROUTE_SEGMENT)
                parents {
                    journey.gasCertIssueDateStep.hasOutcome(GasCertIssueDateMode.GAS_SAFETY_CERTIFICATE_OUTDATED)
                }
                nextStep { exitStep }
                savable()
            }
            step(journey.gasCertMissingStep) {
                routeSegment(GasCertMissingStep.ROUTE_SEGMENT)
                parents { journey.hasGasCertStep.hasOutcome(HasGasCertMode.NO) }
                nextStep { exitStep }
                savable()
            }
            step(journey.provideGasCertLaterStep) {
                routeSegment(ProvideGasCertLaterStep.ROUTE_SEGMENT)
                parents { journey.hasGasSupplyStep.hasOutcome(HasGasSupplyMode.PROVIDE_LATER) }
                nextStep { exitStep }
                savable()
            }
            exitStep {
                parents {
                    OrParents(
                        journey.hasGasSupplyStep.hasOutcome(HasGasSupplyMode.NO_SUPPLY),
                        journey.provideGasCertLaterStep.isComplete(),
                        journey.gasCertMissingStep.isComplete(),
                        journey.gasCertExpiredStep.isComplete(),
                        journey.checkGasCertUploadsStep.isComplete(),
                    )
                }
            }
        }
}

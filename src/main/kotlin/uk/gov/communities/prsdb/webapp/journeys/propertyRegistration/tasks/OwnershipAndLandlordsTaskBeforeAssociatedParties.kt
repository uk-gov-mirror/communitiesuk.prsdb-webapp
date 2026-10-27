package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.Task
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.OwnershipAndLandlordsStateBeforeAssociatedParties
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipTypeStep
import uk.gov.communities.prsdb.webapp.journeys.shared.inviteJointLandlord.InviteJointLandlordsTaskDependencies

// TODO PDJB-955: remove once ASSOCIATED_PARTIES is released
@JourneyFrameworkComponent
class OwnershipAndLandlordsTaskBeforeAssociatedParties(
    journeyStateService: JourneyStateService,
    override val ownershipTypeStep: OwnershipTypeStep,
    override val jointLandlordsTask: JointLandlordsPropertyRegistrationTask,
) : Task<OwnershipAndLandlordsStateBeforeAssociatedParties, InviteJointLandlordsTaskDependencies>(
        journeyStateService,
    ),
    OwnershipAndLandlordsStateBeforeAssociatedParties {
    override val taskState get() = this

    override fun makeSubJourney(state: OwnershipAndLandlordsStateBeforeAssociatedParties) =
        subJourney(state) {
            step(journey.ownershipTypeStep) {
                routeSegment(OwnershipTypeStep.ROUTE_SEGMENT)
                nextStep { journey.jointLandlordsTask.firstStep }
                savable()
            }
            task(journey.jointLandlordsTask) {
                withDependencies { this@OwnershipAndLandlordsTaskBeforeAssociatedParties.dependencies }
                parents { journey.ownershipTypeStep.isComplete() }
                nextStep { exitStep }
                savable()
            }
            exitStep {
                parents { journey.jointLandlordsTask.isComplete() }
            }
        }
}

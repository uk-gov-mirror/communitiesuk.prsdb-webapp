package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.constants.enums.TaskStatus
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.Task
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.OwnershipAndLandlordsState
import uk.gov.communities.prsdb.webapp.journeys.shared.inviteJointLandlord.InviteJointLandlordsTaskDependencies

@JourneyFrameworkComponent
class OwnershipAndLandlordsTask(
    journeyStateService: JourneyStateService,
    override val jointLandlordsTask: JointLandlordsPropertyRegistrationTask,
    override val ownershipStructureTask: OwnershipStructureTask,
    override val propertyManagerTask: PropertyManagerTask,
) : Task<OwnershipAndLandlordsState, InviteJointLandlordsTaskDependencies>(
        journeyStateService,
    ),
    OwnershipAndLandlordsState {
    override val taskState get() = this

    override fun makeSubJourney(state: OwnershipAndLandlordsState) =
        subJourney(state) {
            // The first step is the joint landlords task's internal routing step, which always has an outcome once
            // reachable, so the default status would show "In progress" before the user has answered anything.
            taskStatus {
                when {
                    exitStep.isStepReachable -> TaskStatus.COMPLETED
                    else ->
                        when (val jointLandlordsStatus = journey.jointLandlordsTask.taskStatus()) {
                            TaskStatus.COMPLETED -> TaskStatus.IN_PROGRESS
                            else -> jointLandlordsStatus
                        }
                }
            }
            task(journey.jointLandlordsTask) {
                withDependencies { this@OwnershipAndLandlordsTask.dependencies }
                nextStep { journey.ownershipStructureTask.firstStep }
                savable()
            }
            task(journey.ownershipStructureTask) {
                parents { journey.jointLandlordsTask.isComplete() }
                nextStep { journey.propertyManagerTask.firstStep }
                savable()
            }
            task(journey.propertyManagerTask) {
                parents { journey.ownershipStructureTask.isComplete() }
                nextStep { exitStep }
                savable()
            }
            exitStep {
                parents { journey.propertyManagerTask.isComplete() }
            }
        }
}

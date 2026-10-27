package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.TaskWithoutDependencies
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.OwnershipStructureState

// TODO PDJB-959: Finish routing
@JourneyFrameworkComponent
class OwnershipStructureTask(
    journeyStateService: JourneyStateService,
    override val ownershipTypeTask: OwnershipTypeTask,
    override val freeholdersTask: FreeholdersTask,
    override val superiorLandlordsTask: SuperiorLandlordsTask,
) : TaskWithoutDependencies<OwnershipStructureState>(journeyStateService),
    OwnershipStructureState {
    override val taskState get() = this

    override fun makeSubJourney(state: OwnershipStructureState) =
        subJourney(state) {
            task(journey.ownershipTypeTask) {
                nextStep { journey.freeholdersTask.firstStep }
                savable()
            }
            task(journey.freeholdersTask) {
                parents { journey.ownershipTypeTask.isComplete() }
                nextStep { journey.superiorLandlordsTask.firstStep }
                savable()
            }
            task(journey.superiorLandlordsTask) {
                parents { journey.freeholdersTask.isComplete() }
                nextStep { exitStep }
                savable()
            }
            exitStep {
                parents { journey.superiorLandlordsTask.isComplete() }
            }
        }
}

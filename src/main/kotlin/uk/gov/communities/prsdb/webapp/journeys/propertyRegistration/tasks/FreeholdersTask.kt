package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.TaskWithoutDependencies
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.FreeholdersState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdersStep

@JourneyFrameworkComponent
class FreeholdersTask(
    journeyStateService: JourneyStateService,
    override val freeholdersStep: FreeholdersStep,
) : TaskWithoutDependencies<FreeholdersState>(journeyStateService),
    FreeholdersState {
    override val taskState get() = this

    override fun makeSubJourney(state: FreeholdersState) =
        subJourney(state) {
            step(journey.freeholdersStep) {
                routeSegment(FreeholdersStep.ROUTE_SEGMENT)
                nextStep { exitStep }
                savable()
            }
            exitStep {
                parents { journey.freeholdersStep.isComplete() }
            }
        }
}

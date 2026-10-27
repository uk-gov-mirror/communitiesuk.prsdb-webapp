package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.TaskWithoutDependencies
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.SuperiorLandlordsState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.SuperiorLandlordsStep

@JourneyFrameworkComponent
class SuperiorLandlordsTask(
    journeyStateService: JourneyStateService,
    override val superiorLandlordsStep: SuperiorLandlordsStep,
) : TaskWithoutDependencies<SuperiorLandlordsState>(journeyStateService),
    SuperiorLandlordsState {
    override val taskState get() = this

    override fun makeSubJourney(state: SuperiorLandlordsState) =
        subJourney(state) {
            step(journey.superiorLandlordsStep) {
                routeSegment(SuperiorLandlordsStep.ROUTE_SEGMENT)
                nextStep { exitStep }
                savable()
            }
            exitStep {
                parents { journey.superiorLandlordsStep.isComplete() }
            }
        }
}

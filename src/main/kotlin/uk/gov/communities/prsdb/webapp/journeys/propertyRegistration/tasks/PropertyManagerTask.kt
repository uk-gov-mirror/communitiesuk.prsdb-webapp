package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.TaskWithoutDependencies
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.PropertyManagerState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyManagerStep

@JourneyFrameworkComponent
class PropertyManagerTask(
    journeyStateService: JourneyStateService,
    override val propertyManagerStep: PropertyManagerStep,
) : TaskWithoutDependencies<PropertyManagerState>(journeyStateService),
    PropertyManagerState {
    override val taskState get() = this

    override fun makeSubJourney(state: PropertyManagerState) =
        subJourney(state) {
            step(journey.propertyManagerStep) {
                routeSegment(PropertyManagerStep.ROUTE_SEGMENT)
                nextStep { exitStep }
                savable()
            }
            exitStep {
                parents { journey.propertyManagerStep.isComplete() }
            }
        }
}

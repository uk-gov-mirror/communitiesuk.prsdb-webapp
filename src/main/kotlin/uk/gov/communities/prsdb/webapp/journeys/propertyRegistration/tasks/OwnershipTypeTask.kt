package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.TaskWithoutDependencies
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.OwnershipTypeState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipKindStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnsPropertyStep

// TODO PDJB-959: Finish routing
@JourneyFrameworkComponent
class OwnershipTypeTask(
    journeyStateService: JourneyStateService,
    override val ownsPropertyStep: OwnsPropertyStep,
    override val ownershipKindStep: OwnershipKindStep,
    override val freeholdTypeStep: FreeholdTypeStep,
) : TaskWithoutDependencies<OwnershipTypeState>(journeyStateService),
    OwnershipTypeState {
    override val taskState get() = this

    override fun makeSubJourney(state: OwnershipTypeState) =
        subJourney(state) {
            step(journey.ownsPropertyStep) {
                routeSegment(OwnsPropertyStep.ROUTE_SEGMENT)
                nextStep { journey.ownershipKindStep }
                savable()
            }
            step(journey.ownershipKindStep) {
                routeSegment(OwnershipKindStep.ROUTE_SEGMENT)
                parents { journey.ownsPropertyStep.isComplete() }
                nextStep { journey.freeholdTypeStep }
                savable()
            }
            step(journey.freeholdTypeStep) {
                routeSegment(FreeholdTypeStep.ROUTE_SEGMENT)
                parents { journey.ownershipKindStep.isComplete() }
                nextStep { exitStep }
                savable()
            }
            exitStep {
                parents { journey.freeholdTypeStep.isComplete() }
            }
        }
}

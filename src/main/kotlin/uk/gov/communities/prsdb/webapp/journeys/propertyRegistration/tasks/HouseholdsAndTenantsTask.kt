package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.OrParents
import uk.gov.communities.prsdb.webapp.journeys.Task
import uk.gov.communities.prsdb.webapp.journeys.hasOutcome
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.HouseholdsAndTenantsState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideTenancyDetailsLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.TenantsStep
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete

@JourneyFrameworkComponent
class HouseholdsAndTenantsTask(
    journeyStateService: JourneyStateService,
    override val households: HouseholdStep,
    override val tenants: TenantsStep,
    override val provideTenancyDetailsLaterStep: ProvideTenancyDetailsLaterStep,
) : Task<HouseholdsAndTenantsState, HouseholdsAndTenantsDependencies>(journeyStateService),
    HouseholdsAndTenantsState {
    override val taskState get() = this

    override fun makeSubJourney(state: HouseholdsAndTenantsState) =
        subJourney(state) {
            step(journey.households) {
                routeSegment(HouseholdStep.ROUTE_SEGMENT)
                nextStep { mode ->
                    when (mode) {
                        HouseholdMode.PROVIDE_THIS_LATER -> journey.provideTenancyDetailsLaterStep
                        else -> journey.tenants
                    }
                }
                savable()
            }
            step(journey.tenants) {
                routeSegment(TenantsStep.ROUTE_SEGMENT)
                parents { journey.households.hasOutcome(HouseholdMode.COMPLETE) }
                nextStep { exitStep }
                savable()
            }
            step(journey.provideTenancyDetailsLaterStep) {
                routeSegment(ProvideTenancyDetailsLaterStep.ROUTE_SEGMENT)
                parents { journey.households.hasOutcome(HouseholdMode.PROVIDE_THIS_LATER) }
                nextStep { exitStep }
                savable()
            }
            exitStep {
                parents {
                    OrParents(
                        journey.tenants.hasOutcome(Complete.COMPLETE),
                        journey.provideTenancyDetailsLaterStep.isComplete(),
                    )
                }
            }
        }
}

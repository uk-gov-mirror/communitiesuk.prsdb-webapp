package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AndParents
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.OrParents
import uk.gov.communities.prsdb.webapp.journeys.Task
import uk.gov.communities.prsdb.webapp.journeys.doesNotHaveOutcome
import uk.gov.communities.prsdb.webapp.journeys.hasOutcome
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.TenancyDetailsState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FurnishedStatusStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdMode
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete

@JourneyFrameworkComponent
class TenancyDetailsTask(
    journeyStateService: JourneyStateService,
    override val householdsAndTenantsTask: HouseholdsAndTenantsTask,
    override val rentIncludesBillsTask: RentIncludesBillsTask,
    override val furnishedStatus: FurnishedStatusStep,
    override val rentFrequencyAndAmountTask: RentFrequencyAndAmountTask,
) : Task<TenancyDetailsState, HouseholdsAndTenantsDependencies>(journeyStateService),
    TenancyDetailsState {
    override val taskState get() = this

    override fun makeSubJourney(state: TenancyDetailsState) =
        subJourney(state) {
            task(journey.householdsAndTenantsTask) {
                withDependencies { dependencies }
                nextStep {
                    when (state.householdsAndTenantsTask.households.outcome) {
                        HouseholdMode.PROVIDE_THIS_LATER -> exitStep
                        else -> journey.rentIncludesBillsTask.firstStep
                    }
                }
                savable()
            }
            task(journey.rentIncludesBillsTask) {
                parents {
                    AndParents(
                        journey.householdsAndTenantsTask.isComplete(),
                        journey.householdsAndTenantsTask.households.doesNotHaveOutcome(HouseholdMode.PROVIDE_THIS_LATER),
                    )
                }
                backUrl { journey.householdsAndTenantsTask.exitStep.backUrl }
                nextStep { journey.furnishedStatus }
                savable()
            }
            step(journey.furnishedStatus) {
                routeSegment(FurnishedStatusStep.ROUTE_SEGMENT)
                parents { journey.rentIncludesBillsTask.isComplete() }
                nextStep { journey.rentFrequencyAndAmountTask.firstStep }
                savable()
            }
            task(journey.rentFrequencyAndAmountTask) {
                parents { journey.furnishedStatus.hasOutcome(Complete.COMPLETE) }
                nextStep { exitStep }
                savable()
            }
            exitStep {
                savable()
                parents {
                    OrParents(
                        journey.rentFrequencyAndAmountTask.isComplete(),
                        AndParents(
                            journey.householdsAndTenantsTask.isComplete(),
                            journey.householdsAndTenantsTask.households.hasOutcome(HouseholdMode.PROVIDE_THIS_LATER),
                        ),
                    )
                }
            }
        }
}

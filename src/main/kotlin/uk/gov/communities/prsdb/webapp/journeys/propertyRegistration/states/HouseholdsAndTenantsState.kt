package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideTenancyDetailsLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.TenantsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseholdsAndTenantsDependencies

interface HouseholdsAndTenantsState : JourneyState {
    val households: HouseholdStep
    val tenants: TenantsStep
    val provideTenancyDetailsLaterStep: ProvideTenancyDetailsLaterStep
    val dependencies: HouseholdsAndTenantsDependencies
}

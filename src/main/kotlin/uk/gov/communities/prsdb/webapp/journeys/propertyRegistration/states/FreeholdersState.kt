package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdersBackRoutingStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdersInfoStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.FreeholdersDependencies
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.FreeholdersDetailsTask
import uk.gov.communities.prsdb.webapp.models.dataModels.FreeholderDataModel

interface FreeholdersState :
    JourneyState,
    FreeholdersListState,
    FreeholdersDependencies {
    val freeholdersInfoStep: FreeholdersInfoStep
    val freeholdersDetailsTask: FreeholdersDetailsTask
    val freeholdersBackRoutingStep: FreeholdersBackRoutingStep
}

// Null if none were collected, which is an impossible state for an ownership type requiring freeholder records.
fun FreeholdersState.getFreeholdersOrNull(): Map<Int, FreeholderDataModel>? = freeholdersMap?.takeIf { it.isNotEmpty() }

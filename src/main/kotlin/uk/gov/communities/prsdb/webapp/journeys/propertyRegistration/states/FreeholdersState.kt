package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdersStep

interface FreeholdersState : JourneyState {
    val freeholdersStep: FreeholdersStep
}

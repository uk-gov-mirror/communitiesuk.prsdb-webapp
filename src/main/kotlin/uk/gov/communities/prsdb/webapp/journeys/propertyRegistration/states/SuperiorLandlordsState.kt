package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.SuperiorLandlordsStep

interface SuperiorLandlordsState : JourneyState {
    val superiorLandlordsStep: SuperiorLandlordsStep
}

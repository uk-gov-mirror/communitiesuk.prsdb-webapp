package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.constants.enums.WhoProvidesRentalDetails
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.WhoProvidesRentalDetailsStep

interface WhoProvidesDetailsState : JourneyState {
    val whoProvidesRentalDetailsStep: WhoProvidesRentalDetailsStep
    val lettingAgentEmailStep: LettingAgentEmailStep
    var cachedWhoProvidesRentalDetails: WhoProvidesRentalDetails?
    val loggedInLandlordEmail: String
}

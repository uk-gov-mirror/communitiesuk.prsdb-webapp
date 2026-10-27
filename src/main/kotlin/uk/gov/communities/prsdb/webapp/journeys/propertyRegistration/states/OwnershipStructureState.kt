package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.FreeholdersTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipTypeTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.SuperiorLandlordsTask

interface OwnershipStructureState : JourneyState {
    val ownershipTypeTask: OwnershipTypeTask
    val freeholdersTask: FreeholdersTask
    val superiorLandlordsTask: SuperiorLandlordsTask
}

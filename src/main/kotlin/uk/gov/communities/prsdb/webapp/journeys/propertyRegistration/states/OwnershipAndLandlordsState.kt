package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.JointLandlordsPropertyRegistrationTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipStructureTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyManagerTask

interface OwnershipAndLandlordsState : JourneyState {
    val jointLandlordsTask: JointLandlordsPropertyRegistrationTask
    val ownershipStructureTask: OwnershipStructureTask
    val propertyManagerTask: PropertyManagerTask
}

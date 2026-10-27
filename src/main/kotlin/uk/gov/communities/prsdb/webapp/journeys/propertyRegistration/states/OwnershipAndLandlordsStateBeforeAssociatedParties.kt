package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.JointLandlordsPropertyRegistrationTask

interface OwnershipAndLandlordsStateBeforeAssociatedParties : JourneyState {
    val ownershipTypeStep: OwnershipTypeStep
    val jointLandlordsTask: JointLandlordsPropertyRegistrationTask
}

package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.AnyoneInOwnershipChainStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.SuperiorLandlordsBackRoutingStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.SuperiorLandlordsDetailsTask
import uk.gov.communities.prsdb.webapp.models.dataModels.SuperiorLandlordDataModel

interface SuperiorLandlordsState :
    JourneyState,
    SuperiorLandlordsListState {
    val anyoneInOwnershipChainStep: AnyoneInOwnershipChainStep
    val superiorLandlordsDetailsTask: SuperiorLandlordsDetailsTask
    val superiorLandlordsBackRoutingStep: SuperiorLandlordsBackRoutingStep
}

// Null until the anyone in ownership chain step is answered, which only happens for ownership types requiring
// superior landlord info.
val SuperiorLandlordsState.anyoneInOwnershipChain: Boolean?
    get() = anyoneInOwnershipChainStep.formModelIfReachableOrNull?.anyoneInOwnershipChain

// Null if none were collected, which is an impossible state once anyoneInOwnershipChain is true.
fun SuperiorLandlordsState.getSuperiorLandlordsOrNull(): Map<Int, SuperiorLandlordDataModel>? =
    superiorLandlordsMap?.takeIf { it.isNotEmpty() }

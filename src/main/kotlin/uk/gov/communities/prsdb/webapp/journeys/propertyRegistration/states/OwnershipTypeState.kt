package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.helpers.converters.OwnershipTypeAnswersConverter
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdTypeStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipKindStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnsPropertyStep

interface OwnershipTypeState : JourneyState {
    val ownsPropertyStep: OwnsPropertyStep
    val ownershipKindStep: OwnershipKindStep
    val freeholdTypeStep: FreeholdTypeStep

    val ownershipType: OwnershipType?
        get() =
            OwnershipTypeAnswersConverter.convertOrNull(
                ownsPropertyStep.outcome,
                ownershipKindStep.outcome,
                freeholdTypeStep.outcome,
            )

    // False until the ownership type task is complete, which is always before the freeholders are asked for.
    val isJointFreeholder: Boolean
        get() = ownershipType?.isJointFreeholder() ?: false
}

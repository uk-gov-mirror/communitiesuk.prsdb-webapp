package uk.gov.communities.prsdb.webapp.helpers.converters

import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.FreeholdType
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.OwnershipKind
import uk.gov.communities.prsdb.webapp.journeys.shared.YesOrNo

object OwnershipTypeAnswersConverter {
    // Null until enough of the ownership type task has been answered to determine the type, which
    // is exactly when the task is complete (see OwnershipTypeTask.exitStep).
    fun convertOrNull(
        ownsProperty: YesOrNo?,
        ownershipKind: OwnershipKind?,
        freeholdType: FreeholdType?,
    ): OwnershipType? =
        when (ownsProperty) {
            YesOrNo.NO -> OwnershipType.NONE
            YesOrNo.YES ->
                when (ownershipKind) {
                    OwnershipKind.LEASEHOLD_ONLY -> OwnershipType.LEASEHOLD_ONLY
                    OwnershipKind.FREEHOLD_ONLY ->
                        when (freeholdType) {
                            FreeholdType.SOLE_OWNERSHIP -> OwnershipType.SOLE_FREEHOLD
                            FreeholdType.JOINT_TENANTS -> OwnershipType.JOINT_TENANTS
                            FreeholdType.TENANTS_IN_COMMON -> OwnershipType.TENANTS_IN_COMMON
                            FreeholdType.COMMONHOLD -> OwnershipType.COMMONHOLD
                            null -> null
                        }
                    OwnershipKind.FREEHOLD_AND_LEASEHOLD ->
                        when (freeholdType) {
                            FreeholdType.SOLE_OWNERSHIP -> OwnershipType.SHARE_OF_SOLE_FREEHOLD
                            FreeholdType.JOINT_TENANTS -> OwnershipType.SHARE_OF_JOINT_TENANTS
                            FreeholdType.TENANTS_IN_COMMON -> OwnershipType.SHARE_OF_TENANTS_IN_COMMON
                            FreeholdType.COMMONHOLD -> OwnershipType.SHARE_OF_COMMONHOLD
                            null -> null
                        }
                    null -> null
                }
            null -> null
        }

    fun convert(ownershipType: OwnershipType): OwnershipTypeAnswers =
        when (ownershipType) {
            OwnershipType.NONE -> OwnershipTypeAnswers(YesOrNo.NO, null, null)
            OwnershipType.LEASEHOLD_ONLY -> OwnershipTypeAnswers(YesOrNo.YES, OwnershipKind.LEASEHOLD_ONLY, null)
            OwnershipType.SOLE_FREEHOLD -> freeholdOnly(FreeholdType.SOLE_OWNERSHIP)
            OwnershipType.JOINT_TENANTS -> freeholdOnly(FreeholdType.JOINT_TENANTS)
            OwnershipType.TENANTS_IN_COMMON -> freeholdOnly(FreeholdType.TENANTS_IN_COMMON)
            OwnershipType.COMMONHOLD -> freeholdOnly(FreeholdType.COMMONHOLD)
            OwnershipType.SHARE_OF_SOLE_FREEHOLD -> freeholdAndLeasehold(FreeholdType.SOLE_OWNERSHIP)
            OwnershipType.SHARE_OF_JOINT_TENANTS -> freeholdAndLeasehold(FreeholdType.JOINT_TENANTS)
            OwnershipType.SHARE_OF_TENANTS_IN_COMMON -> freeholdAndLeasehold(FreeholdType.TENANTS_IN_COMMON)
            OwnershipType.SHARE_OF_COMMONHOLD -> freeholdAndLeasehold(FreeholdType.COMMONHOLD)
        }

    private fun freeholdOnly(freeholdType: FreeholdType) = OwnershipTypeAnswers(YesOrNo.YES, OwnershipKind.FREEHOLD_ONLY, freeholdType)

    private fun freeholdAndLeasehold(freeholdType: FreeholdType) =
        OwnershipTypeAnswers(YesOrNo.YES, OwnershipKind.FREEHOLD_AND_LEASEHOLD, freeholdType)
}

// A later answer is null where that question is not asked: ownershipKind when the landlord does not
// own the property, and freeholdType when there is no freehold to describe.
data class OwnershipTypeAnswers(
    val ownsProperty: YesOrNo,
    val ownershipKind: OwnershipKind?,
    val freeholdType: FreeholdType?,
)

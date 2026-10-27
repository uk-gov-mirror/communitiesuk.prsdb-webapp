package uk.gov.communities.prsdb.webapp.models.dataModels

import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import java.time.LocalDate

data class OutstandingActionsDataModel(
    val propertyOwnershipId: Long,
    val singleLineAddress: String,
    val isOccupied: Boolean,
    val isLicensingProvideLater: Boolean,
    val isTenancyProvideLater: Boolean,
    val provideLaterDeadline: LocalDate?,
    val complianceStatus: ComplianceStatusDataModel?,
) {
    val shouldShowLicensingAction: Boolean
        get() = isOccupied && isLicensingProvideLater

    val shouldShowTenancyAction: Boolean
        get() = isOccupied && isTenancyProvideLater

    val hasPropertyDetailsAction: Boolean
        get() = shouldShowLicensingAction || shouldShowTenancyAction

    val shouldShowOnOutstandingActionsPage: Boolean
        get() = hasPropertyDetailsAction || complianceStatus?.shouldShowOnComplianceActionsPage == true

    companion object {
        fun fromPropertyOwnership(propertyOwnership: PropertyOwnership): OutstandingActionsDataModel =
            OutstandingActionsDataModel(
                propertyOwnershipId = propertyOwnership.id,
                singleLineAddress = propertyOwnership.address.singleLineAddress,
                isOccupied = propertyOwnership.isOccupied,
                isLicensingProvideLater = propertyOwnership.licenseProvideLater == true,
                isTenancyProvideLater = propertyOwnership.tenancyProvideLater == true,
                provideLaterDeadline = propertyOwnership.provideLaterDeadline,
                complianceStatus = propertyOwnership.propertyCompliance?.let { ComplianceStatusDataModel.fromPropertyCompliance(it) },
            )
    }
}

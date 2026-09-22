package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels

import org.springframework.context.MessageSource
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.helpers.extensions.MessageSourceExtensions.Companion.getMessageForKey

class PropertyDetailsViewModel(
    propertyOwnership: PropertyOwnership,
    isLandlordView: Boolean = true,
    messageSource: MessageSource,
    // TODO PDJB-1733: Remove when the correspondence is always shown
    showCorrespondenceSection: Boolean = true,
) : PropertyDetailsViewModelBase(
        propertyOwnership,
        if (isLandlordView) PropertyDetailsViewType.LANDLORD else PropertyDetailsViewType.LOCAL_COUNCIL,
        messageSource,
    ) {
    val showTenancySection: Boolean = isOccupied

    val tenancyHeadingKey: String = "propertyDetails.propertyRecord.tenancy.heading"

    val registrationDetails: List<SummaryListRowViewModel> =
        listOf(registrationNumberRow(), registrationDateRow())

    val propertyDetailsSection: List<SummaryListRowViewModel> =
        listOf(addressRow(), localCouncilRow(), propertyTypeRow(), bedroomsRow())

    val ownershipSection: List<SummaryListRowViewModel> =
        listOf(ownershipTypeRow("propertyDetails.propertyRecord.ownership.ownershipType"))

    // TODO PDJB-1736: replace the placeholder email and address below with values
    //  read from the correspondence entity once the DB schema and repository exist.
    val correspondenceSection: List<SummaryListRowViewModel>? =
        if (showCorrespondenceSection) {
            listOf(
                SummaryListRowViewModel(
                    fieldHeading = "propertyDetails.propertyRecord.correspondence.emailAddress",
                    fieldValue = "landlord@example.com",
                    // TODO: PDJB-1595: Change link
                ),
                SummaryListRowViewModel(
                    fieldHeading = "propertyDetails.propertyRecord.correspondence.address",
                    fieldValue = listOf("Flat 1", "11 Elm Drive", "London", "NW8 2DK"),
                    // TODO: PDJB-1596: Change link
                ),
            )
        } else {
            null
        }

    val occupiedSection: List<SummaryListRowViewModel> =
        listOf(occupiedRow("propertyDetails.propertyRecord.occupation.isOccupied"))

    val licensingSection: List<SummaryListRowViewModel> = buildLicensingSection()

    val licensingProvideLaterParagraph: String? =
        if (isLicensingProvideLater && !isLandlordView) {
            if (hasBeenOccupiedSinceRegistration) {
                getProvideLaterDeadlineText("propertyDetails.propertyRecord.licensing.councilOccupied")
            } else {
                messageSource.getMessageForKey("propertyDetails.propertyRecord.licensing.councilNotProvided")
            }
        } else {
            null
        }

    val tenancySection: List<SummaryListRowViewModel> = buildTenancySection()

    val tenancyProvideLaterParagraph: String? =
        when {
            !showTenancySection || isLandlordView -> {
                null
            }

            isTenancyProvideLater && hasBeenOccupiedSinceRegistration -> {
                getProvideLaterDeadlineText("propertyDetails.propertyRecord.tenancy.councilOccupied")
            }

            isTenancyProvideLater -> {
                messageSource.getMessageForKey("propertyDetails.propertyRecord.tenancy.councilNotProvided")
            }

            else -> {
                null
            }
        }
}

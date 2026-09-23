package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels

import org.springframework.context.MessageSource
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateCorrespondenceAddressController
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.helpers.extensions.MessageSourceExtensions.Companion.getMessageForKey
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep

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

    val correspondenceSection: List<SummaryListRowViewModel>? =
        if (showCorrespondenceSection) {
            listOf(
                SummaryListRowViewModel(
                    fieldHeading = "propertyDetails.propertyRecord.correspondence.emailAddress",
                    fieldValue = propertyOwnership.correspondenceEmail,
                    // TODO PDJB-1595: when adding the landlord change link, build this row via
                    //  PropertyDetailsViewModelBase.rowWithViewTypeSpecificChangeLink
                ),
                rowWithViewTypeSpecificChangeLink(
                    key = "propertyDetails.propertyRecord.correspondence.address",
                    value = propertyOwnership.correspondenceAddress.toMultiLineAddress().split("\n"),
                    landlordActionLink =
                        LandlordUpdateCorrespondenceAddressController
                            .getUpdateCorrespondenceAddressRoute(propertyOwnership.id) +
                            "/${LookupAddressStep.ROUTE_SEGMENT}",
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

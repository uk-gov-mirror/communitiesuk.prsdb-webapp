package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.correspondenceAddress

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.exceptions.UpdateConflictException
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.AbstractCheckYourAnswersStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.AbstractCheckYourAnswersStepConfig
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.PropertyUpdateEmailService

@JourneyFrameworkComponent
class UpdateCorrespondenceAddressCyaConfig(
    private val propertyOwnershipService: PropertyOwnershipService,
    private val propertyUpdateEmailService: PropertyUpdateEmailService,
) : AbstractCheckYourAnswersStepConfig<UpdateCorrespondenceAddressJourneyState>() {
    override fun getStepSpecificContent(state: UpdateCorrespondenceAddressJourneyState): Map<String, Any> =
        mapOf(
            "title" to "propertyDetails.update.title",
            "showWarning" to true,
            "submitButtonText" to "forms.buttons.confirmAndSubmitUpdate",
            "insetText" to true,
            "summaryName" to "forms.update.checkCorrespondenceAddress.summaryName",
            "summaryListData" to getSummaryList(state),
        )

    override fun afterStepDataIsAdded(state: UpdateCorrespondenceAddressJourneyState) {
        try {
            propertyOwnershipService.updateCorrespondenceAddress(
                id = state.propertyId,
                address = state.addressTask.getAddress(),
                initialLastModifiedDate = Instant.parse(state.lastModifiedDate).toJavaInstant(),
            )
        } catch (ex: UpdateConflictException) {
            state.deleteJourney()
            throw ex
        }
        sendUpdateConfirmationEmail(state)
    }

    private fun sendUpdateConfirmationEmail(state: UpdateCorrespondenceAddressJourneyState) {
        propertyUpdateEmailService.sendUpdateEmails(
            state.propertyId,
            listOf("The postal address the council should contact"),
        )
    }

    private fun getSummaryList(state: UpdateCorrespondenceAddressJourneyState): List<SummaryListRowViewModel> {
        val lookupAddressStep = state.addressTask.lookupAddressStep
        return listOf(
            SummaryListRowViewModel.forCheckYourAnswersPage(
                fieldHeading = "forms.update.checkCorrespondenceAddress.postalAddress",
                fieldValue =
                    state.addressTask
                        .getAddress()
                        .toMultiLineAddress()
                        .split("\n"),
                destination =
                    Destination.VisitableStep(
                        lookupAddressStep,
                        state.getCyaJourneyId(lookupAddressStep),
                    ),
            ),
        )
    }
}

@JourneyFrameworkComponent
final class UpdateCorrespondenceAddressCyaStep(
    stepConfig: UpdateCorrespondenceAddressCyaConfig,
) : AbstractCheckYourAnswersStep<UpdateCorrespondenceAddressJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "correspondence-address-check-your-answers"
    }
}

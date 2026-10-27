package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.constants.EPC_GUIDE_URL
import uk.gov.communities.prsdb.webapp.constants.MEES_EXEMPTION_GUIDE_URL
import uk.gov.communities.prsdb.webapp.exceptions.NotNullFormModelValueIsNullException
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.EpcDetailState
import uk.gov.communities.prsdb.webapp.journeys.shared.YesOrNo
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.ConfirmEpcDetailsFromUprnFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.formModels.RadiosViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryCardActionViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.services.EpcCertificateUrlProvider

@JourneyFrameworkComponent
class ConfirmEpcRetrievedByUprnStepConfig(
    private val epcCertificateUrlProvider: EpcCertificateUrlProvider,
) : AbstractConfirmEpcDetailsStepConfig<ConfirmEpcDetailsFromUprnFormModel>() {
    override val formModelClass = ConfirmEpcDetailsFromUprnFormModel::class

    override fun getStepSpecificContent(state: EpcDetailState) =
        getRelevantEpc(state)?.let { epcDetails ->
            mapOf(
                "summaryCardTitle" to "propertyCompliance.epcTask.confirmEpcDetailsFromUprn.summaryCard.title",
                "summaryCardActions" to
                    listOf(
                        SummaryCardActionViewModel(
                            text = "propertyCompliance.epcTask.confirmEpcDetailsFromUprn.summaryCard.action",
                            url = epcCertificateUrlProvider.getEpcCertificateUrl(epcDetails.certificateNumber),
                            opensInNewTab = true,
                        ),
                    ),
                "summaryListRows" to
                    listOf(
                        SummaryListRowViewModel(
                            fieldHeading = "propertyCompliance.epcTask.confirmEpcDetailsFromUprn.summaryList.address.heading",
                            fieldValue = epcDetails.singleLineAddress,
                        ),
                        SummaryListRowViewModel(
                            fieldHeading = "propertyCompliance.epcTask.confirmEpcDetailsFromUprn.summaryList.energyRating.heading",
                            fieldValue = epcDetails.energyRatingUppercase,
                        ),
                        SummaryListRowViewModel(
                            fieldHeading = "propertyCompliance.epcTask.confirmEpcDetailsFromUprn.summaryList.expiryDate.heading",
                            fieldValue = epcDetails.expiryDate,
                        ),
                        SummaryListRowViewModel(
                            fieldHeading = "propertyCompliance.epcTask.confirmEpcDetailsFromUprn.summaryList.certificateNumber.heading",
                            fieldValue = epcDetails.certificateNumber,
                        ),
                    ),
                "whenYouCanRegisterAnExemptionUrl" to MEES_EXEMPTION_GUIDE_URL,
                "epcGuideUrl" to EPC_GUIDE_URL,
                "radioOptions" to
                    RadiosViewModel.yesOrNoRadios(noLabel = "propertyCompliance.epcTask.confirmEpcDetailsFromUprn.radios.no.label"),
            )
        } ?: throw NotNullFormModelValueIsNullException(
            "Attempting to access relevantEpc for ConfirmEpcRetrievedByUprnStepConfig but it was null.",
        )

    override fun chooseTemplate(state: EpcDetailState): String = "forms/confirmEpcDetailsByUprnForm"

    override fun afterStepIsReached(state: EpcDetailState) {
        if (state.epcRetrievedByUprnUpdatedSinceUserReview == true) {
            state.clearStepData(stepDataKey)
            state.epcRetrievedByUprnUpdatedSinceUserReview = false
            state.markAnswersChanged()
        }
    }
}

@JourneyFrameworkComponent
final class ConfirmEpcRetrievedByUprnStep(
    stepConfig: ConfirmEpcRetrievedByUprnStepConfig,
) : JourneyStep.RequestableStep<YesOrNo, ConfirmEpcDetailsFromUprnFormModel, EpcDetailState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "check-epc-details"
    }
}

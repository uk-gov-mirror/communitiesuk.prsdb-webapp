package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.exceptions.NotNullFormModelValueIsNullException
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.EpcDetailState
import uk.gov.communities.prsdb.webapp.journeys.shared.YesOrNo
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.ConfirmEpcDetailsFromCertificateNumberFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.formModels.RadiosButtonViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryCardActionViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.services.EpcCertificateUrlProvider

@JourneyFrameworkComponent
class ConfirmEpcDetailsRetrievedByCertificateNumberStepConfig(
    private val epcCertificateUrlProvider: EpcCertificateUrlProvider,
) : AbstractConfirmEpcDetailsStepConfig<ConfirmEpcDetailsFromCertificateNumberFormModel>() {
    override val formModelClass = ConfirmEpcDetailsFromCertificateNumberFormModel::class

    // TODO PDJB-746 - update content as required
    override fun getStepSpecificContent(state: EpcDetailState) =
        getRelevantEpc(state)?.let { epcDetails ->
            mapOf(
                "summaryCardTitle" to "propertyCompliance.epcTask.confirmEpcDetailsFromCertificateNumber.summaryCard.title",
                "summaryCardActions" to
                    listOf(
                        SummaryCardActionViewModel(
                            text = "propertyCompliance.epcTask.confirmEpcDetailsFromCertificateNumber.summaryCard.viewFullEpc",
                            url = epcCertificateUrlProvider.getEpcCertificateUrl(epcDetails.certificateNumber),
                            opensInNewTab = true,
                        ),
                    ),
                "summaryListRows" to
                    listOf(
                        SummaryListRowViewModel(
                            fieldHeading = "propertyCompliance.epcTask.confirmEpcDetailsFromCertificateNumber.summaryCard.address",
                            fieldValue = epcDetails.singleLineAddress,
                        ),
                        SummaryListRowViewModel(
                            fieldHeading =
                                "propertyCompliance.epcTask.confirmEpcDetailsFromCertificateNumber.summaryCard.energyEfficiencyRating",
                            fieldValue = epcDetails.energyRatingUppercase,
                        ),
                        SummaryListRowViewModel(
                            fieldHeading = "propertyCompliance.epcTask.confirmEpcDetailsFromCertificateNumber.summaryCard.expiryDate",
                            fieldValue = epcDetails.expiryDate,
                        ),
                        SummaryListRowViewModel(
                            fieldHeading =
                                "propertyCompliance.epcTask.confirmEpcDetailsFromCertificateNumber.summaryCard.certificateNumber",
                            fieldValue = epcDetails.certificateNumber,
                        ),
                    ),
                "radioOptions" to
                    listOf(
                        RadiosButtonViewModel(
                            value = true,
                            valueStr = "yes",
                            labelMsgKey = "forms.radios.option.yes.label",
                        ),
                        RadiosButtonViewModel(
                            value = false,
                            valueStr = "no",
                            labelMsgKey = "propertyCompliance.epcTask.confirmEpcDetailsFromCertificateNumber.radios.no.label",
                        ),
                    ),
            )
        } ?: throw NotNullFormModelValueIsNullException(
            "Attempting to access relevantEpc for ConfirmEpcDetailsRetrievedByCertificateNumberStepConfig but it was null.",
        )

    override fun chooseTemplate(state: EpcDetailState): String = "forms/confirmEpcDetailsRetrievedByCertificateNumberForm"

    override fun afterStepIsReached(state: EpcDetailState) {
        if (state.epcRetrievedByCertificateNumberUpdatedSinceUserReview == true) {
            state.clearStepData(stepDataKey)
            state.epcRetrievedByCertificateNumberUpdatedSinceUserReview = false
            state.markAnswersChanged()
        }
    }
}

@JourneyFrameworkComponent
final class ConfirmEpcDetailsRetrievedByCertificateNumberStep(
    stepConfig: ConfirmEpcDetailsRetrievedByCertificateNumberStepConfig,
) : JourneyStep.RequestableStep<YesOrNo, ConfirmEpcDetailsFromCertificateNumberFormModel, EpcDetailState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "confirm-epc-details"
    }
}

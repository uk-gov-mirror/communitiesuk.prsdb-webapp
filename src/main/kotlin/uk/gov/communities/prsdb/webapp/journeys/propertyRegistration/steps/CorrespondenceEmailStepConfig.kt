package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.CorrespondenceState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.CorrespondenceEmailFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.formModels.RadiosButtonViewModel

@JourneyFrameworkComponent
class CorrespondenceEmailStepConfig : AbstractRequestableStepConfig<Complete, CorrespondenceEmailFormModel, CorrespondenceState>() {
    override val formModelClass = CorrespondenceEmailFormModel::class

    override fun getStepSpecificContent(state: CorrespondenceState): Map<String, Any?> =
        mapOf(
            "radioOptions" to
                listOf(
                    RadiosButtonViewModel(
                        value = CorrespondenceEmailOption.ACCOUNT_EMAIL,
                        labelMsgKey = "registerProperty.correspondenceEmail.radios.option.accountEmail.label",
                        // TODO: PDJB-1738: Use the current organisational sub-user's email rather than the organisation's email.
                        hintValue = state.loggedInLandlordEmail,
                    ),
                    RadiosButtonViewModel(
                        value = CorrespondenceEmailOption.DIFFERENT_EMAIL,
                        labelMsgKey = "registerProperty.correspondenceEmail.radios.option.differentEmail.label",
                        conditionalFragment = "differentCorrespondenceEmailInput",
                    ),
                ),
        )

    override fun chooseTemplate(state: CorrespondenceState) = "forms/correspondenceEmailForm"

    override fun mode(state: CorrespondenceState): Complete? = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }
}

@JourneyFrameworkComponent
final class CorrespondenceEmailStep(
    stepConfig: CorrespondenceEmailStepConfig,
) : RequestableStep<Complete, CorrespondenceEmailFormModel, CorrespondenceState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "correspondence-email"
    }
}

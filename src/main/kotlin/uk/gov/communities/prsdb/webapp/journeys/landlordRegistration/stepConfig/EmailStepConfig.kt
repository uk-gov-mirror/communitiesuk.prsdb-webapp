package uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.EmailFormModel

@JourneyFrameworkComponent
class EmailStepConfig(
    private val featureFlagManager: FeatureFlagManager,
) : AbstractRequestableStepConfig<Complete, EmailFormModel, JourneyState>() {
    override val formModelClass = EmailFormModel::class

    override fun getStepSpecificContent(state: JourneyState) =
        mapOf(
            "fieldSetHeading" to "forms.email.fieldSetHeading",
            "fieldSetHint" to "forms.email.fieldSetHint",
            "label" to "forms.email.label",
            "submitButtonText" to "forms.buttons.continue",
        )

    override fun chooseTemplate(state: JourneyState) = template

    override fun mode(state: JourneyState) = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }

    private var template: String = EmailStep.DEFAULT_TEMPLATE

    fun withCorrespondenceTemplateIfFlagIsSet(): EmailStepConfig {
        if (featureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS)) {
            this.template = EmailStep.CORRESPONDENCE_TEMPLATE
        }
        return this
    }
}

@JourneyFrameworkComponent
final class EmailStep(
    stepConfig: EmailStepConfig,
) : RequestableStep<Complete, EmailFormModel, JourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "email"
        const val DEFAULT_TEMPLATE = "forms/emailForm"
        const val CORRESPONDENCE_TEMPLATE = "forms/updateEmailForm"
    }
}

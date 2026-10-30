package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.shared.YesOrNo
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyManagerFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.formModels.RadiosButtonViewModel

@JourneyFrameworkComponent
class PropertyManagerStepConfig :
    AbstractRequestableStepConfig<YesOrNo, PropertyManagerFormModel, JourneyState>() {
    override val formModelClass = PropertyManagerFormModel::class

    override fun getStepSpecificContent(state: JourneyState): Map<String, Any?> =
        mapOf(
            "radioOptions" to
                listOf(
                    RadiosButtonViewModel(
                        value = true,
                        labelMsgKey = "forms.radios.option.yes.label",
                    ),
                    RadiosButtonViewModel(
                        value = false,
                        labelMsgKey = "registerProperty.propertyManager.usingPropertyManager.radios.no",
                    ),
                ),
        )

    override fun chooseTemplate(state: JourneyState) = "forms/propertyManagerForm"

    override fun mode(state: JourneyState): YesOrNo? =
        getFormModelFromStateOrNull(state)?.usingPropertyManager?.let { if (it) YesOrNo.YES else YesOrNo.NO }
}

@JourneyFrameworkComponent
final class PropertyManagerStep(
    stepConfig: PropertyManagerStepConfig,
) : RequestableStep<YesOrNo, PropertyManagerFormModel, JourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "property-manager"
    }
}

package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.GasSafetyRegistrationCyaSummaryRowsFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSafetyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.services.UploadService

@JourneyFrameworkComponent
class CheckGasSafetyAnswersStepConfig(
    private val uploadService: UploadService,
) : AbstractRequestableStepConfig<Complete, NoInputFormModel, GasSafetyState>() {
    override val formModelClass = NoInputFormModel::class

    override fun getStepSpecificContent(state: GasSafetyState): Map<String, Any?> {
        val factory = GasSafetyRegistrationCyaSummaryRowsFactory(state.gasSafetyDetailsTask, uploadService)
        return mapOf(
            "gasSupplyRows" to factory.createGasSupplyRows(),
            "certRows" to factory.createCertRows(),
            "insetTextKey" to factory.getInsetTextKey(),
            "submitButtonText" to "forms.buttons.saveAndContinue",
        )
    }

    override fun chooseTemplate(state: GasSafetyState) = "forms/checkGasSafetyAnswersForm"

    override fun mode(state: GasSafetyState) = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }
}

@JourneyFrameworkComponent
final class CheckGasSafetyAnswersStep(
    stepConfig: CheckGasSafetyAnswersStepConfig,
) : RequestableStep<Complete, NoInputFormModel, GasSafetyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "check-gas-safety-answers"
    }
}

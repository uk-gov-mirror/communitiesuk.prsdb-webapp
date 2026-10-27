package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.ConfirmMissingComplianceFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.formModels.RadiosButtonViewModel

@JourneyFrameworkComponent
class ConfirmMissingComplianceStepConfig :
    AbstractRequestableStepConfig<ConfirmMissingComplianceMode, ConfirmMissingComplianceFormModel, PropertyRegistrationJourneyState>() {
    override val formModelClass = ConfirmMissingComplianceFormModel::class

    override val changesAnswers = false

    override fun getStepSpecificContent(state: PropertyRegistrationJourneyState) =
        mapOf(
            "isGasMissing" to HasMissingComplianceStepConfig.isGasCertInvalid(state.gasSafetyTask),
            "isElectricalMissing" to HasMissingComplianceStepConfig.isElectricalCertInvalid(state.electricalSafetyTask),
            "isEpcMissing" to HasMissingComplianceStepConfig.isEpcInvalid(state.epcTask),
            "radioOptions" to
                listOf(
                    RadiosButtonViewModel(
                        value = true,
                        valueStr = "yes",
                        labelMsgKey = "registerProperty.confirmMissingCompliance.radios.yes",
                    ),
                    RadiosButtonViewModel(
                        value = false,
                        valueStr = "no",
                        labelMsgKey = "registerProperty.confirmMissingCompliance.radios.no",
                    ),
                ),
        )

    override fun chooseTemplate(state: PropertyRegistrationJourneyState) = "forms/confirmMissingCompliance"

    override fun mode(state: PropertyRegistrationJourneyState): ConfirmMissingComplianceMode? =
        getFormModelFromStateOrNull(state)?.wantsToProceed?.let {
            if (it) ConfirmMissingComplianceMode.CONFIRMED else ConfirmMissingComplianceMode.GO_BACK
        }
}

@JourneyFrameworkComponent
final class ConfirmMissingComplianceStep(
    stepConfig: ConfirmMissingComplianceStepConfig,
) : RequestableStep<ConfirmMissingComplianceMode, ConfirmMissingComplianceFormModel, PropertyRegistrationJourneyState>(
        stepConfig,
    ) {
    companion object {
        const val ROUTE_SEGMENT = "confirm-missing-compliance"
    }
}

enum class ConfirmMissingComplianceMode {
    CONFIRMED,
    GO_BACK,
}

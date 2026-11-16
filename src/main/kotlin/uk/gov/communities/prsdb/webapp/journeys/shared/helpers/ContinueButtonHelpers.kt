package uk.gov.communities.prsdb.webapp.journeys.shared.helpers

import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.TenancyDetailsState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyTask

private fun JourneyBuilder<*>.useSubmitButtonText(
    vararg steps: JourneyStep<*, *, *>,
    messageKey: () -> String,
) {
    steps.forEach { step ->
        configureStep(step) {
            withAdditionalContentProperty { "submitButtonText" to messageKey() }
        }
    }
}

private fun JourneyBuilder<*>.useContinueButtons(vararg steps: JourneyStep<*, *, *>) =
    useSubmitButtonText(*steps) { "forms.buttons.continue" }

private fun JourneyBuilder<*>.useContinueWithoutCertButtons(
    vararg steps: JourneyStep<*, *, *>,
    isOccupied: () -> Boolean,
    continueWithoutCertMessageKey: String,
) = useSubmitButtonText(*steps) {
    if (isOccupied()) continueWithoutCertMessageKey else "forms.buttons.continue"
}

fun JourneyBuilder<*>.useContinueButtons(task: GasSafetyDetailsTask) {
    useContinueButtons(task.hasGasSupplyStep, task.hasGasCertStep, task.gasCertIssueDateStep, task.checkGasCertUploadsStep)
    useContinueWithoutCertButtons(
        task.gasCertExpiredStep,
        isOccupied = task::isOccupied,
        continueWithoutCertMessageKey = "forms.buttons.continueWithoutGasSafety",
    )
}

fun JourneyBuilder<*>.useContinueButtons(task: GasSafetyTask) {
    useContinueButtons(task.gasSafetyDetailsTask)
    useContinueButtons(task.checkGasSafetyAnswersStep)
}

fun JourneyBuilder<*>.useContinueButtons(task: ElectricalSafetyDetailsTask) {
    useContinueButtons(
        task.hasElectricalCertStep,
        // TODO PDJB-1679: Remove eicExpiryDateStep when the EIC_ISSUE_DATE flag is removed
        task.eicExpiryDateStep,
        task.eicIssueDateStep,
        task.checkEicUploadsStep,
        task.eicrExpiryDateStep,
        task.checkEicrUploadsStep,
    )
    useContinueWithoutCertButtons(
        task.eicExpiredStep,
        task.eicrExpiredStep,
        isOccupied = task::isOccupied,
        continueWithoutCertMessageKey = "forms.buttons.continueWithoutElectricalSafety",
    )
}

fun JourneyBuilder<*>.useContinueButtons(task: ElectricalSafetyTask) {
    useContinueButtons(task.electricalSafetyDetailsTask)
    useContinueButtons(task.checkElectricalSafetyAnswersStep)
}

fun JourneyBuilder<*>.useContinueButtons(task: EpcDetailsTask) {
    useContinueButtons(
        task.hasEpcStep,
        task.checkUprnMatchedEpcStep,
        task.confirmEpcDetailsRetrievedByCertificateNumberStep,
        task.epcInDateAtStartOfTenancyCheckStep,
        task.isEpcRequiredStep,
        task.epcExemptionStep,
        task.hasMeesExemptionStep,
        task.meesExemptionStep,
    )
}

fun JourneyBuilder<*>.useContinueButtons(task: EpcTask) {
    useContinueButtons(task.epcDetailsTask)
    useContinueButtons(task.checkEpcAnswersStep)
}

fun JourneyBuilder<*>.useContinueButtons(tenancyDetails: TenancyDetailsState) {
    useContinueButtons(
        tenancyDetails.householdsAndTenantsTask.households,
        tenancyDetails.householdsAndTenantsTask.tenants,
        tenancyDetails.rentIncludesBillsTask.rentIncludesBills,
        tenancyDetails.rentIncludesBillsTask.billsIncluded,
        tenancyDetails.furnishedStatus,
        tenancyDetails.rentFrequencyAndAmountTask.rentFrequency,
        tenancyDetails.rentFrequencyAndAmountTask.rentAmount,
    )
}

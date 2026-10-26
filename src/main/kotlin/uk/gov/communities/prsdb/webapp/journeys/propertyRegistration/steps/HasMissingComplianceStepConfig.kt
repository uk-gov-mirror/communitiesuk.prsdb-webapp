package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractInternalStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.ElectricalSafetyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.EpcState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSafetyState

@JourneyFrameworkComponent
class HasMissingComplianceStepConfig :
    AbstractInternalStepConfig<ConfirmMissingComplianceCheckResult, PropertyRegistrationJourneyState>() {
    override fun mode(state: PropertyRegistrationJourneyState): ConfirmMissingComplianceCheckResult {
        val anyInvalid =
            isGasCertInvalid(state.gasSafetyTask) ||
                isElectricalCertInvalid(state.electricalSafetyTask) ||
                isEpcInvalid(state.epcTask)
        return if (state.isOccupied && anyInvalid && !state.isDelegatedToLettingAgent()) {
            ConfirmMissingComplianceCheckResult.OCCUPIED_AND_HAS_INVALID_CERTIFICATES
        } else {
            ConfirmMissingComplianceCheckResult.UNOCCUPIED_OR_VALID_CERTIFICATES_OR_DELEGATED
        }
    }

    companion object {
        fun isGasCertInvalid(state: GasSafetyState): Boolean {
            if (state.gasSafetyDetailsTask.hasGasSupplyStep.outcome in
                listOf(HasGasSupplyMode.NO_SUPPLY, HasGasSupplyMode.PROVIDE_LATER, null)
            ) {
                return false
            }
            val isOutdated = state.gasSafetyDetailsTask.getGasSafetyCertificateIsOutdated()
            return isOutdated == null || isOutdated
        }

        fun isElectricalCertInvalid(state: ElectricalSafetyState): Boolean {
            if (state.electricalSafetyDetailsTask.hasElectricalCertStep.outcome == HasElectricalCertMode.PROVIDE_THIS_LATER) return false
            val isOutdated = state.electricalSafetyDetailsTask.getElectricalCertificateIsOutdated()
            return isOutdated == null || isOutdated
        }

        fun isEpcInvalid(state: EpcState): Boolean {
            if (state.epcDetailsTask.hasEpcStep.outcome == HasEpcMode.PROVIDE_LATER) return false
            val acceptedEpc =
                state.epcDetailsTask.acceptedEpcIfStillAccepted
                    ?: return state.epcDetailsTask.epcExemptionStep.formModelIfReachableOrNull?.exemptionReason == null
            return (
                (
                    acceptedEpc.isPastExpiryDate() &&
                        (state.epcDetailsTask.epcInDateAtStartOfTenancyCheckStep.outcome != EpcInDateAtStartOfTenancyCheckMode.IN_DATE)
                ) ||
                    (
                        !acceptedEpc.isEnergyRatingEOrBetter() &&
                            (state.epcDetailsTask.meesExemptionStep.formModelIfReachableOrNull?.exemptionReason == null)
                    )
            )
        }
    }
}

@JourneyFrameworkComponent
class HasMissingComplianceStep(
    stepConfig: HasMissingComplianceStepConfig,
) : JourneyStep.InternalStep<ConfirmMissingComplianceCheckResult, PropertyRegistrationJourneyState>(stepConfig)

enum class ConfirmMissingComplianceCheckResult {
    UNOCCUPIED_OR_VALID_CERTIFICATES_OR_DELEGATED,
    OCCUPIED_AND_HAS_INVALID_CERTIFICATES,
}

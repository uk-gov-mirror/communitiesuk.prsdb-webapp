package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.services.PaymentReferenceParameterService

@JourneyFrameworkComponent
class PaymentReturnStepConfig(
    private val paymentReferenceParameterService: PaymentReferenceParameterService,
) : AbstractRequestableStepConfig<Complete, NoInputFormModel, PropertyRegistrationJourneyState>() {
    override val formModelClass = NoInputFormModel::class

    override val changesAnswers = false

    override fun getStepSpecificContent(state: PropertyRegistrationJourneyState): Map<String, Any?> = emptyMap()

    override fun chooseTemplate(state: PropertyRegistrationJourneyState) = "forms/paymentProcessingForm"

    override fun beforeAttemptingToReachStep(state: PropertyRegistrationJourneyState): Boolean =
        paymentReferenceParameterService.getParameterOrNull() != null || state.paymentReference != null

    override fun afterStepIsReached(state: PropertyRegistrationJourneyState) {
        val paymentReference = paymentReferenceParameterService.getParameterOrNull() ?: return
        state.paymentReference = paymentReference
    }

    override fun mode(state: PropertyRegistrationJourneyState) = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }
}

@JourneyFrameworkComponent
final class PaymentReturnStep(
    stepConfig: PaymentReturnStepConfig,
) : RequestableStep<Complete, NoInputFormModel, PropertyRegistrationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "payment-return"
    }
}

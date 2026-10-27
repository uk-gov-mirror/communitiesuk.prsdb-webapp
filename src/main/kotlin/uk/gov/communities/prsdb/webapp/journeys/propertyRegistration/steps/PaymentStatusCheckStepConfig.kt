package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.constants.enums.PaymentStatus
import uk.gov.communities.prsdb.webapp.exceptions.CyaDataHasChangedException
import uk.gov.communities.prsdb.webapp.journeys.AbstractInternalStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationDataModelFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.services.AddressAvailabilityService
import uk.gov.communities.prsdb.webapp.services.PaymentService

enum class PaymentOutcome {
    SUCCESS,
    FAILURE,
    IN_PROGRESS,
}

@JourneyFrameworkComponent
class PaymentStatusCheckStepConfig(
    private val paymentService: PaymentService,
    private val propertyRegistrationDataModelFactory: PropertyRegistrationDataModelFactory,
    private val addressAvailabilityService: AddressAvailabilityService,
) : AbstractInternalStepConfig<PaymentOutcome, PropertyRegistrationJourneyState>() {
    override fun afterStepIsReached(state: PropertyRegistrationJourneyState) {
        val paymentReference = checkNotNull(state.paymentReference) { "No payment reference for journey ${state.journeyId}" }
        val govUkPayStatus = paymentService.getPropertyRegistrationPaymentStatus(state.journeyId, paymentReference)
        if (govUkPayStatus.isCreated()) {
            state.paymentOutcome = PaymentOutcome.IN_PROGRESS
            return
        }

        if (govUkPayStatus.status == PaymentStatus.CAPTURABLE && answersChangedSinceConfirmation(state)) {
            val cancelledStatus = paymentService.cancelUnfinalisedPayment(govUkPayStatus.paymentId)
            if (cancelledStatus == PaymentStatus.SUCCEEDED) {
                state.paymentOutcome = PaymentOutcome.SUCCESS
                return
            }
            throw CyaDataHasChangedException("Answers changed after confirmation for journey ${state.journeyId}; payment cancelled")
        }

        val finalisedStatus =
            paymentService.finalisePayment(
                govUkPayStatus.paymentId,
                govUkPayStatus.status,
                propertyRegistrationDataModelFactory.fromJourneyState(state),
            )

        val paymentOutcome =
            when (finalisedStatus) {
                PaymentStatus.SUCCEEDED -> PaymentOutcome.SUCCESS
                PaymentStatus.FAILED, PaymentStatus.CANCELLED -> PaymentOutcome.FAILURE
                PaymentStatus.CREATED, PaymentStatus.CAPTURABLE -> PaymentOutcome.IN_PROGRESS
            }
        state.paymentOutcome = paymentOutcome

        if (paymentOutcome == PaymentOutcome.FAILURE) {
            val addressTask = state.propertyDetailsTask.addressTask
            addressTask.isAddressAlreadyRegistered = addressTask.getAddress().uprn?.let { addressAvailabilityService.isAddressOwned(it) }
        }
    }

    override fun mode(state: PropertyRegistrationJourneyState) = state.paymentOutcome

    private fun answersChangedSinceConfirmation(state: PropertyRegistrationJourneyState) =
        state.answersLastChanged != state.answersConfirmedAt

    override fun resolveNextDestination(
        state: PropertyRegistrationJourneyState,
        defaultDestination: Destination,
    ): Destination {
        if (state.paymentOutcome == PaymentOutcome.SUCCESS) {
            state.deleteJourney()
        }
        if (state.paymentOutcome == PaymentOutcome.FAILURE) {
            val addressTask = state.propertyDetailsTask.addressTask
            if (addressTask.isAddressAlreadyRegistered == true) {
                return Destination(addressTask.alreadyRegisteredStep)
            }
        }
        return defaultDestination
    }
}

@JourneyFrameworkComponent
final class PaymentStatusCheckStep(
    stepConfig: PaymentStatusCheckStepConfig,
) : JourneyStep.InternalStep<PaymentOutcome, PropertyRegistrationJourneyState>(stepConfig)

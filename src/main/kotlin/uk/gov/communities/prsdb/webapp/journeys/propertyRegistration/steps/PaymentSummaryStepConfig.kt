package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.helpers.PaymentHelper
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyRegistrationPaymentQuote
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.services.AddressAvailabilityService
import uk.gov.communities.prsdb.webapp.services.PaymentService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@JourneyFrameworkComponent
class PaymentSummaryStepConfig(
    private val addressAvailabilityService: AddressAvailabilityService,
    private val paymentService: PaymentService,
) : AbstractRequestableStepConfig<Complete, NoInputFormModel, PropertyRegistrationJourneyState>() {
    override val formModelClass = NoInputFormModel::class

    override val changesAnswers = false

    override fun getStepSpecificContent(state: PropertyRegistrationJourneyState): Map<String, Any?> {
        val quote = paymentService.getPropertyRegistrationPaymentQuote()
        state.paymentQuote = quote

        val firstChargeableDate = paymentService.getFirstChargeableDate(quote.quoteDate)

        return mapOf(
            "annualFee" to PaymentHelper.formatPence(paymentService.getAnnualFeeInPence()),
            "firstChargeableDate" to firstChargeableDate.format(DATE_FORMATTER),
            "isPreGratisPeriod" to firstChargeableDate.isAfter(quote.quoteDate),
            "summaryListRows" to getSummaryListRows(state, quote),
        )
    }

    private fun getSummaryListRows(
        state: PropertyRegistrationJourneyState,
        quote: PropertyRegistrationPaymentQuote,
    ) = listOf(
        SummaryListRowViewModel(
            "registerProperty.paymentSummary.summary.address",
            state.propertyDetailsTask.addressTask
                .getAddress()
                .toMultiLineAddress()
                .split("\n"),
        ),
        SummaryListRowViewModel("registerProperty.paymentSummary.summary.payment", PaymentHelper.formatPence(quote.amountInPence)),
        SummaryListRowViewModel("registerProperty.paymentSummary.summary.paymentDate", quote.quoteDate.format(DATE_FORMATTER)),
        SummaryListRowViewModel("registerProperty.paymentSummary.summary.renewalDate", quote.renewalDate.format(DATE_FORMATTER)),
    )

    override fun chooseTemplate(state: PropertyRegistrationJourneyState) = "forms/paymentSummary"

    override fun mode(state: PropertyRegistrationJourneyState) = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }

    override fun afterStepDataIsAdded(state: PropertyRegistrationJourneyState) {
        state.paymentOutcome = null
        state.paymentReference = null

        val addressTask = state.propertyDetailsTask.addressTask
        val uprn = addressTask.getAddress().uprn
        if (uprn != null && addressAvailabilityService.isAddressOwned(uprn)) {
            addressTask.isAddressAlreadyRegistered = true
        }
    }

    override fun resolveNextDestination(
        state: PropertyRegistrationJourneyState,
        defaultDestination: Destination,
    ): Destination {
        val addressTask = state.propertyDetailsTask.addressTask
        if (addressTask.isAddressAlreadyRegistered == true) {
            return Destination(addressTask.alreadyRegisteredStep)
        }

        val today = LocalDate.now(DateTimeHelper.UK_ZONE)
        val quote = state.paymentQuote

        // TODO PDJB-1861: Check that the quote the user was shown matches the stored quote before creating the payment (e.g. when                                                                                                                                                                                                                                               ┃
        //  the page is open in several tabs), and explain to the user when the quoted amount has changed.
        if (quote == null || quote.quoteDate != today) {
            state.paymentQuote = paymentService.getPropertyRegistrationPaymentQuote(today)
            // TODO PDJB-1869: Inform the user about the updated payment total when a stale quote redirects them back to the payment summary
            //  page. For now, we will just redirect them back to the payment summary page with the updated total and no info text.
            return Destination(state.paymentSummaryStep)
        }

        val nextUrl =
            paymentService.createPropertyRegistrationPayment(
                state.journeyId,
                state.loggedInLandlordEmailAtStartOfJourney,
                quote,
            )
        return Destination.ExternalUrl(nextUrl)
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
    }
}

@JourneyFrameworkComponent
final class PaymentSummaryStep(
    stepConfig: PaymentSummaryStepConfig,
) : RequestableStep<Complete, NoInputFormModel, PropertyRegistrationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "payment-summary"
    }
}

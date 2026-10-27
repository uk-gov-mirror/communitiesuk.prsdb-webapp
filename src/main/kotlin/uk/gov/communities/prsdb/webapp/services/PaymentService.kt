package uk.gov.communities.prsdb.webapp.services

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.clients.GovUkPayClient
import uk.gov.communities.prsdb.webapp.constants.enums.PaymentFailureType
import uk.gov.communities.prsdb.webapp.constants.enums.PaymentStatus
import uk.gov.communities.prsdb.webapp.database.entity.LandlordIncompleteProperty
import uk.gov.communities.prsdb.webapp.database.entity.Payment
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.repository.LandlordIncompletePropertiesRepository
import uk.gov.communities.prsdb.webapp.database.repository.PaymentRepository
import uk.gov.communities.prsdb.webapp.database.repository.SavedJourneyStateRepository
import uk.gov.communities.prsdb.webapp.exceptions.GovUkPayException
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.helpers.RenewalDateHelper
import uk.gov.communities.prsdb.webapp.helpers.extensions.MessageSourceExtensions.Companion.getMessageForKey
import uk.gov.communities.prsdb.webapp.models.dataModels.PaymentStatusCheckDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyRegistrationDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyRegistrationPaymentQuote
import uk.gov.communities.prsdb.webapp.models.dataModels.govUkPay.GovUkPayCreatePaymentRequest
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.Year
import java.time.temporal.ChronoUnit
import java.util.UUID

@PrsdbWebService
class PaymentService(
    @Value("\${gov-uk-pay.annual-payment-amount-in-pence}") private val annualFeeInPence: Int,
    @Value("\${gov-uk-pay.gratis-period-end-date}") gratisPeriodEndDate: String,
    private val govUkPayClient: GovUkPayClient,
    private val paymentRepository: PaymentRepository,
    private val landlordIncompletePropertiesRepository: LandlordIncompletePropertiesRepository,
    private val savedJourneyStateRepository: SavedJourneyStateRepository,
    private val userToLandlordService: UserToLandlordService,
    private val propertyRegistrationService: PropertyRegistrationService,
    private val absoluteUrlProvider: AbsoluteUrlProvider,
    private val messageSource: MessageSource,
    private val transactionTemplate: TransactionTemplate,
    private val entityManager: EntityManager,
) {
    private val gratisPeriodEndDate: LocalDate = LocalDate.parse(gratisPeriodEndDate)

    fun getPropertyRegistrationPaymentQuote(
        quoteDate: LocalDate = LocalDate.now(DateTimeHelper.UK_ZONE),
    ): PropertyRegistrationPaymentQuote {
        val anniversary = userToLandlordService.getCurrentLandlordForUser().anniversary ?: MonthDay.from(quoteDate)
        val renewalDate = RenewalDateHelper.getRenewalDate(anniversary, quoteDate)

        return PropertyRegistrationPaymentQuote(
            amountInPence = calculateProRatedFeeInPence(renewalDate, quoteDate),
            quoteDate = quoteDate,
            renewalDate = renewalDate,
        )
    }

    fun getAnnualFeeInPence(): Int = annualFeeInPence

    fun getFirstChargeableDate(quoteDate: LocalDate): LocalDate = maxOf(quoteDate, gratisPeriodEndDate.plusDays(1))

    fun createPropertyRegistrationPayment(
        journeyId: String,
        email: String,
        quote: PropertyRegistrationPaymentQuote,
    ): String {
        val incompleteProperty = getIncompletePropertyForCurrentUser(journeyId)

        val renewalDate = quote.renewalDate
        val amountInPence = quote.amountInPence
        check(amountInPence > 0) {
            "Cannot create a GOV.UK Pay payment for journey $journeyId: the fee for the period ending $renewalDate is " +
                "${amountInPence}p but GOV.UK Pay only accepts amounts greater than zero"
        }

        val reconciledStoredStatuses = reconcileStoredPaymentStatuses(incompleteProperty, journeyId)

        reconciledStoredStatuses.filterValues { it == PaymentStatus.SUCCEEDED }.keys.firstOrNull()?.let { succeededPaymentId ->
            throw IllegalStateException(
                "Cannot create a GOV.UK Pay payment for journey $journeyId: payment $succeededPaymentId for this " +
                    "property has already succeeded",
            )
        }

        val reference = UUID.randomUUID().toString()
        val returnUrl = absoluteUrlProvider.buildPropertyRegistrationPaymentReturnUri(journeyId, reference).toString()
        val createdGovUkPayPayment =
            govUkPayClient.createPayment(
                GovUkPayCreatePaymentRequest(
                    amount = amountInPence,
                    reference = reference,
                    description = messageSource.getMessageForKey("registerProperty.paymentDescription"),
                    returnUrl = returnUrl,
                    email = email,
                ),
            )

        paymentRepository.save(Payment.fromGovUkPay(createdGovUkPayPayment.payment, renewalDate, incompleteProperty))

        return createdGovUkPayPayment.nextUrl
    }

    private fun reconcileStoredPaymentStatuses(
        incompleteProperty: LandlordIncompleteProperty,
        journeyId: String,
    ): Map<String, PaymentStatus> =
        paymentRepository.findAllByAssociatedIncompleteProperty(incompleteProperty).associate { storedPayment ->
            storedPayment.paymentId to
                if (storedPayment.status in PaymentStatus.IN_PROGRESS_STATUSES) {
                    cancelOrReconcilePayment(storedPayment.paymentId, journeyId)
                } else {
                    storedPayment.status
                }
        }

    private fun cancelOrReconcilePayment(
        paymentId: String,
        journeyId: String,
    ): PaymentStatus {
        val govUkPayStatusBeforeCancellation = getGovUkPayPaymentStatus(paymentId)
        val govUkPayStatus =
            if (govUkPayStatusBeforeCancellation.isCancellable) {
                cancelOrGetFinishedGovUkPayStatus(paymentId, "before creating a new payment for journey $journeyId")
            } else {
                govUkPayStatusBeforeCancellation.status
            }

        if (govUkPayStatus !in PaymentStatus.IN_PROGRESS_STATUSES) {
            paymentRepository.updateStatusIfCurrentStatusIn(paymentId, PaymentStatus.IN_PROGRESS_STATUSES, govUkPayStatus, Instant.now())
        }
        return getStoredPaymentStatus(paymentId)
    }

    private fun cancelOrGetFinishedGovUkPayStatus(
        paymentId: String,
        errorContext: String,
    ): PaymentStatus =
        try {
            govUkPayClient.cancelPayment(paymentId)
            PaymentStatus.CANCELLED
        } catch (exception: GovUkPayException) {
            val govUkPayStatusAfterFailedCancellation = getGovUkPayPaymentStatus(paymentId)
            if (govUkPayStatusAfterFailedCancellation.isCancellable) {
                throw GovUkPayException(
                    "Could not cancel in-progress GOV.UK Pay payment $paymentId $errorContext: GOV.UK Pay rejected the " +
                        "cancellation but still reports the payment as cancellable",
                    exception,
                )
            }
            govUkPayStatusAfterFailedCancellation.status
        }

    fun getPropertyRegistrationPaymentStatus(
        journeyId: String,
        paymentReference: String,
    ): PaymentStatusCheckDataModel {
        val incompleteProperty = getIncompletePropertyForCurrentUser(journeyId)
        val payment =
            checkNotNull(paymentRepository.findByReferenceAndAssociatedIncompleteProperty(paymentReference, incompleteProperty)) {
                "No payment with reference $paymentReference found for journey $journeyId"
            }
        return getGovUkPayPaymentStatus(payment.paymentId)
    }

    fun getGovUkPayPaymentStatus(paymentId: String): PaymentStatusCheckDataModel {
        val govUkPayPayment = govUkPayClient.getPayment(paymentId)
        val status = PaymentStatus.fromGovUKPayStatus(govUkPayPayment.state.status)
        val failureType =
            if (status.isFailedOrCancelled()) {
                PaymentFailureType.fromGovUkPayCode(govUkPayPayment.state.code)
            } else {
                null
            }

        return PaymentStatusCheckDataModel(
            paymentId = paymentId,
            status = status,
            isCancellable = govUkPayPayment.links.cancelUrl != null,
            failureType = failureType,
        )
    }

    fun getLatestSucceededPaymentForProperty(propertyOwnership: PropertyOwnership): Payment? =
        paymentRepository.findFirstByAssociatedPropertyAndStatusOrderByPaymentCreatedAtDesc(
            propertyOwnership,
            PaymentStatus.SUCCEEDED,
        )

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    fun finalisePayment(
        paymentId: String,
        govUkPayStatus: PaymentStatus,
        registrationData: PropertyRegistrationDataModel,
    ): PaymentStatus =
        when (govUkPayStatus) {
            PaymentStatus.CREATED -> {
                throw IllegalArgumentException("Payment $paymentId cannot be finalised while in progress")
            }

            PaymentStatus.SUCCEEDED -> {
                getStoredPaymentStatus(paymentId)
            }

            PaymentStatus.FAILED, PaymentStatus.CANCELLED -> {
                paymentRepository.updateStatusIfCurrentStatusIn(
                    paymentId,
                    PaymentStatus.IN_PROGRESS_STATUSES,
                    govUkPayStatus,
                    Instant.now(),
                )
                getStoredPaymentStatus(paymentId)
            }

            PaymentStatus.CAPTURABLE -> {
                finaliseCapturablePayment(paymentId, registrationData)
            }
        }

    private fun finaliseCapturablePayment(
        paymentId: String,
        registrationData: PropertyRegistrationDataModel,
    ): PaymentStatus {
        recordPaymentAsCapturable(paymentId)

        return try {
            registerPropertyAndCapturePayment(paymentId, registrationData)
        } catch (exception: Exception) {
            println("Failed to finalise payment $paymentId, so cancelling it: ${exception.stackTraceToString()}")
            cancelUnfinalisedPayment(paymentId, exception)
        }
    }

    private fun recordPaymentAsCapturable(paymentId: String) {
        paymentRepository.updateStatusIfCurrentStatusIn(paymentId, listOf(PaymentStatus.CREATED), PaymentStatus.CAPTURABLE, Instant.now())
    }

    private fun registerPropertyAndCapturePayment(
        paymentId: String,
        registrationData: PropertyRegistrationDataModel,
    ): PaymentStatus =
        checkNotNull(
            transactionTemplate.execute { transactionStatus ->
                val incompleteProperty =
                    paymentRepository.findByIdOrNull(paymentId)?.associatedIncompleteProperty
                        ?: return@execute getStoredPaymentStatus(paymentId)

                val isClaimed =
                    paymentRepository.updateStatusIfCurrentStatusIn(
                        paymentId,
                        listOf(PaymentStatus.CAPTURABLE),
                        PaymentStatus.SUCCEEDED,
                        Instant.now(),
                    ) == 1
                if (!isClaimed) return@execute getStoredPaymentStatus(paymentId)

                val otherPaymentIds = getStoredPaymentIds(incompleteProperty) - paymentId
                checkNoOtherPaymentHasSucceeded(otherPaymentIds, paymentId)
                cancelInProgressPayments(otherPaymentIds, paymentId)

                val storedPayment = checkNotNull(paymentRepository.findByIdOrNull(paymentId)) { "Payment $paymentId not found" }
                val property =
                    propertyRegistrationService.registerProperty(registrationData.copy(renewalDate = storedPayment.forPeriodEnding))
                storedPayment.associateWithProperty(property)
                storedPayment.status = PaymentStatus.SUCCEEDED
                paymentRepository.saveAndFlush(storedPayment)

                // Detached here because the @MapsId on its savedJourneyState cascades a persist on flush, undoing the journey's deletion
                entityManager.detach(incompleteProperty)
                savedJourneyStateRepository.delete(incompleteProperty.savedJourneyState)
                paymentRepository.flush()

                check(!transactionStatus.isRollbackOnly) { "Transaction finalising payment $paymentId is marked for rollback" }

                // Capturing the payment can't be rolled back, so nothing else should run in this transaction after it.
                // TODO PDJB-1853: Reconcile the payment if the transaction fails to commit after capture.
                govUkPayClient.capturePayment(paymentId)
                PaymentStatus.SUCCEEDED
            },
        )

    private fun getStoredPaymentIds(incompleteProperty: LandlordIncompleteProperty): List<String> =
        paymentRepository.findAllByAssociatedIncompleteProperty(incompleteProperty).map { it.paymentId }

    private fun checkNoOtherPaymentHasSucceeded(
        otherPaymentIds: List<String>,
        paymentId: String,
    ) {
        otherPaymentIds.firstOrNull { getStoredPaymentStatus(it) == PaymentStatus.SUCCEEDED }?.let { succeededPaymentId ->
            throw IllegalStateException(
                "Cannot finalise payment $paymentId: payment $succeededPaymentId for this property has already succeeded",
            )
        }
    }

    private fun cancelInProgressPayments(
        otherPaymentIds: List<String>,
        paymentId: String,
    ) {
        otherPaymentIds.filter { getStoredPaymentStatus(it) in PaymentStatus.IN_PROGRESS_STATUSES }.forEach { otherPaymentId ->
            val govUkPayStatus = cancelOrGetFinishedGovUkPayStatus(otherPaymentId, "before finalising payment $paymentId")
            check(govUkPayStatus.isFailedOrCancelled()) {
                "Cannot finalise payment $paymentId: GOV.UK Pay reports payment $otherPaymentId for this property as $govUkPayStatus"
            }
            paymentRepository.updateStatusIfCurrentStatusIn(
                otherPaymentId,
                PaymentStatus.IN_PROGRESS_STATUSES,
                PaymentStatus.CANCELLED,
                Instant.now(),
            )
        }
    }

    fun cancelUnfinalisedPayment(
        paymentId: String,
        cause: Exception? = null,
    ): PaymentStatus {
        val govUkPayStatus =
            try {
                cancelOrGetFinishedGovUkPayStatus(paymentId, "because it can no longer be finalised")
            } catch (cancelException: Exception) {
                cause?.let { cancelException.addSuppressed(it) }
                throw cancelException
            }

        val wasFinalisedByAnotherRequest =
            govUkPayStatus == PaymentStatus.SUCCEEDED && paymentRepository.findStatusByPaymentId(paymentId) == PaymentStatus.SUCCEEDED
        if (wasFinalisedByAnotherRequest) return PaymentStatus.SUCCEEDED

        if (!govUkPayStatus.isFailedOrCancelled()) {
            throw IllegalStateException("Payment $paymentId could not be finalised, but GOV.UK Pay reports it as $govUkPayStatus", cause)
        }

        paymentRepository.updateStatusIfCurrentStatusIn(
            paymentId,
            PaymentStatus.IN_PROGRESS_STATUSES,
            govUkPayStatus,
            Instant.now(),
        )
        return paymentRepository.findStatusByPaymentId(paymentId) ?: govUkPayStatus
    }

    private fun getStoredPaymentStatus(paymentId: String): PaymentStatus =
        checkNotNull(paymentRepository.findStatusByPaymentId(paymentId)) { "Payment $paymentId not found" }

    private fun getIncompletePropertyForCurrentUser(journeyId: String): LandlordIncompleteProperty {
        val baseUserId = SecurityContextHolder.getContext().authentication!!.name
        return checkNotNull(landlordIncompletePropertiesRepository.findBySavedJourneyState_JourneyIdAndUser_Id(journeyId, baseUserId)) {
            "No incomplete property found for journey $journeyId and user $baseUserId"
        }
    }

    fun calculateProRatedFeeInPence(
        renewalDate: LocalDate,
        today: LocalDate = LocalDate.now(DateTimeHelper.UK_ZONE),
    ): Int {
        require(renewalDate.isAfter(today)) { "Renewal date $renewalDate must be after today ($today)" }

        val chargeableDays = ChronoUnit.DAYS.between(today, renewalDate)
        val gratisDays = calculateGratisDays(today)
        val daysInYear = if (chargeablePeriodIncludesLeapDay(today, renewalDate)) 366L else 365L

        val proRatedFeeInPence = annualFeeInPence * (chargeableDays - gratisDays) / daysInYear

        return proRatedFeeInPence.coerceAtLeast(0L).toInt()
    }

    private fun calculateGratisDays(today: LocalDate): Long =
        if (today.isAfter(gratisPeriodEndDate)) 0L else ChronoUnit.DAYS.between(today, gratisPeriodEndDate.plusDays(1))

    private fun chargeablePeriodIncludesLeapDay(
        today: LocalDate,
        renewalDate: LocalDate,
    ): Boolean =
        (today.year..renewalDate.year)
            .filter { Year.isLeap(it.toLong()) }
            .map { LocalDate.of(it, Month.FEBRUARY, 29) }
            .any { !it.isBefore(today) && it.isBefore(renewalDate) }
}

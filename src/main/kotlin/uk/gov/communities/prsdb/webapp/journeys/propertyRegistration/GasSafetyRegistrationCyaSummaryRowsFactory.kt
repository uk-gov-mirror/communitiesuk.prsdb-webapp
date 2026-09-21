package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasCertOutcome
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSafetyDetailState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSupplyOutcome
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.toUploadedFileUrls
import uk.gov.communities.prsdb.webapp.services.UploadService

class GasSafetyRegistrationCyaSummaryRowsFactory(
    private val state: GasSafetyDetailState,
    private val uploadService: UploadService,
    private val destinationProvider: (JourneyStep.RequestableStep<*, *, *>) -> Destination = { Destination(it) },
) {
    private enum class Outcome {
        NO_GAS_SUPPLY,
        DEFERRED_ON_GAS_SUPPLY_QUESTION,
        NO_CERTIFICATE,
        VALID_CERTIFICATE,

        // TODO PDJB-1617: delete this outcome (and its branches below) when we remove the delegation feature flag -
        //  deferring on the gas-certificate question is the flag-off (legacy) behaviour only.
        DEFERRED_ON_GAS_CERTIFICATE_QUESTION,
    }

    private val outcome: Outcome = determineOutcome()

    fun createGasSupplyRows(): List<SummaryListRowViewModel> =
        when (outcome) {
            Outcome.NO_GAS_SUPPLY -> listOf(gasSupplyRow(hasSupply = false))
            Outcome.DEFERRED_ON_GAS_SUPPLY_QUESTION -> listOf(deferredOnGasSupplyRow())
            Outcome.DEFERRED_ON_GAS_CERTIFICATE_QUESTION -> listOf(gasSupplyRow(hasSupply = true), deferredOnGasCertificateRow())
            Outcome.NO_CERTIFICATE -> listOf(gasSupplyRow(hasSupply = true), noCertificateRow())
            Outcome.VALID_CERTIFICATE -> listOf(gasSupplyRow(hasSupply = true))
        }

    fun createCertRows(): List<SummaryListRowViewModel> =
        when (outcome) {
            Outcome.VALID_CERTIFICATE -> uploadedCertificateRows()
            else -> emptyList()
        }

    fun getInsetTextKey(): String? =
        when (outcome) {
            Outcome.NO_GAS_SUPPLY -> "checkGasSafety.noGasSupplyInsetText"
            Outcome.NO_CERTIFICATE -> if (state.isOccupied) "checkGasSafety.occupiedNoCertInsetText" else null
            else -> null
        }

    private fun determineOutcome(): Outcome =
        when (state.gasSupplyOutcome) {
            GasSupplyOutcome.NO_SUPPLY -> Outcome.NO_GAS_SUPPLY
            GasSupplyOutcome.PROVIDE_LATER -> Outcome.DEFERRED_ON_GAS_SUPPLY_QUESTION
            GasSupplyOutcome.HAS_SUPPLY -> determineCertificateOutcome()
            null -> throw IllegalStateException("CheckGasSafetyAnswersStep is not reachable before hasGasSupply is answered")
        }

    private fun determineCertificateOutcome(): Outcome =
        when (state.gasCertOutcome) {
            // TODO PDJB-1617: remove with the delegation feature flag
            GasCertOutcome.PROVIDE_LATER -> Outcome.DEFERRED_ON_GAS_CERTIFICATE_QUESTION
            GasCertOutcome.NO_CERTIFICATE -> Outcome.NO_CERTIFICATE
            GasCertOutcome.HAS_CERTIFICATE ->
                if (state.getGasSafetyCertificateIsOutdated() == true) Outcome.NO_CERTIFICATE else Outcome.VALID_CERTIFICATE
            null -> throw IllegalStateException("CheckGasSafetyAnswersStep is not reachable before hasGasCert is answered")
        }

    private fun gasSupplyRow(hasSupply: Boolean): SummaryListRowViewModel =
        row(
            fieldHeading = "checkGasSafety.gasSupply.fieldHeading",
            fieldValue = hasSupply,
            step = state.gasSupplyOutcomeStep,
        )

    private fun deferredOnGasSupplyRow(): SummaryListRowViewModel =
        row(
            fieldHeading = "checkGasSafety.gasSupply.fieldHeading",
            fieldValue = provideLaterKey,
            step = state.gasSupplyOutcomeStep,
        )

    // TODO PDJB-1617: remove this row builder with the delegation feature flag (flag-off/legacy behaviour only).
    private fun deferredOnGasCertificateRow(): SummaryListRowViewModel =
        row(
            fieldHeading = "checkGasSafety.gasCert.fieldHeading",
            fieldValue = provideLaterKey,
            step = state.gasCertOutcomeStep,
        )

    private fun noCertificateRow(): SummaryListRowViewModel =
        row(
            fieldHeading = "checkGasSafety.gasCert.fieldHeading",
            fieldValue = if (state.isOccupied) false else provideLaterKey,
            step = state.gasCertOutcomeStep,
        )

    private fun uploadedCertificateRows(): List<SummaryListRowViewModel> =
        listOf(
            row(
                fieldHeading = "checkGasSafety.validGasCert.fieldHeading",
                fieldValue = true,
                step = state.gasCertOutcomeStep,
            ),
            row(
                fieldHeading = "checkGasSafety.issueDate.fieldHeading",
                fieldValue = state.getGasSafetyCertificateIssueDateIfReachable(),
                step = state.gasCertIssueDateStep,
            ),
            row(
                fieldHeading = "checkGasSafety.yourCertificate.fieldHeading",
                fieldValue = uploadedCertificateFileUrls(),
                step = state.checkGasCertUploadsStep,
            ),
        )

    private fun uploadedCertificateFileUrls() =
        state.gasUploadMap
            .toList()
            .sortedBy { it.first }
            .map { (_, upload) -> uploadService.getFileUploadById(upload.fileUploadId) to upload.fileName }
            .toUploadedFileUrls(
                downloadMessageKey = "propertyDetails.complianceInformation.gasSafety.downloadCertificate",
                uploadService = uploadService,
            )

    private val provideLaterKey: String
        get() =
            if (state.isOccupied) {
                "checkGasSafety.provideThisLater.occupied"
            } else {
                "checkGasSafety.provideThisLater.unoccupied"
            }

    private fun row(
        fieldHeading: String,
        fieldValue: Any?,
        step: JourneyStep.RequestableStep<*, *, *>,
    ): SummaryListRowViewModel =
        SummaryListRowViewModel.forCheckYourAnswersPage(
            fieldHeading = fieldHeading,
            fieldValue = fieldValue,
            destination = destinationProvider(step),
        )
}

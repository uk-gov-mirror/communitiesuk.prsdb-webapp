package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import uk.gov.communities.prsdb.webapp.constants.GAS_SAFETY_CERT_VALIDITY_YEARS
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasCertUploadsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasSafetyAnswersStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertExpiredStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertIssueDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertMissingStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasAnyInCollectionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideGasCertLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RemoveGasCertUploadStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.UploadGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyDetailsTask

interface GasSafetyState : JourneyState {
    val gasSafetyDetailsTask: GasSafetyDetailsTask

    val checkGasSafetyAnswersStep: CheckGasSafetyAnswersStep
}

interface GasSafetyDetailState : JourneyState {
    val hasGasSupplyStep: HasGasSupplyStep
    val hasGasCertStep: HasGasCertStep
    val gasCertIssueDateStep: GasCertIssueDateStep
    val uploadGasCertStep: UploadGasCertStep
    val checkGasCertUploadsStep: CheckGasCertUploadsStep
    val removeGasCertUploadStep: RemoveGasCertUploadStep
    val gasCertExpiredStep: GasCertExpiredStep
    val gasCertMissingStep: GasCertMissingStep
    val provideGasCertLaterStep: ProvideGasCertLaterStep
    val hasUploadedCert: HasAnyInCollectionStep

    val isOccupied: Boolean
    val allowProvideCertificateLaterRoute: Boolean
    val propertyOwnershipId: Long?

    fun getGasSafetyCertificateIssueDateIfReachable() =
        gasCertIssueDateStep.formModelIfReachableOrNull?.let { date ->
            DateTimeHelper.parseDateOrNull(date.day, date.month, date.year)
        }

    fun getGasSafetyCertificateIsOutdated(): Boolean? =
        getGasSafetyCertificateIssueDateIfReachable()?.let { issueDate ->
            DateTimeHelper().getCurrentDateInUK() > issueDate.plus(DatePeriod(years = GAS_SAFETY_CERT_VALIDITY_YEARS))
        }

    val gasUploadIds: List<Long>
        get() =
            if (uploadGasCertStep.isStepReachable) {
                gasUploadMap.values.map { it.fileUploadId }
            } else {
                emptyList()
            }
    var gasUploadMap: Map<Int, CertificateUpload>
    var highestAssignedGasMemberId: Int?

    fun getNextGasUploadMemberId(): Int = highestAssignedGasMemberId?.let { it + 1 } ?: 1
}

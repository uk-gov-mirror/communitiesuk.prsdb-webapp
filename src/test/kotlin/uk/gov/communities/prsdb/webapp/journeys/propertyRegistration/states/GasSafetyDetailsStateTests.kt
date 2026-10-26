package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states

import kotlinx.datetime.toKotlinLocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.constants.GAS_SAFETY_CERT_VALIDITY_YEARS
import uk.gov.communities.prsdb.webapp.journeys.AbstractJourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasCertUploadsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertExpiredStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertIssueDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertMissingStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasAnyInCollectionStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.ProvideGasCertLaterStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RemoveGasCertUploadStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.UploadGasCertStep
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.TodayOrPastDateFormModel
import java.time.LocalDate

class GasSafetyDetailsStateTests {
    @Test
    fun `getGasSafetyCertificateIssueDateIfReachable returns the issue date from state as a LocalDate`() {
        // Arrange
        val issueDate = LocalDate.of(2020, 1, 1)
        val issueDateFormModel = TodayOrPastDateFormModel.fromDateOrNull(issueDate)!!
        val state = buildTestGasSafetyState(issueDateFormModel = issueDateFormModel)

        // Act
        val retrievedIssueDate = state.getGasSafetyCertificateIssueDateIfReachable()

        // Assert
        assertEquals(issueDate.toKotlinLocalDate(), retrievedIssueDate)
    }

    @Test
    fun `getGasSafetyCertificateIssueDateIfReachable returns null if the issue date is not set`() {
        val state = buildTestGasSafetyState(issueDateStepShouldBeReachable = true)
        assertNull(state.getGasSafetyCertificateIssueDateIfReachable())
    }

    @Test
    fun `getGasSafetyCertificateIssueDateIfReachable returns null if formModelIfReachableOrNull is null`() {
        val state = buildTestGasSafetyState(issueDateStepShouldBeReachable = false)
        assertNull(state.getGasSafetyCertificateIssueDateIfReachable())
    }

    @Test
    fun `getGasSafetyCertificateIsOutdated returns true if the certificate is older than GAS_SAFETY_CERT_VALIDITY_YEARS`() {
        // Arrange
        val issueDate = LocalDate.now().minusYears(GAS_SAFETY_CERT_VALIDITY_YEARS.toLong()).minusDays(5)
        val issueDateFormModel = TodayOrPastDateFormModel.fromDateOrNull(issueDate)!!
        val state = buildTestGasSafetyState(issueDateFormModel = issueDateFormModel)

        // Act, Assert
        assertTrue(state.getGasSafetyCertificateIsOutdated()!!)
    }

    @Test
    fun `getGasSafetyCertificateIsOutdated returns false if the certificate is newer than GAS_SAFETY_CERT_VALIDITY_YEARS`() {
        // Arrange
        val issueDate = LocalDate.now().minusYears(GAS_SAFETY_CERT_VALIDITY_YEARS.toLong()).plusDays(5)
        val issueDateFormModel = TodayOrPastDateFormModel.fromDateOrNull(issueDate)!!
        val state = buildTestGasSafetyState(issueDateFormModel = issueDateFormModel)

        // Act, Assert
        assertFalse(state.getGasSafetyCertificateIsOutdated()!!)
    }

    @Test
    fun `getGasSafetyCertificateIsOutdated returns false if the certificate expires today`() {
        // Arrange
        val issueDate = LocalDate.now().minusYears(GAS_SAFETY_CERT_VALIDITY_YEARS.toLong())
        val issueDateFormModel = TodayOrPastDateFormModel.fromDateOrNull(issueDate)!!
        val state = buildTestGasSafetyState(issueDateFormModel = issueDateFormModel)

        // Act, Assert
        assertFalse(state.getGasSafetyCertificateIsOutdated()!!)
    }

    @Test
    fun `getGasSafetyCertificateIsOutdated returns null if the issueDate is null`() {
        val state = buildTestGasSafetyState()
        assertNull(state.getGasSafetyCertificateIsOutdated())
    }

    private fun buildTestGasSafetyState(
        issueDateFormModel: TodayOrPastDateFormModel = TodayOrPastDateFormModel(),
        issueDateStepShouldBeReachable: Boolean = true,
    ): GasSafetyDetailState =
        object : AbstractJourneyState(journeyStateService = mock()), GasSafetyDetailState {
            override val allowProvideCertificateLaterRoute: Boolean = true
            override val isOccupied: Boolean = false
            override val propertyOwnershipId: Long? = null
            override var gasUploadMap: Map<Int, CertificateUpload> = emptyMap()
            override var highestAssignedGasMemberId: Int? = null
            override val hasGasSupplyStep = mock<HasGasSupplyStep>()
            override val hasGasCertStep = mock<HasGasCertStep>()
            override val uploadGasCertStep = mock<UploadGasCertStep>()
            override val checkGasCertUploadsStep = mock<CheckGasCertUploadsStep>()
            override val removeGasCertUploadStep = mock<RemoveGasCertUploadStep>()
            override val gasCertExpiredStep = mock<GasCertExpiredStep>()
            override val gasCertMissingStep = mock<GasCertMissingStep>()
            override val provideGasCertLaterStep = mock<ProvideGasCertLaterStep>()
            override val hasUploadedCert: HasAnyInCollectionStep = mock()

            override val gasCertIssueDateStep =
                mock<GasCertIssueDateStep>().apply {
                    if (issueDateStepShouldBeReachable) {
                        whenever(this.formModelIfReachableOrNull).thenReturn(issueDateFormModel)
                    } else {
                        whenever(this.formModelIfReachableOrNull).thenReturn(null)
                    }
                }
        }
}

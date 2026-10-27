package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.EpcDetailState
import uk.gov.communities.prsdb.webapp.services.EpcCertificateUrlProvider
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.AlwaysTrueValidator

@ExtendWith(MockitoExtension::class)
class ConfirmEpcDetailsRetrievedByCertificateNumberStepConfigTests {
    @Mock
    lateinit var mockEpcCertificateUrlProvider: EpcCertificateUrlProvider

    @Mock
    lateinit var mockState: EpcDetailState

    private val routeSegment = ConfirmEpcDetailsRetrievedByCertificateNumberStep.ROUTE_SEGMENT

    private fun setupStepConfig(): ConfirmEpcDetailsRetrievedByCertificateNumberStepConfig {
        val stepConfig = ConfirmEpcDetailsRetrievedByCertificateNumberStepConfig(mockEpcCertificateUrlProvider)
        stepConfig.urlPath = routeSegment
        stepConfig.validator = AlwaysTrueValidator()
        return stepConfig
    }

    @Test
    fun `afterStepIsReached clears step data and resets flag when epcRetrievedByCertificateNumberUpdatedSinceUserReview is true`() {
        // Arrange
        val stepConfig = setupStepConfig()
        whenever(mockState.epcRetrievedByCertificateNumberUpdatedSinceUserReview).thenReturn(true)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState).clearStepData(routeSegment)
        verify(mockState).epcRetrievedByCertificateNumberUpdatedSinceUserReview = false
        verify(mockState).markAnswersChanged()
    }

    @Test
    fun `afterStepIsReached does not clear step data when epcRetrievedByCertificateNumberUpdatedSinceUserReview is false`() {
        // Arrange
        val stepConfig = setupStepConfig()
        whenever(mockState.epcRetrievedByCertificateNumberUpdatedSinceUserReview).thenReturn(false)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState, never()).clearStepData(routeSegment)
        verify(mockState, never()).markAnswersChanged()
    }

    @Test
    fun `afterStepIsReached does not clear step data when epcRetrievedByCertificateNumberUpdatedSinceUserReview is null`() {
        // Arrange
        val stepConfig = setupStepConfig()
        whenever(mockState.epcRetrievedByCertificateNumberUpdatedSinceUserReview).thenReturn(null)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState, never()).clearStepData(routeSegment)
    }
}

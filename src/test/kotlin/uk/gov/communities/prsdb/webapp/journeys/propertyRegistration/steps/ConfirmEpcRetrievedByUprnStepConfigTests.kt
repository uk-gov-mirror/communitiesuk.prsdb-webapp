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
class ConfirmEpcRetrievedByUprnStepConfigTests {
    @Mock
    lateinit var mockEpcCertificateUrlProvider: EpcCertificateUrlProvider

    @Mock
    lateinit var mockState: EpcDetailState

    private val routeSegment = ConfirmEpcRetrievedByUprnStep.ROUTE_SEGMENT

    private fun setupStepConfig(): ConfirmEpcRetrievedByUprnStepConfig {
        val stepConfig = ConfirmEpcRetrievedByUprnStepConfig(mockEpcCertificateUrlProvider)
        stepConfig.urlPath = routeSegment
        stepConfig.validator = AlwaysTrueValidator()
        return stepConfig
    }

    @Test
    fun `afterStepIsReached clears step data and resets flag when epcRetrievedByUprnUpdatedSinceUserReview is true`() {
        // Arrange
        val stepConfig = setupStepConfig()
        whenever(mockState.epcRetrievedByUprnUpdatedSinceUserReview).thenReturn(true)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState).clearStepData(routeSegment)
        verify(mockState).epcRetrievedByUprnUpdatedSinceUserReview = false
        verify(mockState).markAnswersChanged()
    }

    @Test
    fun `afterStepIsReached does not clear step data when epcRetrievedByUprnUpdatedSinceUserReview is false`() {
        // Arrange
        val stepConfig = setupStepConfig()
        whenever(mockState.epcRetrievedByUprnUpdatedSinceUserReview).thenReturn(false)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState, never()).clearStepData(routeSegment)
        verify(mockState, never()).markAnswersChanged()
    }

    @Test
    fun `afterStepIsReached does not clear step data when epcRetrievedByUprnUpdatedSinceUserReview is null`() {
        // Arrange
        val stepConfig = setupStepConfig()
        whenever(mockState.epcRetrievedByUprnUpdatedSinceUserReview).thenReturn(null)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState, never()).clearStepData(routeSegment)
    }
}

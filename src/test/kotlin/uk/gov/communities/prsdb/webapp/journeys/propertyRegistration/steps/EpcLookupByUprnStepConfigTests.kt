package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.EpcDetailState
import uk.gov.communities.prsdb.webapp.models.dataModels.EpcDataModel
import uk.gov.communities.prsdb.webapp.services.EpcLookupService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockEpcData

@ExtendWith(MockitoExtension::class)
class EpcLookupByUprnStepConfigTests {
    @Mock
    lateinit var mockEpcLookupService: EpcLookupService

    @Mock
    lateinit var mockState: EpcDetailState

    val epcDataModel: EpcDataModel = MockEpcData.createEpcDataModel()

    @Test
    fun `mode returns EPC_FOUND when epcRetrievedByUprn is not null`() {
        // Arrange
        val stepConfig = EpcLookupByUprnStepConfig(mockEpcLookupService)
        whenever(mockState.epcRetrievedByUprn).thenReturn(epcDataModel)

        // Act
        val result = stepConfig.mode(mockState)

        // Assert
        assertEquals(EpcLookupByUprnMode.EPC_FOUND, result)
    }

    @Test
    fun `mode returns NOT_FOUND when epcRetrievedByUprn is null`() {
        // Arrange
        val stepConfig = EpcLookupByUprnStepConfig(mockEpcLookupService)
        whenever(mockState.epcRetrievedByUprn).thenReturn(null)

        // Act
        val result = stepConfig.mode(mockState)

        // Assert
        assertEquals(EpcLookupByUprnMode.NOT_FOUND, result)
    }

    @Test
    fun `afterStepIsReached calls epcLookupService when uprn is not null`() {
        // Arrange
        val stepConfig = EpcLookupByUprnStepConfig(mockEpcLookupService)
        val uprn = 123456789L
        whenever(mockState.uprn).thenReturn(uprn)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockEpcLookupService).getEpcByUprn(uprn)
    }

    @Test
    fun `afterStepIsReached sets epcRetrievedByUprn when epcLookupService returns an epc`() {
        // Arrange
        val stepConfig = EpcLookupByUprnStepConfig(mockEpcLookupService)
        val uprn = 123456789L
        whenever(mockState.uprn).thenReturn(uprn)
        whenever(mockEpcLookupService.getEpcByUprn(uprn)).thenReturn(epcDataModel)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState).epcRetrievedByUprn = epcDataModel
    }

    @Test
    fun `afterStepIsReached sets epcRetrievedByUprn to null when service returns null`() {
        // Arrange
        val stepConfig = EpcLookupByUprnStepConfig(mockEpcLookupService)
        val uprn = 123456789L
        whenever(mockState.uprn).thenReturn(uprn)
        whenever(mockEpcLookupService.getEpcByUprn(uprn)).thenReturn(null)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        assertNull(mockState.epcRetrievedByUprn)
    }

    @Test
    fun `afterStepIsReached does not call epcLookupService but sets epcRetrievedByUprn to null when uprn is null`() {
        // Arrange
        val stepConfig = EpcLookupByUprnStepConfig(mockEpcLookupService)
        whenever(mockState.uprn).thenReturn(null)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        assertNull(mockState.epcRetrievedByUprn)
        verify(mockEpcLookupService, never()).getEpcByUprn(any())
    }

    @Test
    fun `afterStepIsReached sets epcRetrievedByUprnUpdatedSinceUserReview to true when epcRetrievedByUprn changes`() {
        // Arrange
        val stepConfig = EpcLookupByUprnStepConfig(mockEpcLookupService)
        val uprn = 123456789L
        whenever(mockState.uprn).thenReturn(uprn)
        whenever(mockState.epcRetrievedByUprn).thenReturn(null)
        whenever(mockEpcLookupService.getEpcByUprn(uprn)).thenReturn(epcDataModel)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState).epcRetrievedByUprnUpdatedSinceUserReview = true
        verify(mockState).markAnswersChanged()
    }

    @Test
    fun `afterStepIsReached does not set epcRetrievedByUprnUpdatedSinceUserReview when epcRetrievedByUprn does not change`() {
        // Arrange
        val stepConfig = EpcLookupByUprnStepConfig(mockEpcLookupService)
        val uprn = 123456789L
        whenever(mockState.uprn).thenReturn(uprn)
        whenever(mockState.epcRetrievedByUprn).thenReturn(epcDataModel)
        whenever(mockEpcLookupService.getEpcByUprn(uprn)).thenReturn(epcDataModel)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState, never()).epcRetrievedByUprnUpdatedSinceUserReview = true
        verify(mockState, never()).markAnswersChanged()
    }
}

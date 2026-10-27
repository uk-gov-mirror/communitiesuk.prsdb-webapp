package uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.exceptions.CyaDataHasChangedException
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import java.time.Instant

class AbstractCheckYourAnswersStepConfigTests {
    private val mockState = mock<CheckYourAnswersJourneyState>()
    private val stepConfig =
        object : AbstractCheckYourAnswersStepConfig<CheckYourAnswersJourneyState>() {
            override fun getStepSpecificContent(state: CheckYourAnswersJourneyState) = emptyMap<String, Any?>()
        }

    @Nested
    inner class ResolvePageContent {
        @Test
        fun `puts answersLastChanged into submittedFilteredJourneyData`() {
            // Arrange
            val changed = Instant.parse("2026-01-01T10:15:30.123456789Z")
            whenever(mockState.answersLastChanged).thenReturn(changed)

            // Act
            val content = stepConfig.resolvePageContent(mockState, emptyMap())

            // Assert
            assertEquals(changed.toString(), content["submittedFilteredJourneyData"])
        }

        @Test
        fun `puts an empty string into submittedFilteredJourneyData when answersLastChanged is null`() {
            // Arrange
            whenever(mockState.answersLastChanged).thenReturn(null)

            // Act
            val content = stepConfig.resolvePageContent(mockState, emptyMap())

            // Assert
            assertEquals("", content["submittedFilteredJourneyData"])
        }
    }

    @Nested
    inner class EnrichSubmittedDataBeforeValidation {
        @Test
        fun `throws CyaDataHasChangedException when answers have changed since the page was loaded`() {
            // Arrange
            whenever(mockState.answersLastChanged).thenReturn(Instant.parse("2026-01-01T10:15:31Z"))
            val formData = mapOf("submittedFilteredJourneyData" to "2026-01-01T10:15:30Z")

            // Act & Assert
            assertThrows<CyaDataHasChangedException> {
                stepConfig.enrichSubmittedDataBeforeValidation(mockState, formData)
            }
        }

        @Test
        fun `throws CyaDataHasChangedException when the page was loaded with no change time but answers have since changed`() {
            // Arrange
            whenever(mockState.answersLastChanged).thenReturn(Instant.parse("2026-01-01T10:15:31Z"))
            val formData = mapOf("submittedFilteredJourneyData" to "")

            // Act & Assert
            assertThrows<CyaDataHasChangedException> {
                stepConfig.enrichSubmittedDataBeforeValidation(mockState, formData)
            }
        }

        @Test
        fun `throws CyaDataHasChangedException when the page was loaded with a change time but the state has none`() {
            // Arrange
            whenever(mockState.answersLastChanged).thenReturn(null)
            val formData = mapOf("submittedFilteredJourneyData" to "2026-01-01T10:15:30Z")

            // Act & Assert
            assertThrows<CyaDataHasChangedException> {
                stepConfig.enrichSubmittedDataBeforeValidation(mockState, formData)
            }
        }

        @Test
        fun `does not throw when answers have not changed since the page was loaded`() {
            // Arrange
            val changed = Instant.parse("2026-01-01T10:15:30.123456789Z")
            whenever(mockState.answersLastChanged).thenReturn(changed)
            val formData = mapOf("submittedFilteredJourneyData" to changed.toString())

            // Act & Assert
            assertDoesNotThrow {
                stepConfig.enrichSubmittedDataBeforeValidation(mockState, formData)
            }
        }

        @Test
        fun `does not throw when neither the page nor the state has a change time`() {
            // Arrange
            whenever(mockState.answersLastChanged).thenReturn(null)
            val formData = mapOf("submittedFilteredJourneyData" to "")

            // Act & Assert
            assertDoesNotThrow {
                stepConfig.enrichSubmittedDataBeforeValidation(mockState, formData)
            }
        }

        @Test
        fun `does not throw when the submitted data has no submittedFilteredJourneyData field`() {
            // Arrange
            whenever(mockState.answersLastChanged).thenReturn(Instant.parse("2026-01-01T10:15:31Z"))

            // Act & Assert
            assertDoesNotThrow {
                stepConfig.enrichSubmittedDataBeforeValidation(mockState, emptyMap())
            }
        }
    }

    @Nested
    inner class BeforeStepDataIsAdded {
        @Test
        fun `sets answersConfirmedAt to answersLastChanged`() {
            // Arrange
            val changed = Instant.parse("2026-01-01T10:15:30.123456789Z")
            whenever(mockState.answersLastChanged).thenReturn(changed)

            // Act
            stepConfig.beforeStepDataIsAdded(mockState, emptyMap())

            // Assert
            verify(mockState).answersConfirmedAt = changed
        }
    }

    @Test
    fun `does not mark answers as changed when submitted`() {
        assertFalse(stepConfig.changesAnswers)
    }
}

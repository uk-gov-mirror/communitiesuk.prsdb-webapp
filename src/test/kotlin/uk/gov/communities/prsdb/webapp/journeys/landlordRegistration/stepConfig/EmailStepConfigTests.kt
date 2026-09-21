package uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.journeys.JourneyState

@ExtendWith(MockitoExtension::class)
class EmailStepConfigTests {
    @Mock
    lateinit var mockState: JourneyState

    @Mock
    lateinit var mockFeatureFlagManager: FeatureFlagManager

    @Test
    fun `chooseTemplate returns the default template by default`() {
        val stepConfig = EmailStepConfig(mockFeatureFlagManager)

        assertEquals(EmailStep.DEFAULT_TEMPLATE, stepConfig.chooseTemplate(mockState))
    }

    @Test
    fun `withCorrespondenceTemplateIfFlagIsSet switches chooseTemplate to the correspondence template when the flag is enabled`() {
        whenever(mockFeatureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS)).thenReturn(true)
        val stepConfig = EmailStepConfig(mockFeatureFlagManager)

        stepConfig.withCorrespondenceTemplateIfFlagIsSet()

        assertEquals(EmailStep.CORRESPONDENCE_TEMPLATE, stepConfig.chooseTemplate(mockState))
    }

    @Test
    fun `withCorrespondenceTemplateIfFlagIsSet leaves chooseTemplate on the default template when the flag is disabled`() {
        whenever(mockFeatureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS)).thenReturn(false)
        val stepConfig = EmailStepConfig(mockFeatureFlagManager)

        stepConfig.withCorrespondenceTemplateIfFlagIsSet()

        assertEquals(EmailStep.DEFAULT_TEMPLATE, stepConfig.chooseTemplate(mockState))
    }
}

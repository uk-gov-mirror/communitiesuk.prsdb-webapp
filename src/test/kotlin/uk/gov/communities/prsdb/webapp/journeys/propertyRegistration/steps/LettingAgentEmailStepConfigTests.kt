package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.WhoProvidesDetailsState
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.AlwaysTrueValidator

@ExtendWith(MockitoExtension::class)
class LettingAgentEmailStepConfigTests {
    @Test
    fun `enrichSubmittedDataBeforeValidation injects the landlord email snapshot taken at the start of the journey`() {
        val mockJourneyState = mock<WhoProvidesDetailsState>()
        val stepConfig = LettingAgentEmailStepConfig()
        stepConfig.urlPath = LettingAgentEmailStep.ROUTE_SEGMENT
        stepConfig.validator = AlwaysTrueValidator()

        whenever(mockJourneyState.loggedInLandlordEmail).thenReturn("original.landlord@example.com")

        val result = stepConfig.enrichSubmittedDataBeforeValidation(mockJourneyState, emptyMap())

        assertEquals("original.landlord@example.com", result["landlordEmailAtStartOfJourney"])
    }

    @Test
    fun `chooseTemplate returns the letting agent email form template`() {
        val mockJourneyState = mock<WhoProvidesDetailsState>()
        val stepConfig = LettingAgentEmailStepConfig()

        assertEquals("forms/lettingAgentEmailForm", stepConfig.chooseTemplate(mockJourneyState))
    }
}

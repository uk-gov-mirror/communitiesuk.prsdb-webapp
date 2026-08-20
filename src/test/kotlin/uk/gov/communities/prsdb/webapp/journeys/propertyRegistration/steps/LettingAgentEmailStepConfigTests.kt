package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.database.entity.Landlord
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.WhoProvidesDetailsState
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.AlwaysTrueValidator

@ExtendWith(MockitoExtension::class)
class LettingAgentEmailStepConfigTests {
    @Mock
    lateinit var mockUserToLandlordService: UserToLandlordService

    @Mock
    lateinit var mockJourneyState: WhoProvidesDetailsState

    @Mock
    lateinit var mockLandlord: Landlord

    @Test
    fun `enrichSubmittedDataBeforeValidation injects the landlord email`() {
        val stepConfig = LettingAgentEmailStepConfig(mockUserToLandlordService)
        stepConfig.urlPath = LettingAgentEmailStep.ROUTE_SEGMENT
        stepConfig.validator = AlwaysTrueValidator()

        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(mockLandlord)
        whenever(mockLandlord.email).thenReturn("landlord@example.com")

        val result = stepConfig.enrichSubmittedDataBeforeValidation(mockJourneyState, emptyMap())

        assertEquals("landlord@example.com", result["landlordEmail"])
    }

    @Test
    fun `chooseTemplate returns the letting agent email form template`() {
        val stepConfig = LettingAgentEmailStepConfig(mockUserToLandlordService)

        assertEquals("forms/lettingAgentEmailForm", stepConfig.chooseTemplate(mockJourneyState))
    }
}

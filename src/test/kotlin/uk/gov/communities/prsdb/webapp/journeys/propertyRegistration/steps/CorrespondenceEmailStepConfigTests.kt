package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.CorrespondenceState
import uk.gov.communities.prsdb.webapp.models.viewModels.formModels.RadiosButtonViewModel
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CorrespondenceEmailStepConfigTests {
    @Test
    fun `getStepSpecificContent uses the landlord email that was snapshotted at the start of the journey for the account email option`() {
        val mockJourneyState = mock<CorrespondenceState>()
        whenever(mockJourneyState.loggedInLandlordEmail).thenReturn("original.landlord@example.com")
        val stepConfig = CorrespondenceEmailStepConfig()

        val content = stepConfig.getStepSpecificContent(mockJourneyState)

        @Suppress("UNCHECKED_CAST")
        val radioOptions = content["radioOptions"] as List<RadiosButtonViewModel<CorrespondenceEmailOption>>

        assertEquals(
            "original.landlord@example.com",
            radioOptions.single { it.value == CorrespondenceEmailOption.ACCOUNT_EMAIL }.hintValue,
        )
    }
}

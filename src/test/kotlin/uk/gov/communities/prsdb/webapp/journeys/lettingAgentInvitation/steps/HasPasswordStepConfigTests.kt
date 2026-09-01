package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.LettingAgentInvitationJourneyState
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService
import uk.gov.communities.prsdb.webapp.services.LettingAgentPasswordService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class HasPasswordStepConfigTests {
    @Mock
    lateinit var mockLettingAgentAccessService: LettingAgentAccessService

    @Mock
    lateinit var mockLettingAgentPasswordService: LettingAgentPasswordService

    @Mock
    lateinit var mockState: LettingAgentInvitationJourneyState

    @Test
    fun `mode returns HAS_PASSWORD when a password has been set for the invitation`() {
        val token = UUID.randomUUID()
        val invitation = MockLettingAgentData.createLettingAgentAccess(token = token)
        whenever(mockState.invitationToken).thenReturn(token.toString())
        whenever(mockLettingAgentAccessService.getInvitationByToken(token)).thenReturn(invitation)
        whenever(mockLettingAgentPasswordService.hasPasswordBeenSet(invitation)).thenReturn(true)

        val result = setupStepConfig().mode(mockState)

        assertEquals(PasswordStatus.HAS_PASSWORD, result)
    }

    @Test
    fun `mode returns NO_PASSWORD when no password has been set for the invitation`() {
        val token = UUID.randomUUID()
        val invitation = MockLettingAgentData.createLettingAgentAccessWithoutPassword(token = token)
        whenever(mockState.invitationToken).thenReturn(token.toString())
        whenever(mockLettingAgentAccessService.getInvitationByToken(token)).thenReturn(invitation)
        whenever(mockLettingAgentPasswordService.hasPasswordBeenSet(invitation)).thenReturn(false)

        val result = setupStepConfig().mode(mockState)

        assertEquals(PasswordStatus.NO_PASSWORD, result)
    }

    private fun setupStepConfig() = HasPasswordStepConfig(mockLettingAgentAccessService, mockLettingAgentPasswordService)
}

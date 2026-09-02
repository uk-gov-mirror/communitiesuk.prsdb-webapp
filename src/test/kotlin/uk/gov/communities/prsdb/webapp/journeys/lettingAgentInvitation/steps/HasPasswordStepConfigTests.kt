package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
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
    fun `mode returns HAS_PASSWORD when hasPassword is cached as true on the state`() {
        whenever(mockState.hasPassword).thenReturn(true)

        assertEquals(PasswordStatus.HAS_PASSWORD, setupStepConfig().mode(mockState))
    }

    @Test
    fun `mode returns NO_PASSWORD when hasPassword is cached as false on the state`() {
        whenever(mockState.hasPassword).thenReturn(false)

        assertEquals(PasswordStatus.NO_PASSWORD, setupStepConfig().mode(mockState))
    }

    @Test
    fun `mode throws when hasPassword has not been set on the state`() {
        whenever(mockState.hasPassword).thenReturn(null)

        assertThrows<PrsdbWebException> { setupStepConfig().mode(mockState) }
    }

    @Test
    fun `afterStepIsReached caches true when the invitation has a password set`() {
        val token = UUID.randomUUID()
        val invitation = MockLettingAgentData.createLettingAgentAccess(token = token)
        whenever(mockState.invitationToken).thenReturn(token.toString())
        whenever(mockLettingAgentAccessService.getInvitationByToken(token)).thenReturn(invitation)
        whenever(mockLettingAgentPasswordService.hasPasswordBeenSet(invitation)).thenReturn(true)

        setupStepConfig().afterStepIsReached(mockState)

        verify(mockState).hasPassword = true
    }

    @Test
    fun `afterStepIsReached caches false when the invitation has no password set`() {
        val token = UUID.randomUUID()
        val invitation = MockLettingAgentData.createLettingAgentAccessWithoutPassword(token = token)
        whenever(mockState.invitationToken).thenReturn(token.toString())
        whenever(mockLettingAgentAccessService.getInvitationByToken(token)).thenReturn(invitation)
        whenever(mockLettingAgentPasswordService.hasPasswordBeenSet(invitation)).thenReturn(false)

        setupStepConfig().afterStepIsReached(mockState)

        verify(mockState).hasPassword = false
    }

    @Test
    fun `afterStepIsReached throws when the invitation token is missing from the state`() {
        whenever(mockState.invitationToken).thenReturn(null)

        assertThrows<IllegalArgumentException> { setupStepConfig().afterStepIsReached(mockState) }
    }

    private fun setupStepConfig() = HasPasswordStepConfig(mockLettingAgentAccessService, mockLettingAgentPasswordService)
}

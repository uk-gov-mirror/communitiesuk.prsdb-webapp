package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.springframework.validation.BeanPropertyBindingResult
import uk.gov.communities.prsdb.webapp.database.entity.LettingAgentAccess
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.LettingAgentInvitationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.EnterPasswordFormModel
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService
import uk.gov.communities.prsdb.webapp.services.LettingAgentPasswordService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class EnterPasswordStepConfigTests {
    @Mock
    private lateinit var lettingAgentAccessService: LettingAgentAccessService

    @Mock
    private lateinit var lettingAgentPasswordService: LettingAgentPasswordService

    @Mock
    private lateinit var state: LettingAgentInvitationJourneyState

    private val token = UUID.randomUUID()

    @Test
    fun `formModelClass is EnterPasswordFormModel`() {
        assertEquals(EnterPasswordFormModel::class, createStepConfig().formModelClass)
    }

    @Test
    fun `chooseTemplate returns the enter password form template`() {
        assertEquals("forms/enterPasswordForm", createStepConfig().chooseTemplate(state))
    }

    @Test
    fun `getStepSpecificContent includes the property address`() {
        val stepConfig = createStepConfig()
        val invitation = MockLettingAgentData.createLettingAgentAccess(token = token)
        setupInvitation(invitation)

        val content = stepConfig.getStepSpecificContent(state)

        assertEquals(
            invitation.propertyOwnership.address
                .toMultiLineAddress()
                .split("\n"),
            content["addressLines"],
        )
    }

    @Test
    fun `afterStepIsReached resolves invitation from journey token`() {
        val stepConfig = createStepConfig()
        setupInvitation(MockLettingAgentData.createLettingAgentAccess(token = token))

        stepConfig.afterStepIsReached(state)

        verify(lettingAgentAccessService).getInvitationByToken(token)
    }

    @Test
    fun `afterStepIsReached throws when no password has been set`() {
        val stepConfig = createStepConfig()
        setupInvitation(MockLettingAgentData.createLettingAgentAccessWithoutPassword(token = token))

        assertThrows(PrsdbWebException::class.java) { stepConfig.afterStepIsReached(state) }
    }

    @Test
    fun `afterPrimaryValidation accepts the correct password`() {
        val stepConfig = createStepConfig()
        val invitation = MockLettingAgentData.createLettingAgentAccess(token = token)
        setupInvitation(invitation)
        whenever(lettingAgentPasswordService.isPasswordCorrect(invitation, "password1")).thenReturn(true)
        val bindingResult = bindingResultFor("password1")

        stepConfig.afterPrimaryValidation(state, bindingResult)

        assertFalse(bindingResult.hasErrors())
    }

    @Test
    fun `afterPrimaryValidation rejects an incorrect password`() {
        val stepConfig = createStepConfig()
        val invitation = MockLettingAgentData.createLettingAgentAccess(token = token)
        setupInvitation(invitation)
        whenever(lettingAgentPasswordService.isPasswordCorrect(invitation, "wrong")).thenReturn(false)
        val bindingResult = bindingResultFor("wrong")

        stepConfig.afterPrimaryValidation(state, bindingResult)

        assertEquals(
            "lettingAgentInvitation.enterPassword.password.error.incorrect",
            bindingResult.getFieldError(EnterPasswordFormModel::password.name)?.defaultMessage,
        )
    }

    @Test
    fun `afterPrimaryValidation does not check the password when the field is already in error`() {
        val stepConfig = createStepConfig()
        val bindingResult = bindingResultFor("")
        bindingResult.rejectValue(
            EnterPasswordFormModel::password.name,
            "RejectValueWithMessageKey",
            "lettingAgentInvitation.enterPassword.password.error.missing",
        )

        stepConfig.afterPrimaryValidation(state, bindingResult)

        verify(lettingAgentPasswordService, never()).isPasswordCorrect(any(), any())
        assertEquals(1, bindingResult.getFieldErrors(EnterPasswordFormModel::password.name).size)
    }

    @Test
    fun `enrichStepDataBeforeItIsAdded returns empty map so raw password is not stored in session`() {
        val stepConfig = createStepConfig()

        val dataToStore =
            stepConfig.enrichStepDataBeforeItIsAdded(
                state,
                mapOf(EnterPasswordFormModel::password.name to "password1"),
            )

        assertEquals(emptyMap<String, Any?>(), dataToStore)
    }

    @Test
    fun `afterStepDataIsAdded sets hasEnteredPassword on state`() {
        createStepConfig().afterStepDataIsAdded(state)

        verify(state).hasEnteredPassword = true
    }

    @Test
    fun `mode is null before hasEnteredPassword is set`() {
        val stepConfig = createStepConfig()
        whenever(state.hasEnteredPassword).thenReturn(null)

        assertNull(stepConfig.mode(state))
    }

    @Test
    fun `mode is complete after hasEnteredPassword is set`() {
        val stepConfig = createStepConfig()
        whenever(state.hasEnteredPassword).thenReturn(true)

        assertEquals(Complete.COMPLETE, stepConfig.mode(state))
    }

    private fun createStepConfig() =
        EnterPasswordStepConfig(lettingAgentAccessService, lettingAgentPasswordService).also {
            it.urlPath = EnterPasswordStep.ROUTE_SEGMENT
        }

    private fun setupInvitation(invitation: LettingAgentAccess) {
        whenever(state.invitationToken).thenReturn(token.toString())
        whenever(lettingAgentAccessService.getInvitationByToken(token)).thenReturn(invitation)
    }

    private fun bindingResultFor(password: String): BeanPropertyBindingResult {
        val formModel = EnterPasswordFormModel().apply { this.password = password }
        return BeanPropertyBindingResult(formModel, "formModel")
    }
}

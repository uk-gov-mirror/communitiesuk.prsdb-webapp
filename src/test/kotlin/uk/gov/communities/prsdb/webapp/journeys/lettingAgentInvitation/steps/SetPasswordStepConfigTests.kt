package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.springframework.validation.BindingResult
import uk.gov.communities.prsdb.webapp.database.entity.LettingAgentAccess
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.LettingAgentInvitationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.SetPasswordFormModel
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService
import uk.gov.communities.prsdb.webapp.services.LettingAgentPasswordService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SetPasswordStepConfigTests {
    @Mock
    private lateinit var lettingAgentAccessService: LettingAgentAccessService

    @Mock
    private lateinit var lettingAgentPasswordService: LettingAgentPasswordService

    @Mock
    private lateinit var state: LettingAgentInvitationJourneyState

    private val token = UUID.randomUUID()

    @Test
    fun `formModelClass is SetPasswordFormModel`() {
        val stepConfig = createStepConfig()

        assertEquals(SetPasswordFormModel::class, stepConfig.formModelClass)
    }

    @Test
    fun `chooseTemplate returns set password form template`() {
        val stepConfig = createStepConfig()

        assertEquals("forms/setPasswordForm", stepConfig.chooseTemplate(state))
    }

    @Test
    fun `getStepSpecificContent returns address lines`() {
        val stepConfig = createStepConfig()
        val invitation = MockLettingAgentData.createLettingAgentAccessWithoutPassword(token = token)
        setupInvitation(invitation)
        val expectedAddressLines = invitation.propertyOwnership.address.toMultiLineAddress().split("\n")

        val content = stepConfig.getStepSpecificContent(state)

        assertEquals(mapOf("addressLines" to expectedAddressLines), content)
    }

    @Test
    fun `afterPrimaryValidation rejects when passwords do not match`() {
        val stepConfig = createStepConfig()
        val formModel =
            SetPasswordFormModel().apply {
                password = "password1"
                confirmPassword = "different1"
            }
        val bindingResult: BindingResult = mock()
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(bindingResult.target).thenReturn(formModel)

        stepConfig.afterPrimaryValidation(state, bindingResult)

        verify(
            bindingResult,
        ).rejectValue("password", "RejectValueWithMessageKey", "lettingAgentInvitation.setPassword.password.error.mismatch")
        verify(bindingResult).rejectValue("confirmPassword", "RejectValueWithMessageKey", "_")
    }

    @Test
    fun `afterPrimaryValidation does not reject when passwords match`() {
        val stepConfig = createStepConfig()
        val formModel =
            SetPasswordFormModel().apply {
                password = "password1"
                confirmPassword = "password1"
            }
        val bindingResult: BindingResult = mock()
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(bindingResult.target).thenReturn(formModel)

        stepConfig.afterPrimaryValidation(state, bindingResult)

        verify(bindingResult, never()).rejectValue(
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `afterPrimaryValidation skips mismatch check when primary validation has errors`() {
        val stepConfig = createStepConfig()
        val bindingResult: BindingResult = mock()
        whenever(bindingResult.hasErrors()).thenReturn(true)

        stepConfig.afterPrimaryValidation(state, bindingResult)

        verify(bindingResult, never()).rejectValue(
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `afterStepIsReached resolves invitation from journey token`() {
        val stepConfig = createStepConfig()
        val invitation = MockLettingAgentData.createLettingAgentAccessWithoutPassword(token = token)
        setupInvitation(invitation)

        stepConfig.afterStepIsReached(state)

        verify(lettingAgentAccessService).getInvitationByToken(token)
    }

    @Test
    fun `afterStepIsReached throws when password is already set`() {
        val stepConfig = createStepConfig()
        val invitation = MockLettingAgentData.createLettingAgentAccess(token = token)
        setupInvitation(invitation)

        assertThrows(PrsdbWebException::class.java) {
            stepConfig.afterStepIsReached(state)
        }
    }

    @Test
    fun `beforeStepDataIsAdded persists password via service`() {
        val stepConfig = createStepConfig()
        val invitation = MockLettingAgentData.createLettingAgentAccessWithoutPassword(token = token)
        setupInvitation(invitation)
        val rawPassword = "password1"

        stepConfig.beforeStepDataIsAdded(
            state,
            mapOf(SetPasswordFormModel::password.name to rawPassword),
        )

        verify(lettingAgentPasswordService).setPassword(invitation.id, rawPassword)
    }

    @Test
    fun `enrichStepDataBeforeItIsAdded returns empty map so raw password is not stored in session`() {
        val stepConfig = createStepConfig()

        val dataToStore =
            stepConfig.enrichStepDataBeforeItIsAdded(
                state,
                mapOf(SetPasswordFormModel::password.name to "password1"),
            )

        assertEquals(emptyMap<String, Any?>(), dataToStore)
    }

    @Test
    fun `afterStepDataIsAdded sets hasSetPassword on state`() {
        val stepConfig = createStepConfig()

        stepConfig.afterStepDataIsAdded(state)

        verify(state).hasSetPassword = true
    }

    @Test
    fun `mode is null before hasSetPassword is set`() {
        val stepConfig = createStepConfig()
        whenever(state.hasSetPassword).thenReturn(null)

        assertNull(stepConfig.mode(state))
    }

    @Test
    fun `mode is complete after hasSetPassword is set`() {
        val stepConfig = createStepConfig()
        whenever(state.hasSetPassword).thenReturn(true)

        assertEquals(Complete.COMPLETE, stepConfig.mode(state))
    }

    private fun createStepConfig() =
        SetPasswordStepConfig(lettingAgentAccessService, lettingAgentPasswordService).also {
            it.urlPath = SetPasswordStep.ROUTE_SEGMENT
        }

    private fun setupInvitation(invitation: LettingAgentAccess) {
        whenever(state.invitationToken).thenReturn(token.toString())
        whenever(lettingAgentAccessService.getInvitationByToken(token)).thenReturn(invitation)
    }
}

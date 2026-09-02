package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import org.springframework.validation.BindingResult
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.FormData
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.LettingAgentInvitationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.EnterPasswordFormModel
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService
import uk.gov.communities.prsdb.webapp.services.LettingAgentPasswordService
import java.util.UUID

@JourneyFrameworkComponent
class EnterPasswordStepConfig(
    private val lettingAgentAccessService: LettingAgentAccessService,
    private val lettingAgentPasswordService: LettingAgentPasswordService,
) : AbstractRequestableStepConfig<Complete, EnterPasswordFormModel, LettingAgentInvitationJourneyState>() {
    override val formModelClass = EnterPasswordFormModel::class

    override fun getStepSpecificContent(state: LettingAgentInvitationJourneyState): Map<String, Any?> =
        mapOf(
            "addressLines" to
                getInvitation(state)
                    .propertyOwnership
                    .address
                    .toMultiLineAddress()
                    .split("\n"),
        )

    override fun chooseTemplate(state: LettingAgentInvitationJourneyState): String = "forms/enterPasswordForm"

    override fun mode(state: LettingAgentInvitationJourneyState): Complete? =
        if (state.hasEnteredPassword == true) Complete.COMPLETE else null

    override fun afterStepIsReached(state: LettingAgentInvitationJourneyState) {
        val invitation = getInvitation(state)
        if (invitation.encodedPassword == null) {
            throw PrsdbWebException("No password has been set for letting agent access ${invitation.id}")
        }
    }

    override fun afterPrimaryValidation(
        state: LettingAgentInvitationJourneyState,
        bindingResult: BindingResult,
    ) {
        val fieldName = EnterPasswordFormModel::password.name
        if (bindingResult.hasFieldErrors(fieldName)) return

        val enteredPassword = bindingResult.getFormModel().password
        if (!lettingAgentPasswordService.isPasswordCorrect(getInvitation(state), enteredPassword)) {
            bindingResult.rejectValueWithMessageKey(
                fieldName,
                "lettingAgentInvitation.enterPassword.password.error.incorrect",
            )
        }
    }

    // Make sure that the raw password in the form model is not saved to the state
    override fun enrichStepDataBeforeItIsAdded(
        state: LettingAgentInvitationJourneyState,
        data: FormData,
    ): FormData = emptyMap()

    override fun afterStepDataIsAdded(state: LettingAgentInvitationJourneyState) {
        state.hasEnteredPassword = true
    }

    private fun getInvitation(state: LettingAgentInvitationJourneyState) =
        lettingAgentAccessService.getInvitationByToken(
            state.invitationToken
                ?.let(UUID::fromString)
                ?: throw PrsdbWebException("Invitation token was not found in journey state"),
        )
}

@JourneyFrameworkComponent
final class EnterPasswordStep(
    stepConfig: EnterPasswordStepConfig,
) : RequestableStep<Complete, EnterPasswordFormModel, LettingAgentInvitationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "enter-password"
    }
}

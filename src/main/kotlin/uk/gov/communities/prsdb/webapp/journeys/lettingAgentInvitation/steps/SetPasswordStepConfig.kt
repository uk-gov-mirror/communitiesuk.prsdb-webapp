package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import org.springframework.validation.BindingResult
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.FormData
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.LettingAgentInvitationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.SetPasswordFormModel
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService
import uk.gov.communities.prsdb.webapp.services.LettingAgentPasswordService
import java.util.UUID

@JourneyFrameworkComponent
class SetPasswordStepConfig(
    private val lettingAgentAccessService: LettingAgentAccessService,
    private val lettingAgentPasswordService: LettingAgentPasswordService,
) : AbstractRequestableStepConfig<Complete, SetPasswordFormModel, LettingAgentInvitationJourneyState>() {
    override val formModelClass = SetPasswordFormModel::class

    override fun getStepSpecificContent(state: LettingAgentInvitationJourneyState): Map<String, Any?> {
        val invitation = getInvitation(state)
        val addressLines = invitation.propertyOwnership.address.toMultiLineAddress().split("\n")
        return mapOf("addressLines" to addressLines)
    }

    override fun chooseTemplate(state: LettingAgentInvitationJourneyState): String = "forms/setPasswordForm"

    override fun mode(state: LettingAgentInvitationJourneyState): Complete? = if (state.hasSetPassword == true) Complete.COMPLETE else null

    override fun afterPrimaryValidation(
        state: LettingAgentInvitationJourneyState,
        bindingResult: BindingResult,
    ) {
        if (!bindingResult.hasErrors()) {
            val formModel = bindingResult.getFormModel()
            if (formModel.password != formModel.confirmPassword) {
                bindingResult.rejectValueWithMessageKey(
                    SetPasswordFormModel::password.name,
                    "lettingAgentInvitation.setPassword.password.error.mismatch",
                )
                bindingResult.rejectValueWithMessageKey(
                    SetPasswordFormModel::confirmPassword.name,
                    "_",
                )
            }
        }
    }

    override fun afterStepIsReached(state: LettingAgentInvitationJourneyState) {
        val invitation = getInvitation(state)
        if (invitation.encodedPassword != null) {
            throw PrsdbWebException("Password has already been set for letting agent access ${invitation.id}")
        }
    }

    override fun beforeStepDataIsAdded(
        state: LettingAgentInvitationJourneyState,
        data: FormData,
    ) {
        lettingAgentPasswordService.setPassword(getInvitation(state).id, data.getPassword())
    }

    // Make sure that the raw password in the form model is not saved to the state
    override fun enrichStepDataBeforeItIsAdded(
        state: LettingAgentInvitationJourneyState,
        data: FormData,
    ): FormData = emptyMap()

    override fun afterStepDataIsAdded(state: LettingAgentInvitationJourneyState) {
        state.hasSetPassword = true
    }

    private fun getInvitation(state: LettingAgentInvitationJourneyState) =
        lettingAgentAccessService.getInvitationByToken(
            state.invitationToken
                ?.let(UUID::fromString)
                ?: throw PrsdbWebException("Invitation token was not found in journey state"),
        )

    companion object {
        private fun FormData.getPassword(): String =
            this[SetPasswordFormModel::password.name] as? String
                ?: throw PrsdbWebException("Password was not provided in form data")
    }
}

@JourneyFrameworkComponent
final class SetPasswordStep(
    stepConfig: SetPasswordStepConfig,
) : RequestableStep<Complete, SetPasswordFormModel, LettingAgentInvitationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "set-password"
    }
}

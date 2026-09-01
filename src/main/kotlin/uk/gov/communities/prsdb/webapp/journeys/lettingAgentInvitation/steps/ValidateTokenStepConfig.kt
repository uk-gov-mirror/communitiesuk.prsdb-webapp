package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.LettingAgentInvitationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService

// TODO PDJB-1660: Implement token validation logic
@JourneyFrameworkComponent("lettingAgentInvitationValidateTokenStepConfig")
class ValidateTokenStepConfig(
    private val lettingAgentAccessService: LettingAgentAccessService,
) : AbstractRequestableStepConfig<Complete, NoInputFormModel, LettingAgentInvitationJourneyState>() {
    override val formModelClass = NoInputFormModel::class

    override fun getStepSpecificContent(state: LettingAgentInvitationJourneyState): Map<String, Any?> =
        mapOf("todoComment" to "TODO: PDJB-1660: Validate token step")

    override fun chooseTemplate(state: LettingAgentInvitationJourneyState): String = "forms/todo"

    override fun mode(state: LettingAgentInvitationJourneyState): Complete = Complete.COMPLETE

    override fun afterStepIsReached(state: LettingAgentInvitationJourneyState) {
        val token = lettingAgentAccessService.getInvitationTokenForJourneyIdFromSession(state.journeyId)
        state.invitationToken = token
    }
}

@JourneyFrameworkComponent("lettingAgentInvitationValidateTokenStep")
final class ValidateTokenStep(
    stepConfig: ValidateTokenStepConfig,
) : RequestableStep<Complete, NoInputFormModel, LettingAgentInvitationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "validate-token"
    }
}

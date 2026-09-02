package uk.gov.communities.prsdb.webapp.testHelpers.builders

import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.HasPasswordStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.StartStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.ValidateTokenStep
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.HasPasswordFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel

class LettingAgentInvitationStateSessionBuilder : JourneyStateSessionBuilder<LettingAgentInvitationStateSessionBuilder>() {
    fun withStartCompleted(): LettingAgentInvitationStateSessionBuilder {
        withSubmittedValue(StartStep.ROUTE_SEGMENT, NoInputFormModel())
        return self()
    }

    fun withValidateTokenCompleted(): LettingAgentInvitationStateSessionBuilder {
        withSubmittedValue(ValidateTokenStep.ROUTE_SEGMENT, NoInputFormModel())
        return self()
    }

    fun withHasPasswordCompletedAsNoPassword(): LettingAgentInvitationStateSessionBuilder {
        withSubmittedValue(HasPasswordStep.ROUTE_SEGMENT, HasPasswordFormModel(hasPassword = false))
        return self()
    }

    fun withHasPasswordCompletedAsHasPassword(): LettingAgentInvitationStateSessionBuilder {
        withSubmittedValue(HasPasswordStep.ROUTE_SEGMENT, HasPasswordFormModel(hasPassword = true))
        return self()
    }

    fun withInvitationToken(token: String): LettingAgentInvitationStateSessionBuilder {
        withAdditionalData("invitationToken", "\"$token\"")
        return self()
    }

    companion object {
        fun beforeSetPassword(token: String): LettingAgentInvitationStateSessionBuilder =
            LettingAgentInvitationStateSessionBuilder()
                .withStartCompleted()
                .withValidateTokenCompleted()
                .withHasPasswordCompletedAsNoPassword()
                .withInvitationToken(token)

        fun beforeEnterPassword(token: String): LettingAgentInvitationStateSessionBuilder =
            LettingAgentInvitationStateSessionBuilder()
                .withStartCompleted()
                .withValidateTokenCompleted()
                .withHasPasswordCompletedAsHasPassword()
                .withInvitationToken(token)
    }
}

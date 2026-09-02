package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.journeys.AbstractInternalStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.InternalStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.LettingAgentInvitationJourneyState
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService
import uk.gov.communities.prsdb.webapp.services.LettingAgentPasswordService
import java.util.UUID

enum class PasswordStatus {
    HAS_PASSWORD,
    NO_PASSWORD,
}

@JourneyFrameworkComponent
class HasPasswordStepConfig(
    private val lettingAgentAccessService: LettingAgentAccessService,
    private val lettingAgentPasswordService: LettingAgentPasswordService,
) : AbstractInternalStepConfig<PasswordStatus, LettingAgentInvitationJourneyState>() {
    override fun mode(state: LettingAgentInvitationJourneyState): PasswordStatus =
        when (state.hasPassword) {
            true -> PasswordStatus.HAS_PASSWORD
            false -> PasswordStatus.NO_PASSWORD
            null -> throw PrsdbWebException("hasPassword has not been set on journey state")
        }

    override fun afterStepIsReached(state: LettingAgentInvitationJourneyState) {
        val token = UUID.fromString(requireNotNull(state.invitationToken) { "Invitation token is missing from the journey state" })
        val invitation = lettingAgentAccessService.getInvitationByToken(token)
        state.hasPassword = lettingAgentPasswordService.hasPasswordBeenSet(invitation)
    }
}

@JourneyFrameworkComponent
final class HasPasswordStep(
    stepConfig: HasPasswordStepConfig,
) : InternalStep<PasswordStatus, LettingAgentInvitationJourneyState>(stepConfig)

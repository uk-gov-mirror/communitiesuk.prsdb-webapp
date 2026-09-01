package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
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
    override fun mode(state: LettingAgentInvitationJourneyState): PasswordStatus {
        val token = UUID.fromString(requireNotNull(state.invitationToken) { "Invitation token is missing from the journey state" })
        val invitation = lettingAgentAccessService.getInvitationByToken(token)
        return if (lettingAgentPasswordService.hasPasswordBeenSet(invitation)) {
            PasswordStatus.HAS_PASSWORD
        } else {
            PasswordStatus.NO_PASSWORD
        }
    }
}

@JourneyFrameworkComponent
final class HasPasswordStep(
    stepConfig: HasPasswordStepConfig,
) : InternalStep<PasswordStatus, LettingAgentInvitationJourneyState>(stepConfig)

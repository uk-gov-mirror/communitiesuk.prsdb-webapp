package uk.gov.communities.prsdb.webapp.journeys.delegateToLettingAgent.stepConfig

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.FormData
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.delegateToLettingAgent.DelegateToLettingAgentJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.services.DelegateToLettingAgentEmailService
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService

@JourneyFrameworkComponent("delegateToLettingAgentAllowLettingAgentStepConfig")
class AllowLettingAgentStepConfig(
    private val userToLandlordService: UserToLandlordService,
    private val propertyOwnershipService: PropertyOwnershipService,
    private val lettingAgentAccessService: LettingAgentAccessService,
    private val delegateToLettingAgentEmailService: DelegateToLettingAgentEmailService,
) : AbstractRequestableStepConfig<Complete, AllowLettingAgentEmailFormModel, DelegateToLettingAgentJourneyState>() {
    override val formModelClass = AllowLettingAgentEmailFormModel::class

    override fun getStepSpecificContent(state: DelegateToLettingAgentJourneyState) = emptyMap<String, Any?>()

    override fun chooseTemplate(state: DelegateToLettingAgentJourneyState) = "forms/allowLettingAgentForm"

    override fun mode(state: DelegateToLettingAgentJourneyState): Complete? = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }

    override fun beforeAttemptingToReachStep(state: DelegateToLettingAgentJourneyState): Boolean =
        !propertyOwnershipService.hasLettingAgent(state.propertyOwnershipId)

    override fun enrichSubmittedDataBeforeValidation(
        state: DelegateToLettingAgentJourneyState,
        formData: FormData,
    ): FormData =
        super.enrichSubmittedDataBeforeValidation(state, formData) +
            (AllowLettingAgentEmailFormModel::landlordEmailAtStartOfJourney.name to userToLandlordService.getCurrentLandlordForUser().email)

    override fun afterStepDataIsAdded(state: DelegateToLettingAgentJourneyState) {
        getFormModelFromState(state).emailAddress?.let { invitedEmailAddress ->
            val propertyOwnership = propertyOwnershipService.getPropertyOwnership(state.propertyOwnershipId)
            val invitation = lettingAgentAccessService.createInvitation(propertyOwnership, invitedEmailAddress)
            lettingAgentAccessService.addDelegatedPropertyOwnershipToSession(state.propertyOwnershipId, invitedEmailAddress)
            delegateToLettingAgentEmailService.sendDelegationEmailToLandlords(state.propertyOwnershipId, invitedEmailAddress)
            delegateToLettingAgentEmailService.sendDelegationEmailToLettingAgent(
                propertyOwnership,
                userToLandlordService.getCurrentLandlordForUser().name,
                invitedEmailAddress,
                invitationToken = invitation.token,
            )
        }
    }

    override fun resolveNextDestination(
        state: DelegateToLettingAgentJourneyState,
        defaultDestination: Destination,
    ): Destination {
        state.deleteJourney()
        return defaultDestination
    }
}

@JourneyFrameworkComponent("delegateToLettingAgentAllowLettingAgentStep")
final class AllowLettingAgentStep(
    stepConfig: AllowLettingAgentStepConfig,
) : RequestableStep<Complete, AllowLettingAgentEmailFormModel, DelegateToLettingAgentJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "allow-letting-agent"
    }
}

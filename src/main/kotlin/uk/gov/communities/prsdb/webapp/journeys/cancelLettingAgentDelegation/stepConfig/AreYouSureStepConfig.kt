package uk.gov.communities.prsdb.webapp.journeys.cancelLettingAgentDelegation.stepConfig

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.cancelLettingAgentDelegation.CancelLettingAgentDelegationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.services.CancelLettingAgentDelegationEmailService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@JourneyFrameworkComponent("cancelLettingAgentDelegationAreYouSureStepConfig")
class AreYouSureStepConfig(
    private val propertyOwnershipService: PropertyOwnershipService,
    private val cancelLettingAgentDelegationEmailService: CancelLettingAgentDelegationEmailService,
) : AbstractRequestableStepConfig<Complete, NoInputFormModel, CancelLettingAgentDelegationJourneyState>() {
    override val formModelClass = NoInputFormModel::class

    override fun getStepSpecificContent(state: CancelLettingAgentDelegationJourneyState): Map<String, Any?> =
        mapOf(
            "todoComment" to "TODO PDJB-1413: are you sure you want to remove your letting agent or property manager?",
        )

    override fun chooseTemplate(state: CancelLettingAgentDelegationJourneyState) = "forms/todo"

    override fun mode(state: CancelLettingAgentDelegationJourneyState): Complete? =
        getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }

    override fun afterStepDataIsAdded(state: CancelLettingAgentDelegationJourneyState) {
        // TODO PDJB-1413: remove the letting agent / property manager delegation for this property
        // NB: emails must be sent before delegation removal — the email service reads lettingAgentAccess from the entity
        val propertyOwnership = propertyOwnershipService.getPropertyOwnership(state.propertyOwnershipId)
        cancelLettingAgentDelegationEmailService.sendCancellationEmails(propertyOwnership)
    }

    override fun resolveNextDestination(
        state: CancelLettingAgentDelegationJourneyState,
        defaultDestination: Destination,
    ): Destination {
        state.deleteJourney()
        return defaultDestination
    }
}

@JourneyFrameworkComponent("cancelLettingAgentDelegationAreYouSureStep")
final class AreYouSureStep(
    stepConfig: AreYouSureStepConfig,
) : RequestableStep<Complete, NoInputFormModel, CancelLettingAgentDelegationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "are-you-sure"
    }
}

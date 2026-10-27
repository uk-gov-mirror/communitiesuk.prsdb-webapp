package uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator.RedirectingStepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.states.OrgGovBodyMembersState
import uk.gov.communities.prsdb.webapp.journeys.shared.AnyMembers
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.services.CollectionKeyParameterService

@JourneyFrameworkComponent
class RemoveGovBodyMemberStepConfig(
    private val collectionKeyParameterService: CollectionKeyParameterService,
) : AbstractRequestableStepConfig<AnyMembers, NoInputFormModel, OrgGovBodyMembersState>() {
    override val formModelClass = NoInputFormModel::class

    override fun getStepLifecycleOrchestrator(journeyStep: JourneyStep<*, *, *>) = RedirectingStepLifecycleOrchestrator(journeyStep)

    override fun getStepSpecificContent(state: OrgGovBodyMembersState): Map<String, Any?> = emptyMap()

    override fun chooseTemplate(state: OrgGovBodyMembersState): String = ""

    override fun beforeAttemptingToReachStep(state: OrgGovBodyMembersState): Boolean {
        val keyToRemove = collectionKeyParameterService.getParameterOrNull()
        val currentMap = state.governingBodyMembersMap ?: emptyMap()
        return keyToRemove != null && keyToRemove in currentMap.keys && (currentMap.size > 1 || state.allowRemovingLastMember)
    }

    override fun afterStepIsReached(state: OrgGovBodyMembersState) {
        val keyToRemove = collectionKeyParameterService.getParameterOrNull() ?: return
        val currentMap = state.governingBodyMembersMap?.toMutableMap() ?: return
        currentMap.remove(keyToRemove)
        state.governingBodyMembersMap = currentMap
        state.markAnswersChanged()
    }

    override fun mode(state: OrgGovBodyMembersState): AnyMembers =
        if (state.governingBodyMembersMap.isNullOrEmpty()) AnyMembers.NO_MEMBERS else AnyMembers.SOME_MEMBERS
}

@JourneyFrameworkComponent
final class RemoveGovBodyMemberStep(
    stepConfig: RemoveGovBodyMemberStepConfig,
) : JourneyStep.RequestableStep<AnyMembers, NoInputFormModel, OrgGovBodyMembersState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "remove-governing-body-member"
    }
}

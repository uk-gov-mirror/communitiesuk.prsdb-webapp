package uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.stepConfig

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator.RedirectingStepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.PropertyDeactivationJourneyState
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@JourneyFrameworkComponent
class CheckCanDeactivateStepConfig(
    private val propertyOwnershipService: PropertyOwnershipService,
) : AbstractRequestableStepConfig<CanDeactivateMode, NoInputFormModel, PropertyDeactivationJourneyState>() {
    override val formModelClass = NoInputFormModel::class

    override fun getStepLifecycleOrchestrator(journeyStep: JourneyStep<*, *, *>) = RedirectingStepLifecycleOrchestrator(journeyStep)

    override fun getStepSpecificContent(state: PropertyDeactivationJourneyState): Map<String, Any?> = emptyMap()

    override fun chooseTemplate(state: PropertyDeactivationJourneyState): String = ""

    override fun mode(state: PropertyDeactivationJourneyState): CanDeactivateMode {
        val propertyOwnership = propertyOwnershipService.getPropertyOwnership(state.propertyOwnershipId)
        return if (propertyOwnership.landlords.size > 1) {
            CanDeactivateMode.HAS_JOINT_LANDLORDS
        } else {
            CanDeactivateMode.SINGLE_LANDLORD
        }
    }
}

@JourneyFrameworkComponent
final class CheckCanDeactivateStep(
    stepConfig: CheckCanDeactivateStepConfig,
) : JourneyStep.RequestableStep<CanDeactivateMode, NoInputFormModel, PropertyDeactivationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "deactivate"
    }
}

enum class CanDeactivateMode {
    SINGLE_LANDLORD,
    HAS_JOINT_LANDLORDS,
}

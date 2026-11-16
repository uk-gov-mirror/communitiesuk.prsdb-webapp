package uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.stepConfig

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.controllers.LeavePropertyController
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.PropertyDeactivationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@JourneyFrameworkComponent
class CannotDeactivateStepConfig(
    private val propertyOwnershipService: PropertyOwnershipService,
) : AbstractRequestableStepConfig<Complete, NoInputFormModel, PropertyDeactivationJourneyState>() {
    override val formModelClass = NoInputFormModel::class

    override fun getStepSpecificContent(state: PropertyDeactivationJourneyState): Map<String, Any?> {
        val propertyOwnership = propertyOwnershipService.getPropertyOwnership(state.propertyOwnershipId)
        return mapOf(
            "addressLines" to propertyOwnership.address.toMultiLineAddress().split("\n"),
            "leavePropertyUrl" to LeavePropertyController.getLeavePropertyPath(state.propertyOwnershipId),
        )
    }

    override fun chooseTemplate(state: PropertyDeactivationJourneyState) = "cannotDeactivatePropertyJointLandlords"

    override fun mode(state: PropertyDeactivationJourneyState): Complete? = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }
}

@JourneyFrameworkComponent
final class CannotDeactivateStep(
    stepConfig: CannotDeactivateStepConfig,
) : RequestableStep<Complete, NoInputFormModel, PropertyDeactivationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "cannot-deactivate"
    }
}

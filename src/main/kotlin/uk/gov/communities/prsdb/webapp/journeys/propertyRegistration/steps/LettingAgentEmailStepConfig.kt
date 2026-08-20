package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.FormData
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.WhoProvidesDetailsState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService

@JourneyFrameworkComponent
class LettingAgentEmailStepConfig(
    private val userToLandlordService: UserToLandlordService,
) : AbstractRequestableStepConfig<Complete, AllowLettingAgentEmailFormModel, WhoProvidesDetailsState>() {
    override val formModelClass = AllowLettingAgentEmailFormModel::class

    override fun getStepSpecificContent(state: WhoProvidesDetailsState) = emptyMap<String, Any?>()

    override fun chooseTemplate(state: WhoProvidesDetailsState): String = "forms/lettingAgentEmailForm"

    override fun mode(state: WhoProvidesDetailsState) = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }

    override fun enrichSubmittedDataBeforeValidation(
        state: WhoProvidesDetailsState,
        formData: FormData,
    ): FormData =
        super.enrichSubmittedDataBeforeValidation(state, formData) +
            (AllowLettingAgentEmailFormModel::landlordEmail.name to userToLandlordService.getCurrentLandlordForUser().email)
}

@JourneyFrameworkComponent
final class LettingAgentEmailStep(
    stepConfig: LettingAgentEmailStepConfig,
) : RequestableStep<Complete, AllowLettingAgentEmailFormModel, WhoProvidesDetailsState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "letting-agent-email"
    }
}

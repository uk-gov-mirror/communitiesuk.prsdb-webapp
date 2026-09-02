package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel

@JourneyFrameworkComponent
class ConfirmationStepConfig : AbstractRequestableStepConfig<Complete, NoInputFormModel, JourneyState>() {
    override val formModelClass = NoInputFormModel::class

    override fun getStepSpecificContent(state: JourneyState): Map<String, Any?> =
        // TODO PDJB-1567: This is the link the letting agent uses to return to their property. It is currently a
        //  placeholder that mirrors the invitation link emailed to letting agents (also a placeholder, see PDJB-1661).
        //  Both must be updated to the real invitation link once PDJB-1661 is done.
        mapOf("updateLink" to PLACEHOLDER_UPDATE_LINK)

    override fun chooseTemplate(state: JourneyState): String = "forms/lettingAgentInvitationConfirmation"

    override fun mode(state: JourneyState): Complete = Complete.COMPLETE

    companion object {
        private const val PLACEHOLDER_UPDATE_LINK = "https://example.com"
    }
}

@JourneyFrameworkComponent
final class ConfirmationStep(
    stepConfig: ConfirmationStepConfig,
) : RequestableStep<Complete, NoInputFormModel, JourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "confirmation"
    }
}

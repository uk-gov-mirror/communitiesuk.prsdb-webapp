package uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig

import uk.gov.communities.prsdb.webapp.exceptions.CyaDataHasChangedException
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.FormData
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.CheckAnswersFormModel

abstract class AbstractCheckYourAnswersStepConfig<TState : CheckYourAnswersJourneyState> :
    AbstractRequestableStepConfig<Complete, CheckAnswersFormModel, TState>() {
    override val formModelClass = CheckAnswersFormModel::class

    override val changesAnswers = false

    override fun chooseTemplate(state: TState) = "forms/checkAnswersForm"

    override fun mode(state: TState) = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }

    // TODO PDJB-1832: rename submittedFilteredJourneyData (it now holds answersLastChanged) in a follow-up PR
    override fun resolvePageContent(
        state: TState,
        defaultContent: Map<String, Any?>,
    ): Map<String, Any?> = defaultContent + ("submittedFilteredJourneyData" to (state.answersLastChanged?.toString() ?: ""))

    override fun enrichSubmittedDataBeforeValidation(
        state: TState,
        formData: FormData,
    ): FormData {
        checkAnswersNotChangedSincePageLoad(state, formData)
        return formData
    }

    override fun beforeStepDataIsAdded(
        state: TState,
        data: FormData,
    ) {
        state.answersConfirmedAt = state.answersLastChanged
    }

    override fun resolveNextDestination(
        state: TState,
        defaultDestination: Destination,
    ): Destination {
        state.deleteJourney()
        return defaultDestination
    }

    private fun checkAnswersNotChangedSincePageLoad(
        state: TState,
        formData: FormData,
    ) {
        val submittedLastChanged = formData["submittedFilteredJourneyData"] as? String ?: return
        val currentLastChanged = state.answersLastChanged?.toString() ?: ""
        if (submittedLastChanged != currentLastChanged) {
            throw CyaDataHasChangedException("Journey answers have changed since the page was loaded")
        }
    }
}

abstract class AbstractCheckYourAnswersStep<TState : CheckYourAnswersJourneyState>(
    stepConfig: AbstractCheckYourAnswersStepConfig<TState>,
) : RequestableStep<Complete, CheckAnswersFormModel, TState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "check-answers"
    }
}

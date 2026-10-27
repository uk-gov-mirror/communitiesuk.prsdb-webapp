package uk.gov.communities.prsdb.webapp.journeys

import uk.gov.communities.prsdb.webapp.database.entity.SavedJourneyState
import java.time.Instant

abstract class AbstractJourneyState(
    private val journeyStateService: JourneyStateService,
    protected val delegateProvider: JourneyStateDelegateProvider =
        JourneyStateDelegateProvider(journeyStateService),
) : JourneyState,
    DelegateKeysOwner by delegateProvider {
    override fun getStepData(key: String): FormData? = objectToStringKeyedMap(journeyStateService.getSubmittedStepData()[key])

    override fun addStepData(
        key: String,
        value: FormData,
    ) = journeyStateService.addSingleStepData(key, value)

    override fun clearStepData(key: String) = journeyStateService.clearStepData(key)

    override val journeyId: String
        get() = journeyStateService.journeyId

    override val journeyMetadata: JourneyMetadata
        get() = journeyStateService.journeyMetadata

    override fun deleteJourney() = journeyStateService.deleteState()

    override fun getSubmittedStepData() = journeyStateService.getSubmittedStepData()

    override fun initializeState(seed: Any?): String {
        val journeyId = generateJourneyId(seed)

        journeyStateService.initialiseJourneyWithId(journeyId) {}
        return journeyId
    }

    override fun initializeOrRestoreState(seed: Any?): String {
        val journeyId = generateJourneyId(seed)

        journeyStateService.initialiseOrRestoreJourneyWithId(journeyId) {}
        return journeyId
    }

    override fun save(): SavedJourneyState = journeyStateService.save()

    override fun setJourneyId(newJourneyId: String) = journeyStateService.setJourneyId(newJourneyId)

    override fun copyJourneyTo(newJourneyId: String) = journeyStateService.copyJourneyTo(newJourneyId)

    override val answersLastChanged: Instant?
        get() = readInstant(ANSWERS_LAST_CHANGED_KEY)

    override var answersConfirmedAt: Instant?
        get() = readInstant(ANSWERS_CONFIRMED_AT_KEY)
        set(value) = journeyStateService.setValue(ANSWERS_CONFIRMED_AT_KEY, value?.toString())

    override fun markAnswersChanged() {
        val now = Instant.now()
        val newValue = if (now == answersLastChanged) now.plusNanos(1) else now
        journeyStateService.setValue(ANSWERS_LAST_CHANGED_KEY, newValue.toString())
    }

    private fun readInstant(key: String): Instant? = (journeyStateService.getValue(key) as? String)?.let { Instant.parse(it) }

    companion object {
        private const val ANSWERS_LAST_CHANGED_KEY = "answersLastChanged"
        private const val ANSWERS_CONFIRMED_AT_KEY = "answersConfirmedAt"
    }
}

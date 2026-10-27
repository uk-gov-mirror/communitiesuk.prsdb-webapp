package uk.gov.communities.prsdb.webapp.journeys

import kotlinx.datetime.Instant
import uk.gov.communities.prsdb.webapp.database.entity.SavedJourneyState

interface JourneyState : DelegateKeysOwner {
    fun getStepData(key: String): FormData?

    fun addStepData(
        key: String,
        value: FormData,
    )

    fun clearStepData(key: String)

    fun getSubmittedStepData(): Map<String, Any?>

    val journeyId: String
    val journeyMetadata: JourneyMetadata

    fun deleteJourney()

    fun initializeState(seed: Any? = null): String

    fun initializeOrRestoreState(seed: Any?): String

    fun generateJourneyId(seed: Any? = null): String =
        if (seed == null) {
            val allowedChars = ('a'..'z') + ('0'..'9')
            String(CharArray(7) { allowedChars.random() })
        } else {
            seed
                .hashCode()
                .toUInt()
                .times(111113111U)
                .and(0x7FFFFFFFu)
                .toString(36)
        }

    fun save(): SavedJourneyState

    fun setJourneyId(newJourneyId: String)

    fun copyJourneyTo(newJourneyId: String)

    val answersLastChanged: Instant?

    var answersConfirmedAt: Instant?

    fun markAnswersChanged()
}

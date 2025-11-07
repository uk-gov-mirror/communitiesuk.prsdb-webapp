package uk.gov.communities.prsdb.webapp.journeys

import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.forms.PageData
import uk.gov.communities.prsdb.webapp.forms.objectToStringKeyedMap
import uk.gov.communities.prsdb.webapp.forms.objectToTypedStringKeyedMap
import java.io.Serializable
import java.util.UUID

data class JourneyMetadata(
    val dataKey: String,
    val baseJourneyId: String? = null,
    val subJourneyName: String? = null,
) : Serializable {
    companion object {
        fun withNewDataKey(): JourneyMetadata = JourneyMetadata(UUID.randomUUID().toString())
    }
}

@PrsdbWebService
@Scope("request")
class JourneyStateService(
    private val session: HttpSession,
    private val journeyIdOrNull: String?,
) {
    val journeyId: String get() = journeyIdOrNull ?: throw NoSuchJourneyException()

    @Autowired
    constructor(
        session: HttpSession,
        request: ServletRequest,
    ) : this(
        session,
        request.getParameter(JOURNEY_ID_PARAM),
    )

    val journeyStateMetadataMap get() =
        objectToTypedStringKeyedMap<JourneyMetadata>(session.getAttribute(JOURNEY_STATE_KEY_STORE_KEY)) ?: mapOf()

    val journeyMetadata get() = journeyStateMetadataMap[journeyId] ?: throw NoSuchJourneyException(journeyId)

    fun getValue(key: String): Any? = objectToStringKeyedMap(session.getAttribute(journeyMetadata.dataKey))?.get(key)

    fun addSingleStepData(
        key: String,
        value: PageData,
    ) {
        val newJourneyData = getSubmittedStepData() + (key to value)
        setValue(STEP_DATA_KEY, newJourneyData)
    }

    fun getSubmittedStepData() = objectToStringKeyedMap(getValue(STEP_DATA_KEY)) ?: emptyMap()

    fun setValue(
        key: String,
        value: Any?,
    ) {
        val journeyState = objectToStringKeyedMap(session.getAttribute(journeyMetadata.dataKey)) ?: mapOf()
        session.setAttribute(journeyMetadata.dataKey, journeyState + (key to value))
    }

    fun deleteState() {
        session.removeAttribute(journeyMetadata.dataKey)
        // TODO PRSD-1550 - Ensure other metadata keys referencing this journey are also cleaned up
        val newMap = journeyStateMetadataMap - journeyId
        session.setAttribute(JOURNEY_STATE_KEY_STORE_KEY, newMap)
    }

    fun initialiseJourneyWithId(
        newJourneyId: String,
        stateInitialiser: JourneyStateService.() -> Unit = {},
    ) {
        val existingMetadata = journeyStateMetadataMap[newJourneyId]
        if (existingMetadata == null) {
            val newMap = journeyStateMetadataMap + (newJourneyId to JourneyMetadata.withNewDataKey())
            session.setAttribute(JOURNEY_STATE_KEY_STORE_KEY, newMap)
        }
        JourneyStateService(session, newJourneyId).stateInitialiser()
    }

    fun initialiseSubJourney(
        newJourneyId: String,
        subJourneyName: String,
    ) {
        val existingMetadata = journeyStateMetadataMap[newJourneyId]
        if (existingMetadata == null) {
            val metadata =
                JourneyMetadata(
                    dataKey = journeyMetadata.dataKey,
                    baseJourneyId = journeyId,
                    subJourneyName = subJourneyName,
                )
            val newMap = journeyStateMetadataMap + (newJourneyId to metadata)
            session.setAttribute(
                JOURNEY_STATE_KEY_STORE_KEY,
                newMap,
            )
        }
    }

    companion object {
        private const val STEP_DATA_KEY = "journeyData"
        private const val JOURNEY_STATE_KEY_STORE_KEY = "journeyStateKeyStore"
        private const val JOURNEY_ID_PARAM = "journeyId"

        fun urlWithJourneyState(
            path: String,
            journeyId: String,
        ): String =
            UriComponentsBuilder
                .newInstance()
                .path(path)
                .queryParam(JOURNEY_ID_PARAM, journeyId)
                .build(true)
                .toUriString()
    }
}

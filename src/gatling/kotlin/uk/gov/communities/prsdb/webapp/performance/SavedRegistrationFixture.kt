package uk.gov.communities.prsdb.webapp.performance

import com.fasterxml.jackson.databind.ObjectMapper

data class SavedRegistrationFixture(
    val subject: String,
    val resumeJourneyId: String,
    val deleteJourneyId: String,
    val address: String,
) {
    companion object {
        fun parse(
            json: String,
            required: Int,
        ): List<SavedRegistrationFixture> {
            require(required > 0) { "At least one saved-registration fixture is required" }
            val root = ObjectMapper().readTree(json)
            require(root.isArray) { "Saved-registration fixtures must be a JSON array" }
            val fixtures =
                root.map { fields ->
                    fun field(name: String): String {
                        val value = fields.get(name)
                        require(value != null && value.isTextual && value.asText().isNotBlank()) { "Missing fixture field: $name" }
                        return value.asText()
                    }
                    SavedRegistrationFixture(
                        field("subject"),
                        field("resumeJourneyId"),
                        field("deleteJourneyId"),
                        field("address"),
                    )
                }
            require(fixtures.size >= required) { "Not enough distinct saved-registration owners: need $required, found ${fixtures.size}" }
            require(fixtures.map { it.subject }.toSet().size == fixtures.size) { "Saved-registration owners must not be shared" }
            val journeyIds = fixtures.flatMap { listOf(it.resumeJourneyId, it.deleteJourneyId) }
            require(journeyIds.toSet().size == journeyIds.size) { "Saved registrations must not be reused across fixtures" }
            require(journeyIds.all { it.matches(Regex("[A-Za-z0-9-]+")) }) { "Saved-registration IDs must be safe path/query values" }
            return fixtures.take(required)
        }
    }
}

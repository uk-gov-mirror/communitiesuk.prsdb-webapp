package uk.gov.communities.prsdb.webapp.performance

import com.fasterxml.jackson.databind.ObjectMapper

data class CompletedPropertyFixture(
    val subject: String,
    val propertyOwnershipId: Long,
    val address: String,
) {
    companion object {
        fun parse(
            json: String,
            required: Int,
        ): List<CompletedPropertyFixture> {
            require(required > 0) { "At least one completed-property fixture is required" }
            val root = ObjectMapper().readTree(json)
            require(root.isArray) { "Completed-property fixtures must be a JSON array" }
            val fixtures =
                root.map { fields ->
                    fun text(name: String): String {
                        val value = fields.get(name)
                        require(value != null && value.isTextual && value.asText().isNotBlank()) { "Missing fixture field: $name" }
                        return value.asText()
                    }
                    val id = fields.get("propertyOwnershipId")
                    require(id != null && id.isIntegralNumber && id.canConvertToLong() && id.asLong() > 0) {
                        "A positive numeric propertyOwnershipId is required"
                    }
                    CompletedPropertyFixture(text("subject"), id.asLong(), text("address"))
                }
            require(fixtures.size >= required) { "Not enough distinct completed-property owners: need $required, found ${fixtures.size}" }
            require(fixtures.map { it.subject }.toSet().size == fixtures.size) { "Completed-property owners must not be shared" }
            require(fixtures.map { it.propertyOwnershipId }.toSet().size == fixtures.size) { "Completed properties must not be reused" }
            return fixtures.take(required)
        }
    }
}

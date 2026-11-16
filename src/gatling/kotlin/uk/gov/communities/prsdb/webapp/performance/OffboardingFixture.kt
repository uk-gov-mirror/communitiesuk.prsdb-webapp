package uk.gov.communities.prsdb.webapp.performance

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

data class OffboardingFixture(
    val subject: String,
    val propertyOwnershipId: Long,
    val address: String,
) {
    companion object {
        fun parse(
            json: String,
            required: Int,
        ): List<OffboardingFixture> {
            require(required > 0) { "At least one offboarding fixture is required" }
            val root = ObjectMapper().readTree(json)
            require(root.isArray) { "Offboarding fixtures must be a JSON array" }
            val fixtures = root.map(::parseFixture)
            require(fixtures.size >= required) {
                "Not enough distinct offboarding fixtures: need $required, found ${fixtures.size}"
            }
            require(fixtures.map { it.subject }.toSet().size == fixtures.size) {
                "Offboarding subjects must not be shared"
            }
            val propertyIds = fixtures.map { it.propertyOwnershipId }
            require(propertyIds.toSet().size == propertyIds.size) {
                "Offboarding properties must not be reused"
            }
            return fixtures.take(required)
        }

        private fun parseFixture(node: JsonNode): OffboardingFixture {
            require(node.isObject) { "Every offboarding fixture must be an object" }
            require(requiredText(node, "action") == "deregister-property") { "Offboarding requires property deregistration fixtures" }
            return OffboardingFixture(
                requiredText(node, "subject"),
                requiredPositiveLong(node, "propertyOwnershipId"),
                requiredText(node, "address"),
            )
        }

        private fun requiredText(
            node: JsonNode,
            name: String,
        ): String {
            val value = node.get(name)
            require(value != null && value.isTextual && value.asText().isNotBlank()) {
                "Missing or invalid fixture field: $name"
            }
            return value.asText()
        }

        private fun requiredPositiveLong(
            node: JsonNode,
            name: String,
        ): Long {
            val value = node.get(name)
            require(value != null && value.isIntegralNumber && value.canConvertToLong() && value.asLong() > 0) {
                "Missing or invalid fixture field: $name"
            }
            return value.asLong()
        }
    }
}

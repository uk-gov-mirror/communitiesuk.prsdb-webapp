package uk.gov.communities.prsdb.webapp.performance

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

data class OperatorFixture(
    val subject: String,
    val councilId: Long,
    val councilName: String,
    val targetId: Long,
    val targetName: String,
    val targetEmail: String,
    val targetSubject: String,
    val targetManager: Boolean,
) {
    companion object {
        fun parse(
            json: String,
            required: Int,
        ): List<OperatorFixture> {
            val fixtures =
                rows(json, required).map {
                    OperatorFixture(
                        it.text("subject"),
                        it.id("councilId"),
                        it.text("councilName"),
                        it.id("targetId"),
                        it.text("targetName"),
                        it.text("targetEmail"),
                        it.text("targetSubject"),
                        it.boolean("targetManager"),
                    )
                }
            require(fixtures.map { it.targetId }.distinct().size == fixtures.size) { "Administrator targets must not be shared" }
            require(fixtures.none { target -> fixtures.any { it.subject == target.targetSubject } }) { "Actors cannot be mutation targets" }
            return fixtures.take(required)
        }

        private fun rows(
            json: String,
            required: Int,
        ): List<JsonNode> {
            require(required > 0) { "At least one fixture is required" }
            val root = ObjectMapper().readTree(json)
            require(root.isArray) { "Fixtures must be a JSON array" }
            val rows = root.toList()
            require(rows.size >= required) { "Not enough distinct role-correct fixtures: need $required, found ${rows.size}" }
            require(rows.map { it.text("subject") }.distinct().size == rows.size) { "Actors cannot be reused" }
            return rows
        }

        private fun JsonNode.text(name: String): String {
            val value = get(name)
            require(value != null && value.isTextual && value.asText().isNotBlank()) { "Missing fixture field: $name" }
            return value.asText()
        }

        private fun JsonNode.id(name: String): Long {
            val value = get(name)
            require(value != null && value.isIntegralNumber && value.canConvertToLong() && value.asLong() > 0) {
                "A positive numeric $name is required"
            }
            return value.asLong()
        }

        private fun JsonNode.boolean(name: String): Boolean {
            val value = get(name)
            require(value != null && value.isBoolean) { "A boolean $name is required" }
            return value.asBoolean()
        }
    }
}

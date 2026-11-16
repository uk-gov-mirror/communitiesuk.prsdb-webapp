package uk.gov.communities.prsdb.webapp.performance

import com.fasterxml.jackson.databind.ObjectMapper

data class OrganisationFixture(
    val subject: String,
    val organisationId: Long,
    val registrationNumber: Long,
    val name: String,
    val invitationToken: String? = null,
) {
    companion object {
        fun parse(
            json: String,
            required: Int,
        ): List<OrganisationFixture> {
            require(required > 0) { "At least one organisation fixture is required" }
            val root = ObjectMapper().readTree(json)
            require(root.isArray) { "Organisation fixtures must be a JSON array" }
            val fixtures =
                root.map { fields ->
                    fun text(name: String): String {
                        val value = fields.get(name)
                        require(value != null && value.isTextual && value.asText().isNotBlank()) { "Missing fixture field: $name" }
                        return value.asText()
                    }

                    fun id(name: String): Long {
                        val value = fields.get(name)
                        require(value != null && value.isIntegralNumber && value.canConvertToLong() && value.asLong() > 0) {
                            "A positive numeric $name is required"
                        }
                        return value.asLong()
                    }
                    OrganisationFixture(
                        text("subject"),
                        id("organisationId"),
                        id("registrationNumber"),
                        text("name"),
                        fields.get("invitationToken")?.let { text("invitationToken") },
                    )
                }
            require(fixtures.size >= required) { "Not enough distinct organisation administrators: need $required, found ${fixtures.size}" }
            require(fixtures.map { it.subject }.toSet().size == fixtures.size) { "Organisation administrators must not be shared" }
            require(fixtures.map { it.organisationId }.toSet().size == fixtures.size) { "Organisations must not be reused" }
            require(
                fixtures.map { it.registrationNumber }.toSet().size == fixtures.size,
            ) { "Organisation registration numbers must be unique" }
            val tokens = fixtures.mapNotNull { it.invitationToken }
            require(tokens.toSet().size == tokens.size) { "Organisation invitation tokens must not be reused" }
            return fixtures.take(required)
        }
    }
}

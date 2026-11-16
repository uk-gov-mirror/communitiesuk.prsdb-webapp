package uk.gov.communities.prsdb.webapp.performance

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

data class AgentFixture(
    val ownerSubject: String,
    val propertyOwnershipId: Long,
    val address: String,
    val token: String?,
    val invitedEmail: String?,
    val hasPassword: Boolean?,
) {
    companion object {
        fun parseAccessFixtures(
            json: String,
            required: Int,
            hasPassword: Boolean,
        ): List<AgentFixture> =
            parse(json, required).also { fixtures ->
                require(fixtures.all { it.token != null && it.invitedEmail != null && it.hasPassword == hasPassword }) {
                    "Letting-agent access fixtures must have tokens and match the requested password status"
                }
            }

        fun parse(
            json: String,
            required: Int,
        ): List<AgentFixture> {
            require(required > 0) { "At least one letting-agent fixture is required" }
            val root = ObjectMapper().readTree(json)
            require(root.isArray) { "Letting-agent fixtures must be a JSON array" }
            val fixtures = root.map(::parseFixture)
            require(fixtures.size >= required) {
                "Not enough distinct letting-agent fixtures: need $required, found ${fixtures.size}"
            }
            require(fixtures.map { it.ownerSubject }.toSet().size == fixtures.size) {
                "Letting-agent owner subjects must not be shared"
            }
            require(fixtures.map { it.propertyOwnershipId }.toSet().size == fixtures.size) {
                "Letting-agent properties must not be reused"
            }
            val tokens = fixtures.mapNotNull { it.token }
            require(tokens.toSet().size == tokens.size) { "Letting-agent access tokens must not be reused" }
            return fixtures.take(required)
        }

        private fun parseFixture(node: JsonNode): AgentFixture {
            require(node.isObject) { "Every letting-agent fixture must be an object" }
            val ownerSubject = requiredText(node, "ownerSubject")
            val propertyOwnershipId = requiredPositiveLong(node, "propertyOwnershipId")
            val address = requiredText(node, "address")
            val token = optionalText(node, "token")
            val invitedEmail = optionalText(node, "invitedEmail")
            val hasPassword = optionalBoolean(node, "hasPassword")
            val accessValues = listOf(token, invitedEmail, hasPassword)
            require(accessValues.all { it == null } || accessValues.all { it != null }) {
                "Letting-agent access data must be complete when present"
            }
            token?.let {
                try {
                    UUID.fromString(it)
                } catch (_: IllegalArgumentException) {
                    throw IllegalArgumentException("Letting-agent access token must be a UUID")
                }
            }
            return AgentFixture(ownerSubject, propertyOwnershipId, address, token, invitedEmail, hasPassword)
        }

        private fun requiredText(
            node: JsonNode,
            name: String,
        ): String =
            optionalText(node, name)
                ?: throw IllegalArgumentException("Missing fixture field: $name")

        private fun optionalText(
            node: JsonNode,
            name: String,
        ): String? {
            val value = node.get(name) ?: return null
            require(value.isTextual && value.asText().isNotBlank()) { "Invalid fixture field: $name" }
            return value.asText()
        }

        private fun requiredPositiveLong(
            node: JsonNode,
            name: String,
        ): Long =
            optionalPositiveLong(node, name)
                ?: throw IllegalArgumentException("Missing fixture field: $name")

        private fun optionalPositiveLong(
            node: JsonNode,
            name: String,
        ): Long? {
            val value = node.get(name) ?: return null
            require(value.isIntegralNumber && value.canConvertToLong() && value.asLong() > 0) {
                "Invalid fixture field: $name"
            }
            return value.asLong()
        }

        private fun optionalBoolean(
            node: JsonNode,
            name: String,
        ): Boolean? {
            val value = node.get(name) ?: return null
            require(value.isBoolean) { "Invalid fixture field: $name" }
            return value.asBoolean()
        }
    }
}

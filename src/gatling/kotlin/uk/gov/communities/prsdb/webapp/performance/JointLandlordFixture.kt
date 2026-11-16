package uk.gov.communities.prsdb.webapp.performance

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

data class JointLandlordFixture(
    val ownerSubject: String,
    val propertyOwnershipId: Long,
    val address: String,
    val invitationId: Long?,
    val token: String?,
    val inviteeSubject: String?,
    val invitedEmail: String?,
) {
    companion object {
        fun parsePreparations(
            json: String,
            required: Int,
        ): List<JointLandlordFixture> =
            parse(json, required).also { fixtures ->
                require(fixtures.all { it.invitationId == null && it.inviteeSubject != null && it.invitedEmail != null }) {
                    "Joint-landlord preparation fixtures must name an invitee and must not contain an existing invitation"
                }
                val actors = fixtures.map { it.ownerSubject } + fixtures.mapNotNull { it.inviteeSubject }
                require(actors.toSet().size == actors.size) { "Joint-landlord preparation actors must be disjoint" }
                val emails = fixtures.mapNotNull { it.invitedEmail?.lowercase() }
                require(emails.toSet().size == emails.size) { "Joint-landlord invited emails must not be reused" }
            }

        fun parseInvitations(
            json: String,
            required: Int,
        ): List<JointLandlordFixture> =
            parse(json, required).also { fixtures ->
                require(
                    fixtures.all { it.invitationId != null && it.token != null && it.inviteeSubject != null && it.invitedEmail != null },
                ) {
                    "Joint-landlord decision fixtures must contain pending invitations and invitee identities"
                }
            }

        fun parse(
            json: String,
            required: Int,
        ): List<JointLandlordFixture> {
            require(required > 0) { "At least one joint-landlord fixture is required" }
            val root = ObjectMapper().readTree(json)
            require(root.isArray) { "Joint-landlord fixtures must be a JSON array" }
            val fixtures = root.map(::parseFixture)
            require(fixtures.size >= required) {
                "Not enough distinct joint-landlord fixtures: need $required, found ${fixtures.size}"
            }
            require(fixtures.map { it.ownerSubject }.toSet().size == fixtures.size) {
                "Joint-landlord owner subjects must not be shared"
            }
            require(fixtures.map { it.propertyOwnershipId }.toSet().size == fixtures.size) {
                "Joint-landlord properties must not be reused"
            }
            val invitationIds = fixtures.mapNotNull { it.invitationId }
            require(invitationIds.toSet().size == invitationIds.size) {
                "Joint-landlord invitation IDs must not be reused"
            }
            val tokens = fixtures.mapNotNull { it.token }
            require(tokens.toSet().size == tokens.size) { "Joint-landlord invitation tokens must not be reused" }
            val inviteeSubjects = fixtures.mapNotNull { it.inviteeSubject }
            require(inviteeSubjects.toSet().size == inviteeSubjects.size) {
                "Joint-landlord invitee subjects must not be shared"
            }
            return fixtures.take(required)
        }

        private fun parseFixture(node: JsonNode): JointLandlordFixture {
            require(node.isObject) { "Every joint-landlord fixture must be an object" }
            val ownerSubject = requiredText(node, "ownerSubject")
            val propertyOwnershipId = requiredPositiveLong(node, "propertyOwnershipId")
            val address = requiredText(node, "address")
            val invitationId = optionalPositiveLong(node, "invitationId")
            val token = optionalText(node, "token")
            val inviteeSubject = optionalText(node, "inviteeSubject")
            val invitedEmail = optionalText(node, "invitedEmail")
            require(
                (invitationId == null) == (token == null) &&
                    (inviteeSubject == null) == (invitedEmail == null) &&
                    (invitationId == null || inviteeSubject != null),
            ) {
                "Joint-landlord invitation data must be complete when present"
            }
            token?.let {
                try {
                    UUID.fromString(it)
                } catch (_: IllegalArgumentException) {
                    throw IllegalArgumentException("Joint-landlord invitation token must be a UUID")
                }
            }
            return JointLandlordFixture(ownerSubject, propertyOwnershipId, address, invitationId, token, inviteeSubject, invitedEmail)
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
    }
}

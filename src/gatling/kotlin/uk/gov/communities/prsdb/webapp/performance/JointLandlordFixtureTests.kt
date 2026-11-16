package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JointLandlordFixtureTests {
    @Test
    fun `fixture parsing preserves a pending invitation and a new-invitation owner`() {
        val fixtures =
            JointLandlordFixture.parse(
                """
                [
                  {
                    "ownerSubject": "owner-a",
                    "propertyOwnershipId": 101,
                    "address": "1 Fixture Road",
                    "invitationId": 201,
                    "token": "00000000-0000-0000-0000-000000000201",
                    "inviteeSubject": "invitee-a",
                    "invitedEmail": "invitee@example.test"
                  },
                  {
                    "ownerSubject": "owner-b",
                    "propertyOwnershipId": 102,
                    "address": "2 Fixture Road"
                  }
                ]
                """.trimIndent(),
                required = 2,
            )

        assertEquals(2, fixtures.size)
        assertEquals(201L, fixtures.first().invitationId)
        assertEquals("invitee-a", fixtures.first().inviteeSubject)
        assertEquals(null, fixtures.last().token)
    }

    @Test
    fun `fixture parsing rejects insufficient or reused destructive actors`() {
        val fixture = """{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road"}"""
        assertFailsWith<IllegalArgumentException> { JointLandlordFixture.parse("[$fixture]", required = 2) }
        assertFailsWith<IllegalArgumentException> {
            JointLandlordFixture.parse("[$fixture, $fixture]", required = 2)
        }
    }

    @Test
    fun `fixture parsing rejects unsafe identifiers and partial or invalid invitation data`() {
        val base = """"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road""""
        assertFailsWith<IllegalArgumentException> {
            JointLandlordFixture.parse("""[{$base,"invitationId":0}]""", required = 1)
        }
        assertFailsWith<IllegalArgumentException> {
            JointLandlordFixture.parse(
                """[{$base,"invitationId":201,"token":"not-a-uuid","inviteeSubject":"invitee-a","invitedEmail":"invitee@example.test"}]""",
                required = 1,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            JointLandlordFixture.parse(
                """[{$base,"invitationId":201,"token":"00000000-0000-0000-0000-000000000201"}]""",
                required = 1,
            )
        }
        val invite =
            """"invitationId":201,"token":"00000000-0000-0000-0000-000000000201",""" +
                """"inviteeSubject":"invitee-a","invitedEmail":"invitee@example.test""""
        assertFailsWith<IllegalArgumentException> {
            JointLandlordFixture.parse(
                """[{$base,$invite}, {${base.replace("owner-a", "owner-b").replace("101", "102")},${invite.replace("201", "202")}}]""",
                required = 2,
            )
        }
    }

    @Test
    fun `decision fixtures require pending invitations with invitee identities`() {
        val invitation =
            """{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road",""" +
                """"invitationId":201,"token":"00000000-0000-0000-0000-000000000201",""" +
                """"inviteeSubject":"invitee-a","invitedEmail":"invitee@example.test"}"""
        val ownerOnly = """{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road"}"""

        assertEquals("invitee-a", JointLandlordFixture.parseInvitations("[$invitation]", required = 1).single().inviteeSubject)
        assertFailsWith<IllegalArgumentException> { JointLandlordFixture.parseInvitations("[$ownerOnly]", required = 1) }
    }

    @Test
    fun `preparation fixtures require an invitee identity without an existing invitation`() {
        val preparation =
            """{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road",""" +
                """"inviteeSubject":"invitee-a","invitedEmail":"invitee@example.test"}"""
        val ownerOnly = """{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road"}"""
        val invitation =
            """{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road",""" +
                """"invitationId":201,"token":"00000000-0000-0000-0000-000000000201",""" +
                """"inviteeSubject":"invitee-a","invitedEmail":"invitee@example.test"}"""
        val selfInvite =
            """{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road",""" +
                """"inviteeSubject":"owner-a","invitedEmail":"invitee@example.test"}"""

        val fixture = JointLandlordFixture.parsePreparations("[$preparation]", required = 1).single()

        assertEquals("invitee@example.test", fixture.invitedEmail)
        assertEquals(null, fixture.token)
        assertFailsWith<IllegalArgumentException> { JointLandlordFixture.parsePreparations("[$ownerOnly]", required = 1) }
        assertFailsWith<IllegalArgumentException> { JointLandlordFixture.parsePreparations("[$invitation]", required = 1) }
        assertFailsWith<IllegalArgumentException> { JointLandlordFixture.parsePreparations("[$selfInvite]", required = 1) }
        assertFailsWith<IllegalArgumentException> {
            JointLandlordFixture.parse(
                """[{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road","inviteeSubject":"invitee-a"}]""",
                required = 1,
            )
        }
    }
}

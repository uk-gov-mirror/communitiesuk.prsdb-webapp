package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AgentFixtureTests {
    @Test
    fun `fixture parsing separates delegation owners from existing password invitations`() {
        val fixtures =
            AgentFixture.parse(
                """
                [
                  {
                    "ownerSubject": "owner-a",
                    "propertyOwnershipId": 101,
                    "address": "1 Fixture Road"
                  },
                  {
                    "ownerSubject": "owner-b",
                    "propertyOwnershipId": 102,
                    "address": "2 Fixture Road",
                    "token": "00000000-0000-0000-0000-000000000102",
                    "invitedEmail": "agent@example.test",
                    "hasPassword": true
                  }
                ]
                """.trimIndent(),
                required = 2,
            )

        assertEquals(2, fixtures.size)
        assertEquals(null, fixtures.first().token)
        assertEquals(true, fixtures.last().hasPassword)
    }

    @Test
    fun `fixture parsing rejects insufficient or reused owner and access data`() {
        val fixture =
            """{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road",""" +
                """"token":"00000000-0000-0000-0000-000000000101","invitedEmail":"agent@example.test","hasPassword":false}"""
        assertFailsWith<IllegalArgumentException> { AgentFixture.parse("[$fixture]", required = 2) }
        assertFailsWith<IllegalArgumentException> { AgentFixture.parse("[$fixture, $fixture]", required = 2) }
    }

    @Test
    fun `fixture parsing rejects malformed tokens and incomplete password access`() {
        val base = """"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road""""
        assertFailsWith<IllegalArgumentException> {
            AgentFixture.parse(
                """[{$base,"token":"invalid","invitedEmail":"agent@example.test","hasPassword":true}]""",
                required = 1,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AgentFixture.parse(
                """[{$base,"token":"00000000-0000-0000-0000-000000000101","hasPassword":false}]""",
                required = 1,
            )
        }
    }

    @Test
    fun `existing-password access fixtures require claimed invitations`() {
        val fixture =
            """{"ownerSubject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road",""" +
                """"token":"00000000-0000-0000-0000-000000000101","invitedEmail":"agent@example.test","hasPassword":true}"""
        val unclaimedFixture = fixture.replace("\"hasPassword\":true", "\"hasPassword\":false")

        assertEquals(true, AgentFixture.parseAccessFixtures("[$fixture]", required = 1, hasPassword = true).single().hasPassword)
        assertFailsWith<IllegalArgumentException> {
            AgentFixture.parseAccessFixtures("[$unclaimedFixture]", required = 1, hasPassword = true)
        }
    }
}

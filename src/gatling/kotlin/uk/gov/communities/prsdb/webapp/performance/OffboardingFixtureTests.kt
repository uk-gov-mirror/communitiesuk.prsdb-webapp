package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OffboardingFixtureTests {
    @Test
    fun `fixture parsing retains distinct actors and expected offboarding actions`() {
        val fixtures =
            OffboardingFixture.parse(
                """
                [
                  {
                    "action": "deregister-property",
                    "subject": "owner-a",
                    "propertyOwnershipId": 101,
                    "address": "1 Fixture Road"
                  },
                  {
                    "action": "deregister-property",
                    "subject": "owner-b",
                    "propertyOwnershipId": 102,
                    "address": "2 Fixture Road"
                  }
                ]
                """.trimIndent(),
                required = 2,
            )

        assertEquals(setOf(101L, 102L), fixtures.map { it.propertyOwnershipId }.toSet())
    }

    @Test
    fun `fixture parsing rejects unsupported actions duplicate actors and insufficient records`() {
        val fixture =
            """{"action":"deregister-property","subject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road"}"""
        assertFailsWith<IllegalArgumentException> { OffboardingFixture.parse("[$fixture]", required = 2) }
        assertFailsWith<IllegalArgumentException> {
            OffboardingFixture.parse("[$fixture, $fixture]", required = 2)
        }
        assertFailsWith<IllegalArgumentException> {
            OffboardingFixture.parse(
                """[{"action":"erase-everything","subject":"owner-a","propertyOwnershipId":101,"address":"1 Fixture Road"}]""",
                required = 1,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            OffboardingFixture.parse(
                """[{"action":"deregister-landlord","subject":"owner-c"}]""",
                required = 1,
            )
        }
    }
}

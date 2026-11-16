package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompletedPropertyFixtureTests {
    private val first = """{"subject":"owner-a","propertyOwnershipId":11,"address":"1 Fixture Road"}"""
    private val second = """{"subject":"owner-b","propertyOwnershipId":12,"address":"2 Fixture Road"}"""

    @Test
    fun `allocates distinct owned properties without cycling`() {
        val fixtures = CompletedPropertyFixture.parse("[$first,$second]", 2)
        assertEquals(listOf("owner-a", "owner-b"), fixtures.map { it.subject })
        assertEquals(listOf(11L, 12L), fixtures.map { it.propertyOwnershipId })
        assertEquals(1, CompletedPropertyFixture.parse("[$first,$second]", 1).size)
    }

    @Test
    fun `rejects underfilled shared malformed or unsafe property fixtures`() {
        for (json in listOf(
            "[$first]",
            "[$first,${second.replace("owner-b", "owner-a")}]",
            "[$first,${second.replace(":12", ":11")}]",
            "[$first,${second.replace(":12", ":0")}]",
            "[$first,${second.replace(":12", ":1.5")}]",
            "[$first,${second.replace("2 Fixture Road", "")}]",
            """[$first,{"subject":"owner-b","propertyOwnershipId":"../12","address":"Road"}]""",
        )) {
            assertFailsWith<IllegalArgumentException>(json) { CompletedPropertyFixture.parse(json, 2) }
        }
    }
}

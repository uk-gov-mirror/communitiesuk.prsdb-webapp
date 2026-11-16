package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrganisationFixtureTests {
    private fun fixture(
        subject: String = "admin-a",
        id: String = "100",
        number: String = "200",
    ) = """{"subject":"$subject","organisationId":$id,"registrationNumber":$number,"name":"Fixture organisation"}"""

    @Test
    fun `allocates distinct administrators and organisations without recycling`() {
        val fixtures = OrganisationFixture.parse("[${fixture()},${fixture("admin-b", "101", "201")}]", 2)
        assertEquals(listOf("admin-a", "admin-b"), fixtures.map { it.subject })
        assertEquals(listOf(100L, 101L), fixtures.map { it.organisationId })
    }

    @Test
    fun `rejects insufficient shared and invalid organisation fixtures before authentication`() {
        for (json in listOf(
            "[]",
            "[${fixture()}]",
            "[${fixture()},${fixture("admin-a", "101", "201")}]",
            "[${fixture()},${fixture("admin-b", "100", "201")}]",
            "[${fixture()},${fixture("admin-b", "101", "200")}]",
            "[${fixture()},${fixture("admin-b", "1.5", "201")}]",
            "[${fixture()},${fixture("admin-b", "101", "0")}]",
            "[${fixture()},${fixture(" ", "101", "201")}]",
        )) {
            assertFailsWith<IllegalArgumentException>(json) { OrganisationFixture.parse(json, 2) }
        }
    }

    @Test
    fun `reads distinct pending invitation tokens for invitees accepting organisation invitations`() {
        fun invited(
            subject: String,
            id: String,
            token: String,
        ) = fixture(subject, id, id).dropLast(1) + ""","invitationToken":"$token"}"""
        val fixtures = OrganisationFixture.parse("[${invited("admin-a", "100", "token-a")},${fixture("admin-b", "101", "201")}]", 2)
        assertEquals(listOf("token-a", null), fixtures.map { it.invitationToken })
        for (json in listOf(
            "[${invited("admin-a", "100", " ")},${fixture("admin-b", "101", "201")}]",
            "[${invited("admin-a", "100", "token-a")},${invited("admin-b", "101", "token-a")}]",
        )) {
            assertFailsWith<IllegalArgumentException>(json) { OrganisationFixture.parse(json, 2) }
        }
    }
}

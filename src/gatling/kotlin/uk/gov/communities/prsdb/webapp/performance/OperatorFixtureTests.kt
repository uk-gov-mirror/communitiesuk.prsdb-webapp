package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OperatorFixtureTests {
    @Test
    fun `operator exports allocate exclusive targets and reject mutation of the actor`() {
        val row = """{"subject":"operator","councilId":2,"councilName":"Council","targetId":42,
            "targetName":"Target","targetEmail":"target@example.com","targetSubject":"target","targetManager":true}"""
        assertEquals("operator", OperatorFixture.parse("[$row]", 1).single().subject)
        assertFailsWith<IllegalArgumentException> { OperatorFixture.parse("[$row,$row]", 1) }
        assertFailsWith<IllegalArgumentException> {
            OperatorFixture.parse("[$row]".replace("\"targetSubject\":\"target\"", "\"targetSubject\":\"operator\""), 1)
        }
        assertFailsWith<IllegalArgumentException> { OperatorFixture.parse("[]", 1) }
        assertFailsWith<IllegalArgumentException> { OperatorFixture.parse("[$row]", 0) }
    }
}

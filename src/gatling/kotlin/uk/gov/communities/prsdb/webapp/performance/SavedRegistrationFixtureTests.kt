package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SavedRegistrationFixtureTests {
    @Test
    fun `fixture allocation consumes distinct owners and saved registrations without recycling`() {
        val fixtures = SavedRegistrationFixture.parse(json(), 2)
        assertEquals(listOf("owner-a", "owner-b"), fixtures.map { it.subject })
    }

    @Test
    fun `insufficient owners and reused subjects or saved ids fail before any mutation`() {
        assertFailsWith<IllegalArgumentException> { SavedRegistrationFixture.parse(json(), 3) }
        assertFailsWith<IllegalArgumentException> { SavedRegistrationFixture.parse(json().replace("owner-b", "owner-a"), 2) }
        assertFailsWith<IllegalArgumentException> { SavedRegistrationFixture.parse(json().replace("delete-b", "resume-a"), 2) }
        assertFailsWith<IllegalArgumentException> { SavedRegistrationFixture.parse(json().replace("resume-a", "../wrong"), 2) }
    }

    private fun json() =
        """[
            {"subject":"owner-a","resumeJourneyId":"resume-a","deleteJourneyId":"delete-a","address":"1 Fixture Road, EG1 2AA"},
            {"subject":"owner-b","resumeJourneyId":"resume-b","deleteJourneyId":"delete-b","address":"2 Fixture Road, EG1 2AA"}
        ]"""
}

package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class WorkloadReservationsTests {
    @Test
    fun `different families cannot share a subject or property`() {
        val reservations = WorkloadReservations()
        reservations.reserve(subjects = listOf("owner", "invitee"), properties = listOf(42))

        assertFailsWith<IllegalArgumentException> { reservations.reserve(subjects = listOf("invitee")) }
        assertFailsWith<IllegalArgumentException> { reservations.reserve(properties = listOf(42)) }
    }

    @Test
    fun `a family cannot reuse an actor or a property within its own allocation`() {
        assertFailsWith<IllegalArgumentException> {
            WorkloadReservations().reserve(subjects = listOf("same", "same"))
        }
        assertFailsWith<IllegalArgumentException> {
            WorkloadReservations().reserve(properties = listOf(42, 42))
        }
    }

    @Test
    fun `different families cannot mutate the same organisation`() {
        val reservations = WorkloadReservations()
        reservations.reserve(organisations = listOf(123))

        assertFailsWith<IllegalArgumentException> { reservations.reserve(organisations = listOf(123)) }
    }
}

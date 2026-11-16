package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RampScheduleTests {
    @Test
    fun `baseline schedules all supported families at their approved rates`() {
        val counts = BaselineLoadProfile.schedules(emptyMap()).mapValues { it.value.arrivalTimesMillis().size }
        assertEquals(
            mapOf(
                BaselineFamily.LANDLORD to 455,
                BaselineFamily.SAVED to 70,
                BaselineFamily.PROPERTY to 70,
                BaselineFamily.ORGANISATION_PROFILE to 70,
                BaselineFamily.COMPLIANCE to 17,
                BaselineFamily.ORGANISATION_TEAM to 17,
                BaselineFamily.JOINT to 17,
                BaselineFamily.AGENT to 17,
                BaselineFamily.OPERATOR to 17,
                BaselineFamily.OFFBOARDING to 17,
            ),
            counts,
        )
        assertEquals(767, counts.values.sum())
    }

    @Test
    fun `arrivals ramp up then hold at the target rate`() {
        val times = RampSchedule(perMinute = 26.0, rampSeconds = 300.0, holdSeconds = 900.0).arrivalTimesMillis()

        // 26/min for 15 minutes plus half of 26/min for 5 minutes, i.e. 390 + 65
        assertEquals(455, times.size)
        assertTrue(times.zipWithNext().all { (first, second) -> second >= first })
        assertTrue(times.last() <= 1_200_000)
    }

    @Test
    fun `arrivals are sparser during the ramp than during the hold`() {
        val times = RampSchedule(perMinute = 26.0, rampSeconds = 300.0, holdSeconds = 900.0).arrivalTimesMillis()

        val firstMinute = times.count { it < 60_000 }
        val lastMinute = times.count { it >= 1_140_000 }
        assertTrue(firstMinute < 3)
        assertTrue(lastMinute in 25..27)
    }
}

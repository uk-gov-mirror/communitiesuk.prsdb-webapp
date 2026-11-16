package uk.gov.communities.prsdb.webapp.performance

import kotlin.math.sqrt

// Arrival times for a rate that rises linearly from zero to the target over the ramp, then holds until the end of the run.
data class RampSchedule(
    val perMinute: Double,
    val rampSeconds: Double,
    val holdSeconds: Double,
) {
    init {
        require(perMinute > 0 && rampSeconds > 0 && holdSeconds >= 0) { "Ramp rate, ramp time and hold time must be positive" }
    }

    fun arrivalTimesMillis(): List<Long> {
        val perSecond = perMinute / SECONDS_PER_MINUTE
        val arrivalsDuringRamp = perSecond * rampSeconds / 2
        val totalSeconds = rampSeconds + holdSeconds
        return generateSequence(1) { it + 1 }
            .map { arrival ->
                if (arrival <= arrivalsDuringRamp) {
                    sqrt(2 * rampSeconds * arrival / perSecond)
                } else {
                    rampSeconds + (arrival - arrivalsDuringRamp) / perSecond
                }
            }.takeWhile { it <= totalSeconds }
            .map { (it * MILLIS_PER_SECOND).toLong() }
            .toList()
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60.0
        const val MILLIS_PER_SECOND = 1000
    }
}

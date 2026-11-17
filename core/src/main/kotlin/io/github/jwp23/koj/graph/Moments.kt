package io.github.jwp23.koj.graph

import java.time.Instant

/**
 * Drinks that share one touch target on the graph: every drink, fast or sipped, that starts at the
 * same instant. [end] is the longest end among them, so the target spans the longest bar; a fast
 * drink ends when it starts. [markers] are in order of start, then end, then id. Drinks that start
 * at different instants never share a moment, even when their bars overlap.
 */
data class IntakeMoment(val markers: List<IntakeMarker>) {
    init {
        require(markers.isNotEmpty()) { "A moment holds at least one drink" }
    }

    val start: Instant = markers.first().start
    val end: Instant = markers.maxOf { it.end }
}

/**
 * Check-ins that share one touch target on the graph because they were made at the same instant.
 */
data class CheckInMoment(val markers: List<CheckInMarker>) {
    init {
        require(markers.isNotEmpty()) { "A moment holds at least one check-in" }
    }

    val at: Instant = markers.first().at
}

/** These markers grouped by moment, in time order. */
fun List<IntakeMarker>.intakeMoments(): List<IntakeMoment> =
    sortedWith(compareBy({ it.start }, { it.end }, { it.drinkId }))
        .groupBy { it.start }
        .values
        .map(::IntakeMoment)

/** These markers grouped by moment, in time order. */
fun List<CheckInMarker>.checkInMoments(): List<CheckInMoment> =
    sortedWith(compareBy({ it.at }, { it.checkInId })).groupBy { it.at }.values.map(::CheckInMoment)

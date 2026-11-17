package io.github.jwp23.koj.graph

import java.time.Instant

/**
 * Drinks that share one touch target on the graph: fast drinks that start together, or sipped
 * drinks with the same start and the same end. A fast drink ends when it starts, so one key of
 * start and end covers both. [markers] are in order of start, then id.
 */
data class IntakeMoment(val markers: List<IntakeMarker>) {
    val start: Instant = markers.first().start
    val end: Instant = markers.first().end
}

/**
 * Check-ins that share one touch target on the graph because they were made at the same instant.
 */
data class CheckInMoment(val markers: List<CheckInMarker>) {
    val at: Instant = markers.first().at
}

/** These markers grouped by moment, in time order. */
fun List<IntakeMarker>.intakeMoments(): List<IntakeMoment> =
    sortedWith(compareBy({ it.start }, { it.end }, { it.drinkId }))
        .groupBy { it.start to it.end }
        .values
        .map(::IntakeMoment)

/** These markers grouped by moment, in time order. */
fun List<CheckInMarker>.checkInMoments(): List<CheckInMoment> =
    sortedWith(compareBy({ it.at }, { it.checkInId })).groupBy { it.at }.values.map(::CheckInMoment)

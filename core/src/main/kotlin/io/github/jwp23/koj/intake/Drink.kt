package io.github.jwp23.koj.intake

import java.time.Duration
import java.time.Instant

/** How a drink is taken: all at once, or a little at a time. */
enum class IntakeStyle {
    FAST,
    SIPPED,
}

/**
 * One logged drink or supplement, as the intake model sees it.
 *
 * [caffeineMg] and [theanineMg] are the amounts actually taken, after any fraction of a serving has
 * been applied. Either may be zero. [finish] is null while a sipped drink is in progress; the model
 * then assumes it will finish [defaultDuration] after [start]. A fast drink uses neither.
 */
data class Drink(
    val caffeineMg: Double,
    val theanineMg: Double,
    val style: IntakeStyle,
    val start: Instant,
    val finish: Instant? = null,
    val defaultDuration: Duration = Duration.ZERO,
) {
    init {
        require(caffeineMg >= 0.0) { "Caffeine amount must not be negative, was $caffeineMg" }
        require(theanineMg >= 0.0) { "L-theanine amount must not be negative, was $theanineMg" }
        require(finish == null || !finish.isBefore(start)) {
            "Finish $finish must not be before start $start"
        }
        require(!defaultDuration.isNegative) {
            "Default duration must not be negative, was $defaultDuration"
        }
    }
}

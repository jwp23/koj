package io.github.jwp23.koj.record

import io.github.jwp23.koj.intake.IntakeStyle
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Something the user drinks or takes, with what one serving contains.
 *
 * [id] is 0 until the source is stored. Either per-serving amount may be zero, so a supplement can
 * be tracked as an event. [defaultDuration] is how long a sipped serving usually takes; a sipped
 * source needs one. [usualTime] is the time of day the user usually has it, if any.
 */
data class Source(
    val id: Long = 0,
    val name: String,
    val caffeineMgPerServing: Double,
    val theanineMgPerServing: Double,
    val style: IntakeStyle,
    val defaultDuration: Duration = Duration.ZERO,
    val usualTime: LocalTime? = null,
    val archived: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Name must not be blank" }
        require(caffeineMgPerServing.isFinite() && caffeineMgPerServing >= 0.0) {
            "Caffeine per serving must be finite and not negative, was $caffeineMgPerServing"
        }
        require(theanineMgPerServing.isFinite() && theanineMgPerServing >= 0.0) {
            "L-theanine per serving must be finite and not negative, was $theanineMgPerServing"
        }
        require(!defaultDuration.isNegative) {
            "Default duration must not be negative, was $defaultDuration"
        }
        require(style != IntakeStyle.SIPPED || !defaultDuration.isZero) {
            "A sipped source needs a default duration"
        }
    }

    /**
     * A drink of this source: [servings] of a serving, started at [start]. A fast drink finishes as
     * it starts; a sipped drink is in progress until its finish is recorded.
     */
    fun logged(servings: Double, start: Instant, zone: ZoneId): LoggedDrink =
        LoggedDrink(
            sourceId = id,
            sourceName = name,
            servings = servings,
            caffeineMgPerServing = caffeineMgPerServing,
            theanineMgPerServing = theanineMgPerServing,
            style = style,
            defaultDuration = defaultDuration,
            start = start,
            zone = zone,
            finish = if (style == IntakeStyle.FAST) start else null,
        )
}

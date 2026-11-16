package io.github.jwp23.koj.record

import io.github.jwp23.koj.intake.Amounts
import io.github.jwp23.koj.intake.Drink
import io.github.jwp23.koj.intake.IntakeStyle
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * One drink as stored. It keeps its own copy of the per-serving amounts, so editing the source
 * later does not change what was drunk.
 *
 * [sourceName] is the source's current name. [zone] is the time zone the drink was logged in.
 * [finish] is null while a sipped drink is in progress.
 */
data class LoggedDrink(
    val id: Long = 0,
    val sourceId: Long,
    val sourceName: String,
    val servings: Double,
    val caffeineMgPerServing: Double,
    val theanineMgPerServing: Double,
    val style: IntakeStyle,
    val defaultDuration: Duration,
    val start: Instant,
    val zone: ZoneId,
    val finish: Instant? = null,
) {
    init {
        require(servings.isFinite() && servings > 0.0) {
            "Servings must be finite and above zero, was $servings"
        }
        require(caffeineMgPerServing.isFinite() && caffeineMgPerServing >= 0.0) {
            "Caffeine per serving must be finite and not negative, was $caffeineMgPerServing"
        }
        require(theanineMgPerServing.isFinite() && theanineMgPerServing >= 0.0) {
            "L-theanine per serving must be finite and not negative, was $theanineMgPerServing"
        }
        require(finish == null || !finish.isBefore(start)) {
            "Finish $finish must not be before start $start"
        }
        require(!defaultDuration.isNegative) {
            "Default duration must not be negative, was $defaultDuration"
        }
    }

    /** True for a sipped drink that has not been marked finished. */
    val inProgress: Boolean
        get() = style == IntakeStyle.SIPPED && finish == null

    /** The amounts dosed: the per-serving amounts times [servings]. */
    val taken: Amounts
        get() = Amounts(caffeineMgPerServing * servings, theanineMgPerServing * servings)

    /**
     * When intake ends: [finish], or [start] plus [defaultDuration] while in progress; [start] for
     * a fast drink.
     */
    val end: Instant
        get() =
            when {
                style == IntakeStyle.FAST -> start
                finish != null -> finish
                else -> start.plus(defaultDuration)
            }

    /** This drink as the intake model computes from it. */
    fun toDrink(): Drink =
        Drink(taken.caffeineMg, taken.theanineMg, style, start, finish, defaultDuration)
}

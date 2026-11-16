package io.github.jwp23.koj.form

import io.github.jwp23.koj.intake.Kinetics
import io.github.jwp23.koj.record.Settings
import java.time.Duration
import java.time.LocalTime

/**
 * The longest half-life the form accepts: a year. It is a limit on what can be typed, so that every
 * accepted number converts to a stored duration; it is not a claim about any real half-life.
 */
const val MAX_HALF_LIFE_HOURS = 24L * 365

/** The longest delay after a drink the form accepts: a whole day. */
const val MAX_CHECK_IN_DELAY_MINUTES = 24L * 60

private const val MILLIS_PER_MINUTE = 60_000.0
private const val MILLIS_PER_HOUR = 3_600_000.0
private val maxHalfLifeMillis = Duration.ofHours(MAX_HALF_LIFE_HOURS).toMillis()

/** The fields of [SettingsForm] that can be wrong. */
enum class SettingsField {
    CAFFEINE_HALF_LIFE,
    CAFFEINE_ABSORPTION,
    THEANINE_HALF_LIFE,
    THEANINE_ABSORPTION,
    AFTER_DRINK_CHECK_INS,
    DAILY_CAP,
    SIP,
}

/**
 * What the user has entered as settings. Numbers are text as typed; times are chosen in pickers, so
 * they are times and cannot be badly written.
 */
data class SettingsForm(
    val caffeineHalfLifeHours: String,
    val caffeineAbsorptionMinutes: String,
    val theanineHalfLifeMinutes: String,
    val theanineAbsorptionMinutes: String,
    /** Whole minutes separated by commas, such as "45, 180". */
    val afterDrinkCheckInMinutes: String,
    /** Earliest first, no time twice. */
    val fixedCheckInTimes: List<LocalTime>,
    val dailyCap: String,
    val morningTime: LocalTime,
    val quietTime: LocalTime,
    val bedtime: LocalTime,
    val sipServings: String,
) {
    /** The fields that cannot be saved as typed. Empty when the form is valid. */
    fun errors(): Set<SettingsField> = buildSet {
        if (halfLife(caffeineHalfLifeHours, MILLIS_PER_HOUR) == null) {
            add(SettingsField.CAFFEINE_HALF_LIFE)
        }
        if (halfLife(caffeineAbsorptionMinutes, MILLIS_PER_MINUTE) == null) {
            add(SettingsField.CAFFEINE_ABSORPTION)
        }
        if (halfLife(theanineHalfLifeMinutes, MILLIS_PER_MINUTE) == null) {
            add(SettingsField.THEANINE_HALF_LIFE)
        }
        if (halfLife(theanineAbsorptionMinutes, MILLIS_PER_MINUTE) == null) {
            add(SettingsField.THEANINE_ABSORPTION)
        }
        if (delays() == null) add(SettingsField.AFTER_DRINK_CHECK_INS)
        if (cap() == null) add(SettingsField.DAILY_CAP)
        if (sip() == null) add(SettingsField.SIP)
    }

    /** The settings this form describes. Call only when [errors] is empty. */
    fun toSettings(): Settings =
        Settings(
            caffeine =
                Kinetics(
                    absorptionHalfLife = halfLife(caffeineAbsorptionMinutes, MILLIS_PER_MINUTE)!!,
                    eliminationHalfLife = halfLife(caffeineHalfLifeHours, MILLIS_PER_HOUR)!!,
                ),
            theanine =
                Kinetics(
                    absorptionHalfLife = halfLife(theanineAbsorptionMinutes, MILLIS_PER_MINUTE)!!,
                    eliminationHalfLife = halfLife(theanineHalfLifeMinutes, MILLIS_PER_MINUTE)!!,
                ),
            afterDrinkCheckIns = delays()!!,
            fixedCheckInTimes = fixedCheckInTimes,
            dailyCheckInCap = cap()!!,
            morningTime = morningTime,
            quietTime = quietTime,
            bedtime = bedtime,
            sipServings = sip()!!,
        )

    /**
     * This form with a check-in at [time] added, keeping the times earliest first and none twice.
     */
    fun withFixedCheckIn(time: LocalTime): SettingsForm =
        copy(fixedCheckInTimes = (fixedCheckInTimes + time).distinct().sorted())

    fun withoutFixedCheckIn(time: LocalTime): SettingsForm =
        copy(fixedCheckInTimes = fixedCheckInTimes - time)

    /** A decimal count of units as whole milliseconds, 1 up to a year; null when it is not. */
    private fun halfLife(text: String, millisPerUnit: Double): Duration? =
        parseDecimal(text)
            ?.let { it * millisPerUnit }
            ?.takeIf { it <= maxHalfLifeMillis }
            ?.let { Math.round(it) }
            ?.takeIf { it >= 1 }
            ?.let(Duration::ofMillis)

    /** Whole minutes, each 1 to a day, separated by commas; null when the text is not that. */
    private fun delays(): List<Duration>? {
        if (afterDrinkCheckInMinutes.isBlank()) return emptyList()
        val minutes = afterDrinkCheckInMinutes.split(",").map { wholeNumber(it) ?: return null }
        return minutes
            .takeIf { list -> list.all { it in 1..MAX_CHECK_IN_DELAY_MINUTES } }
            ?.map(Duration::ofMinutes)
    }

    private fun cap(): Int? =
        wholeNumber(dailyCap)?.let { if (it <= Int.MAX_VALUE) it.toInt() else null }

    private fun sip(): Double? = parseDecimal(sipServings)?.takeIf { it > 0.0 }
}

/** A whole number written with digits only, or null (also when it is too large for a Long). */
private fun wholeNumber(text: String): Long? =
    text.trim().takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }?.toLongOrNull()

fun Settings.toForm(): SettingsForm =
    SettingsForm(
        caffeineHalfLifeHours =
            formatDecimal(caffeine.eliminationHalfLife.toMillis() / MILLIS_PER_HOUR),
        caffeineAbsorptionMinutes =
            formatDecimal(caffeine.absorptionHalfLife.toMillis() / MILLIS_PER_MINUTE),
        theanineHalfLifeMinutes =
            formatDecimal(theanine.eliminationHalfLife.toMillis() / MILLIS_PER_MINUTE),
        theanineAbsorptionMinutes =
            formatDecimal(theanine.absorptionHalfLife.toMillis() / MILLIS_PER_MINUTE),
        afterDrinkCheckInMinutes = afterDrinkCheckIns.joinToString(", ") { "${it.toMinutes()}" },
        fixedCheckInTimes = fixedCheckInTimes,
        dailyCap = "$dailyCheckInCap",
        morningTime = morningTime,
        quietTime = quietTime,
        bedtime = bedtime,
        sipServings = formatDecimal(sipServings),
    )

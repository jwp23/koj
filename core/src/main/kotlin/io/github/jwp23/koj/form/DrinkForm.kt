package io.github.jwp23.koj.form

import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.LoggedDrink
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * The most servings one drink may have, so that amounts and the graph stay within sensible sizes.
 */
const val MAX_SERVINGS = 1000.0

/** The fields of [DrinkForm] that can be wrong. */
enum class DrinkField {
    SERVINGS,
    SERVINGS_TOO_MANY,
    START_IN_FUTURE,
    FINISH_DATE,
    FINISH_TIME,
    FINISH_BEFORE_START,
    FINISH_IN_FUTURE,
}

/** ISO "yyyy-MM-dd". */
fun formatDate(date: LocalDate): String = date.toString()

/** [date] as a Material date picker holds it: midnight UTC in epoch milliseconds. */
fun pickerMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** The date a Material date picker means by [millis]. */
fun pickedDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * What the user entered about one drink. The servings are text as typed; dates and times are chosen
 * in pickers, so they cannot be badly written. The finish fields are used only when editing a
 * sipped drink.
 */
data class DrinkForm(
    val servings: String,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val finishDate: LocalDate? = null,
    val finishTime: LocalTime? = null,
) {
    /**
     * The fields that cannot be saved as entered. Empty when the form is valid. [now] is passed in
     * so that a start or finish in the future can be refused.
     */
    fun errors(zone: ZoneId, now: Instant): Set<DrinkField> = buildSet {
        servingsError()?.let(::add)
        val start = start(zone)
        if (start.isAfter(now)) add(DrinkField.START_IN_FUTURE)
        if (finishDate == null && finishTime != null) add(DrinkField.FINISH_DATE)
        if (finishDate != null && finishTime == null) add(DrinkField.FINISH_TIME)
        val finish = finish(zone)
        if (finish != null) {
            if (finish.isBefore(start)) add(DrinkField.FINISH_BEFORE_START)
            if (finish.isAfter(now)) add(DrinkField.FINISH_IN_FUTURE)
        }
    }

    /** What is wrong with [servings] alone, or null when it is an amount that can be logged. */
    fun servingsError(): DrinkField? {
        val amount = parseDecimal(servings)
        return when {
            amount == null || amount <= 0.0 -> DrinkField.SERVINGS
            amount > MAX_SERVINGS -> DrinkField.SERVINGS_TOO_MANY
            else -> null
        }
    }

    /** Call only when [errors] is empty. */
    fun servingsValue(): Double = parseDecimal(servings)!!

    fun start(zone: ZoneId): Instant = instant(startDate, startTime, zone)

    /** Null when either finish field is null. */
    fun finish(zone: ZoneId): Instant? =
        if (finishDate != null && finishTime != null) instant(finishDate, finishTime, zone)
        else null

    /**
     * [drink] with the servings, start, and finish typed here; the per-serving amounts and the zone
     * are kept, the times are read in the drink's zone, and a fast drink finishes where it starts.
     * Call only when [errors] is empty.
     */
    fun applyTo(drink: LoggedDrink): LoggedDrink {
        val start = start(drink.zone)
        val finish = if (drink.style == IntakeStyle.FAST) start else finish(drink.zone)
        return drink.copy(servings = servingsValue(), start = start, finish = finish)
    }

    private fun instant(date: LocalDate, time: LocalTime, zone: ZoneId): Instant =
        LocalDateTime.of(date, time).atZone(zone).toInstant()
}

/**
 * A form for a new drink starting at [now]: one serving, today's date and the current time in
 * [zone], cut to the whole minute.
 */
fun newDrinkForm(now: Instant, zone: ZoneId): DrinkForm {
    val local = now.atZone(zone)
    return DrinkForm("1", local.toLocalDate(), local.toLocalTime().truncatedTo(ChronoUnit.MINUTES))
}

/** The form for editing this drink: times in its own zone, cut to the whole minute. */
fun LoggedDrink.toForm(): DrinkForm {
    val local = start.atZone(zone)
    val finishLocal = finish?.atZone(zone)
    return DrinkForm(
        servings = formatDecimal(servings),
        startDate = local.toLocalDate(),
        startTime = local.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
        finishDate = finishLocal?.toLocalDate(),
        finishTime = finishLocal?.toLocalTime()?.truncatedTo(ChronoUnit.MINUTES),
    )
}

/** The form for editing this drink: a fast drink has no finish of its own to edit. */
fun LoggedDrink.toEditForm(): DrinkForm {
    val form = toForm()
    return if (style == IntakeStyle.FAST) form.copy(finishDate = null, finishTime = null) else form
}

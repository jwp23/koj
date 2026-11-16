package io.github.jwp23.koj.graph

import io.github.jwp23.koj.intake.Amounts
import io.github.jwp23.koj.intake.IntakeModel
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.Settings
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class GraphPoint(val at: Instant, val inBody: Amounts)

data class IntakeMarker(
    val drinkId: Long,
    val start: Instant,
    val end: Instant, // equal to start for a fast drink
    val sourceName: String,
    val taken: Amounts,
)

data class CheckInMarker(
    val checkInId: Long,
    val at: Instant,
    val awake: Int?,
    val focus: Int?,
    val calm: Int?,
)

data class DayGraph(
    val start: Instant,
    val end: Instant,
    /** The lines up to now, or the whole span for a day that is over. */
    val past: List<GraphPoint>,
    /** From now to bedtime. Empty for a day that is over, and once bedtime has passed. */
    val projection: List<GraphPoint>,
    /** Caffeine estimated to remain at bedtime. Null when there is no projection. */
    val caffeineAtBedtimeMg: Double?,
    val intakeMarkers: List<IntakeMarker>,
    val checkInMarkers: List<CheckInMarker>,
    /** The largest value each line reaches, for scaling. */
    val peak: Amounts,
    /** What the day's drinks dosed. */
    val taken: Amounts,
) {
    /** Where [at] falls along the span: 0 at the start, 1 at the end, clamped to that range. */
    fun position(at: Instant): Float {
        val fraction =
            Duration.between(start, at).toMillis().toDouble() /
                Duration.between(start, end).toMillis()
        return fraction.coerceIn(0.0, 1.0).toFloat()
    }
}

// After ten elimination half-lives 2^-10 = 1/1024 is left, under a thousandth. With absorption
// included, what remains of a dose is at most ka / (ka - ke) x 2^-10: for the default caffeine
// (7 and 300 minutes) that is 300 / 293 x 1 / 1024 = 0.00099989 of the dose. Ten is a cutoff for
// negligible amounts, not a bound for every setting: absorption slower than elimination leaves
// more.
private const val CARRY_OVER_HALF_LIVES = 10L

/** How far back a drink can still matter: ten times the longer elimination half-life. */
fun Settings.carryOver(): Duration =
    maxOf(caffeine.eliminationHalfLife, theanine.eliminationHalfLife)
        .multipliedBy(CARRY_OVER_HALF_LIVES)

/** The start of [date] and of the day after it, in [zone]. */
fun dayBounds(date: LocalDate, zone: ZoneId): Pair<Instant, Instant> =
    date.atStartOfDay(zone).toInstant() to date.plusDays(1).atStartOfDay(zone).toInstant()

/**
 * The graph of [date]. [drinks] must include earlier drinks that may not have cleared (everything
 * that starts within [Settings.carryOver] before the day); [checkIns] may include other days'.
 */
fun dayGraph(
    date: LocalDate,
    zone: ZoneId,
    now: Instant,
    settings: Settings,
    drinks: List<LoggedDrink>,
    checkIns: List<CheckIn>,
    step: Duration = Duration.ofMinutes(5),
): DayGraph {
    val (dayStart, dayEnd) = dayBounds(date, zone)
    require(!now.isBefore(dayStart)) { "Now, $now, is before $date starts at $dayStart" }
    val isToday = now.isBefore(dayEnd)
    val daysDrinks = drinks.filter { it.start >= dayStart && it.start < dayEnd }
    val daysCheckIns = checkIns.filter { it.at >= dayStart && it.at < dayEnd }
    val nowIfToday = listOfNotNull(now.takeIf { isToday })

    val morning = date.atTime(settings.morningTime).atZone(zone).toInstant()
    val bedtimeDate = if (settings.bedtime.isAfter(settings.morningTime)) date else date.plusDays(1)
    val bedtime = bedtimeDate.atTime(settings.bedtime).atZone(zone).toInstant()
    val start =
        (listOf(morning) + daysDrinks.map { it.start } + daysCheckIns.map { it.at } + nowIfToday)
            .min()
    val end =
        (listOf(bedtime) + daysDrinks.map { it.end } + daysCheckIns.map { it.at } + nowIfToday)
            .max()

    val model = IntakeModel(settings.caffeine, settings.theanine)
    val modelDrinks = drinks.map { it.toDrink() }
    fun line(first: Instant, last: Instant): List<GraphPoint> =
        pointTimes(first, last, step).map { GraphPoint(it, model.inBody(modelDrinks, it)) }

    val past = line(start, if (isToday) now else end)
    val projection = if (isToday && now.isBefore(bedtime)) line(now, bedtime) else emptyList()
    val lines = past + projection

    return DayGraph(
        start = start,
        end = end,
        past = past,
        projection = projection,
        caffeineAtBedtimeMg = projection.lastOrNull()?.inBody?.caffeineMg,
        intakeMarkers = emptyList(),
        checkInMarkers = emptyList(),
        peak =
            Amounts(
                caffeineMg = lines.maxOf { it.inBody.caffeineMg },
                theanineMg = lines.maxOf { it.inBody.theanineMg },
            ),
        taken = Amounts(0.0, 0.0),
    )
}

/** Every [step] from [first], then [last] whether or not it falls on a step. */
private fun pointTimes(first: Instant, last: Instant, step: Duration): List<Instant> =
    generateSequence(first) { it.plus(step) }.takeWhile { it.isBefore(last) }.toList() + last

package io.github.jwp23.koj.history

import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.LoggedDrink
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One calendar day that has something logged. */
data class HistoryDay(
    val date: LocalDate,
    /** The zone of the day's earliest entry; the day's graph is drawn in it. */
    val zone: ZoneId,
    val drinks: List<LoggedDrink>, // oldest first
    val checkIns: List<CheckIn>, // oldest first
) {
    /** The drinks and check-ins together, oldest first. */
    fun entries(): List<HistoryEntry> =
        (drinks.map(::DrinkEntry) + checkIns.map(::CheckInEntry)).sortedBy { it.at }
}

/** One line of a day: a drink or a check-in. */
sealed interface HistoryEntry {
    val at: Instant
}

data class DrinkEntry(val drink: LoggedDrink) : HistoryEntry {
    override val at: Instant
        get() = drink.start
}

data class CheckInEntry(val checkIn: CheckIn) : HistoryEntry {
    override val at: Instant
        get() = checkIn.at
}

/**
 * Days with at least one drink or check-in, newest first. Each entry falls on the date of its own
 * stored zone.
 */
fun historyDays(drinks: List<LoggedDrink>, checkIns: List<CheckIn>): List<HistoryDay> {
    val drinksByDate = drinks.sortedBy { it.start }.groupBy { it.start.dateIn(it.zone) }
    val checkInsByDate = checkIns.sortedBy { it.at }.groupBy { it.at.dateIn(it.zone) }
    return (drinksByDate.keys + checkInsByDate.keys).sortedDescending().map { date ->
        val daysDrinks = drinksByDate[date].orEmpty()
        val daysCheckIns = checkInsByDate[date].orEmpty()
        HistoryDay(date, earliestZone(daysDrinks, daysCheckIns), daysDrinks, daysCheckIns)
    }
}

private fun Instant.dateIn(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

/** The zone of the earliest of the entries; there is at least one. */
private fun earliestZone(drinks: List<LoggedDrink>, checkIns: List<CheckIn>): ZoneId {
    val firstDrink = drinks.firstOrNull()
    val firstCheckIn = checkIns.firstOrNull()
    return when {
        firstCheckIn == null -> firstDrink!!.zone
        firstDrink == null || firstCheckIn.at < firstDrink.start -> firstCheckIn.zone
        else -> firstDrink.zone
    }
}

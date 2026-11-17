package io.github.jwp23.koj.history

import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.PromptKind
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryDayTest {
    private val utc = ZoneOffset.UTC
    private val auckland = ZoneId.of("Pacific/Auckland")

    private fun drink(id: Long, start: String, zone: ZoneId = utc) =
        LoggedDrink(
            id = id,
            sourceId = 1,
            sourceName = "Espresso",
            servings = 1.0,
            caffeineMgPerServing = 80.0,
            theanineMgPerServing = 0.0,
            style = IntakeStyle.FAST,
            defaultDuration = Duration.ZERO,
            start = Instant.parse(start),
            zone = zone,
            finish = Instant.parse(start),
        )

    private fun checkIn(id: Long, at: String, zone: ZoneId = utc) =
        CheckIn(id = id, at = Instant.parse(at), zone = zone, awake = 3, kind = PromptKind.MORNING)

    @Test
    fun nothingLoggedIsNoDays() {
        assertTrue(historyDays(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun daysAreNewestFirstAndEntriesOldestFirst() {
        val days =
            historyDays(
                listOf(
                    drink(3, "2026-03-10T15:00:00Z"),
                    drink(1, "2026-03-09T09:00:00Z"),
                    drink(2, "2026-03-10T08:00:00Z"),
                ),
                emptyList(),
            )
        assertEquals(
            listOf(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 9)),
            days.map { it.date },
        )
        assertEquals(listOf(2L, 3L), days[0].drinks.map { it.id })
        assertEquals(listOf(1L), days[1].drinks.map { it.id })
    }

    @Test
    fun checkInsWithinADayAreOldestFirst() {
        val days =
            historyDays(
                emptyList(),
                listOf(checkIn(2, "2026-03-10T18:00:00Z"), checkIn(1, "2026-03-10T07:00:00Z")),
            )
        assertEquals(listOf(1L, 2L), days.single().checkIns.map { it.id })
    }

    @Test
    fun aDayWithOnlyACheckInIsListed() {
        val days = historyDays(emptyList(), listOf(checkIn(1, "2026-03-08T07:00:00Z")))
        assertEquals(LocalDate.of(2026, 3, 8), days.single().date)
        assertTrue(days.single().drinks.isEmpty())
        assertEquals(1, days.single().checkIns.size)
    }

    @Test
    fun aDrinkAndACheckInOfTheSameDayShareIt() {
        val days =
            historyDays(
                listOf(drink(1, "2026-03-10T09:00:00Z")),
                listOf(checkIn(1, "2026-03-10T11:00:00Z")),
            )
        assertEquals(1, days.size)
        assertEquals(1, days.single().drinks.size)
        assertEquals(1, days.single().checkIns.size)
    }

    @Test
    fun anEntryFallsOnTheDateOfItsOwnZone() {
        // 11:30Z in Auckland (UTC+13 in March) is 00:30 on the 11th; in UTC it is still the 10th.
        val days =
            historyDays(
                listOf(
                    drink(1, "2026-03-10T11:30:00Z", auckland),
                    drink(2, "2026-03-10T11:30:00Z", utc),
                ),
                emptyList(),
            )
        assertEquals(
            listOf(LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 10)),
            days.map { it.date },
        )
        assertEquals(listOf(1L), days[0].drinks.map { it.id })
        assertEquals(listOf(2L), days[1].drinks.map { it.id })
    }

    @Test
    fun aDayUsesTheZoneOfItsEarliestEntry() {
        // Both fall on 2026-03-10 in their own zones: 08:00 UTC and 23:30 Auckland (10:30Z).
        // The 08:00Z drink is the earlier instant, so the day is in UTC.
        val days =
            historyDays(
                listOf(
                    drink(1, "2026-03-10T10:30:00Z", auckland),
                    drink(2, "2026-03-10T08:00:00Z", utc),
                ),
                emptyList(),
            )
        assertEquals(1, days.size)
        assertEquals(utc, days.single().zone)
    }

    @Test
    fun aCheckInCanSetTheZoneOfTheDay() {
        // The check-in at 2026-03-09T11:00Z is 2026-03-10 00:00 in Auckland, before the UTC drink
        // at 2026-03-10T09:00Z, so the day is in Auckland.
        val days =
            historyDays(
                listOf(drink(1, "2026-03-10T09:00:00Z", utc)),
                listOf(checkIn(1, "2026-03-09T11:00:00Z", auckland)),
            )
        assertEquals(1, days.size)
        assertEquals(auckland, days.single().zone)
    }

    @Test
    fun aDaysEntriesAreInTimeOrder() {
        val day =
            historyDays(
                    listOf(drink(1, "2026-03-10T09:00:00Z"), drink(2, "2026-03-10T12:00:00Z")),
                    listOf(checkIn(1, "2026-03-10T10:00:00Z")),
                )
                .single()
        assertEquals(
            listOf(
                DrinkEntry(day.drinks[0]),
                CheckInEntry(day.checkIns[0]),
                DrinkEntry(day.drinks[1]),
            ),
            day.entries(),
        )
    }
}

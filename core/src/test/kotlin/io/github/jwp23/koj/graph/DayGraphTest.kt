package io.github.jwp23.koj.graph

import io.github.jwp23.koj.intake.Amounts
import io.github.jwp23.koj.intake.IntakeModel
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.PromptKind
import io.github.jwp23.koj.record.Settings
import io.github.jwp23.koj.record.Source
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DayGraphTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val date = LocalDate.of(2026, 3, 10)
    private val settings = Settings()

    // Two days after the graphed day begins, so the day is over.
    private val dayOver = Instant.parse("2026-03-12T00:00:00Z")

    private val espresso =
        Source(
            id = 1,
            name = "Espresso",
            caffeineMgPerServing = 100.0,
            theanineMgPerServing = 0.0,
            style = IntakeStyle.FAST,
        )
    private val greenTea =
        Source(
            id = 2,
            name = "Green tea",
            caffeineMgPerServing = 30.0,
            theanineMgPerServing = 20.0,
            style = IntakeStyle.SIPPED,
            defaultDuration = Duration.ofMinutes(60),
        )

    /** [time], HH:mm, on the graphed date in UTC. */
    private fun at(time: String): Instant = Instant.parse("2026-03-10T$time:00Z")

    private fun drink(source: Source, start: Instant, id: Long, servings: Double = 1.0) =
        source.logged(servings, start, zone).copy(id = id)

    private fun checkIn(id: Long, at: Instant) =
        CheckIn(id = id, at = at, zone = zone, awake = 3, kind = PromptKind.FIXED_TIME)

    private fun graph(
        now: Instant = dayOver,
        settings: Settings = this.settings,
        drinks: List<LoggedDrink> = emptyList(),
        checkIns: List<CheckIn> = emptyList(),
        zone: ZoneId = this.zone,
    ) = dayGraph(date, zone, now, settings, drinks, checkIns)

    @Test
    fun aDayThatIsOverCoversMorningTimeToBedtime() {
        val graph = graph()

        assertEquals(at("08:00"), graph.start)
        assertEquals(at("21:00"), graph.end)
        assertEquals(graph.start, graph.past.first().at)
        assertEquals(graph.end, graph.past.last().at)
        // 08:00 to 21:00 is 13 hours x 12 points an hour = 156 steps, plus the first point.
        assertEquals(157, graph.past.size)
        assertEquals(emptyList<GraphPoint>(), graph.projection)
        assertNull(graph.caffeineAtBedtimeMg)
        assertTrue(graph.past.all { it.inBody == Amounts(0.0, 0.0) })
    }

    @Test
    fun anEarlyDrinkWidensTheStart() {
        val graph = graph(drinks = listOf(drink(espresso, at("06:30"), id = 1)))

        assertEquals(at("06:30"), graph.start)
    }

    @Test
    fun aLateCheckInWidensTheEnd() {
        val graph = graph(checkIns = listOf(checkIn(id = 1, at = at("22:15"))))

        assertEquals(at("22:15"), graph.end)
    }

    @Test
    fun aSippedDrinkStillGoingWidensTheEndToWhenItShouldFinish() {
        // Started 20:30 with a 60-minute default duration and no finish: it should end 21:30.
        val graph = graph(drinks = listOf(drink(greenTea, at("20:30"), id = 1)))

        assertEquals(at("21:30"), graph.end)
    }

    @Test
    fun todayIsSplitAtNow() {
        val graph = graph(now = at("14:02"))

        assertEquals(at("14:02"), graph.past.last().at)
        // 08:00 to 14:00 is 72 steps, 73 points, plus 14:02.
        assertEquals(74, graph.past.size)
        assertEquals(at("14:02"), graph.projection.first().at)
        assertEquals(at("21:00"), graph.projection.last().at)
    }

    @Test
    fun todayBeforeMorningTimeStartsAtNow() {
        val graph = graph(now = at("06:10"))

        assertEquals(at("06:10"), graph.start)
        assertEquals(listOf(GraphPoint(at("06:10"), Amounts(0.0, 0.0))), graph.past)
    }

    @Test
    fun afterBedtimeThereIsNoProjection() {
        val graph = graph(now = at("23:10"))

        assertEquals(at("23:10"), graph.end)
        assertEquals(emptyList<GraphPoint>(), graph.projection)
        assertNull(graph.caffeineAtBedtimeMg)
    }

    @Test
    fun caffeineAtBedtimeComesFromTheModel() {
        val drink = drink(espresso, at("10:00"), id = 1)
        val graph = graph(now = at("14:00"), drinks = listOf(drink))

        val modelAtBedtime =
            IntakeModel(settings.caffeine, settings.theanine)
                .inBody(listOf(drink.toDrink()), at("21:00"))
                .caffeineMg
        assertEquals(modelAtBedtime, graph.caffeineAtBedtimeMg!!, 1e-9)
        // 660 minutes after the dose.
        // ka = ln2 / 7 = 0.099021 per minute; ke = ln2 / 300 = 0.0023105 per minute
        // ka / (ka - ke) = 1.023891
        // e^(-ke x 660) = 0.217638; e^(-ka x 660) is negligible
        // 100 x 1.023891 x 0.217638 = 22.28
        assertEquals(22.28, graph.caffeineAtBedtimeMg!!, 0.01)
    }

    @Test
    fun eachPointIsTheModelsEstimateAtThatTime() {
        val graph = graph(now = at("14:00"), drinks = listOf(drink(espresso, at("10:00"), id = 1)))

        // 60 minutes after the dose; ka / (ka - ke) = 1.023891 as above.
        // e^(-ke x 60) = 0.870551; e^(-ka x 60) = 0.002629
        // 100 x 1.023891 x (0.870551 - 0.002629) = 88.87
        assertEquals(88.87, graph.past.single { it.at == at("11:00") }.inBody.caffeineMg, 0.01)
    }

    @Test
    fun yesterdaysDrinkCountsTowardsTheLinesButIsNotMarked() {
        val yesterday = drink(espresso, Instant.parse("2026-03-09T20:00:00Z"), id = 1)
        val graph = graph(drinks = listOf(yesterday))

        assertTrue(graph.past.first().inBody.caffeineMg > 0.0)
        assertEquals(emptyList<IntakeMarker>(), graph.intakeMarkers)
        assertEquals(Amounts(0.0, 0.0), graph.taken)
    }

    @Test
    fun aFastDrinkIsMarkedAtOneMoment() {
        val graph = graph(drinks = listOf(drink(espresso, at("10:00"), id = 7)))

        val marker = graph.intakeMarkers.single()
        assertEquals(7L, marker.drinkId)
        assertEquals(at("10:00"), marker.start)
        assertEquals(at("10:00"), marker.end)
        assertEquals("Espresso", marker.sourceName)
        // 1 serving x 100 mg caffeine and 0 mg L-theanine.
        assertEquals(Amounts(100.0, 0.0), marker.taken)
    }

    @Test
    fun aFinishedSippedDrinkIsMarkedFromStartToFinish() {
        val finished = drink(greenTea, at("09:00"), id = 1).copy(finish = at("09:40"))
        val graph = graph(drinks = listOf(finished))

        assertEquals(at("09:40"), graph.intakeMarkers.single().end)
    }

    @Test
    fun aSippedDrinkStillGoingIsMarkedToWhenItShouldFinish() {
        // Started 09:00 with a 60-minute default duration: it should finish at 10:00.
        val graph = graph(now = at("09:20"), drinks = listOf(drink(greenTea, at("09:00"), id = 1)))

        assertEquals(at("10:00"), graph.intakeMarkers.single().end)
    }

    @Test
    fun checkInsOfTheDayAreMarked() {
        val today =
            CheckIn(
                id = 5,
                at = at("11:00"),
                zone = zone,
                awake = 4,
                calm = 2,
                kind = PromptKind.FIXED_TIME,
            )
        val yesterday = checkIn(id = 6, at = Instant.parse("2026-03-09T11:00:00Z"))
        val graph = graph(checkIns = listOf(today, yesterday))

        assertEquals(
            listOf(
                CheckInMarker(checkInId = 5, at = at("11:00"), awake = 4, focus = null, calm = 2),
            ),
            graph.checkInMarkers,
        )
    }

    @Test
    fun takenAddsUpTheDaysDrinks() {
        val drinks =
            listOf(
                drink(espresso, at("08:30"), id = 1),
                drink(greenTea, at("10:00"), id = 2, servings = 0.5),
                drink(greenTea, at("15:00"), id = 3, servings = 0.5),
            )
        val graph = graph(drinks = drinks)

        // Caffeine 100 + 0.5 x 30 + 0.5 x 30 = 100 + 15 + 15 = 130.
        // L-theanine 0 + 0.5 x 20 + 0.5 x 20 = 0 + 10 + 10 = 20.
        assertEquals(Amounts(130.0, 20.0), graph.taken)
    }

    @Test
    fun markersAreOrderedByTime() {
        val drinks =
            listOf(drink(espresso, at("15:00"), id = 1), drink(espresso, at("09:00"), id = 2))
        val checkIns = listOf(checkIn(id = 3, at = at("16:00")), checkIn(id = 4, at = at("10:00")))
        val graph = graph(drinks = drinks, checkIns = checkIns)

        assertEquals(listOf(2L, 1L), graph.intakeMarkers.map { it.drinkId })
        assertEquals(listOf(4L, 3L), graph.checkInMarkers.map { it.checkInId })
    }

    @Test
    fun peakIsTheHighestPointOfEachLine() {
        val graph = graph(drinks = listOf(drink(espresso, at("10:00"), id = 1)))

        assertEquals(graph.past.maxOf { it.inBody.caffeineMg }, graph.peak.caffeineMg, 0.0)
        assertTrue(graph.peak.caffeineMg > 0.0)
    }

    @Test
    fun aBedtimeAfterMidnightBelongsToTheSameDay() {
        // 00:30 is not after the 08:00 morning time, so it is 00:30 on the next date.
        val graph = graph(settings = settings.copy(bedtime = LocalTime.of(0, 30)))

        assertEquals(Instant.parse("2026-03-11T00:30:00Z"), graph.end)
    }

    @Test
    fun timesFollowTheZone() {
        // New York is on daylight time (UTC-4) on 2026-03-10; it began on 8 March 2026.
        // 08:00 - (-4h) = 12:00Z; 21:00 - (-4h) = 01:00Z the next day; midnight is 04:00Z.
        val newYork = ZoneId.of("America/New_York")
        val graph = graph(zone = newYork)

        assertEquals(Instant.parse("2026-03-10T12:00:00Z"), graph.start)
        assertEquals(Instant.parse("2026-03-11T01:00:00Z"), graph.end)
        assertEquals(
            Instant.parse("2026-03-10T04:00:00Z") to Instant.parse("2026-03-11T04:00:00Z"),
            dayBounds(date, newYork),
        )
    }

    @Test
    fun aMorningTimeInADaylightSavingGapStillGivesAForwardSpan() {
        // 02:30 does not exist in New York on 2026-03-08 (clocks go 02:00 -> 03:00), so it is
        // read as 03:30, after the 03:00 bedtime.
        val springForward = LocalDate.of(2026, 3, 8)
        val newYork = ZoneId.of("America/New_York")
        val gapSettings =
            settings.copy(morningTime = LocalTime.of(2, 30), bedtime = LocalTime.of(3, 0))
        val graph =
            dayGraph(
                springForward,
                newYork,
                Instant.parse("2026-03-10T00:00:00Z"),
                gapSettings,
                emptyList(),
                emptyList(),
            )

        assertEquals(Instant.parse("2026-03-08T07:00:00Z"), graph.start)
        assertEquals(Instant.parse("2026-03-08T07:30:00Z"), graph.end)
        assertTrue(graph.past.all { it.at >= graph.start && it.at <= graph.end })
    }

    @Test
    fun positionRunsFromZeroToOneAlongTheSpan() {
        val graph = graph()

        assertEquals(0f, graph.position(at("08:00")), 0f)
        assertEquals(1f, graph.position(at("21:00")), 0f)
        // 08:00 to 14:30 is 6.5 hours of the 13-hour span: 6.5 / 13 = 0.5.
        assertEquals(0.5f, graph.position(at("14:30")), 0f)
        assertEquals(0f, graph.position(at("07:00")), 0f)
        assertEquals(1f, graph.position(at("22:00")), 0f)
    }

    @Test
    fun carryOverIsTenTimesTheLongerHalfLife() {
        // Defaults: caffeine 5 hours, L-theanine 65 minutes; 10 x 5 hours = 50 hours.
        assertEquals(Duration.ofHours(50), settings.carryOver())
        // L-theanine at 6 hours is now the longer: 10 x 6 hours = 60 hours.
        val slowTheanine =
            settings.copy(
                theanine = settings.theanine.copy(eliminationHalfLife = Duration.ofHours(6)),
            )
        assertEquals(Duration.ofHours(60), slowTheanine.carryOver())
    }

    @Test
    fun aDayThatHasNotStartedIsRefused() {
        assertThrows(IllegalArgumentException::class.java) {
            graph(now = Instant.parse("2026-03-09T23:00:00Z"))
        }
    }

    @Test
    fun aZeroStepIsRefused() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                dayGraph(date, zone, dayOver, settings, emptyList(), emptyList(), Duration.ZERO)
            }

        assertEquals("Step must be positive, was PT0S", error.message)
    }

    @Test
    fun aNegativeStepIsRefused() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                dayGraph(
                    date,
                    zone,
                    dayOver,
                    settings,
                    emptyList(),
                    emptyList(),
                    Duration.ofMinutes(-5),
                )
            }

        assertEquals("Step must be positive, was PT-5M", error.message)
    }
}

package io.github.jwp23.koj.graph

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MomentsTest {
    private fun at(time: String): Instant = Instant.parse("2026-03-10T$time:00Z")

    private fun intake(id: Long, name: String, start: String, end: String = start) =
        IntakeMarker(
            drinkId = id,
            start = at(start),
            end = at(end),
            sourceName = name,
            taken = io.github.jwp23.koj.intake.Amounts(0.0, 0.0),
        )

    private fun checkIn(id: Long, time: String) =
        CheckInMarker(checkInId = id, at = at(time), awake = 3, focus = null, calm = null)

    @Test
    fun fastDrinksStartingTogetherShareOneMomentOrderedById() {
        val moments =
            listOf(intake(7, "Theanine", "10:00"), intake(5, "Green tea", "10:00")).intakeMoments()
        assertEquals(1, moments.size)
        assertEquals(listOf(5L, 7L), moments[0].markers.map { it.drinkId })
        assertEquals(at("10:00"), moments[0].start)
        assertEquals(at("10:00"), moments[0].end)
    }

    @Test
    fun drinksAtDifferentTimesAreSeparateMomentsInTimeOrder() {
        val moments =
            listOf(intake(1, "Mate", "15:00"), intake(2, "Green tea", "09:00")).intakeMoments()
        assertEquals(
            listOf(listOf(2L), listOf(1L)),
            moments.map { m -> m.markers.map { it.drinkId } },
        )
    }

    // Drinks 1, 2 and 3 all start at 09:00, so they share a moment; the longest end, 10:00, is
    // the moment's end. Drink 4 starts at 09:30, inside that bar, so it stays apart.
    @Test
    fun drinksStartingTogetherShareOneMomentEndingWithTheirLongestEnd() {
        val moments =
            listOf(
                    intake(3, "Matcha", "09:00", "09:30"),
                    intake(1, "Tea", "09:00", "10:00"),
                    intake(2, "Espresso", "09:00"),
                    intake(4, "Mate", "09:30"),
                )
                .intakeMoments()
        assertEquals(
            listOf(listOf(2L, 3L, 1L), listOf(4L)),
            moments.map { m -> m.markers.map { it.drinkId } },
        )
        assertEquals(at("09:00"), moments[0].start)
        assertEquals(at("10:00"), moments[0].end)
        assertEquals(at("09:30"), moments[1].end)
    }

    @Test
    fun aMomentWithNoMarkersIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { IntakeMoment(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { CheckInMoment(emptyList()) }
    }

    @Test
    fun checkInsAtTheSameInstantShareOneMoment() {
        val moments =
            listOf(checkIn(9, "11:00"), checkIn(4, "11:00"), checkIn(6, "12:00")).checkInMoments()
        assertEquals(
            listOf(listOf(4L, 9L), listOf(6L)),
            moments.map { m -> m.markers.map { it.checkInId } },
        )
        assertEquals(at("11:00"), moments[0].at)
    }

    @Test
    fun noMarkersMakeNoMoments() {
        assertEquals(0, emptyList<IntakeMarker>().intakeMoments().size)
        assertEquals(0, emptyList<CheckInMarker>().checkInMoments().size)
    }
}

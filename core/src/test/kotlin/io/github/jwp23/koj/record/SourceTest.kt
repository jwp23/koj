package io.github.jwp23.koj.record

import io.github.jwp23.koj.intake.IntakeStyle
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceTest {
    private fun greenTea(
        name: String = "Green tea",
        caffeineMgPerServing: Double = 30.0,
        theanineMgPerServing: Double = 20.0,
        style: IntakeStyle = IntakeStyle.FAST,
        defaultDuration: Duration = Duration.ZERO,
    ) =
        Source(
            name = name,
            caffeineMgPerServing = caffeineMgPerServing,
            theanineMgPerServing = theanineMgPerServing,
            style = style,
            defaultDuration = defaultDuration,
        )

    @Test
    fun nameMustNotBeBlank() {
        assertThrows(IllegalArgumentException::class.java) { greenTea(name = "  ") }
    }

    @Test
    fun caffeineAmountMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) { greenTea(caffeineMgPerServing = -1.0) }
    }

    @Test
    fun caffeineAmountMustBeFinite() {
        assertThrows(IllegalArgumentException::class.java) {
            greenTea(caffeineMgPerServing = Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            greenTea(caffeineMgPerServing = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun theanineAmountMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) { greenTea(theanineMgPerServing = -1.0) }
    }

    @Test
    fun theanineAmountMustBeFinite() {
        assertThrows(IllegalArgumentException::class.java) {
            greenTea(theanineMgPerServing = Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            greenTea(theanineMgPerServing = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun defaultDurationMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) {
            greenTea(defaultDuration = Duration.ofMinutes(-1))
        }
    }

    @Test
    fun sippedSourceMustHaveADefaultDuration() {
        assertThrows(IllegalArgumentException::class.java) {
            greenTea(style = IntakeStyle.SIPPED, defaultDuration = Duration.ZERO)
        }
    }

    @Test
    fun bothAmountsMayBeZeroForASupplementTrackedAsAnEvent() {
        greenTea(caffeineMgPerServing = 0.0, theanineMgPerServing = 0.0)
    }

    private val start = Instant.parse("2026-03-10T09:00:00Z")
    private val zone = ZoneId.of("Europe/Paris")

    @Test
    fun loggedCopiesTheSourceFieldsIntoTheDrink() {
        val source =
            Source(
                id = 7,
                name = "Green tea",
                caffeineMgPerServing = 30.0,
                theanineMgPerServing = 20.0,
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(45),
            )
        val drink = source.logged(servings = 1.5, start = start, zone = zone)
        assertEquals(7L, drink.sourceId)
        assertEquals("Green tea", drink.sourceName)
        assertEquals(1.5, drink.servings, 0.0)
        assertEquals(30.0, drink.caffeineMgPerServing, 0.0)
        assertEquals(20.0, drink.theanineMgPerServing, 0.0)
        assertEquals(IntakeStyle.SIPPED, drink.style)
        assertEquals(Duration.ofMinutes(45), drink.defaultDuration)
        assertEquals(start, drink.start)
        assertEquals(zone, drink.zone)
    }

    @Test
    fun aLoggedFastDrinkFinishesWhenItStarts() {
        assertEquals(start, greenTea().logged(1.0, start, zone).finish)
    }

    @Test
    fun aLoggedSippedDrinkHasNoFinish() {
        val source = greenTea(style = IntakeStyle.SIPPED, defaultDuration = Duration.ofMinutes(45))
        assertNull(source.logged(1.0, start, zone).finish)
    }
}

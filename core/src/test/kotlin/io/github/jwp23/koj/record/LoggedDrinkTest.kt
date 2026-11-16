package io.github.jwp23.koj.record

import io.github.jwp23.koj.intake.Drink
import io.github.jwp23.koj.intake.IntakeStyle
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggedDrinkTest {
    private val nineAm = Instant.parse("2026-03-10T09:00:00Z")

    private fun loggedDrink(
        servings: Double = 0.5,
        caffeineMgPerServing: Double = 80.0,
        theanineMgPerServing: Double = 20.0,
        style: IntakeStyle = IntakeStyle.FAST,
        defaultDuration: Duration = Duration.ZERO,
        start: Instant = nineAm,
        finish: Instant? = start,
    ) =
        LoggedDrink(
            sourceId = 1,
            sourceName = "Green tea",
            servings = servings,
            caffeineMgPerServing = caffeineMgPerServing,
            theanineMgPerServing = theanineMgPerServing,
            style = style,
            defaultDuration = defaultDuration,
            start = start,
            zone = ZoneId.of("UTC"),
            finish = finish,
        )

    @Test
    fun servingsMustNotBeZero() {
        assertThrows(IllegalArgumentException::class.java) { loggedDrink(servings = 0.0) }
    }

    @Test
    fun servingsMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) { loggedDrink(servings = -1.0) }
    }

    @Test
    fun servingsMustBeFinite() {
        assertThrows(IllegalArgumentException::class.java) { loggedDrink(servings = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) {
            loggedDrink(servings = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun caffeinePerServingMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) {
            loggedDrink(caffeineMgPerServing = -1.0)
        }
    }

    @Test
    fun caffeinePerServingMustBeFinite() {
        assertThrows(IllegalArgumentException::class.java) {
            loggedDrink(caffeineMgPerServing = Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            loggedDrink(caffeineMgPerServing = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun theaninePerServingMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) {
            loggedDrink(theanineMgPerServing = -1.0)
        }
    }

    @Test
    fun theaninePerServingMustBeFinite() {
        assertThrows(IllegalArgumentException::class.java) {
            loggedDrink(theanineMgPerServing = Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            loggedDrink(theanineMgPerServing = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun finishMustNotBeBeforeStart() {
        assertThrows(IllegalArgumentException::class.java) {
            loggedDrink(finish = nineAm.minusSeconds(1))
        }
    }

    @Test
    fun defaultDurationMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) {
            loggedDrink(defaultDuration = Duration.ofMinutes(-1))
        }
    }

    @Test
    fun aSippedDrinkWithNoFinishIsInProgress() {
        val drink =
            loggedDrink(
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(90),
                finish = null,
            )
        assertTrue(drink.inProgress)
    }

    @Test
    fun aFinishedSippedDrinkIsNotInProgress() {
        val drink =
            loggedDrink(
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(90),
                finish = nineAm.plusSeconds(600),
            )
        assertFalse(drink.inProgress)
    }

    @Test
    fun aFastDrinkIsNeverInProgress() {
        assertFalse(loggedDrink(style = IntakeStyle.FAST, finish = null).inProgress)
    }

    @Test
    fun takenIsTheAmountPerServingTimesServings() {
        // 80 mg x 0.5 = 40 mg caffeine; 20 mg x 0.5 = 10 mg L-theanine.
        val taken = loggedDrink().taken
        assertEquals(40.0, taken.caffeineMg, 1e-9)
        assertEquals(10.0, taken.theanineMg, 1e-9)
    }

    @Test
    fun aFastDrinkEndsWhenItStarts() {
        assertEquals(nineAm, loggedDrink(style = IntakeStyle.FAST).end)
    }

    @Test
    fun aFinishedSippedDrinkEndsAtItsFinish() {
        val finish = nineAm.plusSeconds(1200)
        val drink =
            loggedDrink(
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(90),
                finish = finish,
            )
        assertEquals(finish, drink.end)
    }

    @Test
    fun aSippedDrinkInProgressEndsAfterItsDefaultDuration() {
        val drink =
            loggedDrink(
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(90),
                finish = null,
            )
        assertEquals(Instant.parse("2026-03-10T10:30:00Z"), drink.end)
    }

    @Test
    fun toDrinkGivesTheIntakeModelTheAmountsActuallyTaken() {
        val drink =
            loggedDrink(
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(90),
                finish = null,
            )
        assertEquals(
            Drink(
                caffeineMg = 40.0,
                theanineMg = 10.0,
                style = IntakeStyle.SIPPED,
                start = nineAm,
                finish = null,
                defaultDuration = Duration.ofMinutes(90),
            ),
            drink.toDrink(),
        )
    }
}

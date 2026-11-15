package io.github.jwp23.koj.intake

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IntakeModelTest {
    // Caffeine: absorption half-life 10 min, elimination half-life 20 min.
    //   ka / (ka - ke) = (1/10) / (1/10 - 1/20) = 2
    //   c(t) = 2 * (2^(-t/20) - 2^(-t/10))
    //   c(10) = 2 * (0.70710678 - 0.5) = 0.41421356    c(20) = 2 * (0.5 - 0.25) = 0.5
    //   c(40) = 2 * (0.25 - 0.0625) = 0.375
    // L-theanine: absorption half-life 5 min, elimination half-life 10 min.
    //   ka / (ka - ke) = (1/5) / (1/5 - 1/10) = 2
    //   t(t) = 2 * (2^(-t/10) - 2^(-t/5))
    //   t(20) = 2 * (0.25 - 0.0625) = 0.375
    private val caffeine = Kinetics(Duration.ofMinutes(10), Duration.ofMinutes(20))
    private val theanine = Kinetics(Duration.ofMinutes(5), Duration.ofMinutes(10))

    // A sipped drink becomes one dose for each 20 minutes of sipping.
    private val model = IntakeModel(caffeine, theanine, sipDoseSpacing = Duration.ofMinutes(20))

    private val eight = Instant.parse("2026-03-10T08:00:00Z")

    private fun minutesAfterEight(minutes: Long): Instant = eight.plus(Duration.ofMinutes(minutes))

    private fun fast(caffeineMg: Double, theanineMg: Double, start: Instant) =
        Drink(caffeineMg, theanineMg, IntakeStyle.FAST, start)

    @Test
    fun nothingIsInTheBodyWithNoDrinks() {
        assertAmounts(0.0, 0.0, model.inBody(emptyList(), eight))
    }

    @Test
    fun fastDrinkIsOneDoseAtItsStart() {
        val drinks = listOf(fast(100.0, 200.0, eight))
        // Caffeine: 100 mg * c(20) = 100 * 0.5 = 50 mg
        // L-theanine: 200 mg * t(20) = 200 * 0.375 = 75 mg
        assertAmounts(50.0, 75.0, model.inBody(drinks, minutesAfterEight(20)))
    }

    @Test
    fun sippedDrinkIsEqualDosesSpreadFromStartToFinish() {
        // 40 minutes of sipping at one dose per 20 minutes is two doses of 50 mg, at the middle
        // of each half: 08:10 and 08:30. The finish time is used, not the default duration.
        val drinks =
            listOf(
                Drink(
                    caffeineMg = 100.0,
                    theanineMg = 0.0,
                    style = IntakeStyle.SIPPED,
                    start = eight,
                    finish = minutesAfterEight(40),
                    defaultDuration = Duration.ofMinutes(120),
                ),
            )
        // At 08:50 the doses are 40 and 20 minutes old:
        //   50 * c(40) + 50 * c(20) = 50 * 0.375 + 50 * 0.5 = 18.75 + 25 = 43.75 mg
        assertAmounts(43.75, 0.0, model.inBody(drinks, minutesAfterEight(50)))
    }

    @Test
    fun sippedDoseNotYetTakenContributesNothing() {
        val drinks =
            listOf(
                Drink(
                    caffeineMg = 100.0,
                    theanineMg = 0.0,
                    style = IntakeStyle.SIPPED,
                    start = eight,
                    finish = minutesAfterEight(40),
                ),
            )
        // At 08:20 only the 08:10 dose has been taken, and it is 10 minutes old:
        //   50 * c(10) = 50 * 0.41421356 = 20.710678 mg
        assertAmounts(20.710678, 0.0, model.inBody(drinks, minutesAfterEight(20)), 1e-6)
    }

    @Test
    fun sippedDrinkInProgressIsAssumedToFinishAtItsDefaultDuration() {
        val drinks =
            listOf(
                Drink(
                    caffeineMg = 100.0,
                    theanineMg = 0.0,
                    style = IntakeStyle.SIPPED,
                    start = eight,
                    finish = null,
                    defaultDuration = Duration.ofMinutes(40),
                ),
            )
        // The same two doses as a drink finished at 08:40: 43.75 mg at 08:50.
        assertAmounts(43.75, 0.0, model.inBody(drinks, minutesAfterEight(50)))
    }

    @Test
    fun sippingTimeThatIsNotAWholeNumberOfSpacingsRoundsUpToMoreDoses() {
        // 20 minutes of sipping at one dose per 15 minutes rounds up to two doses of 50 mg, at
        // the middle of each half: 08:05 and 08:15.
        val everyFifteenMinutes =
            IntakeModel(caffeine, theanine, sipDoseSpacing = Duration.ofMinutes(15))
        val drinks =
            listOf(
                Drink(
                    caffeineMg = 100.0,
                    theanineMg = 0.0,
                    style = IntakeStyle.SIPPED,
                    start = eight,
                    finish = minutesAfterEight(20),
                ),
            )
        // At 08:25 the doses are 20 and 10 minutes old:
        //   50 * c(20) + 50 * c(10) = 25 + 20.710678 = 45.710678 mg
        assertAmounts(
            45.710678,
            0.0,
            everyFifteenMinutes.inBody(drinks, minutesAfterEight(25)),
            1e-6,
        )
    }

    @Test
    fun overlappingDrinksAreSummed() {
        val drinks = listOf(fast(100.0, 0.0, eight), fast(100.0, 0.0, minutesAfterEight(20)))
        // At 08:40: 100 * c(40) + 100 * c(20) = 37.5 + 50 = 87.5 mg
        assertAmounts(87.5, 0.0, model.inBody(drinks, minutesAfterEight(40)))
    }

    @Test
    fun drinkFromAnEarlierDayStillCounts() {
        // Absorption half-life 6 h, elimination half-life 12 h: ka / (ka - ke) = 2 again, so
        //   s(h) = 2 * (2^(-h/12) - 2^(-h/6)) with h in hours
        //   s(12) = 2 * (0.5 - 0.25) = 0.5    s(36) = 2 * (0.125 - 0.015625) = 0.21875
        val slow = Kinetics(Duration.ofHours(6), Duration.ofHours(12))
        val slowModel = IntakeModel(caffeine = slow, theanine = slow)
        val drinks =
            listOf(
                fast(100.0, 0.0, Instant.parse("2026-03-09T08:00:00Z")),
                fast(100.0, 0.0, Instant.parse("2026-03-10T08:00:00Z")),
            )
        // At 20:00 on the 10th the drinks are 36 and 12 hours old:
        //   100 * s(36) + 100 * s(12) = 21.875 + 50 = 71.875 mg
        assertAmounts(
            71.875,
            0.0,
            slowModel.inBody(drinks, Instant.parse("2026-03-10T20:00:00Z")),
        )
    }

    @Test
    fun sourceWithNeitherSubstanceContributesNothing() {
        val drinks = listOf(fast(0.0, 0.0, eight), fast(100.0, 200.0, eight))
        // Only the second drink counts: the fast-drink amounts, 50 mg and 75 mg.
        assertAmounts(50.0, 75.0, model.inBody(drinks, minutesAfterEight(20)))
    }

    @Test
    fun eachSubstanceIsComputedFromItsOwnAmount() {
        val drinks = listOf(fast(0.0, 200.0, eight))
        // No caffeine in the drink; L-theanine is 200 * t(20) = 75 mg.
        assertAmounts(0.0, 75.0, model.inBody(drinks, minutesAfterEight(20)))
    }

    @Test
    fun sipDoseSpacingMustBePositive() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                IntakeModel(caffeine, theanine, sipDoseSpacing = Duration.ZERO)
            }
        assertEquals("Sip dose spacing must be positive, was PT0S", error.message)
    }

    private fun assertAmounts(
        caffeineMg: Double,
        theanineMg: Double,
        actual: Amounts,
        tolerance: Double = 1e-9,
    ) {
        assertEquals("caffeine", caffeineMg, actual.caffeineMg, tolerance)
        assertEquals("L-theanine", theanineMg, actual.theanineMg, tolerance)
    }
}

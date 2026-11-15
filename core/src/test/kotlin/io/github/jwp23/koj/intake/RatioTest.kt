package io.github.jwp23.koj.intake

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatioTest {
    private val eight = Instant.parse("2026-03-10T08:00:00Z")

    private fun fast(caffeineMg: Double, theanineMg: Double) =
        Drink(caffeineMg, theanineMg, IntakeStyle.FAST, eight)

    @Test
    fun takenRatioOfADrinkIsTheanineDosedOverCaffeineDosed() {
        // 160 mg L-theanine / 80 mg caffeine = 2
        assertEquals(2.0, fast(80.0, 160.0).taken.theanineToCaffeineRatio!!, TOLERANCE)
    }

    @Test
    fun takenRatioOfADayIsTotalTheanineOverTotalCaffeine() {
        val day = listOf(fast(80.0, 160.0), fast(40.0, 20.0), fast(0.0, 0.0))
        // Caffeine 80 + 40 + 0 = 120 mg; L-theanine 160 + 20 + 0 = 180 mg; 180 / 120 = 1.5
        val taken = taken(day)
        assertEquals(120.0, taken.caffeineMg, TOLERANCE)
        assertEquals(180.0, taken.theanineMg, TOLERANCE)
        assertEquals(1.5, taken.theanineToCaffeineRatio!!, TOLERANCE)
    }

    @Test
    fun thereIsNoRatioWithoutCaffeine() {
        assertNull(fast(0.0, 200.0).taken.theanineToCaffeineRatio)
        assertNull(taken(emptyList()).theanineToCaffeineRatio)
    }

    @Test
    fun inBodyRatioFallsAsTheanineClearsFasterThanCaffeine() {
        // Caffeine: absorption 10 min, elimination 20 min: c(t) = 2 * (2^(-t/20) - 2^(-t/10))
        //   c(20) = 0.5, c(40) = 0.375
        // L-theanine: absorption 5 min, elimination 10 min: t(t) = 2 * (2^(-t/10) - 2^(-t/5))
        //   t(20) = 0.375, t(40) = 2 * (0.0625 - 0.00390625) = 0.1171875
        val model =
            IntakeModel(
                caffeine = Kinetics(Duration.ofMinutes(10), Duration.ofMinutes(20)),
                theanine = Kinetics(Duration.ofMinutes(5), Duration.ofMinutes(10)),
            )
        val drinks = listOf(fast(100.0, 200.0))
        // At 08:20: L-theanine 200 * 0.375 = 75 mg, caffeine 100 * 0.5 = 50 mg; 75 / 50 = 1.5
        val early = model.inBody(drinks, eight.plus(Duration.ofMinutes(20)))
        assertEquals(1.5, early.theanineToCaffeineRatio!!, TOLERANCE)
        // At 08:40: L-theanine 200 * 0.1171875 = 23.4375 mg, caffeine 100 * 0.375 = 37.5 mg;
        // 23.4375 / 37.5 = 0.625
        val later = model.inBody(drinks, eight.plus(Duration.ofMinutes(40)))
        assertEquals(0.625, later.theanineToCaffeineRatio!!, TOLERANCE)
    }

    @Test
    fun thereIsNoInBodyRatioBeforeAnyCaffeineIsAbsorbed() {
        val model =
            IntakeModel(
                caffeine = Kinetics(Duration.ofMinutes(10), Duration.ofMinutes(20)),
                theanine = Kinetics(Duration.ofMinutes(5), Duration.ofMinutes(10)),
            )
        assertNull(model.inBody(listOf(fast(100.0, 200.0)), eight).theanineToCaffeineRatio)
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}

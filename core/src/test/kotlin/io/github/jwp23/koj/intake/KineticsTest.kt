package io.github.jwp23.koj.intake

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KineticsTest {
    // Absorption half-life 10 min, elimination half-life 20 min.
    // Rates: ka = ln2/10, ke = ln2/20, so ka / (ka - ke) = (1/10) / (1/10 - 1/20) = 2.
    // Fraction of a dose in the body after t minutes:
    //   f(t) = 2 * (2^(-t/20) - 2^(-t/10))
    private val kinetics =
        Kinetics(
            absorptionHalfLife = Duration.ofMinutes(10),
            eliminationHalfLife = Duration.ofMinutes(20),
        )

    @Test
    fun nothingIsInTheBodyAtTheMomentOfTheDose() {
        // f(0) = 2 * (1 - 1) = 0
        assertEquals(0.0, kinetics.amountInBody(100.0, Duration.ZERO), TOLERANCE)
    }

    @Test
    fun nothingIsInTheBodyBeforeTheDose() {
        assertEquals(0.0, kinetics.amountInBody(100.0, Duration.ofMinutes(-5)), TOLERANCE)
    }

    @Test
    fun amountRisesAsTheDoseIsAbsorbed() {
        // f(20) = 2 * (2^-1 - 2^-2) = 2 * (0.5 - 0.25) = 0.5; 100 mg * 0.5 = 50 mg
        assertEquals(50.0, kinetics.amountInBody(100.0, Duration.ofMinutes(20)), TOLERANCE)
    }

    @Test
    fun amountFallsOnceAbsorptionIsMostlyComplete() {
        // f(40) = 2 * (2^-2 - 2^-4) = 2 * (0.25 - 0.0625) = 0.375; 100 mg * 0.375 = 37.5 mg
        assertEquals(37.5, kinetics.amountInBody(100.0, Duration.ofMinutes(40)), TOLERANCE)
        // f(60) = 2 * (2^-3 - 2^-6) = 2 * (0.125 - 0.015625) = 0.21875; 100 mg -> 21.875 mg
        assertEquals(21.875, kinetics.amountInBody(100.0, Duration.ofMinutes(60)), TOLERANCE)
    }

    @Test
    fun equalHalfLivesGiveAFiniteAmount() {
        // With both half-lives at 20 min the rates are equal, k = ln2/20, and the amount is
        //   dose * k * t * e^(-k*t).
        // At t = 20: k*t = ln2, e^(-ln2) = 0.5, so the fraction is ln2 * 0.5 = 0.34657359028.
        // 100 mg * 0.34657359028 = 34.657359028 mg
        val equal = Kinetics(Duration.ofMinutes(20), Duration.ofMinutes(20))
        assertEquals(34.657359028, equal.amountInBody(100.0, Duration.ofMinutes(20)), TOLERANCE)
    }

    @Test
    fun absorptionHalfLifeMustBePositive() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Kinetics(Duration.ZERO, Duration.ofMinutes(20))
            }
        assertEquals("Absorption half-life must be positive, was PT0S", error.message)
    }

    @Test
    fun eliminationHalfLifeMustBePositive() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Kinetics(Duration.ofMinutes(10), Duration.ofMinutes(-1))
            }
        assertEquals("Elimination half-life must be positive, was PT-1M", error.message)
    }

    @Test
    fun halfLifeBelowOneMillisecondIsRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Kinetics(Duration.ofNanos(1), Duration.ofMinutes(20))
            }
        assertEquals(
            "Absorption half-life must be at least one millisecond, was PT0.000000001S",
            error.message,
        )
    }

    @Test
    fun eliminationHalfLifeBelowOneMillisecondIsRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Kinetics(Duration.ofMinutes(10), Duration.ofNanos(1))
            }
        assertEquals(
            "Elimination half-life must be at least one millisecond, was PT0.000000001S",
            error.message,
        )
    }

    @Test
    fun halfLivesThatDifferByLessThanAMillisecondAreTreatedAsEqual() {
        // Both half-lives are 20 minutes at millisecond resolution, so the amount follows the
        // equal-half-life form, f(t) = ka * t * 2^(-t/20) with ka = ln2/20 per minute. At
        // t = 20 minutes: f = ln2 * 0.5 = 0.34657359027997264.
        val nearlyEqual = Kinetics(Duration.ofMinutes(20), Duration.ofMinutes(20).plusNanos(1))
        assertEquals(
            0.34657359027997264,
            nearlyEqual.amountInBody(1.0, Duration.ofMinutes(20)),
            TOLERANCE,
        )
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}

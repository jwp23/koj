package io.github.jwp23.koj.intake

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultKineticsTest {
    @Test
    fun defaultCaffeineLeavesAboutHalfADoseAfterFiveHours() {
        // Absorption half-life 7 min, elimination half-life 300 min.
        //   ka / (ka - ke) = (1/7) / (1/7 - 1/300) = 300 / 293 = 1.0238908
        //   At t = 300 min: 2^(-300/300) = 0.5 and 2^(-300/7) = 1.3e-13, which is negligible.
        //   100 mg * 1.0238908 * 0.5 = 51.19454 mg
        val amount = DefaultKinetics.CAFFEINE.amountInBody(100.0, Duration.ofHours(5))
        assertEquals(51.19454, amount, 1e-5)
    }

    @Test
    fun defaultTheanineLeavesOverHalfADoseAfterSixtyFiveMinutes() {
        // Absorption half-life 15 min, elimination half-life 65 min.
        //   ka / (ka - ke) = (1/15) / (1/15 - 1/65) = 65 / 50 = 1.3
        //   At t = 65 min: 2^(-65/65) = 0.5 and 2^(-65/15) = 2^(-4.3333) = 0.0496063
        //   200 mg * 1.3 * (0.5 - 0.0496063) = 200 * 1.3 * 0.4503937 = 117.10236 mg
        val amount = DefaultKinetics.L_THEANINE.amountInBody(200.0, Duration.ofMinutes(65))
        assertEquals(117.10236, amount, 1e-5)
    }
}

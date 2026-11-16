package io.github.jwp23.koj.record

import io.github.jwp23.koj.intake.IntakeStyle
import java.time.Duration
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
}

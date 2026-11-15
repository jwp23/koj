package io.github.jwp23.koj.intake

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DrinkTest {
    private val eight = Instant.parse("2026-03-10T08:00:00Z")

    @Test
    fun caffeineAmountMustNotBeNegative() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Drink(-1.0, 0.0, IntakeStyle.FAST, eight)
            }
        assertEquals("Caffeine amount must not be negative, was -1.0", error.message)
    }

    @Test
    fun theanineAmountMustNotBeNegative() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Drink(0.0, -1.0, IntakeStyle.FAST, eight)
            }
        assertEquals("L-theanine amount must not be negative, was -1.0", error.message)
    }

    @Test
    fun finishMustNotBeBeforeStart() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Drink(100.0, 0.0, IntakeStyle.SIPPED, eight, finish = eight.minusSeconds(60))
            }
        assertEquals(
            "Finish 2026-03-10T07:59:00Z must not be before start 2026-03-10T08:00:00Z",
            error.message,
        )
    }

    @Test
    fun defaultDurationMustNotBeNegative() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Drink(
                    100.0,
                    0.0,
                    IntakeStyle.SIPPED,
                    eight,
                    defaultDuration = Duration.ofMinutes(-30),
                )
            }
        assertEquals("Default duration must not be negative, was PT-30M", error.message)
    }
}

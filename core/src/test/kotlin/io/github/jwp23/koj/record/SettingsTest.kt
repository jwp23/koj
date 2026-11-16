package io.github.jwp23.koj.record

import io.github.jwp23.koj.intake.DefaultKinetics
import java.time.Duration
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsTest {
    @Test
    fun dailyCapMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) { Settings(dailyCheckInCap = -1) }
    }

    @Test
    fun afterDrinkDelayMustNotBeZero() {
        assertThrows(IllegalArgumentException::class.java) {
            Settings(afterDrinkCheckIns = listOf(Duration.ZERO))
        }
    }

    @Test
    fun afterDrinkDelayMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) {
            Settings(afterDrinkCheckIns = listOf(Duration.ofMinutes(-45)))
        }
    }

    @Test
    fun sipMustNotBeZero() {
        assertThrows(IllegalArgumentException::class.java) { Settings(sipServings = 0.0) }
    }

    @Test
    fun sipMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) { Settings(sipServings = -0.1) }
    }

    @Test
    fun sipMustBeFinite() {
        assertThrows(IllegalArgumentException::class.java) { Settings(sipServings = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) {
            Settings(sipServings = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun caffeineDefaultsToTheDefaultCaffeineKinetics() {
        assertEquals(DefaultKinetics.CAFFEINE, Settings().caffeine)
    }

    @Test
    fun theanineDefaultsToTheDefaultTheanineKinetics() {
        assertEquals(DefaultKinetics.L_THEANINE, Settings().theanine)
    }

    @Test
    fun afterDrinkCheckInsDefaultToFortyFiveMinutesAndThreeHours() {
        assertEquals(
            listOf(Duration.ofMinutes(45), Duration.ofHours(3)),
            Settings().afterDrinkCheckIns,
        )
    }

    @Test
    fun noFixedCheckInTimesByDefault() {
        assertEquals(emptyList<LocalTime>(), Settings().fixedCheckInTimes)
    }

    @Test
    fun dailyCapDefaultsToSix() {
        assertEquals(6, Settings().dailyCheckInCap)
    }

    @Test
    fun morningTimeDefaultsToEightOClock() {
        assertEquals(LocalTime.of(8, 0), Settings().morningTime)
    }

    @Test
    fun quietTimeDefaultsToTwentyHundred() {
        assertEquals(LocalTime.of(20, 0), Settings().quietTime)
    }

    @Test
    fun bedtimeDefaultsToTwentyOneHundred() {
        assertEquals(LocalTime.of(21, 0), Settings().bedtime)
    }

    @Test
    fun sipDefaultsToOneTenthOfAServing() {
        assertEquals(0.1, Settings().sipServings, 0.0)
    }
}

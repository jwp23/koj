package io.github.jwp23.koj.form

import io.github.jwp23.koj.record.Settings
import java.time.Duration
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsFormTest {
    private val defaults = Settings().toForm()

    @Test
    fun theDefaultsAreShownAsText() {
        assertEquals(
            SettingsForm(
                "5",
                "7",
                "65",
                "15",
                "45, 180",
                emptyList(),
                "6",
                LocalTime.of(8, 0),
                LocalTime.of(20, 0),
                LocalTime.of(21, 0),
                "0.1",
            ),
            defaults,
        )
    }

    @Test
    fun theDefaultsRoundTrip() {
        assertEquals(emptySet<SettingsField>(), defaults.errors())
        assertEquals(Settings(), defaults.toSettings())
    }

    @Test
    fun storedSettingsRoundTrip() {
        val settings =
            Settings(
                afterDrinkCheckIns = emptyList(),
                fixedCheckInTimes = listOf(LocalTime.of(10, 0), LocalTime.of(15, 30)),
                dailyCheckInCap = 0,
                bedtime = LocalTime.of(1, 30),
                sipServings = 0.25,
            )
        assertEquals(settings, settings.toForm().toSettings())
    }

    @Test
    fun hoursAndMinutesBecomeDurations() {
        val settings = defaults.copy(caffeineHalfLifeHours = "4.5").toSettings()
        assertEquals(Duration.ofMinutes(270), settings.caffeine.eliminationHalfLife)
    }

    @Test
    fun halfLivesBecomeWholeMilliseconds() {
        // 0.0000004 minutes = 0.024 ms, which rounds to 0 ms, so it is an error; 0.00002 minutes =
        // 1.2 ms, which rounds to 1 ms.
        val form = defaults.copy(theanineAbsorptionMinutes = "0.00002")
        assertEquals(Duration.ofMillis(1), form.toSettings().theanine.absorptionHalfLife)
        assertEquals(
            setOf(SettingsField.THEANINE_ABSORPTION),
            defaults.copy(theanineAbsorptionMinutes = "0.0000004").errors(),
        )
    }

    @Test
    fun halfLivesMustBeNumbersAboveZero() {
        for (bad in listOf("0", "-1", "", "abc")) {
            assertEquals(
                "caffeine $bad",
                setOf(SettingsField.CAFFEINE_HALF_LIFE),
                defaults.copy(caffeineHalfLifeHours = bad).errors(),
            )
            assertEquals(
                setOf(SettingsField.CAFFEINE_ABSORPTION),
                defaults.copy(caffeineAbsorptionMinutes = bad).errors(),
            )
            assertEquals(
                setOf(SettingsField.THEANINE_HALF_LIFE),
                defaults.copy(theanineHalfLifeMinutes = bad).errors(),
            )
            assertEquals(
                setOf(SettingsField.THEANINE_ABSORPTION),
                defaults.copy(theanineAbsorptionMinutes = bad).errors(),
            )
        }
    }

    @Test
    fun aHalfLifeTooLargeToStoreIsAnError() {
        val huge = "9".repeat(30)
        assertEquals(
            setOf(SettingsField.CAFFEINE_HALF_LIFE),
            defaults.copy(caffeineHalfLifeHours = huge).errors(),
        )
        assertEquals(
            setOf(SettingsField.THEANINE_HALF_LIFE),
            defaults.copy(theanineHalfLifeMinutes = "9".repeat(400)).errors(),
        )
    }

    @Test
    fun theLargestHalfLifeIsAcceptedAndConverts() {
        val form = defaults.copy(caffeineHalfLifeHours = "$MAX_HALF_LIFE_HOURS")
        assertEquals(emptySet<SettingsField>(), form.errors())
        assertEquals(
            Duration.ofHours(MAX_HALF_LIFE_HOURS),
            form.toSettings().caffeine.eliminationHalfLife,
        )
        assertEquals(
            setOf(SettingsField.CAFFEINE_HALF_LIFE),
            defaults.copy(caffeineHalfLifeHours = "${MAX_HALF_LIFE_HOURS + 1}").errors(),
        )
    }

    @Test
    fun afterDrinkCheckInsAreWholeMinutes() {
        val settings = defaults.copy(afterDrinkCheckInMinutes = "30,90").toSettings()
        assertEquals(
            listOf(Duration.ofMinutes(30), Duration.ofMinutes(90)),
            settings.afterDrinkCheckIns,
        )
        assertEquals(
            listOf(Duration.ofMinutes(30), Duration.ofMinutes(90)),
            defaults.copy(afterDrinkCheckInMinutes = " 30 , 90 ").toSettings().afterDrinkCheckIns,
        )
    }

    @Test
    fun noAfterDrinkCheckInsIsAllowed() {
        val form = defaults.copy(afterDrinkCheckInMinutes = "")
        assertEquals(emptySet<SettingsField>(), form.errors())
        assertEquals(emptyList<Duration>(), form.toSettings().afterDrinkCheckIns)
    }

    @Test
    fun badAfterDrinkCheckInsAreErrors() {
        for (bad in listOf("0", "30,,90", "30;90", "1.5", "-5", "abc", "30,", ",30")) {
            assertEquals(
                bad,
                setOf(SettingsField.AFTER_DRINK_CHECK_INS),
                defaults.copy(afterDrinkCheckInMinutes = bad).errors(),
            )
        }
    }

    @Test
    fun anAfterDrinkDelayLongerThanADayIsAnError() {
        assertEquals(
            emptySet<SettingsField>(),
            defaults.copy(afterDrinkCheckInMinutes = "$MAX_CHECK_IN_DELAY_MINUTES").errors(),
        )
        for (bad in listOf("${MAX_CHECK_IN_DELAY_MINUTES + 1}", "9".repeat(30))) {
            assertEquals(
                setOf(SettingsField.AFTER_DRINK_CHECK_INS),
                defaults.copy(afterDrinkCheckInMinutes = bad).errors(),
            )
        }
    }

    @Test
    fun fixedCheckInsAreKeptEarliestFirstAndNeverTwice() {
        val form =
            defaults.withFixedCheckIn(LocalTime.of(15, 30)).withFixedCheckIn(LocalTime.of(10, 0))
        assertEquals(listOf(LocalTime.of(10, 0), LocalTime.of(15, 30)), form.fixedCheckInTimes)
        assertEquals(form, form.withFixedCheckIn(LocalTime.of(10, 0)))
        assertEquals(
            listOf(LocalTime.of(15, 30)),
            form.withoutFixedCheckIn(LocalTime.of(10, 0)).fixedCheckInTimes,
        )
        assertEquals(form, form.withoutFixedCheckIn(LocalTime.of(12, 0)))
        assertEquals(form.fixedCheckInTimes, form.toSettings().fixedCheckInTimes)
        assertEquals(emptySet<SettingsField>(), form.errors())
    }

    @Test
    fun theDailyCapIsAWholeNumberZeroOrMore() {
        assertEquals(0, defaults.copy(dailyCap = "0").toSettings().dailyCheckInCap)
        assertEquals(emptySet<SettingsField>(), defaults.copy(dailyCap = "0").errors())
        for (bad in listOf("-1", "2.5", "", "abc", "99999999999")) {
            assertEquals(
                bad,
                setOf(SettingsField.DAILY_CAP),
                defaults.copy(dailyCap = bad).errors(),
            )
        }
    }

    @Test
    fun timesAreCarriedOverAndABedtimeBeforeMorningIsValid() {
        val form =
            defaults.copy(
                morningTime = LocalTime.of(9, 15),
                quietTime = LocalTime.of(22, 0),
                bedtime = LocalTime.of(1, 0),
            )
        assertEquals(emptySet<SettingsField>(), form.errors())
        val settings = form.toSettings()
        assertEquals(LocalTime.of(9, 15), settings.morningTime)
        assertEquals(LocalTime.of(22, 0), settings.quietTime)
        assertEquals(LocalTime.of(1, 0), settings.bedtime)
    }

    @Test
    fun aSipIsADecimalAboveZero() {
        assertEquals(0.25, defaults.copy(sipServings = "0.25").toSettings().sipServings, 0.0)
        for (bad in listOf("0", "", "-0.1", "abc", "0.${"0".repeat(400)}1", "9".repeat(400))) {
            assertEquals(
                setOf(SettingsField.SIP),
                defaults.copy(sipServings = bad).errors(),
            )
        }
    }

    @Test
    fun severalBadFieldsAreAllReported() {
        val form = defaults.copy(caffeineHalfLifeHours = "0", dailyCap = "-1", sipServings = "x")
        assertEquals(
            setOf(SettingsField.CAFFEINE_HALF_LIFE, SettingsField.DAILY_CAP, SettingsField.SIP),
            form.errors(),
        )
    }
}

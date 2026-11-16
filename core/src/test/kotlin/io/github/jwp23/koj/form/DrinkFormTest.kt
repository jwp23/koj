package io.github.jwp23.koj.form

import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.LoggedDrink
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DrinkFormTest {
    private val utc = ZoneOffset.UTC
    private val now = Instant.parse("2026-03-10T10:00:00Z")
    private val march10 = LocalDate.of(2026, 3, 10)
    private val valid = DrinkForm("1", march10, LocalTime.of(8, 30))

    @Test
    fun aNewFormIsOneServingNow() {
        assertEquals(DrinkForm("1", march10, LocalTime.of(10, 0)), newDrinkForm(now, utc))
    }

    @Test
    fun aNewFormUsesTheDateAndTimeInTheZone() {
        assertEquals(
            DrinkForm("1", march10, LocalTime.of(23, 0)),
            newDrinkForm(now, ZoneId.of("Pacific/Auckland")),
        )
    }

    @Test
    fun aNewFormCutsTheTimeToTheMinute() {
        val seconds = Instant.parse("2026-03-10T10:00:45Z")
        assertEquals(LocalTime.of(10, 0), newDrinkForm(seconds, utc).startTime)
    }

    @Test
    fun aValidFormHasNoErrors() {
        assertEquals(emptySet<DrinkField>(), valid.errors(utc, now))
    }

    @Test
    fun servingsAreADecimalAboveZero() {
        assertEquals(0.5, valid.copy(servings = "0.5").servingsValue(), 0.0)
        assertEquals(1.0, valid.copy(servings = "1").servingsValue(), 0.0)
        assertEquals(emptySet<DrinkField>(), valid.copy(servings = "0.5").errors(utc, now))
    }

    @Test
    fun servingsThatAreNotAboveZeroAreErrors() {
        for (text in listOf("0", "-1", "", "half", "0.0", "1e3")) {
            assertEquals(
                text,
                setOf(DrinkField.SERVINGS),
                valid.copy(servings = text).errors(utc, now),
            )
        }
    }

    @Test
    fun servingsTooSmallToTellFromZeroAreAnError() {
        val text = "0." + "0".repeat(400) + "1"
        assertEquals(setOf(DrinkField.SERVINGS), valid.copy(servings = text).errors(utc, now))
    }

    @Test
    fun servingsAboveTheLimitAreRefusedWithTheirOwnError() {
        assertEquals(
            emptySet<DrinkField>(),
            valid.copy(servings = formatDecimal(MAX_SERVINGS)).errors(utc, now),
        )
        assertEquals(
            setOf(DrinkField.SERVINGS_TOO_MANY),
            valid.copy(servings = "1000.5").errors(utc, now),
        )
    }

    @Test
    fun servingsTooLargeToHoldAreRefusedNotThrown() {
        val huge = "9".repeat(400)
        assertEquals(setOf(DrinkField.SERVINGS), valid.copy(servings = huge).errors(utc, now))
    }

    @Test
    fun theStartIsTheDateAndTimeInTheZone() {
        assertEquals(Instant.parse("2026-03-10T08:30:00Z"), valid.start(utc))
        assertEquals(
            Instant.parse("2026-03-09T19:30:00Z"),
            valid.start(ZoneId.of("Pacific/Auckland")),
        )
    }

    @Test
    fun aStartOneMinuteAfterNowIsInTheFuture() {
        val form = valid.copy(startTime = LocalTime.of(10, 1))
        assertEquals(setOf(DrinkField.START_IN_FUTURE), form.errors(utc, now))
    }

    @Test
    fun aStartEqualToNowIsValid() {
        val form = valid.copy(startTime = LocalTime.of(10, 0))
        assertEquals(emptySet<DrinkField>(), form.errors(utc, now))
    }

    @Test
    fun withNoFinishFieldsThereIsNoFinish() {
        assertNull(valid.finish(utc))
        assertEquals(emptySet<DrinkField>(), valid.errors(utc, now))
    }

    @Test
    fun aFinishNeedsBothItsDateAndItsTime() {
        assertEquals(
            setOf(DrinkField.FINISH_TIME),
            valid.copy(finishDate = march10).errors(utc, now),
        )
        assertEquals(
            setOf(DrinkField.FINISH_DATE),
            valid.copy(finishTime = LocalTime.of(9, 0)).errors(utc, now),
        )
    }

    @Test
    fun aFinishIsTheDateAndTimeInTheZone() {
        val form = valid.copy(finishDate = march10, finishTime = LocalTime.of(9, 10))
        assertEquals(Instant.parse("2026-03-10T09:10:00Z"), form.finish(utc))
    }

    @Test
    fun aFinishBeforeTheStartIsAnError() {
        val form = valid.copy(finishDate = march10, finishTime = LocalTime.of(8, 29))
        assertEquals(setOf(DrinkField.FINISH_BEFORE_START), form.errors(utc, now))
    }

    @Test
    fun aFinishEqualToTheStartIsValid() {
        val form = valid.copy(finishDate = march10, finishTime = LocalTime.of(8, 30))
        assertEquals(emptySet<DrinkField>(), form.errors(utc, now))
    }

    @Test
    fun aFinishAfterNowIsInTheFuture() {
        val form = valid.copy(finishDate = march10, finishTime = LocalTime.of(10, 1))
        assertEquals(setOf(DrinkField.FINISH_IN_FUTURE), form.errors(utc, now))
    }

    private fun drink(finish: Instant?, zone: ZoneId = utc, servings: Double = 0.5) =
        LoggedDrink(
            sourceId = 1,
            sourceName = "Green tea",
            servings = servings,
            caffeineMgPerServing = 30.0,
            theanineMgPerServing = 20.0,
            style = IntakeStyle.SIPPED,
            defaultDuration = Duration.ofMinutes(60),
            start = Instant.parse("2026-03-10T08:30:00Z"),
            zone = zone,
            finish = finish,
        )

    @Test
    fun aStoredDrinkBecomesAForm() {
        assertEquals(
            DrinkForm("0.5", march10, LocalTime.of(8, 30), march10, LocalTime.of(9, 10)),
            drink(Instant.parse("2026-03-10T09:10:00Z")).toForm(),
        )
    }

    @Test
    fun aStoredDrinkWithNoFinishHasNoFinishFields() {
        assertEquals(DrinkForm("0.5", march10, LocalTime.of(8, 30)), drink(null).toForm())
    }

    @Test
    fun aStoredDrinkShowsItsOwnZone() {
        assertEquals(
            DrinkForm("0.5", march10, LocalTime.of(21, 30)),
            drink(null, ZoneId.of("Pacific/Auckland")).toForm(),
        )
    }

    @Test
    fun aFractionalAmountSurvivesAFormRoundTrip() {
        val form = drink(null, servings = 0.1).toForm()
        assertEquals(0.1, form.servingsValue(), 0.0)
        assertEquals(1.0 / 3, drink(null, servings = 1.0 / 3).toForm().servingsValue(), 0.0)
    }

    @Test
    fun datesAreWrittenAsIso() {
        assertEquals("2026-03-10", formatDate(march10))
    }

    @Test
    fun aPickerHoldsADateAsMidnightUtc() {
        assertEquals(1773100800000L, pickerMillis(march10))
        assertEquals(march10, pickedDate(1773100800000L))
        assertEquals(march10, pickedDate(1773100800000L + 86_399_999))
    }

    private val edited = DrinkForm("2", march10, LocalTime.of(9, 0), march10, LocalTime.of(9, 45))

    @Test
    fun applyingChangesTheAmountStartAndFinishOfASippedDrink() {
        val stored = drink(Instant.parse("2026-03-10T09:10:00Z"))
        val changed = edited.applyTo(stored)
        assertEquals(
            stored.copy(
                servings = 2.0,
                start = Instant.parse("2026-03-10T09:00:00Z"),
                finish = Instant.parse("2026-03-10T09:45:00Z"),
            ),
            changed,
        )
    }

    @Test
    fun applyingKeepsASippedDrinkInProgressWhenThereIsNoFinish() {
        val changed = edited.copy(finishDate = null, finishTime = null).applyTo(drink(null))
        assertNull(changed.finish)
    }

    @Test
    fun applyingUsesTheZoneOfTheDrink() {
        // 09:00 on 2026-03-10 in Auckland (UTC+13) is 2026-03-09T20:00:00Z.
        val changed = edited.applyTo(drink(null, ZoneId.of("Pacific/Auckland")))
        assertEquals(Instant.parse("2026-03-09T20:00:00Z"), changed.start)
        assertEquals(ZoneId.of("Pacific/Auckland"), changed.zone)
    }

    @Test
    fun applyingFinishesAFastDrinkWhereItStarts() {
        val fast = drink(null).copy(style = IntakeStyle.FAST, defaultDuration = Duration.ZERO)
        val changed = edited.copy(finishDate = null, finishTime = null).applyTo(fast)
        assertEquals(Instant.parse("2026-03-10T09:00:00Z"), changed.start)
        assertEquals(changed.start, changed.finish)
    }

    @Test
    fun aFastDrinkIsEditedWithoutFinishFields() {
        val fast =
            drink(Instant.parse("2026-03-10T08:30:00Z"))
                .copy(style = IntakeStyle.FAST, defaultDuration = Duration.ZERO)
        assertEquals(DrinkForm("0.5", march10, LocalTime.of(8, 30)), fast.toEditForm())
    }

    @Test
    fun aSippedDrinkIsEditedWithItsFinish() {
        val sipped = drink(Instant.parse("2026-03-10T09:10:00Z"))
        assertEquals(sipped.toForm(), sipped.toEditForm())
    }
}

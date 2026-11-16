package io.github.jwp23.koj.form

import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.Source
import java.time.Duration
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SourceFormTest {
    private val valid = SourceForm(name = "Espresso", caffeineMg = "63")

    @Test
    fun aValidFormHasNoErrors() {
        assertEquals(emptySet<SourceField>(), valid.errors())
    }

    @Test
    fun aBlankNameIsAnError() {
        assertEquals(setOf(SourceField.NAME), valid.copy(name = "").errors())
    }

    @Test
    fun aNameOfSpacesIsAnError() {
        assertEquals(setOf(SourceField.NAME), valid.copy(name = "   ").errors())
    }

    @Test
    fun theSavedNameIsTrimmed() {
        assertEquals("Espresso", valid.copy(name = "  Espresso ").toSource().name)
    }

    @Test
    fun aBlankAmountMeansZero() {
        val source = valid.copy(caffeineMg = "", theanineMg = "").toSource()
        assertEquals(0.0, source.caffeineMgPerServing, 0.0)
        assertEquals(0.0, source.theanineMgPerServing, 0.0)
    }

    @Test
    fun amountsAreParsed() {
        assertEquals(30.0, valid.copy(caffeineMg = "30").toSource().caffeineMgPerServing, 0.0)
        assertEquals(30.5, valid.copy(theanineMg = "30.5").toSource().theanineMgPerServing, 0.0)
    }

    @Test
    fun badCaffeineAmountsAreErrors() {
        for (text in listOf("-1", "abc", "1e3", "NaN", "1,5")) {
            assertEquals(text, setOf(SourceField.CAFFEINE), valid.copy(caffeineMg = text).errors())
        }
    }

    @Test
    fun badTheanineAmountsAreErrors() {
        for (text in listOf("-1", "abc", "1e3", "NaN", "1,5")) {
            assertEquals(text, setOf(SourceField.THEANINE), valid.copy(theanineMg = text).errors())
        }
    }

    @Test
    fun aFastSourceIgnoresTheDuration() {
        val form = valid.copy(style = IntakeStyle.FAST, durationMinutes = "abc")
        assertEquals(emptySet<SourceField>(), form.errors())
        assertEquals(Duration.ZERO, form.toSource().defaultDuration)
    }

    @Test
    fun aSippedSourceTakesWholeMinutes() {
        val source = valid.copy(style = IntakeStyle.SIPPED, durationMinutes = "90").toSource()
        assertEquals(Duration.ofMinutes(90), source.defaultDuration)
    }

    @Test
    fun aSippedSourceNeedsAGoodDuration() {
        for (text in listOf("", "0", "-5", "1.5", "abc")) {
            val form = valid.copy(style = IntakeStyle.SIPPED, durationMinutes = text)
            assertEquals(text, setOf(SourceField.DURATION), form.errors())
        }
    }

    @Test
    fun noUsualTimeGivesASourceWithNone() {
        assertNull(valid.toSource().usualTime)
    }

    @Test
    fun aUsualTimeIsKept() {
        val form = valid.copy(usualTime = LocalTime.of(7, 30))
        assertEquals(emptySet<SourceField>(), form.errors())
        assertEquals(LocalTime.of(7, 30), form.toSource().usualTime)
    }

    @Test
    fun everyBadFieldIsReported() {
        val form =
            SourceForm(
                name = "",
                caffeineMg = "x",
                theanineMg = "y",
                style = IntakeStyle.SIPPED,
                durationMinutes = "z",
            )
        assertEquals(SourceField.entries.toSet(), form.errors())
    }

    @Test
    fun toSourceCarriesTheIdAndIsNotArchived() {
        val source = valid.toSource(id = 7)
        assertEquals(7L, source.id)
        assertFalse(source.archived)
    }

    @Test
    fun aFastSourceRoundTripsThroughTheForm() {
        val source =
            Source(
                id = 3,
                name = "Water",
                caffeineMgPerServing = 12.5,
                theanineMgPerServing = 0.0,
                style = IntakeStyle.FAST,
            )
        val form = source.toForm()
        assertEquals("0", form.theanineMg)
        assertEquals("12.5", form.caffeineMg)
        assertEquals("", form.durationMinutes)
        assertEquals(source, form.toSource(source.id))
    }

    @Test
    fun aSippedSourceRoundTripsThroughTheForm() {
        val source =
            Source(
                id = 4,
                name = "Green tea",
                caffeineMgPerServing = 30.0,
                theanineMgPerServing = 20.0,
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(90),
                usualTime = LocalTime.of(7, 30),
            )
        val form = source.toForm()
        assertEquals("90", form.durationMinutes)
        assertEquals(source, form.toSource(source.id))
    }
}

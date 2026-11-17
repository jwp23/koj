package io.github.jwp23.koj.form

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextTest {
    @Test
    fun aTimeIsWrittenWithTwoDigitsEach() {
        assertEquals("07:05", formatTime(LocalTime.of(7, 5)))
        assertEquals("23:00", formatTime(LocalTime.of(23, 0)))
    }

    @Test
    fun aDecimalIsParsed() {
        assertEquals(2.5, parseDecimal(" 2.5 ")!!, 0.0)
        assertEquals(0.0, parseDecimal("0")!!, 0.0)
        assertEquals(30.0, parseDecimal("30")!!, 0.0)
    }

    @Test
    fun textThatIsNotADecimalIsRejected() {
        for (text in listOf("", ".", "1.2.3", "1e3", "Infinity", "NaN", "-1", "abc", "1,5")) {
            assertNull(text, parseDecimal(text))
        }
    }

    @Test
    fun aNumberTooBigForADoubleIsRejected() {
        assertNull(parseDecimal("9".repeat(309)))
    }

    @Test
    fun aWholeDecimalIsWrittenWithoutAFraction() {
        assertEquals("5", formatDecimal(5.0))
        assertEquals("0", formatDecimal(0.0))
    }

    @Test
    fun aFractionalDecimalKeepsItsDigits() {
        assertEquals("0.25", formatDecimal(0.25))
        assertEquals("2.5", formatDecimal(2.5))
    }

    @Test
    fun milligramsAreRoundedHalfUp() {
        assertEquals("71 mg", formatMg(71.09))
        assertEquals("0 mg", formatMg(0.4))
        assertEquals("1 mg", formatMg(0.5))
        assertEquals("71 mg", formatMg(71.49))
    }

    @Test
    fun aRatioHasTwoDecimals() {
        assertEquals("0.67", formatRatio(2.0 / 3.0))
    }

    @Test
    fun noRatioIsADash() {
        assertEquals("–", formatRatio(null))
    }
}

package io.github.jwp23.koj.form

import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.PromptKind
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckInFormTest {
    private val stored =
        CheckIn(
            id = 7,
            at = Instant.parse("2026-03-10T11:00:00Z"),
            zone = ZoneId.of("Pacific/Auckland"),
            awake = 4,
            calm = 2,
            kind = PromptKind.AFTER_DRINK,
        )

    @Test
    fun aCheckInStartsAsItsRatings() {
        assertEquals(CheckInForm("4", "", "2"), stored.toForm())
    }

    @Test
    fun ratingsFromOneToFiveOrNothingAreValid() {
        for (rating in listOf("1", "2", "3", "4", "5", " 3 ")) {
            assertEquals(emptySet<CheckInField>(), CheckInForm(awake = rating).errors())
        }
        assertEquals(emptySet<CheckInField>(), CheckInForm("1", "5", "").errors())
    }

    @Test
    fun aRatingOutsideOneToFiveOrNotAWholeNumberIsAnErrorForItsField() {
        for (text in listOf("0", "6", "3.5", "x", "-1", "+3", "99999999999999999999", "٣")) {
            assertEquals(
                text,
                setOf(CheckInField.AWAKE),
                CheckInForm(awake = text, focus = "3").errors(),
            )
            assertEquals(
                text,
                setOf(CheckInField.FOCUS),
                CheckInForm(awake = "3", focus = text).errors(),
            )
            assertEquals(
                text,
                setOf(CheckInField.CALM),
                CheckInForm(awake = "3", calm = text).errors(),
            )
        }
    }

    @Test
    fun aCheckInNeedsAtLeastOneRating() {
        assertEquals(setOf(CheckInField.NO_RATING), CheckInForm().errors())
        assertEquals(setOf(CheckInField.NO_RATING), CheckInForm(" ", " ", " ").errors())
    }

    @Test
    fun aBadRatingAloneDoesNotAlsoSayNoRating() {
        assertEquals(setOf(CheckInField.AWAKE), CheckInForm(awake = "9").errors())
    }

    @Test
    fun applyingKeepsTheRecordAndSetsTheRatings() {
        val changed = CheckInForm("", "3", "5").applyTo(stored)
        assertEquals(stored.copy(awake = null, focus = 3, calm = 5), changed)
    }
}

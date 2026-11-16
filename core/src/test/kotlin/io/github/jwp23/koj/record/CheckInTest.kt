package io.github.jwp23.koj.record

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertThrows
import org.junit.Test

class CheckInTest {
    private fun checkIn(awake: Int? = null, focus: Int? = null, calm: Int? = null) =
        CheckIn(
            at = Instant.parse("2026-03-10T10:00:00Z"),
            zone = ZoneId.of("UTC"),
            awake = awake,
            focus = focus,
            calm = calm,
            kind = PromptKind.AFTER_DRINK,
        )

    @Test
    fun awakeRatingBelowOneIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { checkIn(awake = 0) }
    }

    @Test
    fun awakeRatingAboveFiveIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { checkIn(awake = 6) }
    }

    @Test
    fun focusRatingBelowOneIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { checkIn(focus = 0) }
    }

    @Test
    fun focusRatingAboveFiveIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { checkIn(focus = 6) }
    }

    @Test
    fun calmRatingBelowOneIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { checkIn(calm = 0) }
    }

    @Test
    fun calmRatingAboveFiveIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { checkIn(calm = 6) }
    }

    @Test
    fun aCheckInWithNoRatingsIsRejectedBecauseAnUnansweredPromptRecordsNothing() {
        assertThrows(IllegalArgumentException::class.java) { checkIn() }
    }

    @Test
    fun oneRatingWithTheOtherTwoLeftOutIsAccepted() {
        checkIn(awake = 3)
        checkIn(focus = 1)
        checkIn(calm = 5)
    }
}

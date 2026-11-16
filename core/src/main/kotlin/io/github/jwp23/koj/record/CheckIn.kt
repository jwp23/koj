package io.github.jwp23.koj.record

import java.time.Instant
import java.time.ZoneId

/** What caused a check-in prompt. */
enum class PromptKind {
    MORNING,
    WAKE_UP_PING,
    AFTER_DRINK,
    FIXED_TIME,
}

/**
 * One answered check-in: how awake, focused, and calm the user felt at [at], each from 1 to 5.
 *
 * A rating may be left null, but at least one must be given: an unanswered prompt records nothing.
 * [zone] is the time zone the check-in was answered in.
 */
data class CheckIn(
    val id: Long = 0,
    val at: Instant,
    val zone: ZoneId,
    val awake: Int? = null,
    val focus: Int? = null,
    val calm: Int? = null,
    val kind: PromptKind,
) {
    init {
        require(awake == null || awake in RATINGS) { "Awake rating must be 1 to 5, was $awake" }
        require(focus == null || focus in RATINGS) { "Focus rating must be 1 to 5, was $focus" }
        require(calm == null || calm in RATINGS) { "Calm rating must be 1 to 5, was $calm" }
        require(awake != null || focus != null || calm != null) {
            "A check-in needs at least one rating"
        }
    }

    private companion object {
        val RATINGS = 1..5
    }
}

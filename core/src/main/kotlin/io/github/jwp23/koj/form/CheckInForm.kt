package io.github.jwp23.koj.form

import io.github.jwp23.koj.record.CheckIn

/** The fields of [CheckInForm] that can be wrong. */
enum class CheckInField {
    AWAKE,
    FOCUS,
    CALM,
    NO_RATING,
}

private val ratingPattern = Regex("[1-5]")

/**
 * What the user entered about one check-in. Each rating is text as typed: empty, or a whole number
 * from 1 to 5. At least one rating is required.
 */
data class CheckInForm(val awake: String = "", val focus: String = "", val calm: String = "") {
    /** The fields that cannot be saved as entered. Empty when the form is valid. */
    fun errors(): Set<CheckInField> = buildSet {
        if (isBad(awake)) add(CheckInField.AWAKE)
        if (isBad(focus)) add(CheckInField.FOCUS)
        if (isBad(calm)) add(CheckInField.CALM)
        if (isEmpty() && listOf(awake, focus, calm).all { it.isBlank() }) {
            add(CheckInField.NO_RATING)
        }
    }

    /** [checkIn] with the ratings typed here. Call only when [errors] is empty. */
    fun applyTo(checkIn: CheckIn): CheckIn =
        checkIn.copy(awake = rating(awake), focus = rating(focus), calm = rating(calm))

    private fun isBad(text: String): Boolean =
        text.isNotBlank() && !ratingPattern.matches(text.trim())

    private fun rating(text: String): Int? = text.trim().toIntOrNull()
}

/** The form for editing this check-in. */
fun CheckIn.toForm(): CheckInForm =
    CheckInForm(
        awake?.toString().orEmpty(),
        focus?.toString().orEmpty(),
        calm?.toString().orEmpty(),
    )

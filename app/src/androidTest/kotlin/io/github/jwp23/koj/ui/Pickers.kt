package io.github.jwp23.koj.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement

/**
 * Opens the time picker behind the button reading [button] and enters [hour] and [minute] with the
 * keyboard. The descriptions are Material's own English strings; the one that switches to keyboard
 * entry really is worded "Switch to clock mode".
 */
fun SemanticsNodeInteractionsProvider.pickTime(button: String, hour: String, minute: String) {
    onNodeWithText(button).performClick()
    onNodeWithContentDescription("Switch to clock mode for the time input").performClick()
    onNodeWithContentDescription("for hour").performTextReplacement(hour)
    onNodeWithContentDescription("for minutes").performTextReplacement(minute)
    onNodeWithText("OK").performClick()
}

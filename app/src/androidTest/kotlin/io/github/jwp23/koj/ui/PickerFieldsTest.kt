package io.github.jwp23.koj.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PickerFieldsTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(label: String, start: LocalTime?) {
        composeRule.setContent {
            var time by remember { mutableStateOf(start) }
            TimeField(label, time, onTimeChange = { time = it })
        }
    }

    @Test
    fun aTimeIsPickedOnTheClock() {
        show("Start time", LocalTime.of(8, 30))
        composeRule.onNodeWithText("Start time: 08:30").performClick()
        composeRule.onNodeWithContentDescription("9 hours").performClick()
        composeRule.onNodeWithContentDescription("Select minutes").performClick()
        composeRule.onNodeWithContentDescription("45 minutes").performClick()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithText("Start time: 09:45").assertIsDisplayed()
    }

    @Test
    fun aTimeIsTyped() {
        show("Start time", LocalTime.of(8, 30))
        composeRule.pickTime("Start time: 08:30", "14", "07")
        composeRule.onNodeWithText("Start time: 14:07").assertIsDisplayed()
    }

    @Test
    fun anUnsetTimeIsPicked() {
        show("Usual time", null)
        composeRule.pickTime("Usual time", "07", "30")
        composeRule.onNodeWithText("Usual time: 07:30").assertIsDisplayed()
    }

    @Test
    fun cancellingKeepsTheTime() {
        show("Start time", LocalTime.of(8, 30))
        composeRule.onNodeWithText("Start time: 08:30").performClick()
        composeRule.onNodeWithContentDescription("9 hours").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Start time: 08:30").assertIsDisplayed()
    }

    private fun showDate(label: String, start: LocalDate?) {
        composeRule.setContent {
            var date by remember { mutableStateOf(start) }
            DateField(label, date, onDateChange = { date = it })
        }
    }

    @Test
    fun aDateIsPicked() {
        showDate("Start date", LocalDate.of(2026, 3, 10))
        composeRule.pickDate("Start date: 2026-03-10", "Monday, March 9, 2026")
        composeRule.onNodeWithText("Start date: 2026-03-09").assertIsDisplayed()
    }

    @Test
    fun okWithNoDateChosenLeavesItUnset() {
        showDate("Finish date", null)
        composeRule.onNodeWithText("Finish date").performClick()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithText("Finish date").assertIsDisplayed()
    }
}

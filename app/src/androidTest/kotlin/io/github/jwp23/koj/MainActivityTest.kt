package io.github.jwp23.koj

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    private fun heading(text: String) = hasText(text) and isHeading()

    private fun barItem(text: String) = hasText(text) and isSelectable()

    private fun pressBack() {
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    }

    @Test
    fun opensOnToday() {
        composeRule.onNode(heading("Today")).assertIsDisplayed()
        composeRule.onNode(barItem("Today")).assertIsSelected()
    }

    @Test
    fun eachBarDestinationShowsItsScreen() {
        for (name in listOf("History", "Sources", "Settings")) {
            composeRule.onNode(barItem(name)).performClick()
            composeRule.onNode(heading(name)).assertIsDisplayed()
            composeRule.onNode(barItem(name)).assertIsSelected()
        }
    }

    @Test
    fun backFromABarDestinationReturnsToToday() {
        composeRule.onNode(barItem("Sources")).performClick()
        pressBack()
        composeRule.onNode(heading("Today")).assertIsDisplayed()
    }

    @Test
    fun choosingAnotherDestinationDoesNotStackThem() {
        composeRule.onNode(barItem("Sources")).performClick()
        composeRule.onNode(barItem("History")).performClick()
        pressBack()
        composeRule.onNode(heading("Today")).assertIsDisplayed()
    }

    @Test
    fun theBarStaysInsideTheSystemBars() {
        val bar = composeRule.onNode(barItem("Today")).getUnclippedBoundsInRoot()
        val title = composeRule.onNode(heading("Today")).getUnclippedBoundsInRoot()
        assertTrue("bar top ${bar.top}", bar.top > 0.dp)
        assertTrue("heading top ${title.top}", title.top > 0.dp)
    }
}

package io.github.jwp23.koj

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
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

    /**
     * Presses back once the screen with heading [screen] is showing. NavDisplay enables its back
     * handler in a side effect of the composition that shows a screen above Today, and this rule's
     * clock composes only when the test waits on the UI, as asserting the heading does. Pressed
     * before that composition, back finds the handler as the last composition left it: disabled
     * after Today alone, so the activity leaves the app.
     */
    private fun pressBackFrom(screen: String) {
        composeRule.onNode(heading(screen)).assertIsDisplayed()
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
        pressBackFrom("Sources")
        composeRule.onNode(heading("Today")).assertIsDisplayed()
    }

    @Test
    fun choosingAnotherDestinationDoesNotStackThem() {
        composeRule.onNode(barItem("Sources")).performClick()
        composeRule.onNode(barItem("History")).performClick()
        pressBackFrom("History")
        composeRule.onNode(heading("Today")).assertIsDisplayed()
    }

    @Test
    fun theBarStaysInsideTheSystemBars() {
        composeRule.waitForIdle()
        lateinit var insets: Insets
        lateinit var density: Density
        composeRule.runOnUiThread {
            val view = composeRule.activity.window.decorView
            insets = checkNotNull(ViewCompat.getRootWindowInsets(view)).getInsets(systemBars())
            density = Density(view.resources.displayMetrics.density)
        }
        val topInset = with(density) { insets.top.toDp() }
        val bottomInset = with(density) { insets.bottom.toDp() }
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val bar = composeRule.onNode(barItem("Today")).getUnclippedBoundsInRoot()
        val title = composeRule.onNode(heading("Today")).getUnclippedBoundsInRoot()
        assertTrue("heading top ${title.top} above inset $topInset", title.top >= topInset)
        assertTrue(
            "bar bottom ${bar.bottom} below ${root.bottom - bottomInset}",
            bar.bottom <= root.bottom - bottomInset,
        )
    }
}

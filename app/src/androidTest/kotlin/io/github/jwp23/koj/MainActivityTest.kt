package io.github.jwp23.koj

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    /**
     * Starts each test with every table empty. The file cannot be deleted instead: the application
     * keeps the database open for the whole run.
     */
    @get:Rule(order = 0)
    val emptyDatabase =
        object : ExternalResource() {
            override fun before() {
                val container =
                    ApplicationProvider.getApplicationContext<KojApplication>().container
                runBlocking { container.database.clearAllTables() }
            }
        }

    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

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
    fun eachTestStartsWithAnEmptyDatabase() {
        val container = ApplicationProvider.getApplicationContext<KojApplication>().container
        runBlocking {
            assertTrue(container.drinks.drinks().first().isEmpty())
            assertTrue(container.sources.sources().first().isEmpty())
        }
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

    /** Adds a source from an empty list, which fails when an earlier test's source was kept. */
    private fun addSourceToAnEmptyList(name: String) {
        composeRule.onNode(barItem("Sources")).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("No sources yet.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Add source").performClick()
        composeRule.onNodeWithText("Name").performTextInput(name)
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(heading("Sources")).assertIsDisplayed()
        composeRule.onNodeWithText(name).assertIsDisplayed()
    }

    @Test fun aSourceCanBeAddedFromTheSourcesTab() = addSourceToAnEmptyList("Mate")

    @Test fun aSourceAddedByAnotherTestIsNotListed() = addSourceToAnEmptyList("Chai")

    @Test
    fun settingsAreReachedFromTheSettingsTab() {
        composeRule.onNode(barItem("Settings")).performClick()
        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodesWithText("Caffeine half-life (hours)")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Caffeine half-life (hours)").assertIsDisplayed()
    }

    @Test
    fun aSourceAddedInSourcesCanBeLoggedFromToday() {
        composeRule.onNode(barItem("Sources")).performClick()
        composeRule.onNodeWithText("Add source").performClick()
        composeRule.onNodeWithText("Name").performTextInput("Mate")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Mate").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(barItem("Today")).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Mate").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mate").performClick()
        val container = ApplicationProvider.getApplicationContext<KojApplication>().container
        composeRule.waitUntil(5_000) {
            runBlocking { container.drinks.drinks().first().isNotEmpty() }
        }
        val logged = runBlocking { container.drinks.drinks().first() }
        assertEquals(listOf("Mate"), logged.map { it.sourceName })
        composeRule.onNodeWithText("Mate").assertIsDisplayed()
    }

    @Test
    fun optionsOpensTheLogScreenAndReturns() {
        composeRule.onNode(barItem("Sources")).performClick()
        composeRule.onNodeWithText("Add source").performClick()
        composeRule.onNodeWithText("Name").performTextInput("Mate")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Mate").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(barItem("Today")).performClick()
        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodesWithContentDescription("Log Mate with options")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Log Mate with options").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(heading("Log Mate")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Log").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(heading("Today")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(heading("Today")).assertIsDisplayed()
    }

    private fun waitForText(text: String, substring: Boolean = false) =
        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodesWithText(text, substring = substring)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

    private fun addSource(name: String) {
        composeRule.onNode(barItem("Sources")).performClick()
        composeRule.onNodeWithText("Add source").performClick()
        composeRule.onNodeWithText("Name").performTextInput(name)
        composeRule.onNodeWithText("Save").performClick()
        waitForText(name)
    }

    /** Adds a source, logs one serving of it from Today, and opens its row in History. */
    private fun openLoggedDrinkInHistory(name: String) {
        addSource(name)
        composeRule.onNode(barItem("Today")).performClick()
        waitForText(name)
        composeRule.onNodeWithText(name).performClick()
        composeRule.onNode(barItem("History")).performClick()
        val row = hasContentDescription("Edit $name at", substring = true)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(row).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(row).performClick()
        waitForText("Edit $name")
    }

    @Test
    fun aLoggedDrinkCanBeCorrectedFromHistory() {
        openLoggedDrinkInHistory("Mate")
        composeRule.onNode(hasSetTextAction() and hasText("Servings")).performTextReplacement("2")
        composeRule.onNodeWithText("Save").performClick()
        waitForText("Mate × 2", substring = true)
        composeRule.onNode(heading("History")).assertIsDisplayed()
    }

    @Test
    fun aLoggedDrinkCanBeDeletedFromHistory() {
        openLoggedDrinkInHistory("Mate")
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        waitForText("Nothing logged yet.")
    }
}

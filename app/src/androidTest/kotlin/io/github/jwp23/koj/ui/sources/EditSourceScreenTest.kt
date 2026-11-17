package io.github.jwp23.koj.ui.sources

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.form.SourceForm
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.KojDatabase
import io.github.jwp23.koj.storage.SourceStore
import io.github.jwp23.koj.ui.pickTime
import java.time.Duration
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditSourceScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(
                ApplicationProvider.getApplicationContext<Context>(),
            )
            .setDriver(AndroidSQLiteDriver())
            .build()
    private val store = SourceStore(database.sourceDao())
    private var done = false

    @After fun closeDatabase() = database.close()

    private fun show(sourceId: Long? = null) {
        val model = EditSourceViewModel(store, sourceId)
        composeRule.setContent { EditSourceScreen(model, onDone = { done = true }) }
    }

    private fun stored(): List<Source> = runBlocking { store.sources().first() }

    private fun storeGreenTea(usualTime: LocalTime? = null): Long = runBlocking {
        store.save(
            Source(
                name = "Green tea",
                caffeineMgPerServing = 30.0,
                theanineMgPerServing = 20.0,
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(90),
                usualTime = usualTime,
            ),
        )
    }

    private fun waitFor(condition: () -> Boolean) = composeRule.waitUntil(5_000, condition)

    private fun waitForText(text: String) = waitFor {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun type(label: String, text: String) =
        composeRule.onNodeWithText(label).performTextInput(text)

    private fun dialogButton(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    @Test
    fun aNewSourceIsSaved() {
        show()
        type("Name", "Espresso")
        type("Caffeine per serving (mg)", "63")
        composeRule.onNodeWithText("Save").performClick()
        waitFor { done }
        val source = stored().single()
        assertEquals("Espresso", source.name)
        assertEquals(IntakeStyle.FAST, source.style)
        assertEquals(63.0, source.caffeineMgPerServing, 0.0)
        assertEquals(0.0, source.theanineMgPerServing, 0.0)
    }

    @Test
    fun aBadEntryIsExplainedAndNotSaved() {
        show()
        type("Caffeine per serving (mg)", "abc")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Enter a name").assertIsDisplayed()
        composeRule.onNodeWithText("Enter a number, 0 or more").assertIsDisplayed()
        assertTrue(!done)
        assertTrue(stored().isEmpty())
    }

    @Test
    fun choosingSippedAsksForASippingTime() {
        show()
        composeRule.onAllNodesWithText("Sipping time (minutes)").assertCountEquals(0)
        composeRule.onNodeWithText("Sipped").performClick()
        composeRule.onNodeWithText("Sipping time (minutes)").assertIsDisplayed()
        type("Name", "Tea")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Enter whole minutes, 1 to 1440").assertIsDisplayed()
        assertTrue(stored().isEmpty())
    }

    @Test
    fun anExistingSourceIsShownAndChanged() {
        val id = storeGreenTea()
        show(id)
        waitForText("Green tea")
        composeRule.onNodeWithText("30").assertIsDisplayed()
        composeRule.onNodeWithText("20").assertIsDisplayed()
        composeRule.onNodeWithText("90").assertIsDisplayed()
        composeRule.onNodeWithText("Green tea").performTextReplacement("Sencha")
        composeRule.onNodeWithText("Save").performClick()
        waitFor { done }
        val source = stored().single()
        assertEquals(id, source.id)
        assertEquals("Sencha", source.name)
    }

    @Test
    fun deletingAsksFirst() {
        storeGreenTea()
        show(stored().single().id)
        waitForText("Green tea")
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Delete Green tea?").assertIsDisplayed()
        composeRule.onNode(dialogButton("Cancel")).performClick()
        assertEquals(1, stored().size)
        assertTrue(!done)
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNode(dialogButton("Delete")).performClick()
        waitFor { done }
        assertTrue(stored().isEmpty())
    }

    @Test
    fun aUsualTimeIsPickedAndSaved() {
        show()
        type("Name", "Espresso")
        composeRule.pickTime("Usual time", "07", "30")
        composeRule.onNodeWithText("Usual time: 07:30").assertIsDisplayed()
        composeRule.onNodeWithText("Save").performClick()
        waitFor { done }
        assertEquals(LocalTime.of(7, 30), stored().single().usualTime)
    }

    @Test
    fun aUsualTimeIsCleared() {
        val id = storeGreenTea(usualTime = LocalTime.of(7, 30))
        show(id)
        waitForText("Usual time: 07:30")
        composeRule.onNodeWithText("Clear usual time").performClick()
        composeRule.onNodeWithText("Usual time").assertIsDisplayed()
        composeRule.onAllNodesWithText("Clear usual time").assertCountEquals(0)
        composeRule.onNodeWithText("Save").performClick()
        waitFor { done }
        assertNull(stored().single().usualTime)
    }

    @Test
    fun aSourceThatIsNotThereIsSaidToBeUnavailableWithAWayBack() {
        show(sourceId = 999)
        waitForText("This source is no longer available.")
        composeRule.onNodeWithText("Edit source").assertIsDisplayed()
        composeRule.onAllNodesWithText("New source").assertCountEquals(0)
        composeRule.onAllNodesWithText("Name").assertCountEquals(0)
        composeRule.onAllNodesWithText("Save").assertCountEquals(0)
        composeRule.onNodeWithText("Back to Sources").performClick()
        assertTrue(done)
    }

    @Test
    fun savingTwiceBeforeTheFirstSaveFinishesStoresOneSource() {
        val model = EditSourceViewModel(store, null)
        model.change(SourceForm(name = "Espresso"))
        var finished = 0
        composeRule.runOnUiThread {
            model.save { finished++ }
            model.save { finished++ }
        }
        waitFor { finished > 0 }
        composeRule.waitForIdle()
        assertEquals(1, finished)
        assertEquals(1, stored().size)
    }

    @Test
    fun anExistingSourceIsCalledEditOnceLoaded() {
        val id = storeGreenTea()
        show(id)
        waitForText("Green tea")
        composeRule.onNodeWithText("Edit source").assertIsDisplayed()
        composeRule.onAllNodesWithText("New source").assertCountEquals(0)
    }

    @Test
    fun aNewSourceIsCalledNew() {
        show()
        composeRule.onNodeWithText("New source").assertIsDisplayed()
    }
}

package io.github.jwp23.koj.ui.history

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.PromptKind
import io.github.jwp23.koj.storage.CheckInStore
import io.github.jwp23.koj.storage.KojDatabase
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditCheckInScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(
                ApplicationProvider.getApplicationContext<Context>(),
            )
            .setDriver(AndroidSQLiteDriver())
            .build()
    private val checkInStore = CheckInStore(database.checkInDao())
    private var done = false

    @After fun closeDatabase() = database.close()

    private fun saveCheckIn(): CheckIn = runBlocking {
        val checkIn =
            CheckIn(
                at = Instant.parse("2026-03-10T11:00:00Z"),
                zone = ZoneId.of("Pacific/Auckland"),
                awake = 4,
                calm = 2,
                kind = PromptKind.AFTER_DRINK,
            )
        checkIn.copy(id = checkInStore.save(checkIn))
    }

    private fun show(checkInId: Long) {
        val model = EditCheckInViewModel(checkInStore, checkInId)
        composeRule.setContent { EditCheckInScreen(model, onDone = { done = true }) }
    }

    private fun stored(): List<CheckIn> = runBlocking { checkInStore.checkIns().first() }

    private fun waitFor(condition: () -> Boolean) = composeRule.waitUntil(5_000, condition)

    private fun waitForText(text: String) = waitFor {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun field(label: String) = composeRule.onNode(hasSetTextAction() and hasText(label))

    private fun save() = composeRule.onNodeWithText("Save").performClick()

    private fun dialogButton(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    @Test
    fun ratingsAreShownAndChanged() {
        val checkIn = saveCheckIn()
        show(checkIn.id)
        waitForText("Awake (1 to 5)")
        field("Awake (1 to 5)").assertTextEquals("Awake (1 to 5)", "4")
        field("Focus (1 to 5)").assertTextEquals("Focus (1 to 5)", "")
        field("Calm (1 to 5)").assertTextEquals("Calm (1 to 5)", "2")
        field("Focus (1 to 5)").performTextReplacement("3")
        save()
        waitFor { done }
        assertEquals(checkIn.copy(focus = 3), stored().single())
    }

    @Test
    fun aCheckInThatIsNotThereSaysSoInsteadOfLoadingForever() {
        show(999)
        waitForText("This check-in is no longer available.")
        composeRule.onNodeWithText("Edit check-in").assertIsDisplayed()
        composeRule.onAllNodesWithText("Awake (1 to 5)").assertCountEquals(0)
        composeRule.onNodeWithText("Back to History").performClick()
        waitFor { done }
    }

    @Test
    fun aRatingOutOfRangeIsRefused() {
        val checkIn = saveCheckIn()
        show(checkIn.id)
        waitForText("Awake (1 to 5)")
        field("Awake (1 to 5)").performTextReplacement("6")
        save()
        composeRule
            .onNodeWithText("Enter a whole number from 1 to 5, or leave it empty")
            .assertIsDisplayed()
        assertTrue(!done)
        assertEquals(checkIn, stored().single())
    }

    @Test
    fun clearingEveryRatingIsRefused() {
        val checkIn = saveCheckIn()
        show(checkIn.id)
        waitForText("Awake (1 to 5)")
        field("Awake (1 to 5)").performTextReplacement("")
        field("Calm (1 to 5)").performTextReplacement("")
        save()
        composeRule.onNodeWithText("Enter at least one rating").assertIsDisplayed()
        assertEquals(checkIn, stored().single())
    }

    @Test
    fun deletingRemovesIt() {
        show(saveCheckIn().id)
        waitForText("Awake (1 to 5)")
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Delete this check-in?").assertIsDisplayed()
        composeRule.onNode(dialogButton("Cancel")).performClick()
        assertEquals(1, stored().size)
        assertTrue(!done)
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNode(dialogButton("Delete")).performClick()
        waitFor { done }
        assertTrue(stored().isEmpty())
    }

    private fun loadedModel(checkIn: CheckIn): EditCheckInViewModel {
        val model = EditCheckInViewModel(checkInStore, checkIn.id)
        composeRule.waitUntil(5_000) { model.state.value is EditCheckInState.Editing }
        return model
    }

    @Test
    fun savingTwiceBeforeTheFirstSaveFinishesWritesOnce() {
        val model = loadedModel(saveCheckIn())
        var finished = 0
        composeRule.runOnUiThread {
            model.save { finished++ }
            model.save { finished++ }
        }
        waitFor { finished > 0 }
        composeRule.waitForIdle()
        assertEquals(1, finished)
    }

    @Test
    fun anEditMadeWhileASaveIsBeingWrittenIsNotTaken() {
        val model = loadedModel(saveCheckIn())
        val written = (model.state.value as EditCheckInState.Editing).form
        composeRule.runOnUiThread {
            model.save {}
            model.change(written.copy(focus = "3"))
        }
        assertEquals(written, (model.state.value as EditCheckInState.Editing).form)
        composeRule.waitForIdle()
    }

    @Test
    fun deletingTwiceBeforeTheFirstDeleteFinishesFinishesOnce() {
        val model = loadedModel(saveCheckIn())
        var finished = 0
        composeRule.runOnUiThread {
            model.delete { finished++ }
            model.delete { finished++ }
        }
        waitFor { finished > 0 }
        composeRule.waitForIdle()
        assertEquals(1, finished)
        assertTrue(stored().isEmpty())
    }

    @Test
    fun savingACheckInDeletedElsewhereSaysItIsNoLongerAvailable() {
        val checkIn = saveCheckIn()
        val model = loadedModel(checkIn)
        runBlocking { checkInStore.delete(checkIn) }
        composeRule.setContent { EditCheckInScreen(model, onDone = { done = true }) }
        composeRule.onNodeWithText("Save").performClick()
        waitForText("This check-in is no longer available.")
        assertTrue(!done)
        composeRule.onNodeWithText("Back to History").performClick()
        assertTrue(done)
    }
}

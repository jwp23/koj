package io.github.jwp23.koj.ui.history

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.DrinkStore
import io.github.jwp23.koj.storage.KojDatabase
import io.github.jwp23.koj.storage.SourceStore
import io.github.jwp23.koj.testing.MutableClock
import io.github.jwp23.koj.testing.espresso
import io.github.jwp23.koj.testing.greenTea
import io.github.jwp23.koj.ui.pickTime
import java.time.Instant
import java.time.ZoneOffset
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
class EditDrinkScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(
                ApplicationProvider.getApplicationContext<Context>(),
            )
            .setDriver(AndroidSQLiteDriver())
            .build()
    private val sourceStore = SourceStore(database.sourceDao())
    private val drinkStore = DrinkStore(database.drinkDao())
    private val clock = MutableClock(Instant.parse("2026-03-10T14:00:00Z"))
    private var done = false

    @After fun closeDatabase() = database.close()

    private fun saveDrink(source: Source, start: String, finish: String? = null): LoggedDrink =
        runBlocking {
            val saved = source.copy(id = sourceStore.save(source))
            val drink = saved.logged(1.0, Instant.parse(start), ZoneOffset.UTC)
            val withFinish = drink.copy(finish = finish?.let(Instant::parse) ?: drink.finish)
            withFinish.copy(id = drinkStore.save(withFinish))
        }

    private fun show(drinkId: Long) {
        val model = EditDrinkViewModel(drinkStore, clock, drinkId)
        composeRule.setContent { EditDrinkScreen(model, onDone = { done = true }) }
    }

    private fun stored(): List<LoggedDrink> = runBlocking { drinkStore.drinks().first() }

    private fun waitFor(condition: () -> Boolean) = composeRule.waitUntil(5_000, condition)

    private fun waitForText(text: String) = waitFor {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun servings() = composeRule.onNode(hasSetTextAction() and hasText("Servings"))

    private fun save() = composeRule.onNodeWithText("Save").performClick()

    private fun dialogButton(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    @Test
    fun theDrinkIsShown() {
        show(saveDrink(greenTea, "2026-03-10T10:00:00Z", "2026-03-10T10:40:00Z").id)
        waitForText("Servings")
        composeRule.onNode(hasText("Edit drink") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Green tea").assertIsDisplayed()
        servings().assertTextEquals("Servings", "1")
        composeRule.onNodeWithText("Start date: 2026-03-10").assertIsDisplayed()
        composeRule.onNodeWithText("Start time: 10:00").assertIsDisplayed()
        composeRule.onNodeWithText("Finish date: 2026-03-10").assertIsDisplayed()
        composeRule.onNodeWithText("Finish time: 10:40").assertIsDisplayed()
    }

    @Test
    fun aDrinkThatIsNotThereSaysSoInsteadOfLoadingForever() {
        show(999)
        waitForText("This drink is no longer available.")
        composeRule.onNodeWithText("Edit drink").assertIsDisplayed()
        composeRule.onAllNodesWithText("Servings").assertCountEquals(0)
        composeRule.onNodeWithText("Back to History").performClick()
        waitFor { done }
    }

    @Test
    fun aChangedAmountAndStartAreSaved() {
        val drink = saveDrink(greenTea, "2026-03-10T10:00:00Z", "2026-03-10T10:40:00Z")
        show(drink.id)
        waitForText("Servings")
        servings().performTextReplacement("0.5")
        composeRule.pickTime("Start time: 10:00", "09", "30")
        save()
        waitFor { done }
        val changed = stored().single()
        assertEquals(0.5, changed.servings, 0.0)
        assertEquals(Instant.parse("2026-03-10T09:30:00Z"), changed.start)
        assertEquals(Instant.parse("2026-03-10T10:40:00Z"), changed.finish)
        assertEquals(30.0, changed.caffeineMgPerServing, 0.0)
        assertEquals(ZoneOffset.UTC, changed.zone)
    }

    @Test
    fun aFinishBeforeTheStartIsRefused() {
        val drink = saveDrink(greenTea, "2026-03-10T10:00:00Z", "2026-03-10T10:40:00Z")
        show(drink.id)
        waitForText("Servings")
        composeRule.pickTime("Finish time: 10:40", "09", "00")
        save()
        composeRule.onNodeWithText("The finish cannot be before the start").assertIsDisplayed()
        assertTrue(!done)
        assertEquals(drink, stored().single())
    }

    @Test
    fun aFinishInTheFutureIsRefused() {
        val drink = saveDrink(greenTea, "2026-03-10T10:00:00Z", "2026-03-10T10:40:00Z")
        show(drink.id)
        waitForText("Servings")
        composeRule.pickTime("Finish time: 10:40", "14", "01")
        save()
        composeRule.onNodeWithText("The finish cannot be in the future").assertIsDisplayed()
        assertEquals(drink, stored().single())
    }

    @Test
    fun aStartInTheFutureIsRefused() {
        val drink = saveDrink(espresso, "2026-03-10T09:00:00Z")
        show(drink.id)
        waitForText("Servings")
        composeRule.pickTime("Start time: 09:00", "14", "01")
        save()
        composeRule.onNodeWithText("The start cannot be in the future").assertIsDisplayed()
        assertEquals(drink, stored().single())
    }

    @Test
    fun aBadAmountIsRefusedWithTheLimit() {
        val drink = saveDrink(espresso, "2026-03-10T09:00:00Z")
        show(drink.id)
        waitForText("Servings")
        servings().performTextReplacement("0")
        save()
        composeRule.onNodeWithText("Enter a number above 0").assertIsDisplayed()
        servings().performTextReplacement("1001")
        save()
        composeRule.onNodeWithText("Enter at most 1000 servings").assertIsDisplayed()
        assertEquals(drink, stored().single())
    }

    @Test
    fun clearingTheFinishPutsASippedDrinkBackInProgress() {
        show(saveDrink(greenTea, "2026-03-10T10:00:00Z", "2026-03-10T10:40:00Z").id)
        waitForText("Servings")
        composeRule.onNodeWithText("Clear finish").performClick()
        composeRule.onNodeWithText("Finish date").assertIsDisplayed()
        composeRule.onNodeWithText("Finish time").assertIsDisplayed()
        composeRule.onAllNodesWithText("Clear finish").assertCountEquals(0)
        save()
        waitFor { done }
        assertNull(stored().single().finish)
    }

    @Test
    fun aFinishWithNoDateIsRefused() {
        val drink = saveDrink(greenTea, "2026-03-10T10:00:00Z")
        show(drink.id)
        waitForText("Servings")
        composeRule.pickTime("Finish time", "10", "40")
        save()
        composeRule.onNodeWithText("Choose a finish date").assertIsDisplayed()
        assertTrue(!done)
        assertEquals(drink, stored().single())
    }

    @Test
    fun aFastDrinkHasNoFinishFields() {
        show(saveDrink(espresso, "2026-03-10T09:00:00Z").id)
        waitForText("Servings")
        composeRule.onAllNodesWithText("Finish", substring = true).assertCountEquals(0)
        composeRule.pickTime("Start time: 09:00", "08", "00")
        save()
        waitFor { done }
        val changed = stored().single()
        assertEquals(Instant.parse("2026-03-10T08:00:00Z"), changed.start)
        assertEquals(changed.start, changed.finish)
    }

    @Test
    fun aFastDrinkMovedLaterStillFinishesWhereItStarts() {
        show(saveDrink(espresso, "2026-03-10T09:00:00Z").id)
        waitForText("Servings")
        composeRule.pickTime("Start time: 09:00", "13", "00")
        save()
        waitFor { done }
        val changed = stored().single()
        assertEquals(Instant.parse("2026-03-10T13:00:00Z"), changed.start)
        assertEquals(changed.start, changed.finish)
    }

    @Test
    fun deletingAsksFirst() {
        show(saveDrink(greenTea, "2026-03-10T10:00:00Z").id)
        waitForText("Servings")
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Delete this drink?").assertIsDisplayed()
        composeRule.onNode(dialogButton("Cancel")).performClick()
        assertEquals(1, stored().size)
        assertTrue(!done)
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNode(dialogButton("Delete")).performClick()
        waitFor { done }
        assertTrue(stored().isEmpty())
    }

    @Test
    fun theButtonsStayReachableBesideALongName() {
        val name = "Green tea ".repeat(60).trim()
        show(saveDrink(greenTea.copy(name = name), "2026-03-10T10:00:00Z").id)
        waitForText(name)
        composeRule.onNodeWithText("Save").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        save()
        waitFor { done }
    }

    private fun loadedModel(drink: LoggedDrink): EditDrinkViewModel {
        val model = EditDrinkViewModel(drinkStore, clock, drink.id)
        composeRule.waitUntil(5_000) { model.state.value is EditDrinkState.Editing }
        return model
    }

    @Test
    fun savingTwiceBeforeTheFirstSaveFinishesWritesOnce() {
        val model = loadedModel(saveDrink(espresso, "2026-03-10T09:00:00Z"))
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
    fun deletingTwiceBeforeTheFirstDeleteFinishesFinishesOnce() {
        val model = loadedModel(saveDrink(espresso, "2026-03-10T09:00:00Z"))
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
    fun savingADrinkDeletedElsewhereSaysItIsNoLongerAvailable() {
        val drink = saveDrink(espresso, "2026-03-10T09:00:00Z")
        val model = loadedModel(drink)
        runBlocking { drinkStore.delete(drink) }
        composeRule.setContent { EditDrinkScreen(model, onDone = { done = true }) }
        composeRule.onNodeWithText("Save").performClick()
        waitForText("This drink is no longer available.")
        assertTrue(!done)
        composeRule.onNodeWithText("Back to History").performClick()
        assertTrue(done)
    }
}

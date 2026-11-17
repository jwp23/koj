package io.github.jwp23.koj.ui.log

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.Settings
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.DrinkStore
import io.github.jwp23.koj.storage.KojDatabase
import io.github.jwp23.koj.storage.SettingsStore
import io.github.jwp23.koj.storage.SourceStore
import io.github.jwp23.koj.testing.MutableClock
import io.github.jwp23.koj.ui.pickDate
import io.github.jwp23.koj.ui.pickTime
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
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
class LogDrinkScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(
                ApplicationProvider.getApplicationContext<Context>(),
            )
            .setDriver(AndroidSQLiteDriver())
            .build()
    private val sourceStore = SourceStore(database.sourceDao())
    private val drinkStore = DrinkStore(database.drinkDao())
    private val settingsStore = SettingsStore(database.settingsDao())
    private val clock = MutableClock(Instant.parse("2026-03-10T10:00:00Z"))
    private var done = false

    @After fun closeDatabase() = database.close()

    private fun saveSource(name: String = "Green tea"): Long = runBlocking {
        sourceStore.save(
            Source(
                name = name,
                caffeineMgPerServing = 30.0,
                theanineMgPerServing = 20.0,
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(60),
            ),
        )
    }

    private fun show(sourceId: Long = saveSource()): LogDrinkViewModel {
        val model = LogDrinkViewModel(sourceStore, drinkStore, settingsStore, clock, sourceId)
        composeRule.setContent { LogDrinkScreen(model, onDone = { done = true }) }
        return model
    }

    private fun stored(): List<LoggedDrink> = runBlocking { drinkStore.drinks().first() }

    private fun waitForText(text: String) =
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    private fun servings() = composeRule.onNode(hasSetTextAction() and hasText("Servings"))

    private fun log() = composeRule.onNodeWithText("Log").performClick()

    @Test
    fun itStartsAtOneServingNow() {
        show()
        waitForText("Log Green tea")
        servings().assertTextEquals("Servings", "1")
        composeRule.onNodeWithText("Start date: 2026-03-10").assertIsDisplayed()
        composeRule.onNodeWithText("Start time: 10:00").assertIsDisplayed()
        composeRule.onNodeWithText("30 mg caffeine, 20 mg L-theanine").assertIsDisplayed()
    }

    @Test
    fun nothingIsShownToEditBeforeTheSourceIsRead() {
        show(sourceId = 999)
        composeRule.onNodeWithText("Log a drink").assertIsDisplayed()
        composeRule.onAllNodesWithText("Servings").assertCountEquals(0)
        composeRule.onAllNodesWithText("Start time: 10:00").assertCountEquals(0)
        composeRule.onAllNodesWithText("Just a sip").assertCountEquals(0)
    }

    @Test
    fun aDifferentAmountIsLogged() {
        show()
        waitForText("Log Green tea")
        servings().performTextReplacement("0.5")
        composeRule.onNodeWithText("15 mg caffeine, 10 mg L-theanine").assertIsDisplayed()
        log()
        composeRule.waitUntil(5_000) { done }
        val drink = stored().single()
        assertEquals(0.5, drink.servings, 0.0)
        assertEquals(Instant.parse("2026-03-10T10:00:00Z"), drink.start)
        assertNull(drink.finish)
    }

    @Test
    fun justASipUsesTheSipSetting() {
        show()
        waitForText("Log Green tea")
        composeRule.onNodeWithText("Just a sip").performClick()
        servings().assertTextEquals("Servings", "0.1")
        log()
        composeRule.waitUntil(5_000) { done }
        assertEquals(0.1, stored().single().servings, 0.0)
    }

    @Test
    fun justASipUsesTheSavedSipSetting() {
        runBlocking { settingsStore.save(Settings(sipServings = 0.25)) }
        show()
        waitForText("Log Green tea")
        composeRule.onNodeWithText("Just a sip").performClick()
        servings().assertTextEquals("Servings", "0.25")
    }

    @Test
    fun anEarlierStartIsLogged() {
        show()
        waitForText("Log Green tea")
        composeRule.pickTime("Start time: 10:00", "08", "30")
        composeRule.onNodeWithText("Start time: 08:30").assertIsDisplayed()
        log()
        composeRule.waitUntil(5_000) { done }
        assertEquals(Instant.parse("2026-03-10T08:30:00Z"), stored().single().start)
    }

    @Test
    fun anEarlierDayIsLogged() {
        show()
        waitForText("Log Green tea")
        composeRule.pickDate("Start date: 2026-03-10", "Monday, March 9, 2026")
        composeRule.onNodeWithText("Start date: 2026-03-09").assertIsDisplayed()
        log()
        composeRule.waitUntil(5_000) { done }
        assertEquals(Instant.parse("2026-03-09T10:00:00Z"), stored().single().start)
    }

    @Test
    fun aStartInTheFutureIsRefused() {
        show()
        waitForText("Log Green tea")
        composeRule.pickTime("Start time: 10:00", "10", "01")
        log()
        composeRule.onNodeWithText("The start cannot be in the future").assertIsDisplayed()
        assertTrue(!done)
        assertTrue(stored().isEmpty())
    }

    @Test
    fun aBadAmountIsRefused() {
        show()
        waitForText("Log Green tea")
        servings().performTextReplacement("0")
        composeRule.onAllNodesWithText("30 mg caffeine, 20 mg L-theanine").assertCountEquals(0)
        log()
        composeRule.onNodeWithText("Enter a number above 0").assertIsDisplayed()
        assertTrue(!done)
        assertTrue(stored().isEmpty())
    }

    @Test
    fun anAmountAboveTheLimitIsRefusedWithTheLimit() {
        show()
        waitForText("Log Green tea")
        servings().performTextReplacement("1001")
        composeRule.onAllNodesWithText("30 mg caffeine, 20 mg L-theanine").assertCountEquals(0)
        log()
        composeRule.onNodeWithText("Enter at most 1000 servings").assertIsDisplayed()
        composeRule.onAllNodesWithText("Enter a number above 0").assertCountEquals(0)
        assertTrue(stored().isEmpty())
    }

    @Test
    fun cancelLogsNothing() {
        show()
        waitForText("Log Green tea")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitUntil(5_000) { done }
        assertTrue(stored().isEmpty())
    }

    @Test
    fun theButtonsStayReachableBesideALongName() {
        val name = "Green tea ".repeat(60).trim()
        show(saveSource(name))
        waitForText("Log $name")
        composeRule.onNodeWithText("Log").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
        log()
        composeRule.waitUntil(5_000) { done }
        assertEquals(1, stored().size)
    }

    @Test
    fun tappingLogTwiceLogsOneDrink() {
        val model = show()
        waitForText("Log Green tea")
        var finished = 0
        composeRule.runOnUiThread {
            model.log { finished++ }
            model.log { finished++ }
        }
        composeRule.waitUntil(5_000) { finished > 0 }
        composeRule.waitForIdle()
        assertEquals(1, stored().size)
        assertEquals(1, finished)
    }

    @Test
    fun theFormKeepsTheZoneItWasOpenedIn() {
        show()
        waitForText("Log Green tea")
        clock.moveTo(ZoneId.of("Pacific/Auckland"))
        log()
        composeRule.waitUntil(5_000) { done }
        val drink = stored().single()
        assertEquals(Instant.parse("2026-03-10T10:00:00Z"), drink.start)
        assertEquals(ZoneOffset.UTC, drink.zone)
    }
}

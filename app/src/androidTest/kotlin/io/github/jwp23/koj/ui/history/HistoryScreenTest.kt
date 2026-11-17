package io.github.jwp23.koj.ui.history

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.intake.Kinetics
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.PromptKind
import io.github.jwp23.koj.record.Settings
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.CheckInStore
import io.github.jwp23.koj.storage.DrinkStore
import io.github.jwp23.koj.storage.KojDatabase
import io.github.jwp23.koj.storage.SettingsStore
import io.github.jwp23.koj.storage.SourceStore
import io.github.jwp23.koj.testing.MutableClock
import io.github.jwp23.koj.testing.espresso
import io.github.jwp23.koj.testing.greenTea
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(
                ApplicationProvider.getApplicationContext<Context>(),
            )
            .setDriver(AndroidSQLiteDriver())
            .build()
    private val sourceStore = SourceStore(database.sourceDao())
    private val drinkStore = DrinkStore(database.drinkDao())
    private val checkInStore = CheckInStore(database.checkInDao())
    private val settingsStore = SettingsStore(database.settingsDao())
    private val clock = MutableClock(Instant.parse("2026-03-10T14:00:00Z"))
    private val ticks = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }
    private val savedSources = mutableMapOf<String, Source>()
    private var editedDrink: Long? = null
    private var editedCheckIn: Long? = null

    @After fun closeDatabase() = database.close()

    private fun show(): HistoryViewModel {
        val model = HistoryViewModel(drinkStore, checkInStore, settingsStore, clock, ticks)
        composeRule.setContent {
            HistoryScreen(
                model,
                onEditDrink = { editedDrink = it },
                onEditCheckIn = { editedCheckIn = it },
            )
        }
        return model
    }

    private fun saveDrink(
        source: Source,
        start: String,
        finish: String? = null,
        zone: ZoneId = ZoneOffset.UTC,
    ): Long = runBlocking {
        val saved =
            savedSources.getOrPut(source.name) { source.copy(id = sourceStore.save(source)) }
        val drink = saved.logged(1.0, Instant.parse(start), zone)
        drinkStore.save(drink.copy(finish = finish?.let(Instant::parse) ?: drink.finish))
    }

    private fun saveCheckIn(at: String, zone: ZoneId = ZoneOffset.UTC): Long = runBlocking {
        checkInStore.save(
            CheckIn(
                at = Instant.parse(at),
                zone = zone,
                awake = 4,
                calm = 2,
                kind = PromptKind.MORNING,
            ),
        )
    }

    private fun saveTwoDays() {
        saveDrink(espresso, "2026-03-09T09:00:00Z")
        saveDrink(greenTea, "2026-03-10T10:00:00Z", finish = "2026-03-10T10:40:00Z")
    }

    private fun waitForText(text: String) =
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun nothingLoggedSaysSo() {
        show()
        waitForText("Nothing logged yet.")
        composeRule.onNode(hasText("History") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun nothingIsSaidAboutAnEmptyHistoryBeforeItIsRead() {
        saveTwoDays()
        show()
        composeRule.onNode(hasText("History") and isHeading()).assertIsDisplayed()
        composeRule.onAllNodesWithText("Nothing logged yet.").assertCountEquals(0)
        waitForText("2026-03-10")
    }

    @Test
    fun daysAreListedNewestFirstWithTheirEntries() {
        saveTwoDays()
        show()
        waitForText("2026-03-10")
        val newer = composeRule.onNodeWithText("2026-03-10").getUnclippedBoundsInRoot().top
        val older = composeRule.onNodeWithText("2026-03-09").getUnclippedBoundsInRoot().top
        assertTrue("$newer above $older", newer < older)
        composeRule.onNodeWithText("10:00 Green tea × 1").assertIsDisplayed()
        composeRule
            .onNodeWithText("30 mg caffeine, 20 mg L-theanine, ratio 0.67")
            .assertIsDisplayed()
    }

    @Test
    fun aSippedDrinkStillGoingSaysSo() {
        saveDrink(greenTea, "2026-03-10T10:00:00Z")
        show()
        waitForText("10:00 Green tea × 1")
        composeRule
            .onNodeWithText("30 mg caffeine, 20 mg L-theanine, ratio 0.67, in progress")
            .assertIsDisplayed()
    }

    @Test
    fun aCheckInIsListedWithItsRatings() {
        saveCheckIn("2026-03-10T11:00:00Z")
        show()
        waitForText("11:00 Check-in: Awake 4 · Focus – · Calm 2")
    }

    @Test
    fun anEntryShowsItsTimeInItsOwnZone() {
        // 11:30Z is 00:30 on the 11th in Auckland (UTC+13), which is also the day it is listed on.
        saveDrink(espresso, "2026-03-10T11:30:00Z", zone = ZoneId.of("Pacific/Auckland"))
        show()
        waitForText("00:30 Espresso × 1")
        composeRule.onNodeWithText("2026-03-11").assertIsDisplayed()
    }

    @Test
    fun eachDayHasItsGraph() {
        saveTwoDays()
        show()
        waitForText("2026-03-10")
        composeRule
            .onAllNodesWithContentDescription("Graph of caffeine and L-theanine in the body")
            .assertCountEquals(2)
        composeRule.onNodeWithContentDescription("Drink: Green tea at 10:00").assertIsDisplayed()
    }

    @Test
    fun tappingAnEntryAsksToEditIt() {
        val drinkId = saveDrink(greenTea, "2026-03-10T10:00:00Z", finish = "2026-03-10T10:40:00Z")
        val checkInId = saveCheckIn("2026-03-10T11:00:00Z")
        show()
        waitForText("10:00 Green tea × 1")
        composeRule.onNodeWithContentDescription("Edit Green tea at 10:00").performClick()
        assertEquals(drinkId, editedDrink)
        composeRule.onNodeWithContentDescription("Edit check-in at 11:00").performClick()
        assertEquals(checkInId, editedCheckIn)
    }

    @Test
    fun aChangedHalfLifeRedrawsAPastDay() {
        saveDrink(espresso, "2026-03-09T09:00:00Z")
        val model = show()
        composeRule.waitUntil(5_000) { model.state.value != null }
        val before = model.state.value!!
        val peakBefore = before.graph(before.days.single()).peak.caffeineMg
        val faster = Settings(caffeine = Kinetics(Duration.ofMinutes(7), Duration.ofMinutes(60)))
        runBlocking { settingsStore.save(faster) }
        composeRule.waitUntil(5_000) { model.state.value?.settings == faster }
        val after = model.state.value!!
        assertTrue(after.graph(after.days.single()).peak.caffeineMg < peakBefore)
    }

    @Test
    fun everyDayCanBeScrolledTo() {
        for (day in 1..28) saveDrink(espresso, "2026-02-%02dT09:00:00Z".format(day))
        show()
        waitForText("2026-02-28")
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("2026-02-01"))
        composeRule.onNodeWithText("2026-02-01").assertIsDisplayed()
        composeRule.onNode(hasText("History") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun twoDrinksStartingTogetherOnAPastDayShareOneGraphTargetThatShowsBoth() {
        saveDrink(espresso, "2026-03-09T09:00:00Z")
        saveDrink(espresso.copy(name = "Mate"), "2026-03-09T09:00:00Z")
        show()
        waitForText("09:00 Mate × 1")
        composeRule.onNodeWithContentDescription("2 drinks at 09:00").performClick()
        val inDialog = hasAnyAncestor(isDialog())
        composeRule.onNode(hasText("Espresso") and inDialog).assertIsDisplayed()
        composeRule.onNode(hasText("Mate") and inDialog).assertIsDisplayed()
    }
}

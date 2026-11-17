package io.github.jwp23.koj.ui.today

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.DeviceZoneClock
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.intake.Kinetics
import io.github.jwp23.koj.record.Settings
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.DrinkStore
import io.github.jwp23.koj.storage.KojDatabase
import io.github.jwp23.koj.storage.SettingsStore
import io.github.jwp23.koj.storage.SourceStore
import io.github.jwp23.koj.testing.MutableClock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
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
    private val ticks = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    @After fun closeDatabase() = database.close()

    private fun newModel(modelClock: Clock = clock) =
        TodayViewModel(sourceStore, drinkStore, settingsStore, modelClock, ticks)

    private fun show(
        onLogWithOptions: (Long) -> Unit = {},
        onOpenSources: () -> Unit = {},
        modelClock: Clock = clock,
    ): TodayViewModel {
        val model = newModel(modelClock)
        composeRule.setContent { TodayScreen(model, onLogWithOptions, onOpenSources) }
        return model
    }

    private fun saveSources(): Pair<Source, Source> = runBlocking {
        val espresso =
            Source(
                name = "Espresso",
                caffeineMgPerServing = 80.0,
                theanineMgPerServing = 0.0,
                style = IntakeStyle.FAST,
            )
        val greenTea =
            Source(
                name = "Green tea",
                caffeineMgPerServing = 30.0,
                theanineMgPerServing = 20.0,
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(60),
            )
        espresso.copy(id = sourceStore.save(espresso)) to
            greenTea.copy(id = sourceStore.save(greenTea))
    }

    private fun waitForText(text: String) =
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    private fun tickAt(time: String) {
        clock.now = Instant.parse(time)
        ticks.tryEmit(Unit)
    }

    private fun storedDrinks() = runBlocking { drinkStore.drinks().first() }

    private fun waitForADrink() = composeRule.waitUntil(5_000) { storedDrinks().isNotEmpty() }

    @Test
    fun withNoSourcesItPointsToTheSourceList() {
        var opened = false
        show(onOpenSources = { opened = true })
        waitForText("Add a source to start logging.")
        composeRule.onNodeWithText("Add a source to start logging.").assertIsDisplayed()
        composeRule.onNodeWithText("Sources").performClick()
        assertEquals(true, opened)
    }

    @Test
    fun noAmountOrEmptyMessageIsShownBeforeTheFirstRead() {
        saveSources()
        // Ticks that never arrive keep the first values from arriving.
        val model =
            TodayViewModel(sourceStore, drinkStore, settingsStore, clock, MutableSharedFlow())
        composeRule.setContent { TodayScreen(model, onLogWithOptions = {}, onOpenSources = {}) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onAllNodesWithText("Caffeine in body", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("L-theanine in body", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Add a source to start logging.").assertCountEquals(0)
        composeRule.onAllNodesWithText("Espresso").assertCountEquals(0)
    }

    @Test
    fun theEmptyMessageIsNotShownForSavedSources() {
        saveSources()
        show()
        composeRule.onAllNodesWithText("Add a source to start logging.").assertCountEquals(0)
        waitForText("Espresso")
        composeRule.onAllNodesWithText("Add a source to start logging.").assertCountEquals(0)
    }

    @Test
    fun nothingLoggedShowsZero() {
        saveSources()
        show()
        waitForText("Caffeine in body: 0 mg")
        composeRule.onNodeWithText("Caffeine in body: 0 mg").assertIsDisplayed()
        composeRule.onNodeWithText("L-theanine in body: 0 mg").assertIsDisplayed()
    }

    @Test
    fun oneTapLogsOneServingNow() {
        saveSources()
        show()
        waitForText("Espresso")
        composeRule.onNodeWithText("Espresso").performClick()
        waitForADrink()
        val drink = storedDrinks().single()
        assertEquals(1.0, drink.servings, 0.0)
        assertEquals(Instant.parse("2026-03-10T10:00:00Z"), drink.start)
        assertEquals(ZoneOffset.UTC, drink.zone)
        assertEquals("Espresso", drink.sourceName)
        assertEquals(drink.start, drink.finish)
    }

    // 60 minutes after 80 mg: ka = ln2 / 7 = 0.099021 per minute; ke = ln2 / 300 = 0.0023105 per
    // minute; ka / (ka - ke) = 1.023891; e^(-ke x 60) = 0.870551; e^(-ka x 60) = 0.002629;
    // 80 x 1.023891 x (0.870551 - 0.002629) = 71.09.
    private fun logEspressoThenAnHourPasses() {
        saveSources()
        show()
        waitForText("Espresso")
        composeRule.onNodeWithText("Espresso").performClick()
        waitForADrink()
        tickAt("2026-03-10T11:00:00Z")
    }

    @Test
    fun theAmountInTheBodyFollowsTheClock() {
        logEspressoThenAnHourPasses()
        waitForText("Caffeine in body: 71 mg")
        composeRule.onNodeWithText("Caffeine in body: 71 mg").assertIsDisplayed()
    }

    // ke = ln2 / 150 = 0.0046210; ka / (ka - ke) = 1.048951; e^(-ke x 60) = 0.757858;
    // 80 x 1.048951 x (0.757858 - 0.002629) = 63.38.
    @Test
    fun aChangedHalfLifeChangesTheAmount() {
        logEspressoThenAnHourPasses()
        waitForText("Caffeine in body: 71 mg")
        runBlocking {
            settingsStore.save(
                Settings(caffeine = Kinetics(Duration.ofMinutes(7), Duration.ofMinutes(150))),
            )
        }
        waitForText("Caffeine in body: 63 mg")
        composeRule.onNodeWithText("Caffeine in body: 63 mg").assertIsDisplayed()
    }

    @Test
    fun yesterdaysDrinkStillCounts() {
        val (espresso, _) = saveSources()
        runBlocking {
            drinkStore.save(
                espresso.logged(1.0, Instant.parse("2026-03-09T22:00:00Z"), ZoneOffset.UTC),
            )
        }
        show()
        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodes(
                    hasText("Caffeine in body: ", substring = true) and
                        !hasText("Caffeine in body: 0 mg"),
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun aSippedDrinkIsInProgressUntilFinished() {
        saveSources()
        show()
        waitForText("Green tea")
        composeRule.onNodeWithText("Green tea").performClick()
        waitForText("Green tea since 10:00")
        composeRule.onNodeWithText("In progress").assertIsDisplayed()
        composeRule.onNodeWithText("Finish").assertIsDisplayed()
        clock.now = Instant.parse("2026-03-10T10:40:00Z")
        composeRule.onNodeWithText("Finish").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("In progress").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(Instant.parse("2026-03-10T10:40:00Z"), storedDrinks().single().finish)
    }

    @Test
    fun aFastDrinkIsNotShownAsInProgress() {
        saveSources()
        show()
        waitForText("Espresso")
        composeRule.onNodeWithText("Espresso").performClick()
        waitForADrink()
        // An hour on, the screen shows the drink's amount, so it has read the stored drink.
        tickAt("2026-03-10T11:00:00Z")
        waitForText("Caffeine in body: 71 mg")
        composeRule.onAllNodesWithText("In progress").assertCountEquals(0)
    }

    @Test
    fun optionsAsksForTheLogScreen() {
        val (espresso, _) = saveSources()
        var asked: Long? = null
        show(onLogWithOptions = { asked = it })
        waitForText("Espresso")
        composeRule.onNodeWithContentDescription("Log Espresso with options").performClick()
        assertEquals(espresso.id, asked)
    }

    @Test
    fun logButtonsStayReachableWithManySources() {
        runBlocking {
            repeat(40) { number ->
                sourceStore.save(
                    Source(
                        name = "Source $number",
                        caffeineMgPerServing = 10.0,
                        theanineMgPerServing = 0.0,
                        style = IntakeStyle.FAST,
                    ),
                )
            }
        }
        show()
        waitForText("Source 0")
        composeRule.onNodeWithText("Source 0").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Source 39"))
        composeRule.onNodeWithText("Source 39").assertIsDisplayed().performClick()
        waitForADrink()
        assertEquals("Source 39", storedDrinks().single().sourceName)
    }

    private val twoHoursEast = ZoneOffset.ofHours(2)

    @Test
    fun aNewDrinkIsStampedWithTheZoneTheDeviceIsInNow() {
        var deviceZone: ZoneId = ZoneOffset.UTC
        saveSources()
        show(modelClock = DeviceZoneClock(clock) { deviceZone })
        waitForText("Espresso")
        deviceZone = twoHoursEast
        composeRule.onNodeWithText("Espresso").performClick()
        waitForADrink()
        assertEquals(twoHoursEast, storedDrinks().single().zone)
    }

    @Test
    fun aDrinksStartIsShownInTheZoneTheDeviceIsInNow() {
        var deviceZone: ZoneId = ZoneOffset.UTC
        saveSources()
        show(modelClock = DeviceZoneClock(clock) { deviceZone })
        waitForText("Green tea")
        composeRule.onNodeWithText("Green tea").performClick()
        waitForText("Green tea since 10:00")
        deviceZone = twoHoursEast
        ticks.tryEmit(Unit)
        waitForText("Green tea since 12:00")
    }
}

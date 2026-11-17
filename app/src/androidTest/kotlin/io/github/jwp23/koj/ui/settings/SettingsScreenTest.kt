package io.github.jwp23.koj.ui.settings

import android.content.Context
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.record.Settings
import io.github.jwp23.koj.storage.KojDatabase
import io.github.jwp23.koj.storage.SettingsStore
import io.github.jwp23.koj.ui.pickTime
import java.time.Duration
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(
                ApplicationProvider.getApplicationContext<Context>(),
            )
            .setDriver(AndroidSQLiteDriver())
            .build()
    private val store = SettingsStore(database.settingsDao())

    @After fun closeDatabase() = database.close()

    private fun show() {
        val model = SettingsViewModel(store)
        composeRule.setContent { SettingsScreen(model) }
    }

    private fun stored(): Settings = runBlocking { store.settings().first() }

    private fun waitForText(text: String) =
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    private fun field(label: String): SemanticsNodeInteraction =
        composeRule.onNodeWithText(label).performScrollTo()

    private fun fieldHolding(label: String, value: String) =
        composeRule.onNode(hasText(label) and hasText(value))

    private fun save() {
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
    }

    private fun saveAndWaitForSaved() {
        save()
        waitForText("Saved")
    }

    @Test
    fun aFreshInstallShowsTheDefaults() {
        show()
        waitForText("Caffeine half-life (hours)")
        fieldHolding("Caffeine half-life (hours)", "5").assertIsDisplayed()
        field("Morning time: 08:00")
        field("Quiet from: 20:00")
        field("Bedtime: 21:00")
        val heading = field("Check-ins at fixed times").getUnclippedBoundsInRoot()
        val none = field("None").assertIsDisplayed().getUnclippedBoundsInRoot()
        val add = field("Add a check-in time").getUnclippedBoundsInRoot()
        assertTrue("None at ${none.top}, heading at ${heading.top}", heading.top < none.top)
        assertTrue("None at ${none.top}, add at ${add.top}", none.top < add.top)
    }

    @Test
    fun noFieldsAreShownUntilTheStoredSettingsAreRead() {
        val readGate = TestCoroutineScheduler()
        val gatedDatabase =
            Room.inMemoryDatabaseBuilder<KojDatabase>(
                    ApplicationProvider.getApplicationContext<Context>(),
                )
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(StandardTestDispatcher(readGate))
                .build()
        try {
            val model = SettingsViewModel(SettingsStore(gatedDatabase.settingsDao()))
            composeRule.setContent { SettingsScreen(model) }
            composeRule.onNodeWithText("Settings").assertIsDisplayed()
            composeRule.onAllNodesWithText("Caffeine half-life (hours)").assertCountEquals(0)
            composeRule.onAllNodesWithText("Save").assertCountEquals(0)

            composeRule.waitUntil(5_000) {
                readGate.advanceUntilIdle()
                composeRule
                    .onAllNodesWithText("Caffeine half-life (hours)")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        } finally {
            gatedDatabase.close()
        }
    }

    @Test
    fun aChangedSettingIsStored() {
        show()
        waitForText("Caffeine half-life (hours)")
        field("Full check-ins per day, at most").performTextReplacement("4")
        field("Caffeine half-life (hours)").performTextReplacement("4.5")
        saveAndWaitForSaved()
        field("Saved").assertIsDisplayed()
        assertEquals(4, stored().dailyCheckInCap)
        assertEquals(Duration.ofMinutes(270), stored().caffeine.eliminationHalfLife)
    }

    @Test
    fun storedSettingsAreShownWhenTheScreenOpens() {
        runBlocking { store.save(Settings(dailyCheckInCap = 3)) }
        show()
        waitForText("Full check-ins per day, at most")
        fieldHolding("Full check-ins per day, at most", "3").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aBadEntryIsExplainedAndNotStored() {
        show()
        waitForText("Caffeine half-life (hours)")
        field("Caffeine half-life (hours)").performTextReplacement("0")
        field("Full check-ins per day, at most").performTextReplacement("-1")
        save()
        field("Enter hours above 0, up to 8760").assertIsDisplayed()
        field("Enter a whole number, 0 or more").assertIsDisplayed()
        composeRule.onAllNodesWithText("Saved").assertCountEquals(0)
        assertEquals(Settings(), stored())
    }

    @Test
    fun badAfterDrinkCheckInsAreExplained() {
        show()
        waitForText("Caffeine half-life (hours)")
        field("Check-ins after a drink (minutes, separated by commas)")
            .performTextReplacement("30;90")
        save()
        field("Enter whole minutes, 1 to 1440, separated by commas").assertIsDisplayed()
        assertEquals(Settings(), stored())
    }

    @Test
    fun aHalfLifeAtTheLimitIsStoredAndOnePastItIsExplained() {
        show()
        waitForText("Caffeine half-life (hours)")
        field("Caffeine half-life (hours)").performTextReplacement("8761")
        field("L-theanine half-life (minutes)").performTextReplacement("525601")
        save()
        field("Enter hours above 0, up to 8760").assertIsDisplayed()
        field("Enter minutes above 0, up to 525600").assertIsDisplayed()
        assertEquals(Settings(), stored())

        field("Caffeine half-life (hours)").performTextReplacement("8760")
        field("L-theanine half-life (minutes)").performTextReplacement("525600")
        saveAndWaitForSaved()
        assertEquals(Duration.ofHours(8760), stored().caffeine.eliminationHalfLife)
        assertEquals(Duration.ofMinutes(525600), stored().theanine.eliminationHalfLife)
    }

    @Test
    fun anAfterDrinkDelayAtTheLimitIsStoredAndOnePastItIsExplained() {
        show()
        waitForText("Caffeine half-life (hours)")
        val label = "Check-ins after a drink (minutes, separated by commas)"
        field(label).performTextReplacement("45, 1441")
        save()
        field("Enter whole minutes, 1 to 1440, separated by commas").assertIsDisplayed()
        assertEquals(Settings(), stored())

        field(label).performTextReplacement("45, 1440")
        saveAndWaitForSaved()
        assertEquals(
            listOf(Duration.ofMinutes(45), Duration.ofMinutes(1440)),
            stored().afterDrinkCheckIns,
        )
    }

    @Test
    fun aPickedBedtimeIsStored() {
        show()
        waitForText("Caffeine half-life (hours)")
        field("Bedtime: 21:00")
        composeRule.pickTime("Bedtime: 21:00", "22", "30")
        composeRule.onNodeWithText("Bedtime: 22:30").assertIsDisplayed()
        saveAndWaitForSaved()
        assertEquals(LocalTime.of(22, 30), stored().bedtime)
    }

    @Test
    fun fixedCheckInTimesAreAddedAndRemoved() {
        show()
        waitForText("Caffeine half-life (hours)")
        field("Add a check-in time")
        composeRule.pickTime("Add a check-in time", "15", "30")
        composeRule.pickTime("Add a check-in time", "10", "00")
        val earlier = composeRule.onNodeWithText("10:00").getUnclippedBoundsInRoot()
        val later = composeRule.onNodeWithText("15:30").getUnclippedBoundsInRoot()
        assertTrue("10:00 at ${earlier.top}, 15:30 at ${later.top}", earlier.top < later.top)
        saveAndWaitForSaved()
        assertEquals(listOf(LocalTime.of(10, 0), LocalTime.of(15, 30)), stored().fixedCheckInTimes)

        composeRule
            .onNodeWithContentDescription("Remove check-in at 10:00")
            .performScrollTo()
            .performClick()
        composeRule.onAllNodesWithText("10:00").assertCountEquals(0)
        save()
        composeRule.waitUntil(5_000) { stored().fixedCheckInTimes == listOf(LocalTime.of(15, 30)) }
    }

    @Test
    fun anEarlierSaveFinishingLateDoesNotMarkALaterEditSaved() {
        val writeGate = TestCoroutineScheduler()
        val gatedDatabase =
            Room.inMemoryDatabaseBuilder<KojDatabase>(
                    ApplicationProvider.getApplicationContext<Context>(),
                )
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(StandardTestDispatcher(writeGate))
                .build()
        try {
            val gatedStore = SettingsStore(gatedDatabase.settingsDao())
            val model = SettingsViewModel(gatedStore)
            composeRule.setContent { SettingsScreen(model) }
            composeRule.waitUntil(5_000) {
                writeGate.advanceUntilIdle()
                composeRule
                    .onAllNodesWithText("Caffeine half-life (hours)")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            field("Full check-ins per day, at most").performTextReplacement("4")
            save()
            field("Full check-ins per day, at most").performTextReplacement("5")
            save()
            field("Full check-ins per day, at most").performTextReplacement("4")
            writeGate.advanceUntilIdle()
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText("Saved").assertCountEquals(0)
            val read = CoroutineScope(Dispatchers.Default).async { gatedStore.settings().first() }
            composeRule.waitUntil(5_000) {
                writeGate.advanceUntilIdle()
                read.isCompleted
            }
            assertEquals(5, runBlocking { read.await() }.dailyCheckInCap)
        } finally {
            gatedDatabase.close()
        }
    }

    @Test
    fun anEditAfterSavingClearsTheSavedMessage() {
        show()
        waitForText("Caffeine half-life (hours)")
        saveAndWaitForSaved()
        field("Full check-ins per day, at most").performTextReplacement("2")
        composeRule.onAllNodesWithText("Saved").assertCountEquals(0)
    }
}

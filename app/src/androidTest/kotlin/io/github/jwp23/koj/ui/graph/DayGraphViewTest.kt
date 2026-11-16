package io.github.jwp23.koj.ui.graph

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.graph.dayGraph
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.PromptKind
import io.github.jwp23.koj.record.Settings
import io.github.jwp23.koj.record.Source
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayGraphViewTest {
    @get:Rule val composeRule = createComposeRule()

    private val zone: ZoneId = ZoneOffset.UTC
    private val date = LocalDate.of(2026, 3, 10)

    /** [time], HH:mm, on the graphed date in UTC. */
    private fun at(time: String): Instant = Instant.parse("2026-03-10T$time:00Z")

    private fun show(
        drinks: List<LoggedDrink> = emptyList(),
        checkIns: List<CheckIn> = emptyList(),
        now: Instant = at("14:00"),
    ) {
        val graph = dayGraph(date, zone, now, Settings(), drinks, checkIns)
        composeRule.setContent { DayGraphView(graph, zone) }
    }

    private fun fastSource(name: String) =
        Source(
            name = name,
            caffeineMgPerServing = 100.0,
            theanineMgPerServing = 0.0,
            style = IntakeStyle.FAST,
        )

    private val greenTea =
        Source(
                name = "Green tea",
                caffeineMgPerServing = 30.0,
                theanineMgPerServing = 20.0,
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(60),
            )
            .logged(1.0, at("09:00"), zone)
            .copy(id = 2, finish = at("09:40"))

    private fun espressoAt(time: String) = fastSource("Espresso").logged(1.0, at(time), zone)

    private fun leftOf(description: String) =
        composeRule.onNodeWithContentDescription(description).getUnclippedBoundsInRoot().left

    @Test
    fun aDrinkIsMarkedAndItsDetailsOpen() {
        show(drinks = listOf(espressoAt("10:00")))
        composeRule.onNodeWithContentDescription("Drink: Espresso at 10:00").performClick()
        composeRule.onNodeWithText("Espresso").assertIsDisplayed()
        composeRule.onNodeWithText("10:00").assertIsDisplayed()
        composeRule.onNodeWithText("100 mg caffeine").assertIsDisplayed()
        composeRule.onNodeWithText("0 mg L-theanine").assertIsDisplayed()
        composeRule.onNodeWithText("Taken ratio: 0.00").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.onAllNodesWithText("Taken ratio: 0.00").assertCountEquals(0)
        composeRule.onAllNodesWithText("Espresso").assertCountEquals(0)
    }

    @Test
    fun aSippedDrinkShowsWhenItStartedAndEnded() {
        show(drinks = listOf(greenTea))
        composeRule.onNodeWithContentDescription("Drink: Green tea at 09:00").performClick()
        composeRule.onNodeWithText("09:00 to 09:40").assertIsDisplayed()
        // 20 / 30 = 0.667.
        composeRule.onNodeWithText("Taken ratio: 0.67").assertIsDisplayed()
    }

    @Test
    fun markersAreLaidOutInTimeOrder() {
        val mate = fastSource("Mate").logged(1.0, at("15:00"), zone)
        show(drinks = listOf(espressoAt("09:00"), mate))
        assertTrue(leftOf("Drink: Espresso at 09:00") < leftOf("Drink: Mate at 15:00"))
    }

    @Test
    fun theGraphIsDescribed() {
        show()
        composeRule
            .onNodeWithContentDescription("Graph of caffeine and L-theanine in the body")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Caffeine").assertIsDisplayed()
        composeRule.onNodeWithText("L-theanine").assertIsDisplayed()
    }

    @Test
    fun aDayWithNothingLoggedStillDraws() {
        show()
        composeRule
            .onNodeWithContentDescription("Graph of caffeine and L-theanine in the body")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Taken: 0 mg caffeine, 0 mg L-theanine, ratio –")
            .assertIsDisplayed()
    }

    @Test
    fun aCheckInIsMarkedAndItsRatingsOpen() {
        val checkIn =
            CheckIn(
                at = at("11:00"),
                zone = zone,
                awake = 4,
                focus = null,
                calm = 2,
                kind = PromptKind.FIXED_TIME,
            )
        show(checkIns = listOf(checkIn))
        composeRule.onNodeWithContentDescription("Check-in at 11:00").performClick()
        composeRule.onNodeWithText("Check-in at 11:00").assertIsDisplayed()
        composeRule.onNodeWithText("Awake 4 · Focus – · Calm 2").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.onAllNodesWithText("Awake 4 · Focus – · Calm 2").assertCountEquals(0)
    }

    // 660 minutes after 100 mg: ka = ln2 / 7 = 0.099021; ke = ln2 / 300 = 0.0023105;
    // ka / (ka - ke) = 1.023891; e^(-ke x 660) = 0.217638; 100 x 1.023891 x 0.217638 = 22.28.
    @Test
    fun bedtimeCaffeineIsLabelled() {
        show(drinks = listOf(espressoAt("10:00")))
        composeRule.onNodeWithText("At bedtime: 22 mg caffeine").assertIsDisplayed()
    }

    @Test
    fun aDayThatIsOverHasNoBedtimeLabel() {
        show(drinks = listOf(espressoAt("10:00")), now = Instant.parse("2026-03-12T00:00:00Z"))
        composeRule
            .onNodeWithContentDescription("Graph of caffeine and L-theanine in the body")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("At bedtime", substring = true).assertCountEquals(0)
    }

    // 100 + 30 = 130 mg caffeine; 0 + 20 = 20 mg L-theanine; 20 / 130 = 0.154.
    @Test
    fun theDaysIntakeIsTotalled() {
        show(drinks = listOf(espressoAt("10:00"), greenTea))
        composeRule
            .onNodeWithText("Taken: 130 mg caffeine, 20 mg L-theanine, ratio 0.15")
            .assertIsDisplayed()
    }
}

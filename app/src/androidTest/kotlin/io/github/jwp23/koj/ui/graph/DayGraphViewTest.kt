package io.github.jwp23.koj.ui.graph

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.graph.dayGraph
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
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
}

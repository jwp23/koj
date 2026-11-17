package io.github.jwp23.koj.ui.sources

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.KojDatabase
import io.github.jwp23.koj.storage.SourceStore
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SourcesScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(
                ApplicationProvider.getApplicationContext<Context>(),
            )
            .setDriver(AndroidSQLiteDriver())
            .build()
    private val store = SourceStore(database.sourceDao())

    @After fun closeDatabase() = database.close()

    private fun show(onAdd: () -> Unit = {}, onEdit: (Long) -> Unit = {}) {
        val model = SourcesViewModel(store)
        composeRule.setContent { SourcesScreen(model, onAdd = onAdd, onEdit = onEdit) }
    }

    private fun storeGreenTea() = runBlocking {
        store.save(
            Source(
                name = "Green tea",
                caffeineMgPerServing = 30.0,
                theanineMgPerServing = 20.0,
                style = IntakeStyle.SIPPED,
                defaultDuration = Duration.ofMinutes(90),
            ),
        )
    }

    private fun waitForText(text: String) =
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun anEmptyListSaysSo() {
        show()
        waitForText("No sources yet.")
        composeRule.onNodeWithText("No sources yet.").assertIsDisplayed()
    }

    @Test
    fun nothingIsKnownAboutTheSourcesBeforeTheFirstRead() {
        assertNull(SourcesViewModel(store).state.value)
    }

    @Test
    fun theEmptyMessageIsNotShownForSavedSources() {
        storeGreenTea()
        show()
        composeRule.onAllNodesWithText("No sources yet.").assertCountEquals(0)
        waitForText("Green tea")
        composeRule.onAllNodesWithText("No sources yet.").assertCountEquals(0)
    }

    @Test
    fun savedSourcesAreListedWithTheirAmounts() {
        storeGreenTea()
        show()
        waitForText("Green tea")
        composeRule.onNodeWithText("Green tea").assertIsDisplayed()
        composeRule
            .onNodeWithText("30 mg caffeine · 20 mg L-theanine · Sipped, 90 min")
            .assertIsDisplayed()
    }

    @Test
    fun tappingASourceOpensIt() {
        val id = storeGreenTea()
        var opened: Long? = null
        show(onEdit = { opened = it })
        waitForText("Green tea")
        composeRule.onNodeWithText("Green tea").performClick()
        assertEquals(id, opened)
    }

    @Test
    fun addSourceAsksToAdd() {
        var added = false
        show(onAdd = { added = true })
        composeRule.onNodeWithText("Add source").performClick()
        assertEquals(true, added)
    }

    @Test
    fun addSourceStaysReachableWithManySources() {
        runBlocking {
            repeat(40) { number ->
                store.save(
                    Source(
                        name = "Source $number",
                        caffeineMgPerServing = 10.0,
                        theanineMgPerServing = 0.0,
                        style = IntakeStyle.FAST,
                    ),
                )
            }
        }
        var added = false
        show(onAdd = { added = true })
        waitForText("Source 0")
        composeRule.onNodeWithText("Add source").assertIsDisplayed().performClick()
        assertEquals(true, added)
    }
}

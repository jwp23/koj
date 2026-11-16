package io.github.jwp23.koj.storage

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.Source
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SourceStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
    private val store = SourceStore(database.sourceDao())

    @After fun closeDatabase() = database.close()

    private fun greenTea(name: String = "Green tea") =
        Source(
            name = name,
            caffeineMgPerServing = 30.0,
            theanineMgPerServing = 20.0,
            style = IntakeStyle.FAST,
        )

    @Test
    fun aNewSourceIsGivenAnId() = runTest {
        val source = greenTea()

        val id = store.save(source)

        assertTrue(id > 0)
        assertEquals(source.copy(id = id), store.source(id))
    }

    @Test
    fun sourcesAreListedByNameWithoutRegardToCase() = runTest {
        store.save(greenTea("matcha"))
        store.save(greenTea("Black tea"))
        store.save(greenTea("cola"))

        assertEquals(listOf("Black tea", "cola", "matcha"), store.sources().first().map { it.name })
    }

    @Test
    fun savingAnExistingSourceReplacesIt() = runTest {
        val id = store.save(greenTea())

        store.save(greenTea("Sencha").copy(id = id, caffeineMgPerServing = 25.0))

        val listed = store.sources().first()
        assertEquals(1, listed.size)
        assertEquals(greenTea("Sencha").copy(id = id, caffeineMgPerServing = 25.0), listed.single())
    }

    @Test
    fun aSippedSourceKeepsItsDurationAndUsualTime() = runTest {
        val sipped =
            greenTea()
                .copy(
                    style = IntakeStyle.SIPPED,
                    defaultDuration = Duration.ofMinutes(90),
                    usualTime = LocalTime.of(7, 30),
                )

        val id = store.save(sipped)

        assertEquals(sipped.copy(id = id), store.source(id))
    }

    @Test
    fun aSourceWithNoUsualTimeKeepsNone() = runTest {
        val id = store.save(greenTea())

        assertNull(store.source(id)?.usualTime)
    }

    @Test
    fun deletingASourceWithNoDrinksRemovesIt() = runTest {
        val id = store.save(greenTea())
        val other = store.save(greenTea("Espresso"))
        val drinks = DrinkStore(database.drinkDao())
        val otherDrink =
            store
                .source(other)!!
                .logged(1.0, Instant.parse("2026-03-10T09:00:00Z"), ZoneId.of("UTC"))
        val otherDrinkId = drinks.save(otherDrink)

        store.delete(store.source(id)!!)

        assertNull(store.source(id))
        assertEquals(false, store.source(other)?.archived)
        assertEquals(otherDrink.copy(id = otherDrinkId), drinks.drink(otherDrinkId))
    }

    @Test
    fun deletingASourceWithDrinksArchivesIt() = runTest {
        val id = store.save(greenTea())
        val source = store.source(id)!!
        val drinks = DrinkStore(database.drinkDao())
        drinks.save(source.logged(1.0, Instant.parse("2026-03-10T09:00:00Z"), ZoneId.of("UTC")))
        val other = store.save(greenTea("Espresso"))
        val otherDrink =
            store
                .source(other)!!
                .logged(1.0, Instant.parse("2026-03-10T09:00:00Z"), ZoneId.of("UTC"))
        val otherDrinkId = drinks.save(otherDrink)

        store.delete(source)

        assertEquals(listOf("Espresso"), store.sources().first().map { it.name })
        assertEquals(otherDrink.copy(id = otherDrinkId), drinks.drink(otherDrinkId))
        assertEquals(true, store.source(id)?.archived)
        assertEquals(listOf("Green tea", "Espresso"), drinks.drinks().first().map { it.sourceName })
    }

    @Test
    fun sourcesSurviveClosingTheDatabase() = runTest {
        val fileName = "source-store-test.db"
        fun open() =
            Room.databaseBuilder<KojDatabase>(context, fileName)
                .setDriver(AndroidSQLiteDriver())
                .build()
        try {
            val first = open()
            val id = SourceStore(first.sourceDao()).save(greenTea())
            first.close()

            val second = open()
            try {
                assertEquals(greenTea().copy(id = id), SourceStore(second.sourceDao()).source(id))
            } finally {
                second.close()
            }
        } finally {
            context.deleteDatabase(fileName)
        }
    }

    @Test
    fun anArchivedSourceIsStoredButNotListed() = runTest {
        val shown = store.save(greenTea("Black tea"))
        val archivedId = store.save(greenTea().copy(archived = true))

        assertEquals(listOf(shown), store.sources().first().map { it.id })
        assertEquals(true, store.source(archivedId)?.archived)
        assertEquals(false, store.source(shown)?.archived)
    }

    @Test
    fun aSippedDurationShorterThanAMillisecondSurvives() = runTest {
        val sipped =
            greenTea().copy(style = IntakeStyle.SIPPED, defaultDuration = Duration.ofNanos(500_000))

        val id = store.save(sipped)

        assertEquals(sipped.copy(id = id), store.source(id))
        assertEquals(listOf(sipped.copy(id = id)), store.sources().first())
    }

    @Test
    fun savingAnEditedSourceThatWasDeletedFails() = runTest {
        val id = store.save(greenTea())
        val loaded = store.source(id)!!
        store.delete(loaded)

        val failure = runCatching { store.save(loaded.copy(name = "Sencha")) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(store.sources().first().isEmpty())
    }

    @Test fun anUnknownIdGivesNoSource() = runTest { assertNull(store.source(999)) }
}

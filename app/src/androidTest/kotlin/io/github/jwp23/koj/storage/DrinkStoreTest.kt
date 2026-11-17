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
class DrinkStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
    private val sources = SourceStore(database.sourceDao())
    private val drinks = DrinkStore(database.drinkDao())

    private val nine = Instant.parse("2026-03-10T09:00:00Z")
    private val auckland = ZoneId.of("Pacific/Auckland")
    private val utc = ZoneId.of("UTC")

    @After fun closeDatabase() = database.close()

    private val espresso =
        Source(
            name = "Espresso",
            caffeineMgPerServing = 63.0,
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

    private suspend fun stored(source: Source) = source.copy(id = sources.save(source))

    @Test
    fun aSavedDrinkCanBeReadBack() = runTest {
        val drink = stored(espresso).logged(0.5, nine, auckland)

        val id = drinks.save(drink)

        assertTrue(id > 0)
        val read = drinks.drink(id)
        assertEquals(drink.copy(id = id), read)
        assertEquals(auckland, read?.zone)
        assertEquals("Espresso", read?.sourceName)
    }

    @Test
    fun drinksAreListedOldestFirst() = runTest {
        val source = stored(espresso)
        val late = drinks.save(source.logged(1.0, nine.plusSeconds(7200), utc))
        val early = drinks.save(source.logged(1.0, nine, utc))
        val middle = drinks.save(source.logged(1.0, nine.plusSeconds(3600), utc))

        assertEquals(listOf(early, middle, late), drinks.drinks().first().map { it.id })
    }

    @Test
    fun drinksTakenBetweenIncludesTheStartAndExcludesTheEnd() = runTest {
        val source = stored(espresso)
        val first = drinks.save(source.logged(1.0, nine, utc))
        val second = drinks.save(source.logged(1.0, nine.plusSeconds(3600), utc))
        drinks.save(source.logged(1.0, nine.plusSeconds(7200), utc))

        val found = drinks.drinksTakenBetween(nine, nine.plusSeconds(7200)).first()

        assertEquals(listOf(first, second), found.map { it.id })
    }

    @Test
    fun drinksTakenBetweenIncludesSippedDrinksStillBeingSippedAtTheStart() = runTest {
        val sip = stored(greenTea)
        val from = nine.plusSeconds(3600)
        val stillSipping = drinks.save(sip.logged(1.0, nine, utc).copy(finish = from))
        val unfinished = drinks.save(sip.logged(1.0, nine.minusSeconds(60), utc))
        drinks.save(sip.logged(1.0, nine, utc).copy(finish = from.minusSeconds(1)))
        drinks.save(stored(espresso).logged(1.0, nine, utc))

        val found = drinks.drinksTakenBetween(from, from.plusSeconds(7200)).first()

        assertEquals(listOf(unfinished, stillSipping), found.map { it.id })
    }

    @Test
    fun aSippedDrinkIsInProgressUntilItIsFinished() = runTest {
        val drink = stored(greenTea).logged(1.0, nine, utc)
        val id = drinks.save(drink)
        assertEquals(listOf(id), drinks.drinksInProgress().first().map { it.id })

        val finish = nine.plus(Duration.ofMinutes(40))
        drinks.save(drink.copy(id = id, finish = finish))

        assertTrue(drinks.drinksInProgress().first().isEmpty())
        assertEquals(finish, drinks.drink(id)?.finish)
    }

    @Test
    fun aFastDrinkIsNeverInProgress() = runTest {
        drinks.save(stored(espresso).logged(1.0, nine, utc))

        assertTrue(drinks.drinksInProgress().first().isEmpty())
    }

    @Test
    fun editingASourceLeavesItsDrinksAmountsUnchanged() = runTest {
        val source = stored(greenTea)
        val id = drinks.save(source.logged(1.0, nine, utc))

        sources.save(source.copy(caffeineMgPerServing = 45.0))

        assertEquals(30.0, drinks.drink(id)?.caffeineMgPerServing)
    }

    @Test
    fun aRenamedSourceShowsItsNewNameOnEarlierDrinks() = runTest {
        val source = stored(greenTea)
        val id = drinks.save(source.logged(1.0, nine, utc))

        sources.save(source.copy(name = "Sencha"))

        assertEquals("Sencha", drinks.drink(id)?.sourceName)
    }

    @Test
    fun savingAnExistingDrinkReplacesIt() = runTest {
        val drink = stored(espresso).logged(1.0, nine, utc)
        val id = drinks.save(drink)

        val earlier = nine.minusSeconds(3600)
        drinks.save(drink.copy(id = id, servings = 2.0, start = earlier, finish = earlier))

        val listed = drinks.drinks().first()
        assertEquals(1, listed.size)
        assertEquals(2.0, listed.single().servings, 0.0)
        assertEquals(earlier, listed.single().start)
    }

    @Test
    fun savingAnEditedDrinkThatWasDeletedFails() = runTest {
        val id = drinks.save(stored(espresso).logged(1.0, nine, utc))
        val loaded = drinks.drink(id)!!
        drinks.delete(loaded)

        val failure = runCatching { drinks.save(loaded.copy(servings = 2.0)) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertNull(drinks.drink(id))
    }

    @Test
    fun aDeletedDrinkIsGone() = runTest {
        val id = drinks.save(stored(espresso).logged(1.0, nine, utc))

        drinks.delete(drinks.drink(id)!!)

        assertTrue(drinks.drinks().first().isEmpty())
        assertNull(drinks.drink(id))
    }

    @Test
    fun drinksSurviveClosingTheDatabase() = runTest {
        val fileName = "drink-store-test.db"
        fun open() =
            Room.databaseBuilder<KojDatabase>(context, fileName)
                .setDriver(AndroidSQLiteDriver())
                .build()
        try {
            val first = open()
            val source = espresso.copy(id = SourceStore(first.sourceDao()).save(espresso))
            val drink = source.logged(0.5, nine, auckland)
            val id = DrinkStore(first.drinkDao()).save(drink)
            first.close()

            val second = open()
            try {
                assertEquals(drink.copy(id = id), DrinkStore(second.drinkDao()).drink(id))
            } finally {
                second.close()
            }
        } finally {
            context.deleteDatabase(fileName)
        }
    }
}

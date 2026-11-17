package io.github.jwp23.koj.storage

import io.github.jwp23.koj.record.LoggedDrink
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DrinkStore(private val dao: DrinkDao) {
    /** Every drink, oldest first. */
    fun drinks(): Flow<List<LoggedDrink>> = dao.all().map { rows -> rows.map { it.toDrink() } }

    /**
     * Drinks that start at or after [from] and before [until], oldest first, plus sipped drinks
     * that started earlier and were still being sipped at [from] or are not finished.
     */
    fun drinksTakenBetween(from: Instant, until: Instant): Flow<List<LoggedDrink>> =
        dao.takenBetween(from, until).map { rows -> rows.map { it.toDrink() } }

    /** Sipped drinks with no finish time, oldest first. */
    fun drinksInProgress(): Flow<List<LoggedDrink>> =
        dao.inProgress().map { rows -> rows.map { it.toDrink() } }

    suspend fun drink(id: Long): LoggedDrink? = dao.find(id)?.toDrink()

    /**
     * Stores a new drink (id 0) or replaces the stored one. Returns its id. Throws if the drink has
     * an id but no stored drink has it, such as one deleted since it was loaded.
     */
    suspend fun save(drink: LoggedDrink): Long {
        val row = drink.toRow()
        if (drink.id == 0L) return dao.insert(row)
        check(dao.update(row) > 0) { "No stored drink has id ${drink.id}" }
        return drink.id
    }

    suspend fun delete(drink: LoggedDrink) = dao.delete(drink.toRow())
}

private fun DrinkWithSourceName.toDrink() =
    LoggedDrink(
        id = drink.id,
        sourceId = drink.sourceId,
        sourceName = sourceName,
        servings = drink.servings,
        caffeineMgPerServing = drink.caffeineMgPerServing,
        theanineMgPerServing = drink.theanineMgPerServing,
        style = drink.style,
        defaultDuration = drink.defaultDuration,
        start = drink.start,
        zone = drink.zone,
        finish = drink.finish,
    )

private fun LoggedDrink.toRow() =
    DrinkRow(
        id = id,
        sourceId = sourceId,
        servings = servings,
        caffeineMgPerServing = caffeineMgPerServing,
        theanineMgPerServing = theanineMgPerServing,
        style = style,
        defaultDuration = defaultDuration,
        start = start,
        zone = zone,
        finish = finish,
    )

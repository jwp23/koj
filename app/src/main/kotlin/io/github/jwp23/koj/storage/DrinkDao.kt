package io.github.jwp23.koj.storage

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Insert suspend fun insert(row: DrinkRow): Long

    @Update suspend fun update(row: DrinkRow)

    @Delete suspend fun delete(row: DrinkRow)

    @Query(
        "SELECT drink.*, source.name AS sourceName FROM drink " +
            "JOIN source ON source.id = drink.sourceId ORDER BY drink.start, drink.id",
    )
    fun all(): Flow<List<DrinkWithSourceName>>

    @Query(
        "SELECT drink.*, source.name AS sourceName FROM drink " +
            "JOIN source ON source.id = drink.sourceId " +
            "WHERE drink.start >= :from AND drink.start < :until ORDER BY drink.start, drink.id",
    )
    fun startingBetween(from: Instant, until: Instant): Flow<List<DrinkWithSourceName>>

    @Query(
        "SELECT drink.*, source.name AS sourceName FROM drink " +
            "JOIN source ON source.id = drink.sourceId " +
            "WHERE drink.style = 'SIPPED' AND drink.finish IS NULL ORDER BY drink.start, drink.id",
    )
    fun inProgress(): Flow<List<DrinkWithSourceName>>

    @Query(
        "SELECT drink.*, source.name AS sourceName FROM drink " +
            "JOIN source ON source.id = drink.sourceId WHERE drink.id = :id",
    )
    suspend fun find(id: Long): DrinkWithSourceName?
}

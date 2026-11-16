package io.github.jwp23.koj.storage

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Insert suspend fun insert(row: SourceRow): Long

    /** Returns how many rows changed: 0 when no stored source has the row's id. */
    @Update suspend fun update(row: SourceRow): Int

    @Delete suspend fun delete(row: SourceRow)

    @Query("UPDATE source SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)

    @Query("SELECT COUNT(*) FROM drink WHERE sourceId = :id") suspend fun drinkCount(id: Long): Int

    /**
     * Archives the source when a drink refers to it, so the drink keeps its source; else removes
     * it.
     */
    @Transaction
    suspend fun archiveOrRemove(row: SourceRow) {
        if (drinkCount(row.id) > 0) archive(row.id) else delete(row)
    }

    @Query("SELECT * FROM source WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<SourceRow>>

    @Query("SELECT * FROM source WHERE id = :id") suspend fun find(id: Long): SourceRow?
}

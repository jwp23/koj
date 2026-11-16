package io.github.jwp23.koj.storage

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Insert suspend fun insert(row: SourceRow): Long

    /** Returns how many rows changed: 0 when no stored source has the row's id. */
    @Update suspend fun update(row: SourceRow): Int

    @Delete suspend fun delete(row: SourceRow)

    @Query("SELECT * FROM source WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<SourceRow>>

    @Query("SELECT * FROM source WHERE id = :id") suspend fun find(id: Long): SourceRow?
}

package io.github.jwp23.koj.storage

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Insert suspend fun insert(row: CheckInRow): Long

    @Update suspend fun update(row: CheckInRow)

    @Delete suspend fun delete(row: CheckInRow)

    @Query("SELECT * FROM check_in ORDER BY at, id") fun all(): Flow<List<CheckInRow>>

    @Query("SELECT * FROM check_in WHERE at >= :from AND at < :until ORDER BY at, id")
    fun between(from: Instant, until: Instant): Flow<List<CheckInRow>>

    @Query("SELECT * FROM check_in WHERE id = :id") suspend fun find(id: Long): CheckInRow?
}

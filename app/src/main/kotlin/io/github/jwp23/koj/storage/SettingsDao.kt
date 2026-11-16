package io.github.jwp23.koj.storage

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Upsert suspend fun save(row: SettingsRow)

    @Query("SELECT * FROM settings WHERE id = 1") fun settings(): Flow<SettingsRow?>

    @Query("SELECT COUNT(*) FROM settings") suspend fun rowCount(): Int
}

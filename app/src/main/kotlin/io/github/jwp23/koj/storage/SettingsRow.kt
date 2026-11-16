package io.github.jwp23.koj.storage

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.Duration
import java.time.LocalTime

/** The one row of the settings table. The two check-in lists are comma-separated whole numbers. */
@Entity(tableName = "settings")
data class SettingsRow(
    @PrimaryKey val id: Int = 1,
    val caffeineAbsorptionHalfLife: Duration,
    val caffeineEliminationHalfLife: Duration,
    val theanineAbsorptionHalfLife: Duration,
    val theanineEliminationHalfLife: Duration,
    val afterDrinkCheckInSeconds: String,
    val fixedCheckInSecondsOfDay: String,
    val dailyCheckInCap: Int,
    val morningTime: LocalTime,
    val quietTime: LocalTime,
    val bedtime: LocalTime,
    val sipServings: Double,
)

package io.github.jwp23.koj.storage

import io.github.jwp23.koj.intake.Kinetics
import io.github.jwp23.koj.record.Settings
import java.time.Duration
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsStore(private val dao: SettingsDao) {
    /** The stored settings, or the defaults (`Settings()`) while nothing has been saved. */
    fun settings(): Flow<Settings> = dao.settings().map { it?.toSettings() ?: Settings() }

    suspend fun save(settings: Settings) = dao.save(settings.toRow())
}

private fun SettingsRow.toSettings() =
    Settings(
        caffeine = Kinetics(caffeineAbsorptionHalfLife, caffeineEliminationHalfLife),
        theanine = Kinetics(theanineAbsorptionHalfLife, theanineEliminationHalfLife),
        afterDrinkCheckIns = afterDrinkCheckInSeconds.toNumbers().map(Duration::ofSeconds),
        fixedCheckInTimes = fixedCheckInSecondsOfDay.toNumbers().map(LocalTime::ofSecondOfDay),
        dailyCheckInCap = dailyCheckInCap,
        morningTime = morningTime,
        quietTime = quietTime,
        bedtime = bedtime,
        sipServings = sipServings,
    )

private fun Settings.toRow() =
    SettingsRow(
        caffeineAbsorptionHalfLife = caffeine.absorptionHalfLife,
        caffeineEliminationHalfLife = caffeine.eliminationHalfLife,
        theanineAbsorptionHalfLife = theanine.absorptionHalfLife,
        theanineEliminationHalfLife = theanine.eliminationHalfLife,
        afterDrinkCheckInSeconds = afterDrinkCheckIns.joinToString(",") { "${it.seconds}" },
        fixedCheckInSecondsOfDay = fixedCheckInTimes.joinToString(",") { "${it.toSecondOfDay()}" },
        dailyCheckInCap = dailyCheckInCap,
        morningTime = morningTime,
        quietTime = quietTime,
        bedtime = bedtime,
        sipServings = sipServings,
    )

private fun String.toNumbers(): List<Long> =
    if (isEmpty()) emptyList() else split(",").map(String::toLong)

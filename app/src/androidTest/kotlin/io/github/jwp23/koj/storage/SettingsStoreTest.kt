package io.github.jwp23.koj.storage

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.intake.Kinetics
import io.github.jwp23.koj.record.Settings
import java.time.Duration
import java.time.LocalTime
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
    private val store = SettingsStore(database.settingsDao())

    @After fun closeDatabase() = database.close()

    private fun customSettings() =
        Settings(
            caffeine = Kinetics(Duration.ofMinutes(10), Duration.ofHours(4)),
            theanine = Kinetics(Duration.ofMinutes(20), Duration.ofMinutes(70)),
            afterDrinkCheckIns = listOf(Duration.ofMinutes(30)),
            fixedCheckInTimes = listOf(LocalTime.of(10, 0), LocalTime.of(15, 30)),
            dailyCheckInCap = 4,
            morningTime = LocalTime.of(6, 45),
            quietTime = LocalTime.of(19, 0),
            bedtime = LocalTime.of(22, 15),
            sipServings = 0.25,
        )

    @Test
    fun aFreshInstallHasTheDefaultSettings() = runTest {
        assertEquals(Settings(), store.settings().first())
    }

    @Test
    fun savedSettingsCanBeReadBack() = runTest {
        store.save(customSettings())

        assertEquals(customSettings(), store.settings().first())
    }

    @Test
    fun savingAgainReplacesTheSettings() = runTest {
        store.save(Settings(dailyCheckInCap = 3))
        store.save(Settings(dailyCheckInCap = 5))

        assertEquals(Settings(dailyCheckInCap = 5), store.settings().first())
        assertEquals(1, database.settingsDao().rowCount())
    }

    @Test
    fun emptyListsAreKeptEmpty() = runTest {
        val settings = Settings(afterDrinkCheckIns = emptyList(), fixedCheckInTimes = emptyList())

        store.save(settings)

        assertEquals(settings, store.settings().first())
    }

    @Test
    fun aBedtimeAfterMidnightIsKept() = runTest {
        val settings = Settings(bedtime = LocalTime.of(0, 30))

        store.save(settings)

        assertEquals(settings, store.settings().first())
    }

    @Test
    fun settingsSurviveClosingTheDatabase() = runTest {
        val fileName = "settings-store-test.db"
        fun open() =
            Room.databaseBuilder<KojDatabase>(context, fileName)
                .setDriver(AndroidSQLiteDriver())
                .build()
        try {
            val first = open()
            SettingsStore(first.settingsDao()).save(customSettings())
            first.close()

            val second = open()
            try {
                assertEquals(
                    customSettings(),
                    SettingsStore(second.settingsDao()).settings().first(),
                )
            } finally {
                second.close()
            }
        } finally {
            context.deleteDatabase(fileName)
        }
    }

    @Test
    fun aChangeIsEmittedToCollectors() = runTest {
        val emitted = async { store.settings().first { it.dailyCheckInCap == 4 } }

        store.save(Settings(dailyCheckInCap = 4))

        assertEquals(Settings(dailyCheckInCap = 4), emitted.await())
    }
}

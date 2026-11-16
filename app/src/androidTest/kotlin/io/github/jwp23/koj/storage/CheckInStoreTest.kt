package io.github.jwp23.koj.storage

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.PromptKind
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
class CheckInStoreTest {
    private val database =
        Room.inMemoryDatabaseBuilder<KojDatabase>(
                ApplicationProvider.getApplicationContext<Context>(),
            )
            .setDriver(AndroidSQLiteDriver())
            .build()
    private val checkIns = CheckInStore(database.checkInDao())

    private val nine = Instant.parse("2026-03-10T09:00:00Z")
    private val auckland = ZoneId.of("Pacific/Auckland")

    @After fun closeDatabase() = database.close()

    private fun checkInAt(at: Instant) =
        CheckIn(at = at, zone = auckland, awake = 4, kind = PromptKind.MORNING)

    @Test
    fun aSavedCheckInCanBeReadBack() = runTest {
        val checkIn =
            CheckIn(
                at = nine,
                zone = auckland,
                awake = 4,
                focus = null,
                calm = 2,
                kind = PromptKind.AFTER_DRINK,
            )

        val id = checkIns.save(checkIn)

        assertTrue(id > 0)
        assertEquals(checkIn.copy(id = id), checkIns.checkIn(id))
    }

    @Test
    fun checkInsAreListedOldestFirst() = runTest {
        val late = checkIns.save(checkInAt(nine.plusSeconds(7200)))
        val early = checkIns.save(checkInAt(nine))

        assertEquals(listOf(early, late), checkIns.checkIns().first().map { it.id })
    }

    @Test
    fun checkInsBetweenIncludesTheStartAndExcludesTheEnd() = runTest {
        val first = checkIns.save(checkInAt(nine))
        val second = checkIns.save(checkInAt(nine.plusSeconds(3600)))
        checkIns.save(checkInAt(nine.plusSeconds(7200)))

        val found = checkIns.checkInsBetween(nine, nine.plusSeconds(7200)).first()

        assertEquals(listOf(first, second), found.map { it.id })
    }

    @Test
    fun savingAnExistingCheckInReplacesIt() = runTest {
        val id = checkIns.save(checkInAt(nine))

        checkIns.save(checkInAt(nine).copy(id = id, awake = null, focus = 5))

        val listed = checkIns.checkIns().first()
        assertEquals(1, listed.size)
        assertEquals(checkInAt(nine).copy(id = id, awake = null, focus = 5), listed.single())
    }

    @Test
    fun aDeletedCheckInIsGone() = runTest {
        val id = checkIns.save(checkInAt(nine))

        checkIns.delete(checkIns.checkIn(id)!!)

        assertTrue(checkIns.checkIns().first().isEmpty())
        assertNull(checkIns.checkIn(id))
    }
}

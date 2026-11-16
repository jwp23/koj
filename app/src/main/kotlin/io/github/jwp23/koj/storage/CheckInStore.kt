package io.github.jwp23.koj.storage

import io.github.jwp23.koj.record.CheckIn
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CheckInStore(private val dao: CheckInDao) {
    /** Every check-in, oldest first. */
    fun checkIns(): Flow<List<CheckIn>> = dao.all().map { rows -> rows.map { it.toCheckIn() } }

    /** Check-ins at or after [from] and before [until], oldest first. */
    fun checkInsBetween(from: Instant, until: Instant): Flow<List<CheckIn>> =
        dao.between(from, until).map { rows -> rows.map { it.toCheckIn() } }

    suspend fun checkIn(id: Long): CheckIn? = dao.find(id)?.toCheckIn()

    /** Stores a new check-in (id 0) or replaces the stored one. Returns its id. */
    suspend fun save(checkIn: CheckIn): Long {
        val row = checkIn.toRow()
        if (checkIn.id == 0L) return dao.insert(row)
        dao.update(row)
        return checkIn.id
    }

    suspend fun delete(checkIn: CheckIn) = dao.delete(checkIn.toRow())
}

private fun CheckInRow.toCheckIn() =
    CheckIn(id = id, at = at, zone = zone, awake = awake, focus = focus, calm = calm, kind = kind)

private fun CheckIn.toRow() =
    CheckInRow(
        id = id,
        at = at,
        zone = zone,
        awake = awake,
        focus = focus,
        calm = calm,
        kind = kind,
    )

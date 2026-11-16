package io.github.jwp23.koj.storage

import androidx.room3.ColumnTypeConverter
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** How the database stores the time types its tables use. */
class Converters {
    @ColumnTypeConverter fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @ColumnTypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @ColumnTypeConverter fun durationToNanos(value: Duration): Long = value.toNanos()

    @ColumnTypeConverter fun nanosToDuration(value: Long): Duration = Duration.ofNanos(value)

    @ColumnTypeConverter
    fun localTimeToSecondOfDay(value: LocalTime?): Int? = value?.toSecondOfDay()

    @ColumnTypeConverter
    fun secondOfDayToLocalTime(value: Int?): LocalTime? = value?.let {
        LocalTime.ofSecondOfDay(it.toLong())
    }

    @ColumnTypeConverter fun zoneIdToString(value: ZoneId): String = value.id

    @ColumnTypeConverter fun stringToZoneId(value: String): ZoneId = ZoneId.of(value)
}

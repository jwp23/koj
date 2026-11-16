package io.github.jwp23.koj.storage

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import io.github.jwp23.koj.record.PromptKind
import java.time.Instant
import java.time.ZoneId

@Entity(tableName = "check_in", indices = [Index("at")])
data class CheckInRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Instant,
    val zone: ZoneId,
    val awake: Int?,
    val focus: Int?,
    val calm: Int?,
    val kind: PromptKind,
)

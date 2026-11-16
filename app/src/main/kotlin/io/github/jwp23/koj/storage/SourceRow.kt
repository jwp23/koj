package io.github.jwp23.koj.storage

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import io.github.jwp23.koj.intake.IntakeStyle
import java.time.Duration
import java.time.LocalTime

@Entity(tableName = "source")
data class SourceRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caffeineMgPerServing: Double,
    val theanineMgPerServing: Double,
    val style: IntakeStyle,
    val defaultDuration: Duration,
    val usualTime: LocalTime?,
    val archived: Boolean,
)

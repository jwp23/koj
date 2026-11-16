package io.github.jwp23.koj.storage

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import io.github.jwp23.koj.intake.IntakeStyle
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** A drink keeps its own per-serving amounts; its source's name is read by joining `source`. */
@Entity(
    tableName = "drink",
    foreignKeys =
        [
            ForeignKey(
                entity = SourceRow::class,
                parentColumns = ["id"],
                childColumns = ["sourceId"],
                onDelete = ForeignKey.RESTRICT,
            ),
        ],
    indices = [Index("sourceId"), Index("start")],
)
data class DrinkRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val servings: Double,
    val caffeineMgPerServing: Double,
    val theanineMgPerServing: Double,
    val style: IntakeStyle,
    val defaultDuration: Duration,
    val start: Instant,
    val zone: ZoneId,
    val finish: Instant?,
)

/** A drink row with the current name of its source. */
data class DrinkWithSourceName(@Embedded val drink: DrinkRow, val sourceName: String)

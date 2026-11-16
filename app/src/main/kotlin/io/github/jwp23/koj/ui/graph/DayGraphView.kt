package io.github.jwp23.koj.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jwp23.koj.form.formatMg
import io.github.jwp23.koj.form.formatRatio
import io.github.jwp23.koj.form.formatTime
import io.github.jwp23.koj.graph.CheckInMarker
import io.github.jwp23.koj.graph.DayGraph
import io.github.jwp23.koj.graph.GraphPoint
import io.github.jwp23.koj.graph.IntakeMarker
import io.github.jwp23.koj.intake.Amounts
import java.time.Instant
import java.time.ZoneId

private val GraphHeight = 200.dp

/** The smallest a marker's touch target may be. */
private val TouchTarget = 48.dp

/**
 * Half a touch target. The lines are inset by this much at each side, so a marker centred on either
 * end of the span still lies wholly inside the box and can be tapped.
 */
private val MarkerInset = 24.dp

/**
 * How far a marker's visible mark stands in from each side of its touch target: (48 - 12) / 2, so a
 * mark is 12 dp across and its rounded ends are centred on the times it marks.
 */
private val MarkInset = 18.dp

/** How tall a marker's visible mark is. */
private val MarkSize = 12.dp

/** One day's lines, projection, and markers. Tapping a marker shows its details. */
@Composable
fun DayGraphView(graph: DayGraph, zone: ZoneId, modifier: Modifier = Modifier) {
    val caffeineColor = MaterialTheme.colorScheme.primary
    val theanineColor = MaterialTheme.colorScheme.tertiary
    val baselineColor = MaterialTheme.colorScheme.outline
    var openDrink by remember { mutableStateOf<IntakeMarker?>(null) }
    var openCheckIn by remember { mutableStateOf<CheckInMarker?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendEntry("Caffeine", caffeineColor)
            LegendEntry("L-theanine", theanineColor)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(GraphHeight)) {
            // The width the span is drawn across, between the insets at each side.
            val spanWidth = maxWidth - TouchTarget
            Canvas(
                Modifier.fillMaxSize()
                    .padding(
                        start = MarkerInset,
                        end = MarkerInset,
                        top = TouchTarget,
                        bottom = MarkerInset,
                    ),
                contentDescription = "Graph of caffeine and L-theanine in the body",
            ) {
                drawLine(
                    baselineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
                drawAmountLine(graph, graph.past, caffeineColor) { it.caffeineMg }
                drawAmountLine(graph, graph.past, theanineColor) { it.theanineMg }
                val dashes = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                drawAmountLine(graph, graph.projection, caffeineColor, dashes) { it.caffeineMg }
                drawAmountLine(graph, graph.projection, theanineColor, dashes) { it.theanineMg }
            }
            for (marker in graph.intakeMarkers) {
                IntakeMarkerTarget(marker, graph, spanWidth, zone) { openDrink = marker }
            }
            for (marker in graph.checkInMarkers) {
                CheckInMarkerTarget(
                    marker,
                    graph,
                    spanWidth,
                    zone,
                    Modifier.align(Alignment.BottomStart),
                ) {
                    openCheckIn = marker
                }
            }
        }
        graph.caffeineAtBedtimeMg?.let { Text("At bedtime: ${formatMg(it)} caffeine") }
        val taken = graph.taken
        Text(
            "Taken: ${formatMg(taken.caffeineMg)} caffeine, " +
                "${formatMg(taken.theanineMg)} L-theanine, " +
                "ratio ${formatRatio(taken.theanineToCaffeineRatio)}",
        )
    }
    openDrink?.let { DrinkDetails(it, zone, onClose = { openDrink = null }) }
    openCheckIn?.let { CheckInDetails(it, zone, onClose = { openCheckIn = null }) }
}

/**
 * A drink's marker along the top of the box: a dot for a fast drink, a bar from start to end for a
 * sipped one, inside a touch target at least [TouchTarget] square.
 */
@Composable
private fun IntakeMarkerTarget(
    marker: IntakeMarker,
    graph: DayGraph,
    spanWidth: Dp,
    zone: ZoneId,
    onOpen: () -> Unit,
) {
    val color = MaterialTheme.colorScheme.secondary
    Box(
        Modifier.offset(x = spanWidth * graph.position(marker.start))
            .size(
                width = TouchTarget + spanWidth * graph.lengthBetween(marker.start, marker.end),
                height = TouchTarget,
            )
            .semantics {
                contentDescription = "Drink: ${marker.sourceName} at ${marker.start.timeIn(zone)}"
            }
            .clickable(onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth()
                .padding(horizontal = MarkInset)
                .height(MarkSize)
                .background(color, CircleShape),
        )
    }
}

/**
 * A check-in's marker: a ring on the baseline, inside a touch target [TouchTarget] square whose
 * centre is on the baseline.
 */
@Composable
private fun CheckInMarkerTarget(
    marker: CheckInMarker,
    graph: DayGraph,
    spanWidth: Dp,
    zone: ZoneId,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier
            .offset(x = spanWidth * graph.position(marker.at))
            .size(TouchTarget)
            .semantics { contentDescription = "Check-in at ${marker.at.timeIn(zone)}" }
            .clickable(onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(MarkSize).border(2.dp, color, CircleShape))
    }
}

@Composable
private fun DrinkDetails(marker: IntakeMarker, zone: ZoneId, onClose: () -> Unit) {
    val start = marker.start.timeIn(zone)
    val time = if (marker.end == marker.start) start else "$start to ${marker.end.timeIn(zone)}"
    Details(
        title = marker.sourceName,
        lines =
            listOf(
                time,
                "${formatMg(marker.taken.caffeineMg)} caffeine",
                "${formatMg(marker.taken.theanineMg)} L-theanine",
                "Taken ratio: ${formatRatio(marker.taken.theanineToCaffeineRatio)}",
            ),
        onClose = onClose,
    )
}

@Composable
private fun CheckInDetails(marker: CheckInMarker, zone: ZoneId, onClose: () -> Unit) {
    Details(
        title = "Check-in at ${marker.at.timeIn(zone)}",
        lines =
            listOf(
                "Awake ${marker.awake.rating()} · Focus ${marker.focus.rating()} · " +
                    "Calm ${marker.calm.rating()}",
            ),
        onClose = onClose,
    )
}

/** A marker's details, one per line, until closed. */
@Composable
private fun Details(title: String, lines: List<String>, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
        title = { Text(title) },
        text = { Column { lines.forEach { Text(it) } } },
    )
}

/** A rating as a number, or "–" when it was not given. */
private fun Int?.rating(): String = this?.toString() ?: "–"

/** This instant's time of day in [zone], as HH:mm. */
private fun Instant.timeIn(zone: ZoneId): String = formatTime(atZone(zone).toLocalTime())

@Composable
private fun LegendEntry(name: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 16.dp, height = 3.dp).background(color))
        Text(name)
    }
}

/** One substance's line through [points], dashed when [pathEffect] says so. */
private fun DrawScope.drawAmountLine(
    graph: DayGraph,
    points: List<GraphPoint>,
    color: Color,
    pathEffect: PathEffect? = null,
    amount: (Amounts) -> Double,
) {
    val path = Path()
    points.forEachIndexed { index, point ->
        val x = graph.position(point.at) * size.width
        val y = size.height * (1f - graph.level(amount(point.inBody)))
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect))
}

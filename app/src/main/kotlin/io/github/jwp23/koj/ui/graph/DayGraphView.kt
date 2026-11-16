package io.github.jwp23.koj.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.jwp23.koj.form.formatMg
import io.github.jwp23.koj.form.formatRatio
import io.github.jwp23.koj.graph.DayGraph
import io.github.jwp23.koj.graph.GraphPoint
import io.github.jwp23.koj.intake.Amounts
import java.time.ZoneId

private val GraphHeight = 200.dp

/** The smallest a marker's touch target may be. */
private val TouchTarget = 48.dp

/**
 * Half a touch target. The lines are inset by this much at each side, so a marker centred on either
 * end of the span still lies wholly inside the box and can be tapped.
 */
private val MarkerInset = 24.dp

/** One day's lines, projection, and markers. Tapping a marker shows its details. */
@Composable
fun DayGraphView(graph: DayGraph, zone: ZoneId, modifier: Modifier = Modifier) {
    val caffeineColor = MaterialTheme.colorScheme.primary
    val theanineColor = MaterialTheme.colorScheme.tertiary
    val baselineColor = MaterialTheme.colorScheme.outline
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendEntry("Caffeine", caffeineColor)
            LegendEntry("L-theanine", theanineColor)
        }
        Box(Modifier.fillMaxWidth().height(GraphHeight)) {
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
            }
        }
        val taken = graph.taken
        Text(
            "Taken: ${formatMg(taken.caffeineMg)} caffeine, " +
                "${formatMg(taken.theanineMg)} L-theanine, " +
                "ratio ${formatRatio(taken.theanineToCaffeineRatio)}",
        )
    }
}

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

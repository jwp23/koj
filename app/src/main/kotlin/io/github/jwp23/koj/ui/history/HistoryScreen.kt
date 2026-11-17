package io.github.jwp23.koj.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jwp23.koj.form.formatDate
import io.github.jwp23.koj.form.formatDecimal
import io.github.jwp23.koj.form.formatMg
import io.github.jwp23.koj.form.formatRatio
import io.github.jwp23.koj.form.formatTime
import io.github.jwp23.koj.history.CheckInEntry
import io.github.jwp23.koj.history.DrinkEntry
import io.github.jwp23.koj.history.HistoryDay
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.ui.graph.DayGraphView
import io.github.jwp23.koj.ui.log.Heading
import java.time.Instant
import java.time.ZoneId

@Composable
fun HistoryScreen(
    model: HistoryViewModel,
    onEditDrink: (Long) -> Unit,
    onEditCheckIn: (Long) -> Unit,
) {
    val loaded by model.state.collectAsStateWithLifecycle()
    val state = loaded
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Heading("History")
        when {
            state == null -> CircularProgressIndicator()
            state.days.isEmpty() -> Text("Nothing logged yet.")
            else ->
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (day in state.days) {
                        dayItems(day, state, onEditDrink, onEditCheckIn)
                    }
                }
        }
    }
}

private fun LazyListScope.dayItems(
    day: HistoryDay,
    state: HistoryState,
    onEditDrink: (Long) -> Unit,
    onEditCheckIn: (Long) -> Unit,
) {
    item(key = "date ${day.date}") {
        Text(
            formatDate(day.date),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
    item(key = "graph ${day.date}") {
        val graph = remember(state, day) { state.graph(day) }
        DayGraphView(graph, day.zone)
    }
    for (entry in day.entries()) {
        when (entry) {
            is DrinkEntry ->
                item(key = "drink ${entry.drink.id}") { DrinkRow(entry.drink, onEditDrink) }
            is CheckInEntry ->
                item(key = "check-in ${entry.checkIn.id}") {
                    CheckInRow(entry.checkIn, onEditCheckIn)
                }
        }
    }
}

@Composable
private fun DrinkRow(drink: LoggedDrink, onEdit: (Long) -> Unit) {
    val time = timeIn(drink.start, drink.zone)
    val taken = drink.taken
    val progress = if (drink.inProgress) ", in progress" else ""
    Column(
        Modifier.fillMaxWidth()
            .clickable { onEdit(drink.id) }
            .semantics { contentDescription = "Edit ${drink.sourceName} at $time" },
    ) {
        Text("$time ${drink.sourceName} × ${formatDecimal(drink.servings)}")
        Text(
            "${formatMg(taken.caffeineMg)} caffeine, ${formatMg(taken.theanineMg)} L-theanine, " +
                "ratio ${formatRatio(taken.theanineToCaffeineRatio)}$progress",
        )
    }
}

@Composable
private fun CheckInRow(checkIn: CheckIn, onEdit: (Long) -> Unit) {
    val time = timeIn(checkIn.at, checkIn.zone)
    Column(
        Modifier.fillMaxWidth()
            .clickable { onEdit(checkIn.id) }
            .semantics { contentDescription = "Edit check-in at $time" },
    ) {
        Text(
            "$time Check-in: Awake ${rating(checkIn.awake)} · Focus ${rating(checkIn.focus)} · " +
                "Calm ${rating(checkIn.calm)}",
        )
    }
}

private fun rating(value: Int?): String = value?.toString() ?: "–"

private fun timeIn(at: Instant, zone: ZoneId): String = formatTime(at.atZone(zone).toLocalTime())

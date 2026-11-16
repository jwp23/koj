package io.github.jwp23.koj.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jwp23.koj.form.formatMg
import io.github.jwp23.koj.form.formatTime
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.ui.graph.DayGraphView

@Composable
fun TodayScreen(
    model: TodayViewModel,
    onLogWithOptions: (Long) -> Unit,
    onOpenSources: () -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Today",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        val loaded = state ?: return@LazyColumn
        item { Text("Caffeine in body: ${formatMg(loaded.inBody.caffeineMg)}") }
        item { Text("L-theanine in body: ${formatMg(loaded.inBody.theanineMg)}") }
        item { DayGraphView(loaded.graph, loaded.zone) }
        if (loaded.sources.isEmpty()) {
            item { Text("Add a source to start logging.") }
            item { Button(onClick = onOpenSources) { Text("Sources") } }
        }
        items(loaded.sources, key = { "source-${it.id}" }) { source ->
            SourceRow(source, onLog = { model.logOneServing(source) }, onLogWithOptions)
        }
        if (loaded.inProgress.isNotEmpty()) {
            item {
                Text(
                    "In progress",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            items(loaded.inProgress, key = { "drink-${it.id}" }) { drink ->
                InProgressRow(drink, loaded, onFinish = { model.finish(drink) })
            }
        }
    }
}

@Composable
private fun SourceRow(source: Source, onLog: () -> Unit, onLogWithOptions: (Long) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onLog, Modifier.weight(1f)) { Text(source.name) }
        Button(
            onClick = { onLogWithOptions(source.id) },
            Modifier.semantics { contentDescription = "Log ${source.name} with options" },
        ) {
            Text("Options")
        }
    }
}

@Composable
private fun InProgressRow(drink: LoggedDrink, state: TodayState, onFinish: () -> Unit) {
    val since = formatTime(drink.start.atZone(state.zone).toLocalTime())
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${drink.sourceName} since $since", Modifier.weight(1f))
        Button(
            onClick = onFinish,
            Modifier.semantics { contentDescription = "Finish ${drink.sourceName}" },
        ) {
            Text("Finish")
        }
    }
}

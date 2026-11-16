package io.github.jwp23.koj.ui.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jwp23.koj.form.formatMg
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.Source

@Composable
fun SourcesScreen(model: SourcesViewModel, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val sources by model.state.collectAsStateWithLifecycle()
    Column(Modifier.padding(16.dp)) {
        Text(
            "Sources",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (sources.isEmpty()) Text("No sources yet.", Modifier.padding(vertical = 8.dp))
        LazyColumn {
            items(sources, key = { it.id }) { source ->
                Column(
                    Modifier.fillMaxWidth()
                        .clickable { onEdit(source.id) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(source.name, style = MaterialTheme.typography.titleMedium)
                    Text(source.summary(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Button(onClick = onAdd, Modifier.padding(top = 8.dp)) { Text("Add source") }
    }
}

private fun Source.summary(): String {
    val amounts =
        "${formatMg(caffeineMgPerServing)} caffeine · ${formatMg(theanineMgPerServing)} L-theanine"
    return when (style) {
        IntakeStyle.FAST -> "$amounts · Fast"
        IntakeStyle.SIPPED -> "$amounts · Sipped, ${defaultDuration.toMinutes()} min"
    }
}

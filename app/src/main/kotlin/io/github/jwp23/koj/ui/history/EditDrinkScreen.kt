package io.github.jwp23.koj.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jwp23.koj.form.DrinkField
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.ui.DateField
import io.github.jwp23.koj.ui.TimeField
import io.github.jwp23.koj.ui.log.Heading
import io.github.jwp23.koj.ui.log.ServingsField
import io.github.jwp23.koj.ui.log.drinkFieldMessage

@Composable
fun EditDrinkScreen(model: EditDrinkViewModel, onDone: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (val current = state) {
            EditDrinkState.Loading -> {
                Heading("Edit drink")
                CircularProgressIndicator()
            }
            EditDrinkState.NotFound -> {
                Heading("Edit drink")
                Text("This drink is no longer available.")
                Button(onClick = onDone) { Text("Back to History") }
            }
            is EditDrinkState.Editing -> Editing(current, model, onDone)
        }
    }
}

@Composable
private fun ColumnScope.Editing(
    state: EditDrinkState.Editing,
    model: EditDrinkViewModel,
    onDone: () -> Unit,
) {
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val form = state.form
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Heading("Edit drink")
        Text(state.drink.sourceName)
        ServingsField(
            form.servings,
            state.errors,
            onChange = { model.change(form.copy(servings = it)) },
            Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField(
                "Start date",
                form.startDate,
                onDateChange = { model.change(form.copy(startDate = it)) },
            )
            TimeField(
                "Start time",
                form.startTime,
                onTimeChange = { model.change(form.copy(startTime = it)) },
            )
        }
        if (state.drink.style == IntakeStyle.SIPPED) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    "Finish date",
                    form.finishDate,
                    onDateChange = { model.change(form.copy(finishDate = it)) },
                )
                TimeField(
                    "Finish time",
                    form.finishTime,
                    onTimeChange = { model.change(form.copy(finishTime = it)) },
                )
            }
            if (form.finishDate != null || form.finishTime != null) {
                TextButton(
                    onClick = { model.change(form.copy(finishDate = null, finishTime = null)) },
                ) {
                    Text("Clear finish")
                }
            }
        }
        for (error in state.errors - setOf(DrinkField.SERVINGS, DrinkField.SERVINGS_TOO_MANY)) {
            Text(drinkFieldMessage(error), color = MaterialTheme.colorScheme.error)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { model.save(onDone) }) { Text("Save") }
        OutlinedButton(onClick = { confirmingDelete = true }) { Text("Delete") }
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this drink?") },
            confirmButton = { TextButton(onClick = { model.delete(onDone) }) { Text("Delete") } },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") }
            },
        )
    }
}

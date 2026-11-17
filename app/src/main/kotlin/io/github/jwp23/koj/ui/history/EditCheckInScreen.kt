package io.github.jwp23.koj.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jwp23.koj.form.CheckInField
import io.github.jwp23.koj.ui.log.Heading

@Composable
fun EditCheckInScreen(model: EditCheckInViewModel, onDone: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (val current = state) {
            EditCheckInState.Loading -> {
                Heading("Edit check-in")
                CircularProgressIndicator()
            }
            EditCheckInState.NotFound -> {
                Heading("Edit check-in")
                Text("This check-in is no longer available.")
                Button(onClick = onDone) { Text("Back to History") }
            }
            is EditCheckInState.Editing -> Editing(current, model, onDone)
        }
    }
}

@Composable
private fun ColumnScope.Editing(
    state: EditCheckInState.Editing,
    model: EditCheckInViewModel,
    onDone: () -> Unit,
) {
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val form = state.form
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Heading("Edit check-in")
        RatingField("Awake (1 to 5)", form.awake, CheckInField.AWAKE in state.errors) {
            model.change(form.copy(awake = it))
        }
        RatingField("Focus (1 to 5)", form.focus, CheckInField.FOCUS in state.errors) {
            model.change(form.copy(focus = it))
        }
        RatingField("Calm (1 to 5)", form.calm, CheckInField.CALM in state.errors) {
            model.change(form.copy(calm = it))
        }
        if (CheckInField.NO_RATING in state.errors) {
            Text("Enter at least one rating", color = MaterialTheme.colorScheme.error)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { model.save(onDone) }, enabled = !state.busy) { Text("Save") }
        OutlinedButton(onClick = { confirmingDelete = true }, enabled = !state.busy) {
            Text("Delete")
        }
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this check-in?") },
            confirmButton = {
                TextButton(onClick = { model.delete(onDone) }, enabled = !state.busy) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun RatingField(
    label: String,
    value: String,
    isError: Boolean,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = isError,
        supportingText =
            if (isError) ({ Text("Enter a whole number from 1 to 5, or leave it empty") })
            else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

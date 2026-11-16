package io.github.jwp23.koj.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jwp23.koj.form.SourceField
import io.github.jwp23.koj.form.SourceForm
import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.ui.TimeField
import java.time.LocalTime

@Composable
fun EditSourceScreen(model: EditSourceViewModel, onDone: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val form = state.form
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (state.stored == null) "New source" else "Edit source",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        FormField("Name", form.name, SourceField.NAME in state.errors, "Enter a name") {
            model.change(form.copy(name = it))
        }
        FormField(
            "Caffeine per serving (mg)",
            form.caffeineMg,
            SourceField.CAFFEINE in state.errors,
            "Enter a number, 0 or more",
            KeyboardType.Decimal,
        ) {
            model.change(form.copy(caffeineMg = it))
        }
        FormField(
            "L-theanine per serving (mg)",
            form.theanineMg,
            SourceField.THEANINE in state.errors,
            "Enter a number, 0 or more",
            KeyboardType.Decimal,
        ) {
            model.change(form.copy(theanineMg = it))
        }
        StyleChoice(form.style) { model.change(form.copy(style = it)) }
        if (form.style == IntakeStyle.SIPPED) {
            FormField(
                "Sipping time (minutes)",
                form.durationMinutes,
                SourceField.DURATION in state.errors,
                "Enter whole minutes, 1 or more",
                KeyboardType.Number,
            ) {
                model.change(form.copy(durationMinutes = it))
            }
        }
        UsualTime(form) { model.change(form.copy(usualTime = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { model.save(onDone) }) { Text("Save") }
            if (state.stored != null) {
                OutlinedButton(onClick = { confirmingDelete = true }) { Text("Delete") }
            }
        }
    }
    val stored = state.stored
    if (confirmingDelete && stored != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete ${stored.name}?") },
            confirmButton = {
                TextButton(onClick = { model.delete(onDone) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    isError: Boolean,
    message: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = isError,
        supportingText = if (isError) ({ Text(message) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StyleChoice(style: IntakeStyle, onChange: (IntakeStyle) -> Unit) {
    val choices = listOf(IntakeStyle.FAST to "Fast", IntakeStyle.SIPPED to "Sipped")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        choices.forEachIndexed { index, (choice, label) ->
            SegmentedButton(
                selected = style == choice,
                onClick = { onChange(choice) },
                shape = SegmentedButtonDefaults.itemShape(index, choices.size),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun UsualTime(form: SourceForm, onChange: (LocalTime?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeField("Usual time", form.usualTime, onTimeChange = onChange)
        if (form.usualTime != null) {
            TextButton(onClick = { onChange(null) }) { Text("Clear usual time") }
        }
    }
}

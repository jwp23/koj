package io.github.jwp23.koj.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jwp23.koj.form.DrinkField
import io.github.jwp23.koj.form.MAX_SERVINGS
import io.github.jwp23.koj.form.formatDecimal
import io.github.jwp23.koj.form.formatMg
import io.github.jwp23.koj.ui.DateField
import io.github.jwp23.koj.ui.TimeField

@Composable
fun LogDrinkScreen(model: LogDrinkViewModel, onDone: () -> Unit) {
    val loaded by model.state.collectAsStateWithLifecycle()
    val state = loaded
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state == null) {
            Heading("Log a drink")
            CircularProgressIndicator()
            return@Column
        }
        val form = state.form
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Heading("Log ${state.source.name}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val error =
                    state.errors.firstOrNull {
                        it == DrinkField.SERVINGS || it == DrinkField.SERVINGS_TOO_MANY
                    }
                OutlinedTextField(
                    value = form.servings,
                    onValueChange = { model.change(form.copy(servings = it)) },
                    label = { Text("Servings") },
                    isError = error != null,
                    supportingText = error?.let { { Text(message(it)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = model::justASip) { Text("Just a sip") }
            }
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
            if (DrinkField.START_IN_FUTURE in state.errors) {
                Text(message(DrinkField.START_IN_FUTURE), color = MaterialTheme.colorScheme.error)
            }
            state.amounts?.let {
                Text(
                    "${formatMg(it.caffeineMg)} caffeine, ${formatMg(it.theanineMg)} L-theanine",
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { model.log(onDone) }) { Text("Log") }
            OutlinedButton(onClick = onDone) { Text("Cancel") }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
}

private fun message(field: DrinkField): String =
    when (field) {
        DrinkField.SERVINGS -> "Enter a number above 0"
        DrinkField.SERVINGS_TOO_MANY -> "Enter at most ${formatDecimal(MAX_SERVINGS)} servings"
        DrinkField.START_IN_FUTURE -> "The start cannot be in the future"
        else -> error("The Log a drink screen has no $field")
    }

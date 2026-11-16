package io.github.jwp23.koj.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jwp23.koj.form.SettingsField
import io.github.jwp23.koj.form.SettingsForm
import io.github.jwp23.koj.form.formatTime
import io.github.jwp23.koj.ui.TimeField
import java.time.LocalTime

private const val NUMBER_ABOVE_ZERO = "Enter a number above 0"

@Composable
fun SettingsScreen(model: SettingsViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        val current = state
        if (current == null) {
            CircularProgressIndicator()
            return@Column
        }
        val form = current.form
        val errors = current.errors
        SettingsFields(form, errors, model::change)
        Button(onClick = model::save) { Text("Save") }
        if (current.saved) Text("Saved")
    }
}

@Composable
private fun SettingsFields(
    form: SettingsForm,
    errors: Set<SettingsField>,
    onChange: (SettingsForm) -> Unit,
) {
    NumberField(
        "Caffeine half-life (hours)",
        form.caffeineHalfLifeHours,
        SettingsField.CAFFEINE_HALF_LIFE in errors,
        NUMBER_ABOVE_ZERO,
        KeyboardType.Decimal,
    ) {
        onChange(form.copy(caffeineHalfLifeHours = it))
    }
    NumberField(
        "Caffeine absorption half-life (minutes)",
        form.caffeineAbsorptionMinutes,
        SettingsField.CAFFEINE_ABSORPTION in errors,
        NUMBER_ABOVE_ZERO,
        KeyboardType.Decimal,
    ) {
        onChange(form.copy(caffeineAbsorptionMinutes = it))
    }
    NumberField(
        "L-theanine half-life (minutes)",
        form.theanineHalfLifeMinutes,
        SettingsField.THEANINE_HALF_LIFE in errors,
        NUMBER_ABOVE_ZERO,
        KeyboardType.Decimal,
    ) {
        onChange(form.copy(theanineHalfLifeMinutes = it))
    }
    NumberField(
        "L-theanine absorption half-life (minutes)",
        form.theanineAbsorptionMinutes,
        SettingsField.THEANINE_ABSORPTION in errors,
        NUMBER_ABOVE_ZERO,
        KeyboardType.Decimal,
    ) {
        onChange(form.copy(theanineAbsorptionMinutes = it))
    }
    Text(
        "These start from averages in published studies. Caffeine's half-life differs widely " +
            "between people.",
        style = MaterialTheme.typography.bodySmall,
    )
    NumberField(
        "Check-ins after a drink (minutes, separated by commas)",
        form.afterDrinkCheckInMinutes,
        SettingsField.AFTER_DRINK_CHECK_INS in errors,
        "Enter whole minutes, 1 or more, separated by commas",
        KeyboardType.Text,
    ) {
        onChange(form.copy(afterDrinkCheckInMinutes = it))
    }
    FixedCheckIns(form, onChange)
    NumberField(
        "Full check-ins per day, at most",
        form.dailyCap,
        SettingsField.DAILY_CAP in errors,
        "Enter a whole number, 0 or more",
        KeyboardType.Number,
    ) {
        onChange(form.copy(dailyCap = it))
    }
    TimeField("Morning time", form.morningTime, { onChange(form.copy(morningTime = it)) })
    TimeField("Quiet from", form.quietTime, { onChange(form.copy(quietTime = it)) })
    TimeField("Bedtime", form.bedtime, { onChange(form.copy(bedtime = it)) })
    NumberField(
        "A sip, as a fraction of a serving",
        form.sipServings,
        SettingsField.SIP in errors,
        NUMBER_ABOVE_ZERO,
        KeyboardType.Decimal,
    ) {
        onChange(form.copy(sipServings = it))
    }
}

@Composable
private fun FixedCheckIns(form: SettingsForm, onChange: (SettingsForm) -> Unit) {
    Text("Check-ins at fixed times", style = MaterialTheme.typography.titleMedium)
    if (form.fixedCheckInTimes.isEmpty()) Text("None")
    for (time in form.fixedCheckInTimes) {
        FixedCheckInRow(time) { onChange(form.withoutFixedCheckIn(time)) }
    }
    TimeField("Add a check-in time", null, { onChange(form.withFixedCheckIn(it)) })
}

@Composable
private fun FixedCheckInRow(time: LocalTime, onRemove: () -> Unit) {
    val text = formatTime(time)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text)
        TextButton(
            onClick = onRemove,
            modifier = Modifier.semantics { contentDescription = "Remove check-in at $text" },
        ) {
            Text("Remove")
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    isError: Boolean,
    message: String,
    keyboardType: KeyboardType,
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

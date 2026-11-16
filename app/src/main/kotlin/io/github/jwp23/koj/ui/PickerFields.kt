package io.github.jwp23.koj.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.jwp23.koj.form.formatDate
import io.github.jwp23.koj.form.formatTime
import io.github.jwp23.koj.form.pickedDate
import io.github.jwp23.koj.form.pickerMillis
import java.time.LocalDate
import java.time.LocalTime

/**
 * A button reading "[label]: HH:mm", or [label] alone while [time] is null. Tapping it opens a
 * 24-hour clock that can be switched to keyboard entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    time: LocalTime?,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { picking = true }, modifier = modifier) {
        Text(if (time == null) label else "$label: ${formatTime(time)}")
    }
    if (picking) {
        val state = rememberTimePickerState(time?.hour ?: 0, time?.minute ?: 0, is24Hour = true)
        var mode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
        TimePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime.of(state.hour, state.minute))
                        picking = false
                    },
                ) {
                    Text("OK")
                }
            },
            title = { Text(label) },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        mode =
                            if (mode == TimePickerDisplayMode.Picker) TimePickerDisplayMode.Input
                            else TimePickerDisplayMode.Picker
                    },
                    displayMode = mode,
                )
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) {
            if (mode == TimePickerDisplayMode.Picker) TimePicker(state) else TimeInput(state)
        }
    }
}

/**
 * A button reading "[label]: yyyy-MM-dd", or [label] alone while [date] is null. Tapping it opens a
 * calendar.
 */
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { picking = true }, modifier = modifier) {
        Text(if (date == null) label else "$label: ${formatDate(date)}")
    }
    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date?.let(::pickerMillis))
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onDateChange(pickedDate(it)) }
                        picking = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) {
            DatePicker(state)
        }
    }
}

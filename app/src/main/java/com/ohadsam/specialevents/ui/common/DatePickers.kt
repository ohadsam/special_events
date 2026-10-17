package com.ohadsam.specialevents.ui.common

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ohadsam.specialevents.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GregorianDatePickerDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = picker.selectedDateMillis != null,
                onClick = { picker.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) { DatePicker(state = picker) }
}

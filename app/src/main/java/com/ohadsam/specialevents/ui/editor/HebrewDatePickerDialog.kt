package com.ohadsam.specialevents.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewDate

private const val DEFAULT_LEAP_YEAR = 5784
private const val MIN_YEAR = 5000
private const val MAX_YEAR = 6000

/**
 * Picks a Hebrew date: year (typed), month and day (menus). With [hasYear] false the year is not stored, so the months are
 * listed for a leap year (all 13) and 30 Cheshvan/Kislev/Adar I stay selectable.
 */
@Composable
fun HebrewDatePickerDialog(initial: HebrewDate, hasYear: Boolean, onDismiss: () -> Unit, onConfirm: (HebrewDate) -> Unit) {
    var yearText by remember { mutableStateOf(initial.year.toString()) }
    var month by remember { mutableStateOf(initial.month) }
    var day by remember { mutableStateOf(initial.day) }

    val year = yearText.toIntOrNull()?.takeIf { it in MIN_YEAR..MAX_YEAR }
    val layoutYear = if (hasYear) (year ?: initial.year) else DEFAULT_LEAP_YEAR
    val months = HebrewCalendar.monthsInOrder(layoutYear)
    if (month !in months) month = months.first()
    val daysInMonth = HebrewCalendar.daysInMonth(layoutYear, month)
    if (day > daysInMonth) day = daysInMonth

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_pick_hebrew_date)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasYear) {
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.editor_hebrew_year)) },
                        supportingText = {
                            Text(if (year != null) HebrewCalendar.yearLabel(year) else stringResource(R.string.editor_hebrew_year_invalid), color = if (year != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                        },
                        singleLine = true,
                        isError = year == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Menu(
                        label = monthLabel(layoutYear, month, hasYear),
                        items = months.map { it to monthLabel(layoutYear, it, hasYear) },
                        onPick = { month = it },
                        modifier = Modifier.weight(1f),
                    )
                    Menu(
                        label = day.toString(),
                        items = (1..daysInMonth).map { it to it.toString() },
                        onPick = { day = it },
                        modifier = Modifier.weight(0.6f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !hasYear || year != null,
                onClick = { onConfirm(if (hasYear) HebrewDate(year ?: initial.year, month, day) else referenceDate(initial.year, month, day)) },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Month name as the user knows it; with no year, month 12 reads "אדר / אדר א׳" because it depends on the year. */
private fun monthLabel(year: Int, month: Int, hasYear: Boolean): String =
    if (!hasYear && month == ADAR) "אדר / אדר א׳" else HebrewCalendar.monthName(year, month)

private const val ADAR = 12

@Composable
private fun Menu(label: String, items: List<Pair<Int, String>>, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(label) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onPick(value)
                        open = false
                    },
                )
            }
        }
    }
}

/** With no stored year, any year in which the month and day exist will do (e.g. a leap year for Adar II, a long Cheshvan for the 30th). */
private fun referenceDate(startYear: Int, month: Int, day: Int): HebrewDate {
    val year = (startYear..startYear + SEARCH_YEARS).firstOrNull {
        month <= HebrewCalendar.monthsInYear(it) && day <= HebrewCalendar.daysInMonth(it, month)
    } ?: DEFAULT_LEAP_YEAR
    return HebrewDate(year, month, day)
}

private const val SEARCH_YEARS = 40

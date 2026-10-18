package com.ohadsam.specialevents.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.views.EventJump
import com.ohadsam.specialevents.ui.common.defaultEmoji
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private const val MONTHS_PER_ROW = 3
private const val MONTHS_IN_YEAR = 12

/** Type a name, pick the event: the calendar then opens on its next day. */
@Composable
fun EventJumpDialog(events: List<SpecialEvent>, onPick: (SpecialEvent) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = remember(query, events) { EventJump.matches(events, query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_jump_event)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.calendar_jump_event_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (query.isNotBlank() && matches.isEmpty()) Text(stringResource(R.string.calendar_jump_event_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                matches.forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onPick(e) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(e.iconKey ?: e.type.defaultEmoji(), style = MaterialTheme.typography.titleLarge)
                        Text(e.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Year stepper and a grid of months: two taps to reach any month. */
@Composable
fun MonthYearPickerDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    var year by remember { mutableIntStateOf(initial.year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { year-- }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_year)) }
                Text(year.toString(), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { year++ }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_year)) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..MONTHS_IN_YEAR).chunked(MONTHS_PER_ROW).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { m ->
                            val selected = year == initial.year && m == initial.monthValue
                            val label = Month.of(m).getDisplayName(TextStyle.FULL, Locale("he"))
                            val modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            if (selected) {
                                FilledTonalButton(onClick = { onPick(LocalDate.of(year, m, 1)) }, modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)) { Text(label, maxLines = 1) }
                            } else {
                                OutlinedButton(onClick = { onPick(LocalDate.of(year, m, 1)) }, modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)) { Text(label, maxLines = 1) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

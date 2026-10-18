package com.ohadsam.specialevents.ui.upcoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.views.GroupBy
import com.ohadsam.specialevents.domain.views.SortBy
import com.ohadsam.specialevents.ui.common.GregorianDatePickerDialog
import com.ohadsam.specialevents.ui.common.gregorianShort
import com.ohadsam.specialevents.ui.common.labelRes
import java.time.LocalDate

/** Grouping, order and the less common filters of the upcoming list. Changes apply at once; nothing to confirm. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewOptionsSheet(state: UpcomingState, actions: ViewActions, onDismiss: () -> Unit) {
    var pickingFrom by rememberSaveable { mutableStateOf(false) }
    var pickingTo by rememberSaveable { mutableStateOf(false) }
    val spec = state.spec
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading(R.string.view_group_by)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupBy.entries.forEach { g -> FilterChip(selected = spec.groupBy == g, onClick = { actions.setGroupBy(g) }, label = { Text(stringResource(g.labelRes())) }) }
            }
            Heading(R.string.view_sort_by)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortBy.entries.forEach { o -> FilterChip(selected = spec.sortBy == o, onClick = { actions.setSortBy(o) }, label = { Text(stringResource(o.labelRes())) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = spec.ascending, onClick = { actions.setAscending(true) }, label = { Text(stringResource(R.string.view_ascending)) })
                FilterChip(selected = !spec.ascending, onClick = { actions.setAscending(false) }, label = { Text(stringResource(R.string.view_descending)) })
            }

            Heading(R.string.view_filter)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.view_milestones_only), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = spec.filter.milestonesOnly, onCheckedChange = actions.setMilestonesOnly)
            }
            if (state.allTags.isNotEmpty()) {
                Text(stringResource(R.string.view_tags), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.allTags.forEach { t -> FilterChip(selected = t in spec.filter.tags, onClick = { actions.toggleTag(t) }, label = { Text(t) }) }
                }
            }
            Text(stringResource(R.string.view_date_range), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pickingFrom = true }) { Text(spec.filter.from?.let { stringResource(R.string.view_from, gregorianShort(it)) } ?: stringResource(R.string.view_from_any)) }
                OutlinedButton(onClick = { pickingTo = true }) { Text(spec.filter.to?.let { stringResource(R.string.view_to, gregorianShort(it)) } ?: stringResource(R.string.view_to_any)) }
                if (spec.filter.from != null || spec.filter.to != null) TextButton(onClick = { actions.setRange(null, null) }) { Text(stringResource(R.string.action_clear)) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = actions.clearFilters, enabled = spec.filter.isActive) { Text(stringResource(R.string.view_clear_filters)) }
                TextButton(onClick = actions.resetView) { Text(stringResource(R.string.view_reset)) }
            }
        }
    }
    if (pickingFrom) {
        GregorianDatePickerDialog(
            initial = spec.filter.from ?: LocalDate.now(),
            onDismiss = { pickingFrom = false },
            onConfirm = {
                pickingFrom = false
                actions.setRange(it, spec.filter.to?.takeIf { to -> !to.isBefore(it) })
            },
        )
    }
    if (pickingTo) {
        GregorianDatePickerDialog(
            initial = spec.filter.to ?: spec.filter.from ?: LocalDate.now(),
            onDismiss = { pickingTo = false },
            onConfirm = {
                pickingTo = false
                actions.setRange(spec.filter.from?.takeIf { from -> !from.isAfter(it) }, it)
            },
        )
    }
}

@Composable
private fun Heading(res: Int) = Text(stringResource(res), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))

/** The things the list screen can ask of its view model; one object instead of a dozen lambdas. */
data class ViewActions(
    val toggleType: (com.ohadsam.specialevents.domain.EventType) -> Unit,
    val toggleTag: (String) -> Unit,
    val setMilestonesOnly: (Boolean) -> Unit,
    val setRange: (LocalDate?, LocalDate?) -> Unit,
    val clearFilters: () -> Unit,
    val setGroupBy: (GroupBy) -> Unit,
    val setSortBy: (SortBy) -> Unit,
    val setAscending: (Boolean) -> Unit,
    val resetView: () -> Unit,
    val showInCalendar: (com.ohadsam.specialevents.domain.UpcomingItem) -> Unit,
)

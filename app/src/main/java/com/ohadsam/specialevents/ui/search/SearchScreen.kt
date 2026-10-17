package com.ohadsam.specialevents.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.search.SavedSearch
import com.ohadsam.specialevents.domain.search.SearchStep
import com.ohadsam.specialevents.domain.search.SearchSuggestions
import com.ohadsam.specialevents.ui.common.GregorianDatePickerDialog
import com.ohadsam.specialevents.ui.common.gregorianShort
import com.ohadsam.specialevents.ui.common.labelRes
import com.ohadsam.specialevents.ui.upcoming.EventCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SearchViewModel, onBack: () -> Unit, onOpenEvent: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                title = {
                    TextField(
                        value = state.input,
                        onValueChange = viewModel::onInput,
                        placeholder = { Text(stringResource(R.string.search_hint_smart)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.commit() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
                actions = {
                    if (state.hasQuery) {
                        IconButton(onClick = { saving = true }) { Icon(Icons.Outlined.Star, contentDescription = stringResource(R.string.search_save)) }
                        IconButton(onClick = viewModel::clear) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.search_clear)) }
                    }
                    IconButton(onClick = { filtersOpen = !filtersOpen }) { Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.search_filters)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StepChips(state, viewModel)
            if (state.input.isNotBlank()) {
                TextButton(onClick = viewModel::commit, modifier = Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.search_refine)) }
            }
            if (filtersOpen) AdvancedFilters(state, viewModel)
            if (state.hasQuery) {
                Results(state, onOpenEvent)
            } else {
                Suggestions(state, viewModel)
            }
        }
    }

    if (saving) SaveDialog(onDismiss = { saving = false }, onSave = {
        saving = false
        viewModel.saveCurrent(it)
    })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepChips(state: SearchState, vm: SearchViewModel) {
    if (state.steps.isEmpty()) return
    FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.committed.forEachIndexed { i, step ->
            InputChip(
                selected = true,
                onClick = { vm.removeStep(i) },
                label = { Text(step.label) },
                trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.search_remove_step, step.label)) },
            )
        }
        state.live.forEach { step -> AssistChip(onClick = {}, label = { Text(step.label) }) }
    }
}

@Composable
private fun Results(state: SearchState, onOpenEvent: (Long) -> Unit) {
    if (state.results.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.search_no_results_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.search_no_results_refine),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }
    Text(stringResource(R.string.search_result_count, state.results.size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.results, key = { "${it.event.id}-${it.kind}-${it.date}" }) { item ->
            EventCard(item, state.settings.calendarMode, onClick = { onOpenEvent(item.event.id) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(state: SearchState, vm: SearchViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.search_help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.saved.isNotEmpty()) {
            Text(stringResource(R.string.search_saved), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            state.saved.forEach { s -> SavedRow(s, onClick = { vm.applySaved(s) }, onDelete = { vm.deleteSaved(s.name) }) }
        }
        if (state.recent.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.search_recent), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp).weight(1f))
                TextButton(onClick = vm::clearRecent) { Text(stringResource(R.string.search_clear_recent)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.recent.forEach { SuggestionChip(onClick = { vm.applyPhrase(it) }, label = { Text(it) }) } }
        }
        Text(stringResource(R.string.search_suggested), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SearchSuggestions.PHRASES.forEach { SuggestionChip(onClick = { vm.applyPhrase(it) }, label = { Text(it) }) } }
    }
}

@Composable
private fun SavedRow(search: SavedSearch, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(search.name, style = MaterialTheme.typography.titleSmall)
                Text(search.steps.joinToString(" · ") { it.label }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.search_delete_saved, search.name)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedFilters(state: SearchState, vm: SearchViewModel) {
    val types = state.committed.filterIsInstance<SearchStep.Types>().firstOrNull()?.types.orEmpty()
    val tags = state.committed.filterIsInstance<SearchStep.Tags>().firstOrNull()?.names.orEmpty()
    val range = state.committed.filterIsInstance<SearchStep.Range>().firstOrNull()
    val age = state.committed.filterIsInstance<SearchStep.Age>().firstOrNull()
    var pickFrom by rememberSaveable { mutableStateOf(false) }
    var pickTo by rememberSaveable { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.search_filter_types), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventType.entries.forEach { t -> FilterChip(selected = t in types, onClick = { vm.toggleType(t) }, label = { Text(stringResource(t.labelRes())) }) }
            }
            if (state.knownTags.isNotEmpty()) {
                Text(stringResource(R.string.search_filter_tags), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.knownTags.forEach { t -> FilterChip(selected = tags.any { it.equals(t.name, true) }, onClick = { vm.toggleTag(t.name) }, label = { Text(t.name) }) }
                }
            }
            Text(stringResource(R.string.search_filter_dates), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pickFrom = true }) { Text(range?.from?.let { gregorianShort(it) } ?: stringResource(R.string.search_filter_from)) }
                OutlinedButton(onClick = { pickTo = true }) { Text(range?.to?.let { gregorianShort(it) } ?: stringResource(R.string.search_filter_to)) }
                if (range != null) TextButton(onClick = { vm.setRange(null, null) }) { Text(stringResource(R.string.search_filter_reset)) }
            }
            Text(stringResource(R.string.search_filter_age), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeField(age?.min, stringResource(R.string.search_filter_age_min), Modifier.weight(1f)) { vm.setAge(it, age?.max) }
                AgeField(age?.max, stringResource(R.string.search_filter_age_max), Modifier.weight(1f)) { vm.setAge(age?.min, it) }
            }
            SwitchLine(stringResource(R.string.search_filter_round), state.committed.contains(SearchStep.RoundOnly)) { vm.toggleRound() }
            SwitchLine(stringResource(R.string.search_filter_shabbat), state.committed.contains(SearchStep.ShabbatOnly)) { vm.toggleShabbat() }
            SwitchLine(stringResource(R.string.search_filter_archived), state.includeArchived) { vm.setIncludeArchived(it) }
        }
    }
    if (pickFrom) {
        GregorianDatePickerDialog(range?.from ?: LocalDate.now(), onDismiss = { pickFrom = false }) {
            pickFrom = false
            vm.setRange(it, range?.to?.takeIf { to -> to >= it })
        }
    }
    if (pickTo) {
        GregorianDatePickerDialog(range?.to ?: LocalDate.now(), onDismiss = { pickTo = false }) {
            pickTo = false
            vm.setRange(range?.from?.takeIf { from -> from <= it } ?: it, it)
        }
    }
}

@Composable
private fun AgeField(value: Int?, label: String, modifier: Modifier, onChange: (Int?) -> Unit) {
    var text by remember(value) { mutableStateOf(value?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter(Char::isDigit).take(3)
            onChange(text.toIntOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun SwitchLine(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.search_save_title)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.search_save_name)) }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

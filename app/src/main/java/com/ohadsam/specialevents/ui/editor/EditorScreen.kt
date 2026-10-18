package com.ohadsam.specialevents.ui.editor

import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.HelpButton
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.domain.EditorDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.ui.common.EventAvatar
import com.ohadsam.specialevents.ui.common.GregorianDatePickerDialog
import com.ohadsam.specialevents.ui.common.gregorianLong
import com.ohadsam.specialevents.ui.common.gregorianNoYear
import com.ohadsam.specialevents.ui.common.labelRes
import com.ohadsam.specialevents.ui.common.weekdayName
import java.time.Instant
import com.ohadsam.specialevents.domain.datetext.DateParse
import com.ohadsam.specialevents.domain.datetext.DateParseError
import com.ohadsam.specialevents.domain.datetext.ParseWarning
import com.ohadsam.specialevents.domain.datetext.ParsedDate
import java.time.LocalDate
import java.time.ZoneOffset

private val PALETTE = listOf(0xFFE57373, 0xFFFFB74D, 0xFFFFD54F, 0xFF81C784, 0xFF4DB6AC, 0xFF64B5F6, 0xFF9575CD, 0xFFF06292, 0xFFA1887F, 0xFF90A4AE)
    .map { it.toInt() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel, onClose: () -> Unit, onSaved: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val requestClose = { if (viewModel.isDirty()) confirmDiscard = true else onClose() }
    BackHandler(onBack = requestClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = requestClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close)) }
                },
                actions = {
                    HelpButton(HelpTopic.EDITOR)
                    TextButton(onClick = viewModel::save, enabled = !state.saving && !state.loading) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TypeChips(state.type, viewModel::setType)
            PersonSection(state, viewModel)
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text(stringResource(titleLabel(state.type))) },
                isError = state.titleError,
                supportingText = if (state.titleError) {
                    { Text(stringResource(R.string.editor_title_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.type == EventType.BIRTHDAY || state.type == EventType.YAHRZEIT || state.type == EventType.BAR_BAT_MITZVAH) {
                GenderChips(state.gender, viewModel::setGender)
            }
            DateTextField(state.dateText, state.dateParse, viewModel::setDateText, viewModel::pickParsedYear)
            DateSection(state.date, state.yearOptional, viewModel::setDate)
            MoreOptions(state, viewModel)
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.editor_discard_title)) },
            text = { Text(stringResource(R.string.editor_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onClose()
                }) { Text(stringResource(R.string.editor_discard_action)) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.editor_discard_keep)) } },
        )
    }
}

@Composable
private fun PersonSection(state: EditorState, vm: EditorViewModel) {
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::onPhotoPicked) }
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri -> uri?.let(vm::onContactPicked) }
    val accent = state.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        EventAvatar(state.photoUri, state.emoji, accent, 72.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(stringResource(if (state.photoUri == null) R.string.editor_photo_add else R.string.editor_photo_change))
                }
                if (state.photoUri != null) TextButton(onClick = vm::removePhoto) { Text(stringResource(R.string.editor_photo_remove)) }
            }
            if (state.contactLookupUri == null) {
                TextButton(onClick = { pickContact.launch(null) }) { Text(stringResource(R.string.editor_contact_link)) }
            } else {
                InputChip(
                    selected = true,
                    onClick = vm::unlinkContact,
                    label = { Text(stringResource(R.string.editor_contact_linked, state.contactName ?: state.title)) },
                    trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.editor_contact_unlink), modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@StringRes
private fun titleLabel(type: EventType): Int = when (type) {
    EventType.BIRTHDAY, EventType.YAHRZEIT, EventType.BAR_BAT_MITZVAH -> R.string.editor_label_person
    EventType.ANNIVERSARY -> R.string.editor_label_couple
    else -> R.string.editor_label_title
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeChips(selected: EventType, onSelect: (EventType) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EventType.entries.forEach { type ->
            FilterChip(selected = type == selected, onClick = { onSelect(type) }, label = { Text(stringResource(type.labelRes())) })
        }
    }
}

@Composable
private fun GenderChips(selected: Gender?, onSelect: (Gender?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.editor_gender), style = MaterialTheme.typography.labelLarge)
        FilterChip(selected = selected == Gender.MALE, onClick = { onSelect(if (selected == Gender.MALE) null else Gender.MALE) }, label = { Text(stringResource(R.string.gender_male)) })
        FilterChip(selected = selected == Gender.FEMALE, onClick = { onSelect(if (selected == Gender.FEMALE) null else Gender.FEMALE) }, label = { Text(stringResource(R.string.gender_female)) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSection(date: EditorDate, yearOptional: Boolean, onChange: (EditorDate) -> Unit) {
    var pickGregorian by rememberSaveable { mutableStateOf(false) }
    var pickHebrew by rememberSaveable { mutableStateOf(false) }
    val hebrewPrimary = date.basis == CalendarBasis.HEBREW

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.editor_date), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !hebrewPrimary, onClick = { onChange(date.withBasis(CalendarBasis.GREGORIAN)) }, label = { Text(stringResource(R.string.editor_basis_gregorian)) })
            FilterChip(selected = hebrewPrimary, onClick = { onChange(date.withBasis(CalendarBasis.HEBREW)) }, label = { Text(stringResource(R.string.editor_basis_hebrew)) })
        }
        OutlinedButton(onClick = { if (hebrewPrimary) pickHebrew = true else pickGregorian = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(primaryLabel(date), style = MaterialTheme.typography.titleMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(secondaryLabel(date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (yearOptional) {
            CheckRow(stringResource(R.string.editor_year_known), date.hasYear, null) { onChange(date.withHasYear(it)) }
        }
        if (!hebrewPrimary) {
            CheckRow(stringResource(R.string.editor_after_sunset), date.afterSunset, stringResource(R.string.editor_after_sunset_desc)) { onChange(date.withAfterSunset(it)) }
        }
    }

    if (pickGregorian) {
        GregorianDatePickerDialog(
            initial = date.gregorian,
            onDismiss = { pickGregorian = false },
            onConfirm = {
                pickGregorian = false
                onChange(date.withGregorian(it))
            },
        )
    }
    if (pickHebrew) {
        HebrewDatePickerDialog(
            initial = date.hebrew,
            hasYear = date.hasYear,
            onDismiss = { pickHebrew = false },
            onConfirm = {
                pickHebrew = false
                onChange(date.withHebrew(it))
            },
        )
    }
}

/** One line to type the date the way people say it; the result is shown right under it and mirrored in the pickers below. */
@Composable
private fun DateTextField(text: String, parse: DateParse?, onText: (String) -> Unit, onPickYear: (Int) -> Unit) {
    val today = remember { LocalDate.now() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            label = { Text(stringResource(R.string.datetext_label)) },
            placeholder = { Text(stringResource(R.string.datetext_example)) },
            isError = parse is DateParse.Failure && text.length > MIN_ERROR_CHARS,
            modifier = Modifier.fillMaxWidth(),
        )
        when (parse) {
            null -> Text(stringResource(R.string.datetext_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            is DateParse.Failure -> if (text.length > MIN_ERROR_CHARS) {
                Text(stringResource(parse.error.messageRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            is DateParse.Ok -> ParsedSummary(parse.date, today, onPickYear)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParsedSummary(date: ParsedDate, today: LocalDate, onPickYear: (Int) -> Unit) {
    val editorDate = date.toEditorDate(today)
    if (date.needsYear) {
        Text(stringResource(R.string.datetext_pick_year, date.weekday?.let { weekdayHe(it) }.orEmpty()), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            date.candidates.forEach { y -> FilterChip(selected = false, onClick = { onPickYear(y) }, label = { Text(y.toString()) }) }
        }
        return
    }
    Text(
        stringResource(R.string.datetext_understood, primaryLabel(editorDate), secondaryLabel(editorDate)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    date.warnings.forEach { w ->
        Text(warningText(w, date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun warningText(w: ParseWarning, date: ParsedDate): String = when (w) {
    ParseWarning.WEEKDAY_MISMATCH -> stringResource(R.string.datetext_w_weekday, date.weekday?.let { weekdayHe(it) }.orEmpty(), date.actualWeekday?.let { weekdayHe(it) }.orEmpty())
    ParseWarning.AMBIGUOUS_YEAR -> stringResource(R.string.datetext_w_ambiguous)
    ParseWarning.TWO_DIGIT_YEAR -> stringResource(R.string.datetext_w_two_digit, date.year ?: 0)
    ParseWarning.ADAR_READ_AS_SECOND -> stringResource(R.string.datetext_w_adar)
    ParseWarning.AGE_UNRESOLVED -> stringResource(R.string.datetext_w_age)
}

private fun DateParseError.messageRes(): Int = when (this) {
    DateParseError.EMPTY -> R.string.datetext_e_empty
    DateParseError.NO_MONTH -> R.string.datetext_e_month
    DateParseError.NO_DAY -> R.string.datetext_e_day
    DateParseError.INVALID_DATE -> R.string.datetext_e_invalid
}

private fun weekdayHe(day: java.time.DayOfWeek): String = weekdayName(LocalDate.of(2026, 10, 18).with(java.time.temporal.TemporalAdjusters.nextOrSame(day)))

private const val MIN_ERROR_CHARS = 5

@Composable
private fun primaryLabel(date: EditorDate): String = when (date.basis) {
    CalendarBasis.GREGORIAN -> if (date.hasYear) gregorianLong(date.gregorian) else gregorianNoYear(date.gregorian)
    CalendarBasis.HEBREW -> date.hebrew.format(withYear = date.hasYear)
}

/** The equivalent date in the other calendar, with the weekday when the year is known. */
@Composable
private fun secondaryLabel(date: EditorDate): String = when (date.basis) {
    CalendarBasis.GREGORIAN -> stringResource(R.string.editor_equiv_hebrew, date.hebrew.format(withYear = date.hasYear))
    CalendarBasis.HEBREW -> {
        val text = if (date.hasYear) "${weekdayName(date.gregorian)}, ${gregorianLong(date.gregorian)}" else gregorianNoYear(date.gregorian)
        stringResource(R.string.editor_equiv_gregorian, text)
    }
}

@Composable
private fun CheckRow(title: String, checked: Boolean, description: String?, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreOptions(state: EditorState, vm: EditorViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(stringResource(if (open) R.string.editor_less_options else R.string.editor_more_options)) }
    if (!open) return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.relation,
            onValueChange = vm::setRelation,
            label = { Text(stringResource(R.string.editor_relation)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.notes,
            onValueChange = vm::setNotes,
            label = { Text(stringResource(R.string.editor_notes)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.editor_icon), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = state.chosenEmoji == null, onClick = { vm.setEmoji(null) }, label = { Text(stringResource(R.string.editor_icon_auto, state.emoji)) })
            state.emojiOptions.forEach { e ->
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(if (state.chosenEmoji == e) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { vm.setEmoji(e) },
                    contentAlignment = Alignment.Center,
                ) { Text(e, fontSize = 24.sp) }
            }
        }
        Text(stringResource(R.string.editor_color), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ColorSwatch(null, state.colorArgb == null) { vm.setColor(null) }
            PALETTE.forEach { c -> ColorSwatch(c, state.colorArgb == c) { vm.setColor(c) } }
        }
        Text(stringResource(R.string.editor_tags), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.tags.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = { vm.removeTag(tag) },
                    label = { Text(tag) },
                    trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.editor_tag_remove, tag), modifier = Modifier.size(18.dp)) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.newTag,
                onValueChange = vm::setNewTag,
                label = { Text(stringResource(R.string.editor_tag_new)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { vm.addTag() }) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.editor_tag_add)) }
        }
        if (state.allGroups.isNotEmpty()) {
            Text(stringResource(R.string.editor_groups), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.allGroups.forEach { g ->
                    FilterChip(selected = g.id in state.groupIds, onClick = { vm.toggleGroup(g.id) }, label = { Text(listOfNotNull(g.emoji, g.name).joinToString(" ")) })
                }
            }
        }
        val suggestions = state.knownTags.filter { k -> state.tags.none { it.equals(k.name, ignoreCase = true) } }
        if (suggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { SuggestionChip(onClick = { vm.addTag(it.name) }, label = { Text(it.name) }) }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Int?, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (color == null) R.string.editor_color_none else R.string.editor_color_pick)
    Box(
        Modifier.minimumInteractiveComponentSize().size(40.dp).clip(CircleShape)
            .background(color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
            .border(BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { if (color == null) Text("∅") }
}

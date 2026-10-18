package com.ohadsam.specialevents.ui.settings

import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.HelpButton
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.BuildConfig
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.AdarInLeapYear
import com.ohadsam.specialevents.calendar.LeapDay
import com.ohadsam.specialevents.calendar.ShortMonthThirtieth
import com.ohadsam.specialevents.calendar.TzeitMethod
import com.ohadsam.specialevents.location.Places
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.data.prefs.ThemeMode
import com.ohadsam.specialevents.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onOpenLogs: () -> Unit, onOpenReminderHealth: () -> Unit, onImportContacts: () -> Unit, onOpenTemplates: () -> Unit, onOpenBackup: () -> Unit, onOpenCalendarSync: () -> Unit, onOpenGroups: () -> Unit, onOpenGuide: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showWhatsNew by remember { mutableStateOf(false) }
    var confirmResetAll by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }, actions = { HelpButton(HelpTopic.SETTINGS) }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_section_appearance))
            ChoiceRow(
                title = stringResource(R.string.settings_theme),
                options = ThemeMode.entries.map { it to stringResource(it.labelRes()) },
                selected = settings.themeMode,
                onSelect = { v -> viewModel.update { it.copy(themeMode = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                description = stringResource(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.string.settings_dynamic_color_desc else R.string.settings_dynamic_color_unsupported),
                checked = settings.dynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onChange = { v -> viewModel.update { it.copy(dynamicColor = v) } },
            )
            ResetRow(viewModel::resetAppearance)

            SectionHeader(stringResource(R.string.settings_section_calendar))
            ChoiceRow(
                title = stringResource(R.string.settings_calendar_mode),
                options = CalendarMode.entries.map { it to stringResource(it.labelRes()) },
                selected = settings.calendarMode,
                onSelect = { v -> viewModel.update { it.copy(calendarMode = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_in_israel),
                description = stringResource(R.string.settings_in_israel_desc),
                checked = settings.inIsrael,
                onChange = { v -> viewModel.update { it.copy(inIsrael = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_week_starts_sunday),
                checked = settings.weekStartsOnSunday,
                onChange = { v -> viewModel.update { it.copy(weekStartsOnSunday = v) } },
            )
            ResetRow(viewModel::resetCalendar)

            SectionHeader(stringResource(R.string.settings_section_date_rules))
            ChoiceRow(
                title = stringResource(R.string.settings_rule_adar),
                description = stringResource(R.string.settings_rule_adar_desc),
                options = AdarInLeapYear.entries.map { it to stringResource(it.labelRes()) },
                selected = settings.dateRules.adar,
                onSelect = { v -> viewModel.update { it.copy(dateRules = it.dateRules.copy(adar = v)) } },
            )
            ChoiceRow(
                title = stringResource(R.string.settings_rule_thirtieth),
                description = stringResource(R.string.settings_rule_thirtieth_desc),
                options = ShortMonthThirtieth.entries.map { it to stringResource(it.labelRes()) },
                selected = settings.dateRules.thirtiethOfShortMonth,
                onSelect = { v -> viewModel.update { it.copy(dateRules = it.dateRules.copy(thirtiethOfShortMonth = v)) } },
            )
            ChoiceRow(
                title = stringResource(R.string.settings_rule_leap_day),
                description = stringResource(R.string.settings_rule_leap_day_desc),
                options = LeapDay.entries.map { it to stringResource(it.labelRes()) },
                selected = settings.dateRules.leapDay,
                onSelect = { v -> viewModel.update { it.copy(dateRules = it.dateRules.copy(leapDay = v)) } },
            )
            ResetRow(viewModel::resetDateRules)

            SectionHeader(stringResource(R.string.settings_section_reminders))
            SwitchRow(
                title = stringResource(R.string.settings_reminders_enabled),
                description = stringResource(R.string.settings_reminders_enabled_desc),
                checked = settings.remindersEnabled,
                onChange = { v -> viewModel.update { it.copy(remindersEnabled = v) } },
            )
            ChoiceRow(
                title = stringResource(R.string.settings_place),
                description = stringResource(R.string.settings_place_desc),
                options = Places.ALL.map { it.id to it.nameHe },
                selected = settings.placeId,
                onSelect = viewModel::setPlace,
            )
            ChoiceRow(
                title = stringResource(R.string.settings_tzeit),
                options = TzeitMethod.entries.map { it to stringResource(it.labelRes()) },
                selected = settings.tzeit,
                onSelect = { v -> viewModel.update { it.copy(tzeit = v) } },
            )
            AboutRow(Icons.Outlined.HealthAndSafety, R.string.settings_reminder_health, R.string.settings_reminder_health_desc, onOpenReminderHealth)
            ResetRow(viewModel::resetReminders)

            SectionHeader(stringResource(R.string.settings_section_greetings))
            AboutRow(Icons.Outlined.Groups, R.string.settings_groups, R.string.settings_groups_desc, onOpenGroups)
            AboutRow(Icons.Outlined.Message, R.string.settings_templates, R.string.settings_templates_desc, onOpenTemplates)
            TextRow(
                title = stringResource(R.string.settings_self_phone),
                value = settings.selfPhone,
                placeholder = stringResource(R.string.settings_not_set),
                description = stringResource(R.string.settings_self_desc),
                onChange = { v -> viewModel.update { it.copy(selfPhone = v) } },
            )
            TextRow(
                title = stringResource(R.string.settings_self_email),
                value = settings.selfEmail,
                placeholder = stringResource(R.string.settings_not_set),
                onChange = { v -> viewModel.update { it.copy(selfEmail = v) } },
            )
            TextRow(
                title = stringResource(R.string.settings_country_code),
                value = settings.countryCode,
                placeholder = "972",
                description = stringResource(R.string.settings_country_code_desc),
                onChange = { v -> viewModel.update { it.copy(countryCode = v.filter(Char::isDigit).ifEmpty { "972" }) } },
            )
            ResetRow(viewModel::resetGreetings)

            SectionHeader(stringResource(R.string.settings_section_backup))
            AboutRow(Icons.Outlined.Backup, R.string.settings_backup, R.string.settings_backup_desc, onOpenBackup)
            AboutRow(Icons.Outlined.Sync, R.string.settings_calsync, R.string.settings_calsync_desc, onOpenCalendarSync)

            SectionHeader(stringResource(R.string.settings_section_general))
            AboutRow(Icons.AutoMirrored.Outlined.MenuBook, R.string.settings_guide, R.string.settings_guide_desc, onOpenGuide)
            AboutRow(Icons.Outlined.Contacts, R.string.settings_import_contacts, R.string.settings_import_contacts_desc, onImportContacts)
            AboutRow(Icons.Outlined.NewReleases, R.string.settings_whats_new, null) { showWhatsNew = true }
            AboutRow(Icons.Outlined.BugReport, R.string.settings_logs, R.string.settings_logs_desc, onOpenLogs)
            AboutRow(Icons.Outlined.RestartAlt, R.string.settings_reset_all, R.string.settings_reset_all_desc) { confirmResetAll = true }
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                supportingContent = { Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) },
            )
        }
    }
    if (showWhatsNew) WhatsNewDialog(onDismiss = { showWhatsNew = false })
    if (confirmResetAll) {
        AlertDialog(
            onDismissRequest = { confirmResetAll = false },
            title = { Text(stringResource(R.string.settings_reset_all_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_all_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAll()
                    confirmResetAll = false
                }) { Text(stringResource(R.string.settings_reset_all_action)) }
            },
            dismissButton = { TextButton(onClick = { confirmResetAll = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun AboutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, subtitle: Int?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(stringResource(title)) },
        supportingContent = if (subtitle != null) {
            { Text(stringResource(subtitle)) }
        } else {
            null
        },
    )
}

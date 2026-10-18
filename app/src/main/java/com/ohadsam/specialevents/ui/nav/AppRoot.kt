package com.ohadsam.specialevents.ui.nav

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ohadsam.specialevents.AppContainer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ohadsam.specialevents.ui.backup.BackupScreen
import com.ohadsam.specialevents.ui.backup.BackupViewModel
import com.ohadsam.specialevents.ui.calendar.CalendarScreen
import com.ohadsam.specialevents.ui.calsync.CalendarSyncScreen
import com.ohadsam.specialevents.ui.calsync.CalendarSyncViewModel
import com.ohadsam.specialevents.ui.checklist.ChecklistScreen
import com.ohadsam.specialevents.ui.checklist.ChecklistViewModel
import com.ohadsam.specialevents.ui.common.ViewModelFactory
import com.ohadsam.specialevents.ui.upcoming.UpcomingScreen
import com.ohadsam.specialevents.ui.contacts.ImportContactsScreen
import com.ohadsam.specialevents.ui.contacts.ImportContactsViewModel
import com.ohadsam.specialevents.ui.detail.DetailScreen
import com.ohadsam.specialevents.ui.greetings.GreetingScreen
import com.ohadsam.specialevents.ui.greetings.GreetingViewModel
import com.ohadsam.specialevents.ui.greetings.TemplatesScreen
import com.ohadsam.specialevents.ui.detail.DetailViewModel
import com.ohadsam.specialevents.ui.editor.EditorScreen
import com.ohadsam.specialevents.ui.help.GuideScreen
import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.LocalOpenGuide
import com.ohadsam.specialevents.ui.groups.GroupEditorScreen
import com.ohadsam.specialevents.ui.groups.GroupEditorViewModel
import com.ohadsam.specialevents.ui.groups.GroupsScreen
import com.ohadsam.specialevents.ui.groups.GroupsViewModel
import com.ohadsam.specialevents.ui.editor.EditorViewModel
import com.ohadsam.specialevents.ui.print.PrintScreen
import com.ohadsam.specialevents.ui.print.PrintViewModel
import com.ohadsam.specialevents.ui.reminders.EventRemindersScreen
import com.ohadsam.specialevents.ui.reminders.EventRemindersViewModel
import com.ohadsam.specialevents.ui.search.SearchScreen
import com.ohadsam.specialevents.ui.share.ShareScreen
import com.ohadsam.specialevents.ui.share.ShareViewModel
import com.ohadsam.specialevents.ui.settings.ReminderHealthScreen
import com.ohadsam.specialevents.ui.settings.LogsScreen
import com.ohadsam.specialevents.ui.settings.SettingsScreen

@Composable
fun AppRoot(container: AppContainer, pendingRoute: String? = null, onRouteHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    val openGuide: (HelpTopic) -> Unit = { nav.navigate(Routes.guide(it.key)) { launchSingleTop = true } }
    CompositionLocalProvider(LocalOpenGuide provides openGuide) { AppScaffold(container, nav, pendingRoute, onRouteHandled) }
}

@Composable
private fun AppScaffold(container: AppContainer, nav: NavHostController, pendingRoute: String?, onRouteHandled: () -> Unit) {
    val factory = remember { ViewModelFactory(container) }
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let {
            nav.navigate(it) { launchSingleTop = true }
            onRouteHandled()
        }
    }
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = TopLevel.entries.any { it.route == currentRoute }

    Scaffold(
        // Insets are handled by the bottom bar and by each screen's own Scaffold; avoid double padding.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    TopLevel.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                nav.navigate(dest.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = TopLevel.UPCOMING.route,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(TopLevel.UPCOMING.route) {
                UpcomingScreen(
                    viewModel(factory = factory),
                    onAddEvent = { nav.navigate(Routes.editor(0)) },
                    onOpenEvent = { nav.navigate(Routes.detail(it)) },
                    onOpenSearch = { nav.navigate(Routes.SEARCH) },
                    onOpenReminderHealth = { nav.navigate(Routes.REMINDER_HEALTH) },
                    onImportContacts = { nav.navigate(Routes.IMPORT_CONTACTS) },
                    onOpenCalendar = {
                        nav.navigate(TopLevel.CALENDAR.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(TopLevel.CALENDAR.route) {
                CalendarScreen(
                    viewModel(factory = factory),
                    onOpenEvent = { nav.navigate(Routes.detail(it)) },
                    onAddEvent = { nav.navigate(Routes.editor(0, it)) },
                )
            }
            composable(TopLevel.SETTINGS.route) {
                SettingsScreen(
                    viewModel(factory = factory),
                    onOpenLogs = { nav.navigate(Routes.LOGS) },
                    onOpenReminderHealth = { nav.navigate(Routes.REMINDER_HEALTH) },
                    onImportContacts = { nav.navigate(Routes.IMPORT_CONTACTS) },
                    onOpenTemplates = { nav.navigate(Routes.TEMPLATES) },
                    onOpenBackup = { nav.navigate(Routes.backup()) },
                    onOpenCalendarSync = { nav.navigate(Routes.CALENDAR_SYNC) },
                    onOpenGroups = { nav.navigate(Routes.GROUPS) },
                    onOpenPrint = { nav.navigate(Routes.print()) },
                    onOpenGuide = { nav.navigate(Routes.guide()) },
                )
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: DetailViewModel = viewModel(key = "detail-$id", factory = DetailViewModel.Factory(container, id))
                DetailScreen(vm, onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.editor(id)) }, onEditReminders = { nav.navigate(Routes.eventReminders(id)) }, onGreet = { nav.navigate(Routes.greeting(id)) }, onOpenChecklist = { nav.navigate(Routes.checklist(id)) }, onShare = { nav.navigate(Routes.share(listOf(id))) }, onPrintCard = { nav.navigate(Routes.print(id)) })
            }
            composable(
                Routes.EDITOR,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("date") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val date = entry.arguments?.getString("date")?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                val vm: EditorViewModel = viewModel(key = "editor-$id-$date", factory = EditorViewModel.Factory(container, id, date))
                EditorScreen(vm, onClose = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(Routes.SEARCH) {
                SearchScreen(viewModel(factory = factory), onBack = { nav.popBackStack() }, onOpenEvent = { nav.navigate(Routes.detail(it)) })
            }
            composable(Routes.EVENT_REMINDERS, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: EventRemindersViewModel = viewModel(key = "reminders-$id", factory = EventRemindersViewModel.Factory(container, id))
                EventRemindersScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.GREETING,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("day") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("rule") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val day = entry.arguments?.getLong("day")?.takeIf { it >= 0 }
                val rule = entry.arguments?.getLong("rule")?.takeIf { it >= 0 }
                val vm: GreetingViewModel = viewModel(key = "greeting-$id-$day-$rule", factory = GreetingViewModel.Factory(container, id, day, rule))
                GreetingScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.CHECKLIST, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: ChecklistViewModel = viewModel(key = "checklist-$id", factory = ChecklistViewModel.Factory(container, id))
                ChecklistScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.BACKUP,
                arguments = listOf(
                    navArgument("uri") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val vm: BackupViewModel = viewModel(factory = BackupViewModel.Factory(container))
                BackupScreen(vm, onBack = { nav.popBackStack() }, openUri = entry.arguments?.getString("uri"), onShareEvents = { nav.navigate(Routes.share()) })
            }
            composable(Routes.CALENDAR_SYNC) {
                val vm: CalendarSyncViewModel = viewModel(factory = CalendarSyncViewModel.Factory(container))
                CalendarSyncScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.GUIDE,
                arguments = listOf(
                    navArgument("topic") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                GuideScreen(HelpTopic.byKey(entry.arguments?.getString("topic")), onBack = { nav.popBackStack() })
            }
            composable(
                Routes.SHARE,
                arguments = listOf(
                    navArgument("ids") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val ids = entry.arguments?.getString("ids")?.split(',')?.mapNotNull { it.toLongOrNull() }?.toSet().orEmpty()
                val vm: ShareViewModel = viewModel(key = "share-$ids", factory = ShareViewModel.Factory(container, ids))
                ShareScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.PRINT,
                arguments = listOf(
                    navArgument("event") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                val eventId = entry.arguments?.getLong("event")?.takeIf { it >= 0 }
                val vm: PrintViewModel = viewModel(key = "print-$eventId", factory = PrintViewModel.Factory(container, eventId))
                PrintScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.GROUPS) {
                GroupsScreen(viewModel(factory = GroupsViewModel.Factory(container)), onBack = { nav.popBackStack() }, onOpenGroup = { nav.navigate(Routes.group(it)) })
            }
            composable(Routes.GROUP_EDITOR, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: GroupEditorViewModel = viewModel(key = "group-$id", factory = GroupEditorViewModel.Factory(container, id))
                GroupEditorScreen(vm, onClose = { nav.popBackStack() }, onShare = { ids -> nav.navigate(Routes.share(ids)) })
            }
            composable(Routes.TEMPLATES) {
                TemplatesScreen(viewModel(factory = factory), onBack = { nav.popBackStack() })
            }
            composable(Routes.IMPORT_CONTACTS) {
                val vm: ImportContactsViewModel = viewModel(factory = factory)
                ImportContactsScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.REMINDER_HEALTH) {
                ReminderHealthScreen(container, onBack = { nav.popBackStack() })
            }
            composable(Routes.LOGS) {
                LogsScreen(logger = container.logger, onBack = { nav.popBackStack() })
            }
        }
    }
}

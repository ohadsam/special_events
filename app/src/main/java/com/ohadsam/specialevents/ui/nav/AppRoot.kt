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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.ohadsam.specialevents.ui.editor.EditorViewModel
import com.ohadsam.specialevents.ui.reminders.EventRemindersScreen
import com.ohadsam.specialevents.ui.reminders.EventRemindersViewModel
import com.ohadsam.specialevents.ui.search.SearchScreen
import com.ohadsam.specialevents.ui.settings.ReminderHealthScreen
import com.ohadsam.specialevents.ui.settings.LogsScreen
import com.ohadsam.specialevents.ui.settings.SettingsScreen

@Composable
fun AppRoot(container: AppContainer, pendingRoute: String? = null, onRouteHandled: () -> Unit = {}) {
    val nav = rememberNavController()
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
                    onOpenBackup = { nav.navigate(Routes.BACKUP) },
                )
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: DetailViewModel = viewModel(key = "detail-$id", factory = DetailViewModel.Factory(container, id))
                DetailScreen(vm, onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.editor(id)) }, onEditReminders = { nav.navigate(Routes.eventReminders(id)) }, onGreet = { nav.navigate(Routes.greeting(id)) }, onOpenChecklist = { nav.navigate(Routes.checklist(id)) })
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
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val day = entry.arguments?.getLong("day")?.takeIf { it >= 0 }
                val vm: GreetingViewModel = viewModel(key = "greeting-$id-$day", factory = GreetingViewModel.Factory(container, id, day))
                GreetingScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.CHECKLIST, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: ChecklistViewModel = viewModel(key = "checklist-$id", factory = ChecklistViewModel.Factory(container, id))
                ChecklistScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.BACKUP) {
                val vm: BackupViewModel = viewModel(factory = BackupViewModel.Factory(container))
                BackupScreen(vm, onBack = { nav.popBackStack() })
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

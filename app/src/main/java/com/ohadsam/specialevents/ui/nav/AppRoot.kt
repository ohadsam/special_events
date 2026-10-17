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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ohadsam.specialevents.ui.common.PlaceholderScreen
import com.ohadsam.specialevents.ui.common.ViewModelFactory
import com.ohadsam.specialevents.ui.upcoming.UpcomingScreen
import com.ohadsam.specialevents.ui.settings.LogsScreen
import com.ohadsam.specialevents.ui.settings.SettingsScreen

@Composable
fun AppRoot(container: AppContainer) {
    val nav = rememberNavController()
    val factory = remember { ViewModelFactory(container) }
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
                UpcomingScreen(viewModel(factory = factory), onAddEvent = {}, onOpenEvent = {})
            }
            composable(TopLevel.CALENDAR.route) {
                PlaceholderScreen(R.string.tab_calendar, R.string.calendar_empty_title, R.string.calendar_empty_body)
            }
            composable(TopLevel.SETTINGS.route) {
                SettingsScreen(viewModel(factory = factory), onOpenLogs = { nav.navigate(Routes.LOGS) })
            }
            composable(Routes.LOGS) {
                LogsScreen(logger = container.logger, onBack = { nav.popBackStack() })
            }
        }
    }
}

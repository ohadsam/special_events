package com.ohadsam.specialevents

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.data.prefs.ThemeMode
import com.ohadsam.specialevents.ui.nav.AppRoot
import com.ohadsam.specialevents.ui.theme.SpecialEventsTheme

class MainActivity : ComponentActivity() {
    /** A screen to open, requested by a notification tap (`EXTRA_ROUTE`). */
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute.value = intent?.getStringExtra(EXTRA_ROUTE)
        enableEdgeToEdge()
        val container = (application as SpecialEventsApp).container
        setContent {
            // Hebrew is the default language: lay out right-to-left regardless of device locale (revisit when English UI ships).
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
                val dark = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                SpecialEventsTheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
                    AppRoot(container, pendingRoute.value, onRouteHandled = { pendingRoute.value = null })
                }
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}

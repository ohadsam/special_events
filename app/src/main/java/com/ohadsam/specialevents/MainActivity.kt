package com.ohadsam.specialevents

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.data.prefs.ThemeMode
import com.ohadsam.specialevents.security.AppLockPolicy
import com.ohadsam.specialevents.security.DeviceAuth
import com.ohadsam.specialevents.ui.nav.AppRoot
import com.ohadsam.specialevents.ui.nav.Routes
import com.ohadsam.specialevents.ui.security.LockScreen
import com.ohadsam.specialevents.ui.theme.SpecialEventsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val locked = mutableStateOf(true)
    private val settingsLoaded = mutableStateOf(false)
    private var appSettings = AppSettings()
    private var backgroundedAt: Long? = null

    /** True while the system's confirm screen is up: our activity stops and restarts around it, which must not count as "left the app". */
    private var authInProgress = false
    private val unlock = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        authInProgress = false
        if (result.resultCode == RESULT_OK) locked.value = false
    }
    /** A screen to open, requested by a notification tap (`EXTRA_ROUTE`). */
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute.value = routeOf(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute.value = intent?.let(::routeOf)
        enableEdgeToEdge()
        val container = (application as SpecialEventsApp).container
        lifecycleScope.launch {
            container.settings.settings.collect { latest ->
                val first = !settingsLoaded.value
                appSettings = latest
                applyContentProtection(latest.hideContent)
                settingsLoaded.value = true
                if (first) relock() else if (!latest.appLockEnabled) locked.value = false
            }
        }
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
                    when {
                        !settingsLoaded.value -> Unit
                        locked.value -> LockScreen(onUnlock = ::authenticate)
                        else -> AppRoot(container, pendingRoute.value, onRouteHandled = { pendingRoute.value = null })
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (settingsLoaded.value && !authInProgress && backgroundedAt != null) relock()
    }

    override fun onStop() {
        super.onStop()
        if (!authInProgress) backgroundedAt = SystemClock.elapsedRealtime()
    }

    /** Locks (and asks for the phone's PIN/biometrics) when the settings and the time away say so; otherwise opens. */
    private fun relock() {
        val lock = AppLockPolicy.shouldLock(appSettings.appLockEnabled, DeviceAuth.isSecure(this), backgroundedAt, SystemClock.elapsedRealtime(), appSettings.appLockTimeoutSeconds)
        locked.value = lock
        if (lock) authenticate()
    }

    private fun authenticate() {
        if (authInProgress) return
        val intent = DeviceAuth.intent(this, getString(R.string.lock_prompt_title), null)
        if (intent == null) {
            locked.value = false
            return
        }
        authInProgress = true
        unlock.launch(intent)
    }

    private fun applyContentProtection(hide: Boolean) {
        if (hide) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /** A notification/widget names its screen; a file opened with this app goes to the backup preview. */
    private fun routeOf(intent: Intent): String? =
        intent.getStringExtra(EXTRA_ROUTE) ?: intent.data?.takeIf { intent.action == Intent.ACTION_VIEW }?.let { Routes.backup(it.toString()) }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}

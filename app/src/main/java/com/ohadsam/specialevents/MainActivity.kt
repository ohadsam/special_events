package com.ohadsam.specialevents

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.ohadsam.specialevents.ui.nav.AppRoot
import com.ohadsam.specialevents.ui.theme.SpecialEventsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SpecialEventsApp).container
        setContent {
            // Hebrew is the default language: lay out right-to-left regardless of device locale (revisit when English UI ships).
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SpecialEventsTheme {
                    AppRoot(container)
                }
            }
        }
    }
}

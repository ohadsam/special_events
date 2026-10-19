package com.ohadsam.specialevents.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.theme.SpecialEventsTheme
import kotlinx.coroutines.launch

/** Asked by the launcher when an event widget is placed (and again to reconfigure it): pick which event it shows. */
@OptIn(ExperimentalMaterial3Api::class)
class EventWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Exported for the launcher, so anything else that starts us without a real widget id is turned away.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val container = appContainer
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SpecialEventsTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
                    val all by container.events.allEvents.collectAsStateWithLifecycle(initialValue = emptyList())
                    val events = all.filter { !it.archived }
                    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.widget_event_pick)) }) }) { padding ->
                        if (events.isEmpty()) {
                            Text(stringResource(R.string.widget_event_none), Modifier.padding(padding).padding(16.dp))
                        }
                        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                            items(events, key = { it.id }) { event ->
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { choose(widgetId, event.id) }.padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(event.iconKey ?: event.type.defaultEmoji(), style = MaterialTheme.typography.titleLarge)
                                    Text(event.title, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val appContainer get() = (application as com.ohadsam.specialevents.SpecialEventsApp).container

    private fun choose(widgetId: Int, eventId: Long) {
        lifecycleScope.launch {
            try {
                val glanceId = GlanceAppWidgetManager(this@EventWidgetConfigActivity).getGlanceIdBy(widgetId)
                updateAppWidgetState(this@EventWidgetConfigActivity, glanceId) { it[EventIdKey] = eventId }
                EventWidget().update(this@EventWidgetConfigActivity, glanceId)
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            } catch (e: IllegalArgumentException) {
                // The launcher removed the widget while the picker was open: nothing to configure any more.
                appContainer.logger.w("Widgets", "configure: widget $widgetId is gone", e)
            }
            finish()
        }
    }
}

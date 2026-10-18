package com.ohadsam.specialevents.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.checklist.Checklist
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.checklist.ChecklistKind
import com.ohadsam.specialevents.ui.common.dateLabel
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.nav.Routes
import java.time.LocalDate

/** Which event a placed widget shows: stored per widget (Glance state), chosen in [EventWidgetConfigActivity]. */
val EventIdKey = longPreferencesKey("event_id")
private val ItemIdKey = ActionParameters.Key<Long>("item_id")

/**
 * One event: name, date, countdown and its checklist with tick boxes you can tap right on the home screen. Rows are rebuilt from
 * live data inside the composition (a value captured once in provideGlance would go stale after an update).
 */
class EventWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        provideContent {
            val eventId = currentState<Preferences>()[EventIdKey]
            val events by c.events.allEvents.collectAsState(initial = emptyList())
            val settings by c.settings.settings.collectAsState(initial = AppSettings())
            val items by c.checklists.all.collectAsState(initial = emptyList())
            val event = events.firstOrNull { it.id == eventId }
            GlanceTheme(colors = widgetColors()) {
                Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(10.dp)) {
                    if (event == null) {
                        Text(
                            context.getString(R.string.widget_event_unset),
                            modifier = GlanceModifier.fillMaxWidth().clickable(openApp(context, "upcoming")),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                        )
                        return@Column
                    }
                    val next = EventTimeline.forEvent(event, LocalDate.now(), 1, settings.dateRules).firstOrNull()
                    val mine = Checklist.ordered(items.filter { it.eventId == event.id })
                    Row(GlanceModifier.fillMaxWidth().clickable(openApp(context, Routes.detail(event.id))), verticalAlignment = Alignment.CenterVertically) {
                        Text(event.iconKey ?: event.type.defaultEmoji(), style = TextStyle(fontSize = 22.sp), modifier = GlanceModifier.padding(end = 8.dp))
                        Column(GlanceModifier.defaultWeight()) {
                            Text(event.title, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                            if (next != null) {
                                Text(dateLabel(next.date, next.hebrew, settings.calendarMode), maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                            }
                        }
                        if (next != null) {
                            Text(countdownLabel(context, next.daysUntil), style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                    if (mine.isNotEmpty()) {
                        val progress = Checklist.progress(mine)
                        Text(
                            context.getString(R.string.checklist_progress, progress.done, progress.total),
                            modifier = GlanceModifier.padding(top = 6.dp, bottom = 2.dp),
                            style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        )
                        mine.take(MAX_ROWS).forEach { ChecklistRow(it) }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ChecklistRow(item: ChecklistItem) {
        Row(
            GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(actionRunCallback<ToggleChecklistItem>(actionParametersOf(ItemIdKey to item.id))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (item.done) "☑" else "☐", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 18.sp), modifier = GlanceModifier.padding(end = 8.dp))
            Text(
                (if (item.kind == ChecklistKind.GIFT) "🎁 " else "") + item.title,
                maxLines = 1,
                style = TextStyle(
                    color = if (item.done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                ),
            )
        }
    }

    private companion object {
        const val MAX_ROWS = 6
    }
}

/** Ticks or unticks one checklist item from the widget, then redraws every widget. */
class ToggleChecklistItem : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ItemIdKey] ?: return
        val checklists = context.container.checklists
        val item = checklists.snapshot().firstOrNull { it.id == id } ?: return
        checklists.setDone(item, !item.done)
        WidgetUpdater.updateAll(context)
    }
}

class EventWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventWidget()
}

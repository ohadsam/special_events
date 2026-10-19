package com.ohadsam.specialevents.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import kotlinx.coroutines.flow.first

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
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val events0 = c.events.allEvents.first()
        val settings0 = c.settings.settings.first()
        val items0 = c.checklists.all.first()
        provideContent {
            val eventId = currentState<Preferences>()[EventIdKey]
            val events by c.events.allEvents.collectAsState(initial = events0)
            val settings by c.settings.settings.collectAsState(initial = settings0)
            val items by c.checklists.all.collectAsState(initial = items0)
            val event = events.firstOrNull { it.id == eventId && !it.archived }
            WidgetTheme(settings.dynamicColor) {
                Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(10.dp)) {
                    if (event == null) {
                        // Also the state after the chosen event was deleted: a tap goes straight back to the picker.
                        Text(
                            context.getString(R.string.widget_event_unset),
                            modifier = GlanceModifier.fillMaxWidth().clickable(pickEvent(context, widgetId)),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                        )
                        return@Column
                    }
                    val next = EventTimeline.forEvent(event, LocalDate.now(), 1, settings.dateRules).firstOrNull()
                    val mine = Checklist.ordered(items.filter { it.eventId == event.id })
                    Row(GlanceModifier.fillMaxWidth().clickable(openApp(context, Routes.detail(event.id))), verticalAlignment = Alignment.CenterVertically) {
                        Text(event.iconKey ?: event.type.defaultEmoji(), style = TextStyle(fontSize = 22.sp), modifier = GlanceModifier.padding(end = 8.dp))
                        Column(GlanceModifier.defaultWeight()) {
                            Text(widgetText(context, settings.hideWidgetDetails, event.title), maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                            if (next != null) {
                                Text(dateLabel(next.date, next.hebrew, settings.calendarMode), maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                            }
                        }
                        Text(
                            if (next != null) countdownLabel(context, next.daysUntil) else context.getString(R.string.widget_event_passed),
                            maxLines = 1,
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                    if (mine.isNotEmpty()) {
                        val progress = Checklist.progress(mine)
                        Text(
                            context.getString(R.string.checklist_progress, progress.done, progress.total),
                            modifier = GlanceModifier.padding(top = 6.dp, bottom = 2.dp),
                            style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        )
                        mine.take(MAX_ROWS).forEach { ChecklistRow(it, settings.hideWidgetDetails) }
                        if (mine.size > MAX_ROWS) {
                            Text(
                                context.getString(R.string.widget_event_more, mine.size - MAX_ROWS),
                                modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp).clickable(openApp(context, Routes.checklist(event.id))),
                                style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 11.sp),
                            )
                        }
                    }
                }
            }
        }
    }

    /** A real check box: one touch target for the whole row and a proper checked state for screen readers. */
    @androidx.compose.runtime.Composable
    private fun ChecklistRow(item: ChecklistItem, hide: Boolean) {
        val context = LocalContext.current
        CheckBox(
            checked = item.done,
            onCheckedChange = actionRunCallback<ToggleChecklistItem>(actionParametersOf(ItemIdKey to item.id)),
            text = (if (item.kind == ChecklistKind.GIFT) "\uD83C\uDF81 " else "") + widgetText(context, hide, item.title),
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
            maxLines = 1,
            style = TextStyle(
                color = if (item.done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                fontSize = 13.sp,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
            ),
        )
    }

    private fun pickEvent(context: Context, widgetId: Int): Action = actionStartActivity(
        Intent(context, EventWidgetConfigActivity::class.java)
            .setData(Uri.parse("specialevents://configure/$widgetId"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

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
        EventWidget().updateAll(context)
    }
}

class EventWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventWidget()
}

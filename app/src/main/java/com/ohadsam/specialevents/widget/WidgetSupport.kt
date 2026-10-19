package com.ohadsam.specialevents.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.runtime.Composable
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.MainActivity
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.SpecialEventsApp
import com.ohadsam.specialevents.ui.theme.DarkColors
import com.ohadsam.specialevents.ui.theme.LightColors

val Context.container: AppContainer get() = (applicationContext as SpecialEventsApp).container

/** The app's own palette (light/dark follow the system). */
fun widgetColors() = ColorProviders(light = LightColors, dark = DarkColors)

/** Follows the app's "dynamic colour" setting so a widget never looks like a different app from the screens it opens. */
@Composable
fun WidgetTheme(dynamic: Boolean, content: @Composable () -> Unit) {
    if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) GlanceTheme(content = content) else GlanceTheme(colors = widgetColors(), content = content)
}

/** Opens the app on [route]; the unique data URI keeps the PendingIntents of different rows from colliding. */
fun openApp(context: Context, route: String): Action = actionStartActivity(
    Intent(context, MainActivity::class.java)
        .setData(Uri.parse("specialevents://open/" + Uri.encode(route)))
        .putExtra(MainActivity.EXTRA_ROUTE, route)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
)

/** A name or checklist item as a widget may show it: hidden when the user asked for privacy on the home screen. */
fun widgetText(context: Context, hide: Boolean, text: String): String = if (hide) context.getString(R.string.widget_hidden_title) else text

/** The same words as the app's countdown pill. */
fun countdownLabel(context: Context, days: Long): String = when (days) {
    0L -> context.getString(R.string.countdown_today)
    1L -> context.getString(R.string.countdown_tomorrow)
    2L -> context.getString(R.string.countdown_day_after)
    else -> context.getString(R.string.countdown_days, days)
}

/** A small tappable label (the + button and the like); the padding around the text keeps the touch target at least 48dp. */
@Composable
fun Pill(text: String, description: String, action: Action, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text,
        modifier = modifier.cornerRadius(16.dp).background(GlanceTheme.colors.secondaryContainer).padding(horizontal = 14.dp, vertical = 10.dp).clickable(action)
            .semantics { contentDescription = description },
        style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Bold),
    )
}

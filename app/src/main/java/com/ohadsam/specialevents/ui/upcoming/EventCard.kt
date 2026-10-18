package com.ohadsam.specialevents.ui.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.ParshaNames
import com.ohadsam.specialevents.calendar.SpecialAge
import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.ItemKind
import com.ohadsam.specialevents.domain.UpcomingItem

import com.ohadsam.specialevents.ui.common.EventAvatar
import com.ohadsam.specialevents.ui.common.dateLabel
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.common.labelRes

@Composable
fun EventCard(item: UpcomingItem, mode: CalendarMode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val event = item.event
    val accent = event.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EventAvatar(event.photoUri, event.iconKey ?: event.type.defaultEmoji(), accent, 52.dp)
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    kindLabel(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    dateLabel(item.date, item.hebrew, mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                milestoneLabel(item)?.let { MilestoneBadge(it, item.milestone?.tier == Tier.MAJOR) }
            }
            CountdownPill(item.daysUntil)
        }
    }
}

@Composable
private fun MilestoneBadge(text: String, major: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (major) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    ) {
        Text(
            (if (major) "🎉 " else "✨ ") + text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CountdownPill(days: Long) {
    val today = days == 0L
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (today) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            countdownText(days),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun countdownText(days: Long): String = when (days) {
    0L -> stringResource(R.string.countdown_today)
    -1L -> stringResource(R.string.countdown_yesterday)
    in Long.MIN_VALUE..-2L -> stringResource(R.string.countdown_ago, -days)
    1L -> stringResource(R.string.countdown_tomorrow)
    2L -> stringResource(R.string.countdown_day_after)
    else -> stringResource(R.string.countdown_days, days)
}

/** "גיל 50" / "25 שנות נישואין" / "בר מצווה · שבת נח" — the second line of a card. */
@Composable
internal fun kindLabel(item: UpcomingItem): String {
    val event = item.event
    if (item.kind == ItemKind.MITZVAH) {
        val title = stringResource(if (event.gender == Gender.FEMALE) R.string.mitzvah_bat else R.string.mitzvah_bar)
        val parsha = item.mitzvah?.parsha
        val shabbat = if (parsha != null) stringResource(R.string.mitzvah_shabbat, ParshaNames.label(parsha)) else stringResource(R.string.mitzvah_shabbat_holiday)
        return "$title · $shabbat"
    }
    val number = item.number ?: return stringResource(event.type.labelRes())
    val count = when (event.type) {
        EventType.BIRTHDAY -> stringResource(R.string.number_age, number)
        EventType.ANNIVERSARY -> stringResource(R.string.number_anniversary, number)
        EventType.YAHRZEIT -> if (number == 1) stringResource(R.string.number_yahrzeit_first) else stringResource(R.string.number_yahrzeit, number)
        else -> null
    }
    return if (count == null) stringResource(event.type.labelRes()) else "${stringResource(event.type.labelRes())} · $count"
}

/** The highlight line for round birthdays, named anniversaries and yahrzeit milestones, or null. */
@Composable
internal fun milestoneLabel(item: UpcomingItem): String? {
    val m = item.milestone ?: return null
    m.gift?.let { return stringResource(R.string.milestone_anniversary_named, m.number, stringResource(it.labelRes())) }
    return when (item.event.type) {
        EventType.BIRTHDAY -> when (val special = m.specialAge) {
            SpecialAge.DECADE -> stringResource(R.string.age_decade, m.number)
            null -> null
            else -> stringResource(special.labelRes())
        }
        EventType.ANNIVERSARY -> stringResource(R.string.number_anniversary, m.number)
        EventType.YAHRZEIT -> if (m.number == 1) stringResource(R.string.number_yahrzeit_first) else stringResource(R.string.number_yahrzeit, m.number)
        else -> null
    }
}

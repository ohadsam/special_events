package com.ohadsam.specialevents.ui.upcoming

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.domain.views.GroupKey
import com.ohadsam.specialevents.ui.common.gregorianMonthYear
import com.ohadsam.specialevents.ui.common.gregorianShort
import com.ohadsam.specialevents.ui.common.labelRes
import com.ohadsam.specialevents.ui.common.weekdayName

private const val DECADE_SPAN = 9

/** The heading text of a group in the list. */
@Composable
fun groupTitle(key: GroupKey, groupNames: Map<Long, String> = emptyMap()): String = groupTitleText(LocalContext.current, key, groupNames)

/** The same heading outside a composition (printing). */
fun groupTitleText(context: Context, key: GroupKey, groupNames: Map<Long, String> = emptyMap()): String = when (key) {
    GroupKey.All -> context.getString(R.string.group_all)
    is GroupKey.Proximity -> context.getString(key.group.labelRes())
    is GroupKey.Day -> weekdayName(key.date) + " · " + gregorianShort(key.date)
    is GroupKey.Week -> context.getString(R.string.group_week_of, gregorianShort(key.start))
    is GroupKey.Month -> gregorianMonthYear(key.month.atDay(1))
    is GroupKey.HebrewMonth -> HebrewCalendar.monthName(key.year, key.month) + " " + HebrewCalendar.yearLabel(key.year)
    is GroupKey.Type -> context.getString(key.type.labelRes())
    is GroupKey.Group -> key.id?.let { groupNames[it] } ?: context.getString(R.string.group_none)
    is GroupKey.Tag -> key.name ?: context.getString(R.string.group_no_tag)
    is GroupKey.AgeDecade -> key.from?.let { context.getString(R.string.group_age_decade, it, it + DECADE_SPAN) } ?: context.getString(R.string.group_no_age)
}

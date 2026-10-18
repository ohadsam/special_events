package com.ohadsam.specialevents.ui.upcoming

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
fun groupTitle(key: GroupKey): String = when (key) {
    GroupKey.All -> stringResource(R.string.group_all)
    is GroupKey.Proximity -> stringResource(key.group.labelRes())
    is GroupKey.Day -> weekdayName(key.date) + " · " + gregorianShort(key.date)
    is GroupKey.Week -> stringResource(R.string.group_week_of, gregorianShort(key.start))
    is GroupKey.Month -> gregorianMonthYear(key.month.atDay(1))
    is GroupKey.HebrewMonth -> HebrewCalendar.monthName(key.year, key.month) + " " + HebrewCalendar.yearLabel(key.year)
    is GroupKey.Type -> stringResource(key.type.labelRes())
    is GroupKey.Tag -> key.name ?: stringResource(R.string.group_no_tag)
    is GroupKey.AgeDecade -> key.from?.let { stringResource(R.string.group_age_decade, it, it + DECADE_SPAN) } ?: stringResource(R.string.group_no_age)
}

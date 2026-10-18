package com.ohadsam.specialevents.domain.print

import com.ohadsam.specialevents.calendar.CalendarMark
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewNumerals
import com.ohadsam.specialevents.calendar.JewishCalendar
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.domain.CalendarData
import com.ohadsam.specialevents.domain.UpcomingItem
import java.time.LocalDate

/** The words a month sheet needs, so this file stays free of Android resources. */
class MonthLabels(
    val weekdays: (sundayFirst: Boolean) -> List<String>,
    val mark: (CalendarMark) -> String,
    val entry: (UpcomingItem) -> String,
    val parsha: (LocalDate) -> String?,
)

/** Turns a month of events and Jewish-calendar marks into a printable grid. Pure. */
object MonthSheetBuilder {
    fun build(
        anchor: LocalDate,
        mode: CalendarMode,
        sundayFirst: Boolean,
        inIsrael: Boolean,
        items: Map<LocalDate, List<UpcomingItem>>,
        labels: MonthLabels,
        title: String,
        subtitle: String?,
        showMarks: Boolean = true,
    ): MonthSheet {
        val hebrewFirst = mode == CalendarMode.HEBREW
        val grid = CalendarData.monthGrid(anchor, hebrewFirst, sundayFirst)
        val weeks = grid.map { week ->
            week.map { cell ->
                val date = cell.date
                val hebrewDay = HebrewNumerals.toHebrew(HebrewCalendar.fromLocalDate(date).day)
                val notes = ArrayList<String>()
                if (showMarks) {
                    JewishCalendar.marksOn(date, inIsrael).forEach { notes += labels.mark(it) }
                    if (CalendarData.isShabbat(date)) labels.parsha(date)?.let { notes += it }
                }
                PrintDay(
                    date = date,
                    inMonth = cell.inMonth,
                    primary = if (hebrewFirst) hebrewDay else date.dayOfMonth.toString(),
                    secondary = when (mode) {
                        CalendarMode.DUAL -> hebrewDay
                        CalendarMode.HEBREW -> date.dayOfMonth.toString()
                        CalendarMode.GREGORIAN -> null
                    },
                    shabbat = CalendarData.isShabbat(date),
                    entries = items[date].orEmpty().map(labels.entry),
                    notes = notes,
                )
            }
        }
        return MonthSheet(title, subtitle, labels.weekdays(sundayFirst), weeks)
    }
}

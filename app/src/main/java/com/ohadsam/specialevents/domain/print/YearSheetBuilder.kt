package com.ohadsam.specialevents.domain.print

import com.ohadsam.specialevents.calendar.JewishCalendar
import com.ohadsam.specialevents.domain.CalendarData
import com.ohadsam.specialevents.domain.UpcomingItem
import java.time.LocalDate

/** The words a year sheet needs, so this file stays free of Android resources. */
class YearLabels(
    val monthTitle: (month: Int) -> String,
    val weekdays: (sundayFirst: Boolean) -> List<String>,
    val entry: (UpcomingItem) -> String,
)

/** Twelve small months on one page, with the planned days marked and listed under each month. Pure. */
object YearSheetBuilder {
    private const val MONTHS = 12

    fun build(
        year: Int,
        sundayFirst: Boolean,
        inIsrael: Boolean,
        items: Map<LocalDate, List<UpcomingItem>>,
        labels: YearLabels,
        title: String,
        subtitle: String?,
        showMarks: Boolean = true,
    ): YearSheet {
        val months = (1..MONTHS).map { month ->
            val first = LocalDate.of(year, month, 1)
            val weeks = CalendarData.monthGrid(first, false, sundayFirst).map { week ->
                week.map { cell ->
                    if (!cell.inMonth) {
                        null
                    } else {
                        val date = cell.date
                        YearDay(
                            number = date.dayOfMonth.toString(),
                            shabbat = CalendarData.isShabbat(date),
                            marked = items[date].orEmpty().isNotEmpty(),
                            holiday = showMarks && JewishCalendar.marksOn(date, inIsrael).any { it.yomTov || it.fast },
                        )
                    }
                }
            }
            val notes = (1..first.lengthOfMonth()).flatMap { day ->
                val date = first.withDayOfMonth(day)
                items[date].orEmpty().map { "$day · " + labels.entry(it) }
            }
            YearMonthBox(labels.monthTitle(month), weeks, notes)
        }
        return YearSheet(title, subtitle, labels.weekdays(sundayFirst), months)
    }
}

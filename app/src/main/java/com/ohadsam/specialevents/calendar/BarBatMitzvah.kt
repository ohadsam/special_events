package com.ohadsam.specialevents.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** The day a child reaches mitzvah age and the Shabbat on which they are called to the Torah. */
data class MitzvahDay(val date: LocalDate, val hebrew: HebrewDate, val shabbat: LocalDate, val parsha: List<Int>?)

object BarBatMitzvah {
    const val BAR_YEARS = 13
    const val BAT_YEARS = 12

    /**
     * [birth] is the Hebrew birth date (already moved to the next day for a birth after sunset). The mitzvah day is the Hebrew
     * birthday 13 (bar) / 12 (bat) years later, with the Adar and 30-Cheshvan/Kislev rules of [rules]; the Shabbat is that day if it is a
     * Shabbat, otherwise the next one. [parsha] is null when that Shabbat has a festival reading.
     */
    fun of(birth: HebrewDate, years: Int, inIsrael: Boolean, rules: DateRules = DateRules()): MitzvahDay {
        val origin = OriginDate(CalendarBasis.HEBREW, birth.year, birth.month, birth.day)
        val date = AnnualDates.occurrenceIn(origin, birth.year + years, rules)
        val shabbat = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        return MitzvahDay(date, HebrewCalendar.fromLocalDate(date), shabbat, Parsha.ofShabbat(shabbat, inIsrael))
    }
}

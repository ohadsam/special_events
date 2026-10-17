package com.ohadsam.specialevents.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnualDatesTest {
    private fun heb(basisYear: Int?, month: Int, day: Int) = OriginDate(CalendarBasis.HEBREW, basisYear, month, day)

    @Test
    fun leapDayFallsBackToFebruary28ThenReturnsInLeapYears() {
        val born = OriginDate(CalendarBasis.GREGORIAN, 2000, 2, 29)
        val o = AnnualDates.next(born, LocalDate.of(2025, 1, 1))
        assertEquals(LocalDate.of(2025, 2, 28), o.date)
        assertEquals(25, o.number)
        assertEquals(LocalDate.of(2028, 2, 29), AnnualDates.next(born, LocalDate.of(2028, 1, 1)).date)
    }

    @Test
    fun leapDayCanBeConfiguredToMarchFirst() {
        val born = OriginDate(CalendarBasis.GREGORIAN, 2000, 2, 29)
        val date = AnnualDates.next(born, LocalDate.of(2025, 1, 1), DateRules(leapDay = LeapDay.MAR_1)).date
        assertEquals(LocalDate.of(2025, 3, 1), date)
    }

    @Test
    fun originYearItselfIsNotARecurrence() {
        val o = AnnualDates.next(OriginDate(CalendarBasis.GREGORIAN, 2000, 3, 5), LocalDate.of(2000, 1, 1))
        assertEquals(Occurrence(LocalDate.of(2001, 3, 5), 1), o)
    }

    @Test
    fun unknownYearHasNoNumberAndRollsToNextYearWhenPast() {
        val born = OriginDate(CalendarBasis.GREGORIAN, null, 3, 5)
        assertEquals(Occurrence(LocalDate.of(2025, 3, 5), null), AnnualDates.next(born, LocalDate.of(2025, 1, 1)))
        assertEquals(LocalDate.of(2026, 3, 5), AnnualDates.next(born, LocalDate.of(2025, 3, 6)).date)
        assertNull(AnnualDates.next(born, LocalDate.of(2025, 3, 6)).number)
    }

    @Test
    fun hebrewRecurrenceNumbersByHebrewYears() {
        val o = AnnualDates.next(heb(5750, 1, 15), LocalDate.of(2026, 1, 1))
        assertEquals(HebrewDate(5786, 1, 15).toLocalDate(), o.date)
        assertEquals(36, o.number)
    }

    @Test
    fun upcomingListsConsecutiveYears() {
        val list = AnnualDates.upcoming(OriginDate(CalendarBasis.GREGORIAN, 1990, 7, 1), LocalDate.of(2025, 1, 1), 3)
        assertEquals(listOf(2025, 2026, 2027), list.map { it.date.year })
        assertEquals(listOf(35, 36, 37), list.map { it.number })
    }

    @Test
    fun thirtiethOfCheshvanInShortYear() {
        val y = (5780..5900).first { HebrewCalendar.daysInMonth(it, 8) == 29 }
        val origin = heb(5700, 8, 30)
        assertEquals(HebrewDate(y, 8, 29).toLocalDate(), AnnualDates.occurrenceIn(origin, y))
        assertEquals(HebrewDate(y, 9, 1).toLocalDate(), AnnualDates.occurrenceIn(origin, y, DateRules(thirtiethOfShortMonth = ShortMonthThirtieth.NEXT_DAY)))
        val long = (5780..5900).first { HebrewCalendar.daysInMonth(it, 8) == 30 }
        assertEquals(HebrewDate(long, 8, 30).toLocalDate(), AnnualDates.occurrenceIn(origin, long))
    }

    @Test
    fun thirtiethOfKislevInShortYear() {
        val y = (5780..5900).first { HebrewCalendar.daysInMonth(it, 9) == 29 }
        val origin = heb(5700, 9, 30)
        assertEquals(HebrewDate(y, 9, 29).toLocalDate(), AnnualDates.occurrenceIn(origin, y))
        assertEquals(HebrewDate(y, 10, 1).toLocalDate(), AnnualDates.occurrenceIn(origin, y, DateRules(thirtiethOfShortMonth = ShortMonthThirtieth.NEXT_DAY)))
    }

    private fun resolved(origin: OriginDate, year: Int, rules: DateRules = DateRules()): HebrewDate =
        HebrewCalendar.fromLocalDate(AnnualDates.occurrenceIn(origin, year, rules))

    @Test
    fun adarOfCommonYearMovesToAdarTwoInLeapYear() {
        val origin = heb(5785, 12, 10) // 5785 is a common year: plain Adar
        assertTrue(HebrewCalendar.isLeapYear(5787) && !HebrewCalendar.isLeapYear(5786))
        assertEquals(HebrewDate(5787, 13, 10), resolved(origin, 5787))
        assertEquals(HebrewDate(5787, 12, 10), resolved(origin, 5787, DateRules(adar = AdarInLeapYear.ADAR_I)))
        assertEquals(HebrewDate(5786, 12, 10), resolved(origin, 5786))
    }

    @Test
    fun adarTwoFallsBackToAdarInCommonYear() {
        val origin = heb(5784, 13, 10)
        assertEquals(HebrewDate(5785, 12, 10), resolved(origin, 5785))
        assertEquals(HebrewDate(5787, 13, 10), resolved(origin, 5787))
    }

    @Test
    fun adarOneStaysAdarOneAndItsThirtiethBecomesShevatThirtiethInCommonYear() {
        assertTrue(HebrewCalendar.isLeapYear(5784))
        val tenth = heb(5784, 12, 10)
        assertEquals(HebrewDate(5787, 12, 10), resolved(tenth, 5787))
        assertEquals(HebrewDate(5785, 12, 10), resolved(tenth, 5785))
        val thirtieth = heb(5784, 12, 30)
        assertEquals(HebrewDate(5787, 12, 30), resolved(thirtieth, 5787))
        assertEquals(HebrewDate(5785, 11, 30), resolved(thirtieth, 5785))
    }

    @Test
    fun dualDateAppliesAfterSunsetShift() {
        val plain = DualDate.fromGregorian(LocalDate.of(2026, 3, 3))
        assertEquals(HebrewDate(5786, 12, 14), plain.hebrew)
        assertEquals(HebrewDate(5786, 12, 15), DualDate.fromGregorian(LocalDate.of(2026, 3, 3), afterSunset = true).hebrew)
        val back = DualDate.fromHebrew(HebrewDate(5786, 12, 15), afterSunset = true)
        assertEquals(LocalDate.of(2026, 3, 3), back.gregorian)
    }
}

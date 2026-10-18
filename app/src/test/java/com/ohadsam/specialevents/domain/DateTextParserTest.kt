package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.domain.datetext.DateParse
import com.ohadsam.specialevents.domain.datetext.DateParseError
import com.ohadsam.specialevents.domain.datetext.DateTextParser
import com.ohadsam.specialevents.domain.datetext.ParseWarning
import com.ohadsam.specialevents.domain.datetext.ParsedDate
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTextParserTest {
    private val today = LocalDate.of(2026, 10, 18)

    private fun ok(text: String, on: LocalDate = today): ParsedDate = (DateTextParser.parse(text, on) as? DateParse.Ok)?.date ?: error("not parsed: $text")

    private fun fails(text: String) = (DateTextParser.parse(text, today) as DateParse.Failure).error

    @Test
    fun readsGregorianDatesWrittenInManyWays() {
        listOf("17 באוקטובר 1980", "17.10.1980", "17/10/1980", "17-10-1980", "1980-10-17", "נולד ב-17 באוקטובר 1980", "ב־17 באוקטובר, 1980").forEach { text ->
            val d = ok(text)
            assertEquals(text, CalendarBasis.GREGORIAN, d.basis)
            assertEquals(text, listOf(1980, 10, 17), listOf(d.year, d.month, d.day))
        }
        assertEquals(DayOfWeek.FRIDAY, ok("17 באוקטובר 1980").actualWeekday)
    }

    @Test
    fun twoDigitYearsAreExpandedAndFlagged() {
        val old = ok("17/10/80")
        assertEquals(1980, old.year)
        assertTrue(ParseWarning.TWO_DIGIT_YEAR in old.warnings)
        assertEquals(2005, ok("17-10-05").year)
    }

    @Test
    fun missingYearStaysUnknown() {
        val d = ok("17 באוקטובר")
        assertNull(d.year)
        assertEquals(listOf(10, 17), listOf(d.month, d.day))
    }

    @Test
    fun ageGivesTheYearOfBirthDependingOnWhetherTheBirthdayPassed() {
        assertEquals(1980, ok("בת 46, נולדה ב-17 באוקטובר").year)
        assertEquals(1979, ok("בת 46, נולדה ב-17 באוקטובר", on = LocalDate.of(2026, 10, 16)).year)
        assertEquals(1980, ok("17 באוקטובר 46 שנים").year)
        assertEquals(1976, ok("בן 50 ב-17 באוקטובר", on = LocalDate.of(2026, 10, 17)).year)
        assertEquals(46, ok("בת 46 17 באוקטובר").age)
    }

    @Test
    fun readsHebrewDates() {
        val a = ok("כ' בחשוון תשמ\"ז")
        assertEquals(CalendarBasis.HEBREW, a.basis)
        assertEquals(listOf(5747, 8, 20), listOf(a.year, a.month, a.day))
        val b = ok("י\"ג באלול תשמ\"ב")
        assertEquals(listOf(5742, 6, 13), listOf(b.year, b.month, b.day))
        val c = ok("20 בחשוון 5747")
        assertEquals(listOf(5747, 8, 20), listOf(c.year, c.month, c.day))
        val d = ok("כ חשוון התשמ\"ז")
        assertEquals(listOf(5747, 8, 20), listOf(d.year, d.month, d.day))
        val e = ok("ט\"ו בשבט")
        assertNull(e.year)
        assertEquals(listOf(11, 15), listOf(e.month, e.day))
    }

    @Test
    fun adarIsReadByTheYearType() {
        assertEquals(13, ok("י\"ד באדר ב").month)
        assertEquals(12, ok("י\"ד באדר א").month)
        assertEquals(12, ok("י\"ד באדר").month)
        val leap = ok("י\"ד באדר 5784")
        assertEquals(13, leap.month)
        assertTrue(ParseWarning.ADAR_READ_AS_SECOND in leap.warnings)
        assertEquals(12, ok("י\"ד באדר 5783").month)
    }

    @Test
    fun hebrewAgeFindsTheHebrewYearOfBirth() {
        // 18 Oct 2026 is 7 Cheshvan 5787; a 20 Cheshvan birthday has not come yet this year.
        assertEquals(5787 - 40 - 1, ok("בן 40, נולד בכ' בחשוון").year)
        // 7 Cheshvan has passed on 18 Oct for a 5 Cheshvan birthday.
        assertEquals(5787 - 40, ok("בן 40, נולד בה' בחשוון").year)
    }

    @Test
    fun afterSunsetIsRecognisedAndRemoved() {
        assertTrue(ok("17 באוקטובר 1980 אחרי השקיעה").afterSunset)
        assertTrue(ok("נולד בלילה, 17.10.1980").afterSunset)
        assertFalse(ok("17 באוקטובר 1980").afterSunset)
    }

    @Test
    fun weekdayIsCheckedAgainstTheDate() {
        val wrong = ok("נולד ביום שלישי 17 באוקטובר 1980")
        assertTrue(ParseWarning.WEEKDAY_MISMATCH in wrong.warnings)
        assertEquals(DayOfWeek.FRIDAY, wrong.actualWeekday)
        assertFalse(ParseWarning.WEEKDAY_MISMATCH in ok("ביום שישי 17 באוקטובר 1980").warnings)
    }

    @Test
    fun weekdayWithoutYearListsTheYearsThatFit() {
        val d = ok("17 באוקטובר ביום שישי")
        assertNull(d.year)
        assertTrue(d.needsYear)
        assertTrue(1980 in d.candidates && 1986 in d.candidates)
        assertTrue(d.candidates.all { LocalDate.of(it, 10, 17).dayOfWeek == DayOfWeek.FRIDAY })
        assertEquals(d.candidates.sortedDescending(), d.candidates)
        assertEquals(1986, d.withYear(1986).year)
        assertFalse(d.withYear(1986).needsYear)
    }

    @Test
    fun weekdayAgeAndDayPinTheYear() {
        val d = ok("בת 46, יום שישי, 17 באוקטובר")
        assertEquals(1980, d.year)
        assertFalse(ParseWarning.WEEKDAY_MISMATCH in d.warnings)
    }

    @Test
    fun wordsThatLookLikeNumeralsAreNotNumbers() {
        val d = ok("יום הולדת של דנה: 17 באוקטובר 1980")
        assertEquals(listOf(1980, 10, 17), listOf(d.year, d.month, d.day))
        val h = ok("נולדה ב כ' חשוון, יום הולדת")
        assertEquals(listOf(8, 20), listOf(h.month, h.day))
        assertNull(h.year)
    }

    @Test
    fun explainsWhyItFailed() {
        assertEquals(DateParseError.EMPTY, fails(""))
        assertEquals(DateParseError.EMPTY, fails("   "))
        assertEquals(DateParseError.NO_MONTH, fails("17"))
        assertEquals(DateParseError.NO_MONTH, fails("שלום עולם"))
        assertEquals(DateParseError.NO_DAY, fails("באוקטובר 1980"))
        assertEquals(DateParseError.INVALID_DATE, fails("31 בנובמבר 2000"))
        assertEquals(DateParseError.INVALID_DATE, fails("29 בפברואר 2001"))
        assertEquals(DateParseError.INVALID_DATE, fails("30 בפברואר"))
    }

    @Test
    fun leapDayWithoutYearIsFine() {
        val d = ok("29 בפברואר")
        assertNull(d.year)
        assertEquals(listOf(2, 29), listOf(d.month, d.day))
        assertEquals(2000, ok("29 בפברואר 2000").year)
    }

    @Test
    fun resultFeedsTheEditorInBothCalendars() {
        val e = ok("17 באוקטובר 1980").toEditorDate(today)
        assertEquals(LocalDate.of(1980, 10, 17), e.gregorian)
        assertTrue(e.hasYear)
        assertEquals(5741, e.hebrew.year)
        val h = ok("כ' בחשוון תשמ\"ז").toEditorDate(today)
        assertEquals(CalendarBasis.HEBREW, h.basis)
        assertEquals(LocalDate.of(1986, 11, 22), h.gregorian)
        assertFalse(ok("17 באוקטובר").toEditorDate(today).hasYear)
    }
}

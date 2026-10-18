package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.calsync.CalendarImportPlanner
import com.ohadsam.specialevents.domain.calsync.DeviceEvent
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarImportPlannerTest {
    private fun device(title: String, date: LocalDate, yearly: Boolean = false, calendar: String = "Google") = DeviceEvent(calendar, title, date, yearly)

    @Test
    fun guessesTypeFromHebrewAndEnglishTitles() {
        assertEquals(EventType.BIRTHDAY, CalendarImportPlanner.guessType("יום הולדת לדנה"))
        assertEquals(EventType.BIRTHDAY, CalendarImportPlanner.guessType("Dana's Birthday"))
        assertEquals(EventType.ANNIVERSARY, CalendarImportPlanner.guessType("יום נישואין 25"))
        assertEquals(EventType.YAHRZEIT, CalendarImportPlanner.guessType("אזכרה לסבא"))
        assertEquals(EventType.BAR_BAT_MITZVAH, CalendarImportPlanner.guessType("בר מצווה של יוני"))
        assertEquals(EventType.CUSTOM, CalendarImportPlanner.guessType("ישיבת צוות"))
    }

    @Test
    fun recognisesYearlyRecurrenceRules() {
        assertTrue(CalendarImportPlanner.isYearlyRule("FREQ=YEARLY;WKST=SU"))
        assertTrue(CalendarImportPlanner.isYearlyRule("freq=yearly"))
        assertFalse(CalendarImportPlanner.isYearlyRule("FREQ=WEEKLY;BYDAY=MO"))
        assertFalse(CalendarImportPlanner.isYearlyRule(null))
    }

    @Test
    fun birthdayKeepsItsYearButAPlainRecurringEntryDoesNot() {
        val birthday = CalendarImportPlanner.toEvent(device("יום הולדת דנה", LocalDate.of(1980, 10, 17), yearly = true))
        assertEquals(EventType.BIRTHDAY, birthday.type)
        assertEquals(Recurrence.YEARLY, birthday.recurrence)
        assertEquals(1980, birthday.origin.year)
        val party = CalendarImportPlanner.toEvent(device("מסיבת חברה", LocalDate.of(2019, 6, 1), yearly = true))
        assertEquals(Recurrence.YEARLY, party.recurrence)
        assertNull(party.origin.year)
        val once = CalendarImportPlanner.toEvent(device("חתונת רותי", LocalDate.of(2027, 5, 5)))
        assertEquals(2027, once.origin.year)
    }

    @Test
    fun planFiltersDeduplicatesAndMarksWhatTheAppHas() {
        val existing = listOf(
            SpecialEvent(type = EventType.BIRTHDAY, title = "יום הולדת דנה", origin = OriginDate(CalendarBasis.GREGORIAN, 1980, 10, 17)),
        )
        val found = listOf(
            device("יום הולדת דנה", LocalDate.of(1980, 10, 17), yearly = true),
            device("יום הולדת דנה", LocalDate.of(1980, 10, 17), yearly = true, calendar = "Outlook"),
            device("יום הולדת יוסי", LocalDate.of(1975, 3, 2), yearly = true),
            device("פגישה עם רופא", LocalDate.of(2026, 11, 3)),
        )
        val strict = CalendarImportPlanner.plan(existing, found, onlyRecurring = true)
        assertEquals(listOf("יום הולדת דנה", "יום הולדת יוסי"), strict.map { it.event.title })
        assertEquals(listOf(true, false), strict.map { it.duplicate })
        assertEquals(3, CalendarImportPlanner.plan(existing, found, onlyRecurring = false).size)
    }
}

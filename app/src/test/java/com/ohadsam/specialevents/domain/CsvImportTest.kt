package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.domain.backup.CsvExport
import com.ohadsam.specialevents.domain.backup.CsvField
import com.ohadsam.specialevents.domain.backup.CsvImport
import com.ohadsam.specialevents.domain.backup.CsvParser
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImportTest {
    private val today = LocalDate.of(2026, 10, 18)

    @Test
    fun parsesQuotesBreaksAndBom() {
        val rows = CsvParser.parse("﻿שם,הערות\r\n\"דנה, לוי\",\"שורה1\nשורה2 \"\"ציטוט\"\"\"\r\nיוסי,\r\n")
        assertEquals(listOf("שם", "הערות"), rows[0])
        assertEquals(listOf("דנה, לוי", "שורה1\nשורה2 \"ציטוט\""), rows[1])
        assertEquals(listOf("יוסי", ""), rows[2])
        assertEquals(3, rows.size)
    }

    @Test
    fun detectsSemicolonAndTabSeparators() {
        assertEquals(listOf("a", "b", "c"), CsvParser.parse("a;b;c\n1;2;3")[0])
        assertEquals(listOf("a", "b"), CsvParser.parse("a\tb\n1\t2")[0])
        assertTrue(CsvParser.parse("").isEmpty())
    }

    @Test
    fun guessesColumnsFromHebrewAndEnglishHeaders() {
        val m = CsvImport.guessMapping(listOf("שם מלא", "תאריך לידה", "Phone", "Email", "הערות", "x"))
        assertEquals(0, m.columns[CsvField.TITLE])
        assertEquals(1, m.columns[CsvField.DATE])
        assertEquals(2, m.columns[CsvField.PHONE])
        assertEquals(3, m.columns[CsvField.EMAIL])
        assertEquals(4, m.columns[CsvField.NOTES])
        assertTrue(CsvImport.isUsable(m))
        assertFalse(CsvImport.isUsable(CsvImport.guessMapping(listOf("שם", "הערות"))))
    }

    @Test
    fun buildsEventsFromAnyDateWritingAndSkipsTheUnreadable() {
        val rows = CsvParser.parse("שם,תאריך,סוג,טלפון,תגיות\nדנה,17.10.1980,יום הולדת,050-1,משפחה #חברים\nיוסי,כ' בחשוון תשמ\"ז,BIRTHDAY,,\nבלי תאריך,,,,\n,17.10.1980,,,\nשגוי,31 בנובמבר 2000,,,").drop(1)
        val result = CsvImport.toEvents(rows, CsvImport.guessMapping(listOf("שם", "תאריך", "סוג", "טלפון", "תגיות")), today)
        assertEquals(3 - 1, result.events.size)
        assertEquals(3, result.skippedRows)
        val dana = result.events[0]
        assertEquals(EventType.BIRTHDAY, dana.type)
        assertEquals(1980, dana.origin.year)
        assertEquals("050-1", dana.contactPhone)
        assertEquals(listOf("משפחה", "חברים"), dana.tags.map { it.name })
        assertEquals(CalendarBasis.HEBREW, result.events[1].origin.basis)
    }

    @Test
    fun readsSeparateYearMonthDayColumnsAndRoundTripsOurOwnExport() {
        val source = listOf(
            SpecialEvent(id = 1, type = EventType.ANNIVERSARY, title = "אבי ושרה", origin = com.ohadsam.specialevents.calendar.OriginDate(CalendarBasis.GREGORIAN, 2001, 5, 6), contactEmail = "a@x.com"),
            SpecialEvent(id = 2, type = EventType.YAHRZEIT, title = "סבא", origin = com.ohadsam.specialevents.calendar.OriginDate(CalendarBasis.HEBREW, null, 8, 20)),
        )
        val rows = CsvParser.parse(CsvExport.build(source))
        val mapping = CsvImport.guessMapping(rows.first())
        assertTrue(CsvImport.isUsable(mapping))
        val back = CsvImport.toEvents(rows.drop(1), mapping, today).events
        assertEquals(2, back.size)
        assertEquals(source[0].origin, back[0].origin)
        assertEquals(source[1].origin, back[1].origin)
        assertEquals(EventType.YAHRZEIT, back[1].type)
        assertEquals("a@x.com", back[0].contactEmail)
        assertNull(back[1].contactEmail)
    }
}

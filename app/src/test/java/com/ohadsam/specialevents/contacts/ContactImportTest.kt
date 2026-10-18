package com.ohadsam.specialevents.contacts

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactImportTest {
    @Test
    fun parsesTheFormatsContactsUse() {
        assertEquals(ContactDate(1980, 5, 4), ContactDateParser.parse("1980-05-04"))
        assertEquals(ContactDate(null, 11, 2), ContactDateParser.parse("--11-02"))
        assertEquals(ContactDate(1975, 12, 31), ContactDateParser.parse("19751231"))
        assertEquals(ContactDate(1990, 1, 9), ContactDateParser.parse(" 1990-1-9T00:00 "))
    }

    @Test
    fun placeholderYearsAndGarbageAreHandled() {
        assertNull(ContactDateParser.parse("1604-03-07")!!.year)
        assertNull(ContactDateParser.parse(""))
        assertNull(ContactDateParser.parse(null))
        assertNull(ContactDateParser.parse("--13-01"))
        assertNull(ContactDateParser.parse("2000-02-30"))
        assertEquals(ContactDate(2000, 2, 29), ContactDateParser.parse("2000-02-29"))
    }

    private fun candidate(name: String, kind: EventType = EventType.BIRTHDAY, month: Int = 5, day: Int = 4, key: String = "k1", label: String? = null) =
        ContactCandidate(1, key, name, kind, label, ContactDate(1980, month, day), phone = "050-1234567", email = "a@b.c")

    @Test
    fun alreadyLinkedOrSameNameAndDayAreDuplicates() {
        val existing = listOf(
            SpecialEvent(type = EventType.BIRTHDAY, title = "דנה כהן", origin = OriginDate(CalendarBasis.GREGORIAN, 1980, 5, 4)),
            SpecialEvent(type = EventType.BIRTHDAY, title = "אחר", origin = OriginDate(CalendarBasis.GREGORIAN, 1990, 1, 1), contactLookupUri = ContactImport.lookupUriOf("linked")),
        )
        val rows = ContactImport.plan(
            existing,
            listOf(candidate("דנה כהן"), candidate("מישהו", key = "linked", month = 7), candidate("דנה כהן", EventType.ANNIVERSARY), candidate("חדש", month = 9)),
        )
        assertEquals(listOf(true, true, false, false), rows.map { it.duplicate })
    }

    @Test
    fun convertsToAnEventWithTheContactDetails() {
        val e = ContactImport.toEvent(candidate("דנה"), photoUri = "file:///p.jpg")
        assertEquals(EventType.BIRTHDAY, e.type)
        assertEquals("דנה", e.title)
        assertEquals("דנה", e.personName)
        assertEquals(OriginDate(CalendarBasis.GREGORIAN, 1980, 5, 4), e.origin)
        assertEquals("050-1234567", e.contactPhone)
        assertEquals("a@b.c", e.contactEmail)
        assertEquals("file:///p.jpg", e.photoUri)
        assertTrue(e.contactLookupUri!!.endsWith("/k1"))
    }

    @Test
    fun customDatesKeepTheirLabelInTheTitle() {
        val e = ContactImport.toEvent(candidate("דנה", EventType.CUSTOM, label = "סיום תואר"))
        assertEquals("דנה — סיום תואר", e.title)
        assertNull(e.personName)
        assertFalse(ContactImport.plan(emptyList(), listOf(candidate("x"))).single().duplicate)
    }
}

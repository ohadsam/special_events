package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.data.GreetingSent
import com.ohadsam.specialevents.data.SendChannel
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.checklist.ChecklistKind
import com.ohadsam.specialevents.domain.review.YearReviewCalculator
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YearReviewTest {
    private val today = LocalDate.of(2026, 6, 15)

    private fun birthday(id: Long, name: String, month: Int, day: Int) =
        SpecialEvent(id = id, type = EventType.BIRTHDAY, title = name, personName = name, origin = OriginDate(CalendarBasis.GREGORIAN, 1980, month, day))

    private fun sent(eventId: Long, date: LocalDate, channel: SendChannel = SendChannel.WHATSAPP) =
        GreetingSent(id = eventId, eventId = eventId, occurrenceDay = date.toEpochDay(), channel = channel, sentAt = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(), preview = "")

    private fun review(events: List<SpecialEvent>, sent: List<GreetingSent> = emptyList(), items: List<ChecklistItem> = emptyList()) =
        YearReviewCalculator.compute(2026, events, sent, items, today, inIsrael = true, zone = ZoneOffset.UTC)

    @Test
    fun countsOccurrencesAndBusiestMonth() {
        val r = review(listOf(birthday(1, "דנה", 3, 1), birthday(2, "רון", 3, 20), birthday(3, "מיה", 9, 5)))
        assertEquals(3, r.occurrences)
        assertEquals(3, r.busiestMonth)
        assertEquals(2, r.busiestMonthCount)
        assertEquals(listOf(EventType.BIRTHDAY to 3), r.byType)
    }

    @Test
    fun coverageCountsOnlyPassedOccurrences() {
        val events = listOf(birthday(1, "דנה", 3, 1), birthday(2, "רון", 4, 2), birthday(3, "מיה", 9, 5))
        val r = review(events, listOf(sent(1, LocalDate.of(2026, 3, 1))))
        assertEquals(2, r.passed)
        assertEquals(1, r.greeted)
        assertEquals(50, r.coveragePercent)
    }

    @Test
    fun coverageIsNullBeforeAnythingPassed() {
        val r = YearReviewCalculator.compute(2026, listOf(birthday(1, "דנה", 12, 1)), emptyList(), emptyList(), LocalDate.of(2026, 1, 1), true, zone = ZoneOffset.UTC)
        assertNull(r.coveragePercent)
    }

    @Test
    fun greetingsAreCountedByYearChannelAndTopPeople() {
        val events = listOf(birthday(1, "דנה", 3, 1), birthday(2, "רון", 4, 2))
        val r = review(
            events,
            listOf(
                sent(1, LocalDate.of(2026, 3, 1), SendChannel.WHATSAPP),
                sent(1, LocalDate.of(2026, 3, 2), SendChannel.SMS),
                sent(2, LocalDate.of(2026, 4, 2), SendChannel.WHATSAPP),
                sent(2, LocalDate.of(2025, 4, 2), SendChannel.EMAIL),
            ),
        )
        assertEquals(3, r.greetingsSent)
        assertEquals(listOf(SendChannel.WHATSAPP to 2, SendChannel.SMS to 1), r.byChannel)
        assertEquals("דנה", r.topPeople.first().name)
        assertEquals(2, r.topPeople.first().count)
    }

    @Test
    fun archivedEventsAreIgnoredAndGiftsAreSummed() {
        val archived = birthday(2, "ישן", 5, 5).copy(archived = true)
        val items = listOf(
            ChecklistItem(eventId = 1, kind = ChecklistKind.GIFT, title = "ספר", amount = 80, done = true),
            ChecklistItem(eventId = 1, kind = ChecklistKind.GIFT, title = "פרחים", amount = 50),
            ChecklistItem(eventId = 2, kind = ChecklistKind.GIFT, title = "לא נספר", amount = 999),
        )
        val r = review(listOf(birthday(1, "דנה", 3, 1), archived), items = items)
        assertEquals(1, r.occurrences)
        assertEquals(80, r.gifts.spent)
        assertEquals(50, r.gifts.budget)
        assertTrue(r.topPeople.isEmpty())
    }
}

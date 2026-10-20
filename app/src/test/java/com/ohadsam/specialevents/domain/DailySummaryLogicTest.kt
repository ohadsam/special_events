package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.reminders.DailySummaryLogic
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DailySummaryLogicTest {
    private val zone = ZoneId.of("Asia/Jerusalem")

    @Test
    fun delayIsToTodayWhenTheHourIsAhead() {
        val now = ZonedDateTime.of(2026, 10, 20, 6, 30, 0, 0, zone)
        assertEquals(90L * 60 * 1000, DailySummaryLogic.delayMillis(now, 8))
    }

    @Test
    fun delayIsToTomorrowWhenTheHourPassed() {
        val now = ZonedDateTime.of(2026, 10, 20, 9, 0, 0, 0, zone)
        assertEquals(23L * 60 * 60 * 1000, DailySummaryLogic.delayMillis(now, 8))
    }

    @Test
    fun pickSplitsTodayFromTomorrow() {
        val ev = SpecialEvent(id = 1, type = EventType.BIRTHDAY, title = "דנה", origin = OriginDate(CalendarBasis.GREGORIAN, 1980, 10, 20))
        val tomorrow = SpecialEvent(id = 2, type = EventType.BIRTHDAY, title = "רן", origin = OriginDate(CalendarBasis.GREGORIAN, 1980, 10, 21))
        val items = EventTimeline.upcoming(listOf(ev, tomorrow), LocalDate.of(2026, 10, 20), true)
        val (today, next) = DailySummaryLogic.pick(items)
        assertEquals(listOf("דנה"), today.map { it.event.title })
        assertEquals(listOf("רן"), next.map { it.event.title })
    }
}

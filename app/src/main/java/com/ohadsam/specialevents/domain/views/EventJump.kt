package com.ohadsam.specialevents.domain.views

import com.ohadsam.specialevents.calendar.DateRules
import com.ohadsam.specialevents.domain.CalendarLevel
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.UpcomingItem
import java.time.LocalDate

/** Where the calendar should open for something the user wants to see "in the calendar". */
data class CalendarTarget(val date: LocalDate, val level: CalendarLevel = CalendarLevel.DAY)

/** "Go to event" and "show in calendar". Pure. */
object EventJump {
    private const val MAX_MATCHES = 8

    /** Events whose name matches [query] (all words, case/nikud-insensitive); active ones before archived, then by title. */
    fun matches(events: List<SpecialEvent>, query: String): List<SpecialEvent> {
        val q = TextSearch.normalize(query)
        if (q.isEmpty()) return emptyList()
        return events.filter { e ->
            val name = TextSearch.normalize(listOfNotNull(e.title, e.personName).joinToString(" "))
            q.split(' ').all { name.contains(it) }
        }.sortedWith(compareBy({ it.archived }, { TextSearch.normalize(it.title) })).take(MAX_MATCHES)
    }

    /** The next occurrence of [event] on or after [today] - what "jump to this event" opens. */
    fun next(event: SpecialEvent, today: LocalDate, rules: DateRules): UpcomingItem? = EventTimeline.forEvent(event, today, 1, rules).firstOrNull()

    fun locate(item: UpcomingItem): CalendarTarget = CalendarTarget(item.date, CalendarLevel.DAY)
}

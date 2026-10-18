package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate

enum class EventType(val defaultBasis: CalendarBasis, val yearly: Boolean) {
    BIRTHDAY(CalendarBasis.GREGORIAN, true),
    ANNIVERSARY(CalendarBasis.GREGORIAN, true),
    YAHRZEIT(CalendarBasis.HEBREW, true),
    BAR_BAT_MITZVAH(CalendarBasis.HEBREW, false),
    WORK(CalendarBasis.GREGORIAN, false),
    FAMILY(CalendarBasis.GREGORIAN, false),
    CUSTOM(CalendarBasis.GREGORIAN, true),
}

enum class Gender { MALE, FEMALE }

enum class Recurrence { YEARLY, ONCE }

data class Tag(val id: Long = 0, val name: String, val colorArgb: Int? = null)

/** An event as the app works with it; storage lives in `data/db` and is mapped by `EventMapper`. */
data class SpecialEvent(
    val id: Long = 0,
    val type: EventType,
    val title: String,
    val personName: String? = null,
    val gender: Gender? = null,
    val relation: String? = null,
    val origin: OriginDate,
    val recurrence: Recurrence = if (type.yearly) Recurrence.YEARLY else Recurrence.ONCE,
    /** The event happened after sunset, so its Hebrew date is the next civil day's (matters when [origin] is Gregorian). */
    val afterSunset: Boolean = false,
    val timeMinutes: Int? = null,
    val notes: String? = null,
    val colorArgb: Int? = null,
    val iconKey: String? = null,
    val photoUri: String? = null,
    val contactLookupUri: String? = null,
    /** Copied from the linked contact when it was picked, so greetings can reach the person without keeping READ_CONTACTS. */
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val archived: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

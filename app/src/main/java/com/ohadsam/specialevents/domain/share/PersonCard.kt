package com.ohadsam.specialevents.domain.share

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.greetings.Span
import com.ohadsam.specialevents.domain.greetings.SpanStyle
import java.time.LocalDate

/** Which details of each event appear on a card. */
enum class CardField { TYPE, DATE_GREGORIAN, DATE_HEBREW, AGE, NEXT, NOTES, CONTACT, TAGS }

/** The words a card needs from the UI layer, so this stays free of Android resources. */
class CardLabels(
    val type: (EventType) -> String,
    val gregorian: (LocalDate) -> String,
    val age: (Int) -> String,
    val years: (Int) -> String,
    val next: (LocalDate) -> String,
    val untitled: String,
)

/**
 * A card with the dates of a person (or any chosen events), as styled text: a bold heading, and for each event the chosen details.
 * Being spans, it can be written as WhatsApp text, HTML for mail, or plain text ([com.ohadsam.specialevents.domain.greetings.RichText]).
 */
object PersonCard {
    val DEFAULT_FIELDS: Set<CardField> = setOf(CardField.TYPE, CardField.DATE_GREGORIAN, CardField.DATE_HEBREW, CardField.AGE, CardField.NEXT)

    /** [next] maps an event id to its next occurrence (for AGE and NEXT); events without one simply skip those lines. */
    fun build(heading: String?, events: List<SpecialEvent>, next: Map<Long, UpcomingItem>, fields: Set<CardField>, labels: CardLabels): List<Span> {
        val spans = ArrayList<Span>()
        spans += Span(heading?.takeIf { it.isNotBlank() } ?: events.firstOrNull()?.let { it.personName ?: it.title } ?: labels.untitled, SpanStyle(bold = true))
        for (e in events) {
            spans += Span("\n\n")
            spans += Span(titleLine(e, fields, labels), SpanStyle(bold = CardField.TYPE !in fields))
            val details = ArrayList<String>()
            val origin = e.origin
            if (origin.year != null && CardField.DATE_GREGORIAN in fields && origin.basis == CalendarBasis.GREGORIAN) {
                details += labels.gregorian(LocalDate.of(origin.year, origin.month, origin.day))
            }
            val item = next[e.id]
            if (CardField.DATE_HEBREW in fields) hebrewLine(e)?.let { details += it }
            if (CardField.AGE in fields && item?.number != null) details += if (e.type == EventType.BIRTHDAY) labels.age(item.number) else labels.years(item.number)
            if (CardField.NEXT in fields && item != null) details += labels.next(item.date)
            if (CardField.CONTACT in fields) listOfNotNull(e.contactPhone, e.contactEmail).takeIf { it.isNotEmpty() }?.let { details += it.joinToString(" · ") }
            if (CardField.TAGS in fields && e.tags.isNotEmpty()) details += e.tags.joinToString(" ") { "#${it.name}" }
            if (CardField.NOTES in fields) e.notes?.takeIf { it.isNotBlank() }?.let { details += it }
            details.forEach { spans += Span("\n$it") }
        }
        return spans
    }

    private fun titleLine(e: SpecialEvent, fields: Set<CardField>, labels: CardLabels): String {
        val emoji = e.iconKey?.let { "$it " }.orEmpty()
        return if (CardField.TYPE in fields) "$emoji${labels.type(e.type)} — ${e.title}" else "$emoji${e.title}"
    }

    private fun hebrewLine(e: SpecialEvent): String? {
        val o = e.origin
        return runCatching {
            when (o.basis) {
                CalendarBasis.HEBREW -> HebrewDate(o.year ?: return null, o.month, o.day).format()
                CalendarBasis.GREGORIAN -> HebrewCalendar.fromLocalDate(LocalDate.of(o.year ?: return null, o.month, o.day)).format()
            }
        }.getOrNull()
    }
}

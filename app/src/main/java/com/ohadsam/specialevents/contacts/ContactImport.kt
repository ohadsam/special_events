package com.ohadsam.specialevents.contacts

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.TextSearch

/** A date as stored in a contact: the year is often missing (`--MM-dd`) or a placeholder. */
data class ContactDate(val year: Int?, val month: Int, val day: Int)

object ContactDateParser {
    private const val MIN_REAL_YEAR = 1900
    private val FULL = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})")
    private val NO_YEAR = Regex("^--(\\d{1,2})-(\\d{1,2})")
    private val COMPACT = Regex("^(\\d{4})(\\d{2})(\\d{2})$")

    /** Understands `yyyy-MM-dd`, `--MM-dd` and `yyyyMMdd`; years before 1900 are placeholders and become "unknown". */
    fun parse(text: String?): ContactDate? {
        val s = text?.trim().orEmpty()
        (FULL.find(s) ?: COMPACT.find(s))?.let { return build(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
        NO_YEAR.find(s)?.let { return build(null, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        return null
    }

    private fun build(year: Int?, month: Int, day: Int): ContactDate? {
        if (month !in 1..12 || day !in 1..daysIn(month)) return null
        return ContactDate(year?.takeIf { it >= MIN_REAL_YEAR }, month, day)
    }

    private fun daysIn(month: Int) = when (month) {
        2 -> 29
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

/** One birthday / anniversary / other date found in the address book. */
data class ContactCandidate(
    val contactId: Long,
    val lookupKey: String,
    val name: String,
    val kind: EventType,
    val customLabel: String?,
    val date: ContactDate,
    val phone: String? = null,
    val email: String? = null,
    val photoThumbUri: String? = null,
)

data class ImportRow(val candidate: ContactCandidate, val duplicate: Boolean)

object ContactImport {
    /** Marks candidates that the app already has (same linked contact and type, or same name, type and day) so they are not imported twice. */
    fun plan(existing: List<SpecialEvent>, candidates: List<ContactCandidate>): List<ImportRow> {
        val byLookup = existing.filter { it.contactLookupUri != null }.map { it.contactLookupUri!! to it.type }.toSet()
        val byName = existing.map { Triple(TextSearch.normalize(it.title), it.type, it.origin.month to it.origin.day) }.toSet()
        return candidates.map { c ->
            val dup = (lookupUriOf(c.lookupKey) to c.kind) in byLookup ||
                Triple(TextSearch.normalize(titleOf(c)), c.kind, c.date.month to c.date.day) in byName
            ImportRow(c, dup)
        }
    }

    fun lookupUriOf(lookupKey: String) = "content://com.android.contacts/contacts/lookup/$lookupKey"

    private fun titleOf(c: ContactCandidate): String = if (c.kind == EventType.CUSTOM && !c.customLabel.isNullOrBlank()) "${c.name} — ${c.customLabel}" else c.name

    fun toEvent(c: ContactCandidate, photoUri: String? = null): SpecialEvent = SpecialEvent(
        type = c.kind,
        title = titleOf(c),
        personName = c.name.takeIf { c.kind == EventType.BIRTHDAY },
        origin = OriginDate(CalendarBasis.GREGORIAN, c.date.year, c.date.month, c.date.day),
        contactLookupUri = lookupUriOf(c.lookupKey),
        contactPhone = c.phone,
        contactEmail = c.email,
        photoUri = photoUri,
    )
}

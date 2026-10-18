package com.ohadsam.specialevents.domain.backup

import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.domain.SpecialEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** One concrete day of an event, as exported to a calendar file. */
data class ExportedDay(val event: SpecialEvent, val date: LocalDate, val number: Int?)

/** iCalendar (.ics) so the events appear in any calendar app; every day is an all-day entry carrying the Hebrew date. */
object IcsExport {
    private val DAY = DateTimeFormatter.BASIC_ISO_DATE
    private const val FOLD_AT = 74

    fun build(days: List<ExportedDay>, stamp: String, appVersion: String): String {
        val lines = ArrayList<String>()
        lines += "BEGIN:VCALENDAR"
        lines += "VERSION:2.0"
        lines += "PRODID:-//Special Events//$appVersion//HE"
        lines += "CALSCALE:GREGORIAN"
        for (d in days) {
            lines += "BEGIN:VEVENT"
            lines += "UID:event-${d.event.id}-${d.date}@special-events"
            lines += "DTSTAMP:$stamp"
            lines += "DTSTART;VALUE=DATE:${DAY.format(d.date)}"
            lines += "DTEND;VALUE=DATE:${DAY.format(d.date.plusDays(1))}"
            lines += "SUMMARY:${escape(summary(d))}"
            lines += "DESCRIPTION:${escape(description(d))}"
            lines += "TRANSP:TRANSPARENT"
            lines += "END:VEVENT"
        }
        lines += "END:VCALENDAR"
        return lines.joinToString("") { fold(it) + "\r\n" }
    }

    private fun summary(d: ExportedDay) = d.number?.let { "${d.event.title} ($it)" } ?: d.event.title

    private fun description(d: ExportedDay): String = listOfNotNull(HebrewCalendar.fromLocalDate(d.date).format(), d.event.notes).joinToString("\n")

    /** RFC 5545 text escaping: backslash, semicolon, comma and line breaks. */
    fun escape(text: String): String = text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\n").replace("\n", "\\n")

    /** Lines longer than 75 octets continue on the next line after a space. Cuts on characters, never inside a surrogate pair or multi-byte letter. */
    fun fold(line: String): String {
        val out = StringBuilder()
        var bytes = 0
        var limit = FOLD_AT
        var i = 0
        while (i < line.length) {
            val cp = line.codePointAt(i)
            val size = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8).size
            if (bytes + size > limit) {
                out.append("\r\n ")
                bytes = 0
                limit = FOLD_AT - 1 // the leading space counts
            }
            out.appendCodePoint(cp)
            bytes += size
            i += Character.charCount(cp)
        }
        return out.toString()
    }
}

/** Spreadsheet export: opens in Excel/Sheets with Hebrew intact (UTF-8 byte-order mark) and quotes anything risky. */
object CsvExport {
    const val BOM = "\uFEFF"
    val HEADER = listOf("סוג", "כותרת", "שם", "לוח", "שנה", "חודש", "יום", "טלפון", "מייל", "תגיות", "הערות")

    fun build(events: List<SpecialEvent>): String {
        val rows = events.map { e ->
            listOf(
                e.type.name, e.title, e.personName.orEmpty(), e.origin.basis.name, e.origin.year?.toString().orEmpty(),
                e.origin.month.toString(), e.origin.day.toString(), e.contactPhone.orEmpty(), e.contactEmail.orEmpty(),
                e.tags.joinToString(" ") { it.name }, e.notes.orEmpty(),
            )
        }
        return BOM + (listOf(HEADER) + rows).joinToString("\r\n") { row -> row.joinToString(",") { cell(it) } } + "\r\n"
    }

    /** Quotes cells with separators or line breaks, and prefixes formula starters so a spreadsheet never runs a cell as a formula. */
    fun cell(raw: String): String {
        val safe = if (raw.isNotEmpty() && raw[0] in FORMULA_STARTERS && !PHONE.matches(raw)) "'$raw" else raw
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    private val FORMULA_STARTERS = setOf('=', '+', '-', '@', '\t')
    private val PHONE = Regex("[+0-9][0-9() -]*")
}

package com.ohadsam.specialevents.domain.backup

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.calendar.hasPossibleDay
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.Tag
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.calsync.CalendarImportPlanner
import com.ohadsam.specialevents.domain.datetext.DateParse
import com.ohadsam.specialevents.domain.datetext.DateTextParser
import java.time.LocalDate

/** The things a spreadsheet column can hold. */
enum class CsvField { TITLE, DATE, TYPE, PHONE, EMAIL, NOTES, TAGS, BASIS, YEAR, MONTH, DAY }

/** Which column (0-based) holds each field; absent = the file has no such column. */
data class CsvMapping(val columns: Map<CsvField, Int>)

data class CsvResult(val events: List<SpecialEvent>, val skippedRows: Int)

object CsvParser {
    private const val MAX_ROWS = 20_000

    /** RFC 4180 style: quoted cells may hold separators, quotes ("") and line breaks. The separator is guessed from the first line. */
    fun parse(input: String): List<List<String>> {
        val text = input.removePrefix("\uFEFF")
        val sep = detectSeparator(text)
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        fun endCell() {
            row.add(cell.toString())
            cell.setLength(0)
        }
        fun endRow() {
            endCell()
            if (row.any { it.isNotBlank() }) rows.add(row)
            row = ArrayList()
        }
        while (i < text.length && rows.size < MAX_ROWS) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> {
                    cell.append('"')
                    i++
                }
                // A quote only opens a quoted cell at its very start; inside a plain cell it is just a character (e.g. תשמ"ז).
                c == '"' && quoted -> quoted = false
                c == '"' && cell.isEmpty() -> quoted = true
                !quoted && c == sep -> endCell()
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    endRow()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }

    private fun detectSeparator(text: String): Char {
        val first = text.lineSequence().firstOrNull().orEmpty()
        return listOf(',', ';', '\t').maxByOrNull { s -> first.count { it == s } }?.takeIf { s -> first.contains(s) } ?: ','
    }
}

object CsvImport {
    private val HEADER_WORDS: Map<CsvField, List<String>> = mapOf(
        CsvField.TITLE to listOf("כותרת", "שם מלא", "שם", "name", "title", "full name"),
        CsvField.DATE to listOf("תאריך", "date", "birthday", "birth"),
        CsvField.TYPE to listOf("סוג", "type", "קטגוריה", "category"),
        CsvField.PHONE to listOf("טלפון", "נייד", "phone", "mobile", "cell"),
        CsvField.EMAIL to listOf("מייל", "דוא", "email", "e-mail", "mail"),
        CsvField.NOTES to listOf("הערות", "הערה", "notes", "note", "comment"),
        CsvField.TAGS to listOf("תגיות", "תגית", "tags", "labels"),
        CsvField.BASIS to listOf("לוח", "basis", "calendar"),
        CsvField.YEAR to listOf("שנה", "year"),
        CsvField.MONTH to listOf("חודש", "month"),
        CsvField.DAY to listOf("יום", "day"),
    )
    private val TYPE_BY_NAME = EventType.entries.associateBy { it.name.lowercase() }
    private const val MAX_TITLE = 200
    private const val MAX_NOTES = 2_000

    /** Picks, for each field, the first column whose header matches; each column is used once and the more specific words win. */
    fun guessMapping(header: List<String>): CsvMapping {
        val used = HashSet<Int>()
        val out = LinkedHashMap<CsvField, Int>()
        for (field in CsvField.entries) {
            val words = HEADER_WORDS.getValue(field)
            val index = header.indices.firstOrNull { i ->
                i !in used && header[i].let { h -> TextSearch.normalize(h).let { n -> words.any { w -> n == w || (w.length > 3 && n.contains(w)) } } }
            } ?: continue
            used += index
            out[field] = index
        }
        return CsvMapping(out)
    }

    /** True when the mapping gives enough to build events: a title, and a date (one column, or at least month and day). */
    fun isUsable(m: CsvMapping): Boolean =
        CsvField.TITLE in m.columns && (CsvField.DATE in m.columns || (CsvField.MONTH in m.columns && CsvField.DAY in m.columns))

    /** [rows] without the header row. Rows that lack a title or a readable date are counted and skipped, never guessed. */
    fun toEvents(rows: List<List<String>>, mapping: CsvMapping, today: LocalDate): CsvResult {
        val events = ArrayList<SpecialEvent>()
        var skipped = 0
        rows.forEachIndexed { index, row ->
            fun cell(f: CsvField): String? = mapping.columns[f]?.let { row.getOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }
            val title = cell(CsvField.TITLE)?.take(MAX_TITLE)
            val origin = title?.let { originOf(::cell, today) }
            if (title == null || origin == null) {
                skipped++
                return@forEachIndexed
            }
            val type = cell(CsvField.TYPE)?.let(::typeOf) ?: CalendarImportPlanner.guessType(title)
            val person = type == EventType.BIRTHDAY || type == EventType.YAHRZEIT || type == EventType.BAR_BAT_MITZVAH
            events += SpecialEvent(
                id = (index + 1).toLong(),
                type = type,
                title = title,
                personName = title.takeIf { person },
                origin = origin,
                recurrence = if (type.yearly) Recurrence.YEARLY else Recurrence.ONCE,
                notes = cell(CsvField.NOTES)?.take(MAX_NOTES),
                contactPhone = cell(CsvField.PHONE)?.take(MAX_TITLE),
                contactEmail = cell(CsvField.EMAIL)?.take(MAX_TITLE),
                tags = cell(CsvField.TAGS)?.split(' ', ',', ';')?.map { it.trim().removePrefix("#") }?.filter { it.isNotEmpty() }?.distinct()?.take(MAX_TAGS)?.map { Tag(name = it.take(MAX_TITLE)) }.orEmpty(),
            )
        }
        return CsvResult(events, skipped)
    }

    private const val MAX_TAGS = 20

    private fun typeOf(text: String): EventType = TYPE_BY_NAME[text.trim().lowercase()] ?: CalendarImportPlanner.guessType(text)

    private fun originOf(cell: (CsvField) -> String?, today: LocalDate): OriginDate? {
        cell(CsvField.DATE)?.let { text ->
            return (DateTextParser.parse(text, today) as? DateParse.Ok)?.date?.takeIf { !it.needsYear }?.origin()
        }
        val month = cell(CsvField.MONTH)?.toIntOrNull() ?: return null
        val day = cell(CsvField.DAY)?.toIntOrNull() ?: return null
        val basis = cell(CsvField.BASIS)?.let { b -> if (b.equals("HEBREW", true) || b.contains("עבר")) CalendarBasis.HEBREW else CalendarBasis.GREGORIAN } ?: CalendarBasis.GREGORIAN
        val limit = if (basis == CalendarBasis.HEBREW) HEBREW_MONTHS else GREGORIAN_MONTHS
        if (month !in 1..limit || day !in 1..MAX_DAY) return null
        return OriginDate(basis, cell(CsvField.YEAR)?.toIntOrNull(), month, day).takeIf { it.hasPossibleDay() }
    }

    private const val GREGORIAN_MONTHS = 12
    private const val HEBREW_MONTHS = 13
    private const val MAX_DAY = 31
}

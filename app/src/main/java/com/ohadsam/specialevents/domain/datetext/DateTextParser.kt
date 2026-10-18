package com.ohadsam.specialevents.domain.datetext

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.HebrewNumerals
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Understands a date written in plain Hebrew: "17 באוקטובר 1980", "17.10.80", "כ' בחשוון תשמ"ז", "בת 46 נולדה ב-17 באוקטובר",
 * "נולד ביום שלישי י"ג באלול", "... אחרי השקיעה". Works offline and never guesses silently: what it had to assume is reported as a
 * [ParseWarning], and a weekday without a year yields the possible years instead of picking one.
 */
object DateTextParser {
    private const val MAX_AGE = 130
    private const val MAX_DAY = 31
    private const val MAX_HEBREW_DAY = 30
    private const val GREGORIAN_YEAR_MIN = 1000
    private const val HEBREW_YEAR_MIN = 3000
    private const val HEBREW_THOUSANDS = 5000
    private const val NUMERAL_YEAR_MIN = 100
    private const val CENTURY = 100
    private const val MAX_CANDIDATES = 8
    private const val LOOKBACK_YEARS = 120
    private const val ADAR = 12
    private const val ADAR_II = 13

    private val GREGORIAN_MONTHS = mapOf(
        "ינואר" to 1, "פברואר" to 2, "מרץ" to 3, "מרס" to 3, "מארס" to 3, "אפריל" to 4, "מאי" to 5, "יוני" to 6,
        "יולי" to 7, "אוגוסט" to 8, "ספטמבר" to 9, "אוקטובר" to 10, "נובמבר" to 11, "דצמבר" to 12,
    )
    private val HEBREW_MONTHS = mapOf(
        "ניסן" to 1, "אייר" to 2, "סיוון" to 3, "סיון" to 3, "תמוז" to 4, "אב" to 5, "אלול" to 6, "תשרי" to 7,
        "חשוון" to 8, "חשון" to 8, "מרחשוון" to 8, "מרחשון" to 8, "כסלו" to 9, "כסליו" to 9, "טבת" to 10, "שבט" to 11,
    )
    private val WEEKDAYS = mapOf(
        "ראשון" to DayOfWeek.SUNDAY, "שני" to DayOfWeek.MONDAY, "שלישי" to DayOfWeek.TUESDAY, "רביעי" to DayOfWeek.WEDNESDAY,
        "חמישי" to DayOfWeek.THURSDAY, "שישי" to DayOfWeek.FRIDAY, "שבת" to DayOfWeek.SATURDAY,
    )

    private val AFTER_SUNSET = Regex("אחר(?:י)? (?:ה)?(?:שקיעה|צאת הכוכבים)|בלילה")
    private val AGE = Regex("(?:בן|בת|גיל|בגיל) (\\d{1,3})(?!\\d)|(?<![\\d./-])(\\d{1,3}) שנ(?:ה|ים)(?!\\p{L})")
    private val WEEKDAY = Regex("(?:ב)?יום (ראשון|שני|שלישי|רביעי|חמישי|שישי|שבת)|(?:ב)?(שבת)(?!\\p{L})")
    private val ISO = Regex("(?<!\\d)(\\d{4})-(\\d{1,2})-(\\d{1,2})(?!\\d)")
    private val NUMERIC = Regex("(?<!\\d)(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2}|\\d{4})(?!\\d)")
    private val SEPARATORS = Regex("[^\\p{L}\\p{N}'\"\u05F3\u05F4]+")
    private val QUOTES = Regex("['\"\u05F3\u05F4]")
    private val MARKS = Regex("[\\u0591-\\u05BD\\u05BF-\\u05C7]")
    private val DAY_NUMERALS: Map<String, Int> = (1..MAX_HEBREW_DAY).associateBy { HebrewNumerals.toHebrew(it, punctuation = false) }
    private val PREPOSITIONS = setOf('ב', 'ל', 'ה', 'ו', 'מ', 'ש')
    private val HUNDREDS_LETTERS = setOf('ק', 'ר', 'ש', 'ת')

    fun parse(text: String, today: LocalDate): DateParse {
        var t = clean(text)
        if (t.isEmpty()) return DateParse.Failure(DateParseError.EMPTY)
        val warnings = LinkedHashSet<ParseWarning>()

        val afterSunset = AFTER_SUNSET.containsMatchIn(t)
        t = t.replace(AFTER_SUNSET, " ")
        val age = AGE.find(t)?.let { m -> (m.groups[1] ?: m.groups[2])?.value?.toIntOrNull()?.takeIf { it in 0..MAX_AGE } }
        t = t.replace(AGE, " ")
        val weekday = WEEKDAY.find(t)?.let { m -> WEEKDAYS[m.groups[1]?.value ?: m.groups[2]?.value] }
        t = t.replace(WEEKDAY, " ")

        val core = readCore(t, today, warnings) ?: return DateParse.Failure(failureOf(t))
        if (core is Core.Missing) return DateParse.Failure(core.error)
        core as Core.Found
        return DateParse.Ok(finish(core, afterSunset, age, weekday, today, warnings) ?: return DateParse.Failure(DateParseError.INVALID_DATE))
    }

    private sealed interface Core {
        data class Found(val basis: CalendarBasis, val year: Int?, val month: Int, val day: Int, val adarPlain: Boolean = false) : Core

        data class Missing(val error: DateParseError) : Core
    }

    private fun failureOf(t: String) = if (t.any { it.isLetterOrDigit() }) DateParseError.NO_MONTH else DateParseError.EMPTY

    /** Nikud off, lower case, single spaces - but geresh and gershayim stay: they tell "ה׳" (the 5th) from the letter ה. */
    private fun clean(text: String): String = text.replace(MARKS, "").lowercase().trim().replace(Regex("\\s+"), " ")

    private data class Tok(val text: String, val marked: Boolean)

    private fun tokensOf(t: String): List<Tok> = t.split(SEPARATORS).filter { it.isNotEmpty() }.mapNotNull { raw ->
        val core = raw.replace(QUOTES, "")
        if (core.isEmpty()) null else Tok(core, core != raw)
    }

    private fun readCore(text: String, today: LocalDate, warnings: MutableSet<ParseWarning>): Core? {
        ISO.find(text)?.let { m -> return Core.Found(CalendarBasis.GREGORIAN, m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }
        NUMERIC.find(text)?.let { m ->
            val y = m.groupValues[3]
            val year = if (y.length == 2) twoDigitYear(y.toInt(), today).also { warnings += ParseWarning.TWO_DIGIT_YEAR } else y.toInt()
            return Core.Found(CalendarBasis.GREGORIAN, year, m.groupValues[2].toInt(), m.groupValues[1].toInt())
        }
        val tokens = tokensOf(text)
        return readHebrew(tokens) ?: readGregorian(tokens, today, warnings)
    }

    private fun twoDigitYear(yy: Int, today: LocalDate): Int = if (yy > today.year % CENTURY) 1900 + yy else 2000 + yy

    /** Strips a leading preposition letter (ב, ל, ו, מ, ה) when what remains is a known word. */
    private fun <V> lookup(token: String, names: Map<String, V>): V? =
        names[token] ?: if (token.length > 1 && token[0] in PREPOSITIONS) names[token.substring(1)] else null

    private fun readHebrew(tokens: List<Tok>): Core? {
        val at = tokens.indexOfFirst { lookup(it.text, HEBREW_MONTHS) != null || isAdar(it.text) }
        if (at < 0) return null
        var consumed = 1
        var adarPlain = false
        val month: Int = if (isAdar(tokens[at].text)) {
            when (tokens.getOrNull(at + 1)?.text) {
                "א", "ראשון" -> ADAR.also { consumed = 2 }
                "ב", "שני" -> ADAR_II.also { consumed = 2 }
                else -> ADAR.also { adarPlain = true }
            }
        } else {
            lookup(tokens[at].text, HEBREW_MONTHS)!!
        }
        val day = dayNear(tokens, at, MAX_HEBREW_DAY, skip = at until at + consumed) ?: return Core.Missing(DateParseError.NO_DAY)
        val year = hebrewYear(tokens.filterIndexed { i, _ -> i !in at until at + consumed && i != day.index })
        return Core.Found(CalendarBasis.HEBREW, year, month, day.value, adarPlain)
    }

    private fun isAdar(token: String) = token == "אדר" || token == "באדר" || token == "לאדר"

    private fun readGregorian(tokens: List<Tok>, today: LocalDate, warnings: MutableSet<ParseWarning>): Core? {
        val at = tokens.indexOfFirst { lookup(it.text, GREGORIAN_MONTHS) != null }
        if (at < 0) return null
        val month = lookup(tokens[at].text, GREGORIAN_MONTHS)!!
        val day = dayNear(tokens, at, MAX_DAY, skip = at..at) ?: return Core.Missing(DateParseError.NO_DAY)
        val rest = tokens.filterIndexed { i, _ -> i != at && i != day.index }.map { it.text }
        val four = rest.firstOrNull { it.length == 4 && it.all(Char::isDigit) && it.toInt() >= GREGORIAN_YEAR_MIN }?.toInt()
        val two = if (four == null) rest.firstOrNull { it.length == 2 && it.all(Char::isDigit) }?.toInt() else null
        val year = four ?: two?.let { twoDigitYear(it, today).also { warnings += ParseWarning.TWO_DIGIT_YEAR } }
        return Core.Found(CalendarBasis.GREGORIAN, year, month, day.value)
    }

    private data class Day(val value: Int, val index: Int)

    /**
     * The day number written next to the month word, nearest first: digits, or a Hebrew numeral in its proper form (טו, כב...).
     * A lone letter that is also a preposition (ב, ה, ו...) only counts with a geresh; "בה׳" is ב + ה׳ (the 5th).
     */
    private fun dayNear(tokens: List<Tok>, monthAt: Int, max: Int, skip: IntRange): Day? {
        val order = ((monthAt - 1 downTo 0) + (monthAt + 1 until tokens.size)).filter { it !in skip }
        for (i in order) {
            val tk = tokens[i]
            val value = when {
                tk.text.all(Char::isDigit) && tk.text.length <= 2 -> tk.text.toInt()
                tk.text in DAY_NUMERALS -> DAY_NUMERALS[tk.text]?.takeUnless { tk.text.length == 1 && tk.text[0] in PREPOSITIONS && tk.text != "ל" && !tk.marked }
                tk.text.length in 2..3 && tk.text[0] == 'ב' -> DAY_NUMERALS[tk.text.substring(1)]
                else -> null
            }
            if (value != null && value in 1..max) return Day(value, i)
        }
        return null
    }

    private fun hebrewYear(rest: List<Tok>): Int? {
        for (tok in rest) {
            val tk = tok.text
            if (tk.all(Char::isDigit)) {
                val n = tk.toIntOrNull() ?: continue
                if (n >= HEBREW_YEAR_MIN) return n
                continue
            }
            if (!tk.all { it in '\u05D0'..'\u05EA' }) continue
            val stripped = if (tk.length > 2 && tk[0] == 'ה') tk.substring(1) else tk
            val value = HebrewNumerals.fromHebrew(stripped) ?: continue
            // A year written in letters starts with its hundreds (ת, ש, ר, ק); this keeps words like "הולדת" from being read as a year.
            if (stripped.length >= 2 && value >= NUMERAL_YEAR_MIN && stripped[0] in HUNDREDS_LETTERS) return if (value < HEBREW_THOUSANDS) value + HEBREW_THOUSANDS else value
        }
        return null
    }

    private fun finish(c: Core.Found, afterSunset: Boolean, age: Int?, weekday: DayOfWeek?, today: LocalDate, warnings: MutableSet<ParseWarning>): ParsedDate? {
        var year = c.year
        var month = c.month
        if (c.basis == CalendarBasis.HEBREW && c.adarPlain && year != null && HebrewCalendar.isLeapYear(year)) {
            month = ADAR_II
            warnings += ParseWarning.ADAR_READ_AS_SECOND
        }
        if (year == null && age != null) {
            year = yearFromAge(c.basis, month, c.day, age, today)
            if (year == null) warnings += ParseWarning.AGE_UNRESOLVED
        }
        if (year != null && toCivil(c.basis, year, month, c.day) == null) return null
        if (year == null && !existsSomeYear(c.basis, month, c.day, today)) return null

        var candidates = emptyList<Int>()
        var actual: DayOfWeek? = null
        if (year != null) {
            actual = toCivil(c.basis, year, month, c.day)?.dayOfWeek
            if (weekday != null && actual != weekday) warnings += ParseWarning.WEEKDAY_MISMATCH
        } else if (weekday != null) {
            candidates = candidateYears(c.basis, month, c.day, weekday, today)
            if (candidates.size > 1) warnings += ParseWarning.AMBIGUOUS_YEAR
            if (candidates.size == 1) {
                year = candidates.single()
                actual = weekday
                candidates = emptyList()
            }
        }
        return ParsedDate(c.basis, year, month, c.day, afterSunset, age, weekday, actual, candidates, warnings)
    }

    private fun toCivil(basis: CalendarBasis, year: Int, month: Int, day: Int): LocalDate? = runCatching {
        when (basis) {
            CalendarBasis.GREGORIAN -> LocalDate.of(year, month, day)
            CalendarBasis.HEBREW -> HebrewCalendar.toLocalDate(HebrewDate(year, month, day))
        }
    }.getOrNull()

    private fun existsSomeYear(basis: CalendarBasis, month: Int, day: Int, today: LocalDate): Boolean {
        val start = if (basis == CalendarBasis.GREGORIAN) today.year else HebrewCalendar.fromLocalDate(today).year
        return (start - LOOKBACK_YEARS..start).any { toCivil(basis, it, month, day) != null }
    }

    /** The year of birth that makes the person exactly [age] today; this year's date counts as passed when it is today or earlier. */
    private fun yearFromAge(basis: CalendarBasis, month: Int, day: Int, age: Int, today: LocalDate): Int? {
        val thisYear = if (basis == CalendarBasis.GREGORIAN) today.year else HebrewCalendar.fromLocalDate(today).year
        val occurrence = toCivil(basis, thisYear, month, day) ?: return null
        val birthYear = thisYear - age - if (occurrence.isAfter(today)) 1 else 0
        return birthYear.takeIf { toCivil(basis, it, month, day) != null }
    }

    private fun candidateYears(basis: CalendarBasis, month: Int, day: Int, weekday: DayOfWeek, today: LocalDate): List<Int> {
        val last = if (basis == CalendarBasis.GREGORIAN) today.year else HebrewCalendar.fromLocalDate(today).year
        return (last downTo last - LOOKBACK_YEARS)
            .filter { y -> toCivil(basis, y, month, day)?.let { it.dayOfWeek == weekday && !it.isAfter(today) } == true }
            .take(MAX_CANDIDATES)
    }
}

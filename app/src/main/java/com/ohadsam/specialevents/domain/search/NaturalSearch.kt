package com.ohadsam.specialevents.domain.search

import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.TextSearch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class ParsedSearch(val steps: List<SearchStep>, val leftover: String)

/**
 * Turns a free Hebrew phrase ("ימי הולדת באדר", "אזכרות בשנה הבאה", "בני 40+", "עגולים החודש #משפחה") into search steps, offline.
 * Anything not recognised becomes a plain text step. The phrase is normalised first (no nikud/quotes), and each recogniser removes
 * the words it used.
 */
object NaturalSearch {
    private val GREGORIAN_MONTHS = listOf("ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני", "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר")

    // Hebrew month names -> HebrewCalendar numbers (Nisan = 1 ... Tishrei = 7 ... Adar = 12, Adar II = 13).
    private val HEBREW_MONTHS: List<Pair<String, Set<Int>>> = listOf(
        "אדר ב" to setOf(13), "אדר א" to setOf(12), "אדר" to setOf(12, 13),
        "תשרי" to setOf(7), "מרחשוון" to setOf(8), "חשוון" to setOf(8), "חשון" to setOf(8), "כסלו" to setOf(9), "טבת" to setOf(10), "שבט" to setOf(11),
        "ניסן" to setOf(1), "אייר" to setOf(2), "סיוון" to setOf(3), "סיון" to setOf(3), "תמוז" to setOf(4), "אב" to setOf(5), "אלול" to setOf(6),
    )

    private val TYPE_WORDS: List<Pair<String, EventType>> = listOf(
        "ימי הולדת|יום הולדת|יומולדת" to EventType.BIRTHDAY,
        "ימי נישואין|יום נישואין|נישואין|חתונות|חתונה" to EventType.ANNIVERSARY,
        "אזכרות|אזכרה|יארצייט|יום פטירה|פטירה" to EventType.YAHRZEIT,
        "בר מצווה|בת מצווה|בר מצוה|בת מצוה|בר מצוות|בת מצוות" to EventType.BAR_BAT_MITZVAH,
        "אירועי עבודה|עבודה" to EventType.WORK,
        "אירועי משפחה|משפחה" to EventType.FAMILY,
    )

    private val STOP_WORDS = setOf("של", "את", "עם", "כל", "ה", "לי", "אירועים", "אירוע")

    private fun word(pattern: String) = Regex("(?<=\\s)(?:$pattern)(?=\\s)")

    fun parse(input: String, today: LocalDate): ParsedSearch {
        var rest = " " + TextSearch.normalize(input) + " "
        val steps = ArrayList<SearchStep>()

        /** Finds [regex] in the remaining text; if [handler] makes a step of it, the matched words are consumed. */
        fun take(regex: Regex, handler: (MatchResult) -> SearchStep?): Boolean {
            val m = regex.find(rest) ?: return false
            val step = handler(m) ?: return false
            steps.add(step)
            rest = " " + rest.removeRange(m.range) + " "
            return true
        }

        // --- months first, so "בחודש אדר" is a month and not "this month" ---
        for ((name, months) in HEBREW_MONTHS) {
            if (take(word("ב$name|חודש $name")) { SearchStep.HebrewMonths(months, "ב$name") }) break
        }
        for ((i, name) in GREGORIAN_MONTHS.withIndex()) {
            if (take(word("ב$name|חודש $name")) { SearchStep.GregorianMonths(setOf(i + 1), "ב$name") }) break
        }

        // --- time windows (longest phrases first; only the first that matches is used) ---
        take(word("(?:ב-?)?(\\d+) (?:ה)?ימים הקרובים")) { SearchStep.Range(today, today.plusDays(it.groupValues[1].toLong()), it.value.trim()) } ||
                take(word("(?:ב|ה)שבוע הבא")) {
                    val start = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                    SearchStep.Range(start, start.plusDays(6), "השבוע הבא")
                } ||
                take(word("(?:ב|ה)שבוע הקרוב")) { SearchStep.Range(today, today.plusDays(6), "השבוע הקרוב") } ||
                take(word("(?:ב|ה)שבוע")) {
                    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                    SearchStep.Range(start, start.plusDays(6), "השבוע")
                } ||
                take(word("(?:ב|ה)חודש הבא")) {
                    val first = today.withDayOfMonth(1).plusMonths(1)
                    SearchStep.Range(first, first.withDayOfMonth(first.lengthOfMonth()), "החודש הבא")
                } ||
                take(word("(?:ב|ה)חודש(?: הקרוב)?")) { SearchStep.Range(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()), "החודש") } ||
                take(word("(?:ב|ה)שנה הבאה")) { SearchStep.Range(LocalDate.of(today.year + 1, 1, 1), LocalDate.of(today.year + 1, 12, 31), "השנה הבאה") } ||
                take(word("(?:ב|ה)שנה הקרובה")) { SearchStep.Range(today, today.plusDays(365), "השנה הקרובה") } ||
                take(word("(?:ב|ה)שנה")) { SearchStep.Range(LocalDate.of(today.year, 1, 1), LocalDate.of(today.year, 12, 31), "השנה") } ||
                take(word("מחרתיים")) { SearchStep.Range(today.plusDays(2), today.plusDays(2), "מחרתיים") } ||
                take(word("מחר")) { SearchStep.Range(today.plusDays(1), today.plusDays(1), "מחר") } ||
                take(word("היום")) { SearchStep.Range(today, today, "היום") } ||
                take(word("בשבת הקרובה")) {
                    val sat = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                    SearchStep.Range(sat, sat, "בשבת הקרובה")
                }

        // --- Shabbat, round numbers, jubilee ---
        take(word("בשבת")) { SearchStep.ShabbatOnly }
        take(word("(?:ה)?עגול(?:ים|ות|ה)?")) { SearchStep.RoundOnly }
        take(word("(?:ב)?יובל")) { SearchStep.Age(50, 50) }

        // --- age ---
        take(word("(?:בני|בנות|גיל|בגיל) (\\d+) ?(?:\\+|ומעלה)")) { SearchStep.Age(it.groupValues[1].toInt(), null) } ||
            take(word("(?:מעל|מעל גיל|מעל ל) ?(\\d+)")) { SearchStep.Age(it.groupValues[1].toInt() + 1, null) } ||
            take(word("מתחת (?:ל|לגיל ?|ל-)?(\\d+)")) { SearchStep.Age(null, it.groupValues[1].toInt() - 1) } ||
            take(word("(?:בני|בנות|גיל|בגיל) (\\d+)")) { SearchStep.Age(it.groupValues[1].toInt(), it.groupValues[1].toInt()) }

        // --- types (all found are combined into one step, shown first) ---
        val types = LinkedHashSet<EventType>()
        for ((pattern, type) in TYPE_WORDS) {
            val m = word("(?:ה)?(?:$pattern)").find(rest) ?: continue
            types.add(type)
            rest = " " + rest.removeRange(m.range) + " "
        }
        if (types.isNotEmpty()) steps.add(0, SearchStep.Types(types))

        // --- #tags ---
        val tags = LinkedHashSet<String>()
        Regex("#(\\S+)").findAll(rest).forEach { tags.add(it.groupValues[1]) }
        if (tags.isNotEmpty()) {
            steps.add(SearchStep.Tags(tags))
            rest = rest.replace(Regex("#\\S+"), " ")
        }

        val leftover = rest.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && it !in STOP_WORDS }.joinToString(" ")
        if (leftover.isNotEmpty()) steps.add(SearchStep.Text(leftover))
        return ParsedSearch(steps, leftover)
    }
}

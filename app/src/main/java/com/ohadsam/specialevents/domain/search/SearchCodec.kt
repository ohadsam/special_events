package com.ohadsam.specialevents.domain.search

import com.ohadsam.specialevents.domain.EventType
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate

/** A search the user saved under a name. */
data class SavedSearch(val name: String, val steps: List<SearchStep>)

/** Compact text form of steps and saved searches for storage; unreadable parts are skipped, never fatal. */
object SearchCodec {
    private const val STEP_SEP = ";"
    private const val FIELD_SEP = "|"
    private const val RECORD_SEP = "\u001E"
    private const val NAME_SEP = "\u001F"

    fun encode(steps: List<SearchStep>): String = steps.joinToString(STEP_SEP) { encodeStep(it) }

    fun decode(text: String): List<SearchStep> = text.split(STEP_SEP).filter { it.isNotBlank() }.mapNotNull { runCatching { decodeStep(it) }.getOrNull() }

    fun encodeSaved(list: List<SavedSearch>): String = list.joinToString(RECORD_SEP) { "${esc(it.name)}$NAME_SEP${encode(it.steps)}" }

    fun decodeSaved(text: String): List<SavedSearch> = text.split(RECORD_SEP).filter { it.isNotBlank() }.mapNotNull { rec ->
        val parts = rec.split(NAME_SEP, limit = 2)
        if (parts.size != 2) null else SavedSearch(unesc(parts[0]), decode(parts[1]))
    }

    private fun encodeStep(step: SearchStep): String = when (step) {
        is SearchStep.Text -> listOf("T", esc(step.query))
        is SearchStep.Types -> listOf("Y", step.types.joinToString(",") { it.name })
        is SearchStep.Tags -> listOf("G", step.names.joinToString(",") { esc(it) })
        is SearchStep.Range -> listOf("R", step.from.toString(), step.to.toString(), esc(step.phrase.orEmpty()))
        is SearchStep.HebrewMonths -> listOf("H", step.months.joinToString(","), esc(step.name))
        is SearchStep.GregorianMonths -> listOf("M", step.months.joinToString(","), esc(step.name))
        is SearchStep.Age -> listOf("A", step.min?.toString().orEmpty(), step.max?.toString().orEmpty())
        SearchStep.RoundOnly -> listOf("O")
        SearchStep.ShabbatOnly -> listOf("S")
    }.joinToString(FIELD_SEP)

    private fun decodeStep(text: String): SearchStep? {
        val f = text.split(FIELD_SEP)
        return when (f[0]) {
            "T" -> SearchStep.Text(unesc(f[1]))
            "Y" -> SearchStep.Types(f[1].split(",").mapNotNull { n -> EventType.entries.firstOrNull { it.name == n } }.toSet())
            "G" -> SearchStep.Tags(f[1].split(",").filter { it.isNotEmpty() }.map(::unesc).toSet())
            "R" -> SearchStep.Range(LocalDate.parse(f[1]), LocalDate.parse(f[2]), unesc(f[3]).ifEmpty { null })
            "H" -> SearchStep.HebrewMonths(ints(f[1]), unesc(f[2]))
            "M" -> SearchStep.GregorianMonths(ints(f[1]), unesc(f[2]))
            "A" -> SearchStep.Age(f[1].toIntOrNull(), f[2].toIntOrNull())
            "O" -> SearchStep.RoundOnly
            "S" -> SearchStep.ShabbatOnly
            else -> null
        }
    }

    private fun ints(s: String) = s.split(",").mapNotNull { it.toIntOrNull() }.toSet()

    private fun esc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun unesc(s: String): String = URLDecoder.decode(s, "UTF-8")
}

/** Ready-made searches offered when the search box is empty; each is a phrase the natural-language engine understands. */
object SearchSuggestions {
    val PHRASES: List<String> = listOf(
        "השבוע",
        "החודש הבא",
        "עגולים בשנה הקרובה",
        "ימי הולדת בחודש הבא",
        "אזכרות בשנה הקרובה",
        "ימי נישואין עגולים",
        "בר מצווה בשנה הקרובה",
        "בשבת הקרובה",
        "אירועי עבודה",
        "בני 60 ומעלה",
    )
}

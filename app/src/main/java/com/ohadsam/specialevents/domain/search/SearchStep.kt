package com.ohadsam.specialevents.domain.search

import com.ohadsam.specialevents.domain.EventType
import java.time.LocalDate

/**
 * One narrowing step of a search. A search is a chain of steps (the "search within search" chips): each step keeps only what
 * matches it. [label] is the Hebrew text shown on the chip; the natural-language engine is Hebrew-specific, so its labels live here.
 */
sealed interface SearchStep {
    val label: String

    data class Text(val query: String) : SearchStep {
        override val label get() = "״$query״"
    }

    data class Types(val types: Set<EventType>) : SearchStep {
        override val label get() = types.joinToString(" / ") { typeName(it) }
    }

    data class Tags(val names: Set<String>) : SearchStep {
        override val label get() = names.joinToString(" ") { "#$it" }
    }

    /** Dates to look at. [phrase] is how the user said it ("השבוע"); without one the dates are shown. */
    data class Range(val from: LocalDate, val to: LocalDate, val phrase: String? = null) : SearchStep {
        override val label get() = phrase ?: if (from == to) dateText(from) else "${dateText(from)} – ${dateText(to)}"
    }

    data class HebrewMonths(val months: Set<Int>, val name: String) : SearchStep {
        override val label get() = name
    }

    data class GregorianMonths(val months: Set<Int>, val name: String) : SearchStep {
        override val label get() = name
    }

    data class Age(val min: Int?, val max: Int?) : SearchStep {
        override val label
            get() = when {
                min != null && max != null && min == max -> "גיל $min"
                min != null && max != null -> "גיל $min–$max"
                min != null -> "גיל $min ומעלה"
                max != null -> "עד גיל $max"
                else -> "גיל"
            }
    }

    data object RoundOnly : SearchStep {
        override val label get() = "עגולים"
    }

    data object ShabbatOnly : SearchStep {
        override val label get() = "בשבת"
    }

    companion object {
        fun typeName(type: EventType): String = when (type) {
            EventType.BIRTHDAY -> "ימי הולדת"
            EventType.ANNIVERSARY -> "ימי נישואין"
            EventType.YAHRZEIT -> "אזכרות"
            EventType.BAR_BAT_MITZVAH -> "בר/בת מצווה"
            EventType.WORK -> "עבודה"
            EventType.FAMILY -> "משפחה"
            EventType.CUSTOM -> "אחר"
        }

        private fun dateText(d: LocalDate) = "${d.dayOfMonth}.${d.monthValue}.${d.year}"
    }
}

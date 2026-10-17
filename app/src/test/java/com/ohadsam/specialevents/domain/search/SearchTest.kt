package com.ohadsam.specialevents.domain.search

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.Tag
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    // 2026-10-17 is a Saturday, 6 Cheshvan 5787.
    private val today = LocalDate.of(2026, 10, 17)

    private fun parse(s: String) = NaturalSearch.parse(s, today).steps

    @Test
    fun typesAndMonths() {
        assertEquals(listOf(SearchStep.Types(setOf(EventType.BIRTHDAY)), SearchStep.HebrewMonths(setOf(12, 13), "באדר")), parse("ימי הולדת באדר").let { listOf(it[0], it[1]) }.sortedBy { it is SearchStep.HebrewMonths })
        assertEquals(SearchStep.GregorianMonths(setOf(3), "במרץ"), parse("אירועים במרץ").first())
        assertEquals(SearchStep.HebrewMonths(setOf(13), "באדר ב"), parse("באדר ב׳").first())
    }

    @Test
    fun timeWindows() {
        assertEquals(SearchStep.Range(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), "השנה הבאה"), parse("אזכרות בשנה הבאה").first { it is SearchStep.Range })
        assertEquals(SearchStep.Range(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "החודש הבא"), parse("החודש הבא").single())
        assertEquals(SearchStep.Range(today, today, "היום"), parse("היום").single())
        assertEquals(SearchStep.Range(today.plusDays(1), today.plusDays(1), "מחר"), parse("מחר").single())
        assertEquals(SearchStep.Range(today, today.plusDays(30), "30 הימים הקרובים"), parse("30 הימים הקרובים").single())
        val week = parse("השבוע").single() as SearchStep.Range
        assertEquals(LocalDate.of(2026, 10, 11), week.from)
        assertEquals(LocalDate.of(2026, 10, 17), week.to)
    }

    @Test
    fun typeWordsDoNotTriggerTimePhrases() {
        // "יום הולדת" contains the word "יום" but must stay a birthday type, not "today".
        val steps = parse("יום הולדת")
        assertEquals(listOf<SearchStep>(SearchStep.Types(setOf(EventType.BIRTHDAY))), steps)
    }

    @Test
    fun ageForms() {
        assertEquals(SearchStep.Age(40, null), parse("בני 40+").single())
        assertEquals(SearchStep.Age(60, null), parse("גיל 60 ומעלה").single())
        assertEquals(SearchStep.Age(41, null), parse("מעל 40").single())
        assertEquals(SearchStep.Age(null, 17), parse("מתחת ל-18").single())
        assertEquals(SearchStep.Age(50, 50), parse("גיל 50").single())
        assertEquals(SearchStep.Age(50, 50), parse("ביובל").single())
    }

    @Test
    fun roundShabbatTagsAndLeftoverText() {
        assertEquals(listOf<SearchStep>(SearchStep.RoundOnly), parse("עגולים"))
        assertEquals(listOf<SearchStep>(SearchStep.ShabbatOnly), parse("בשבת"))
        val steps = parse("דנה #משפחה עגולים")
        assertTrue(SearchStep.RoundOnly in steps)
        assertTrue(SearchStep.Tags(setOf("משפחה")) in steps)
        assertTrue(SearchStep.Text("דנה") in steps)
    }

    @Test
    fun everySuggestionIsUnderstood() {
        SearchSuggestions.PHRASES.forEach { phrase ->
            val parsed = NaturalSearch.parse(phrase, today)
            assertTrue("$phrase -> $parsed", parsed.steps.isNotEmpty() && parsed.leftover.isEmpty())
        }
    }

    private fun ev(title: String, type: EventType, year: Int, month: Int, day: Int, tags: List<String> = emptyList(), gender: Gender? = null, basis: CalendarBasis = CalendarBasis.GREGORIAN) =
        SpecialEvent(type = type, title = title, gender = gender, origin = OriginDate(basis, year, month, day), tags = tags.map { Tag(name = it) })

    private val events = listOf(
        ev("דנה", EventType.BIRTHDAY, 1976, 10, 20, listOf("משפחה")), // turns 50 on 2026-10-20
        ev("יוסי", EventType.BIRTHDAY, 1990, 11, 3, listOf("עבודה")),
        ev("אבי ושרה", EventType.ANNIVERSARY, 2001, 12, 1),
        ev("סבא", EventType.YAHRZEIT, 5750, 8, 20, basis = CalendarBasis.HEBREW),
    )

    private fun run(vararg steps: SearchStep, includeArchived: Boolean = false) =
        SearchEngine.run(events, steps.toList(), today, inIsrael = true, includeArchived = includeArchived).map { it.event.title }

    @Test
    fun enginePicksByTypeAndSortsByDate() {
        assertEquals(listOf("דנה", "יוסי"), run(SearchStep.Types(setOf(EventType.BIRTHDAY))))
        assertEquals(listOf("דנה", "יוסי", "סבא", "אבי ושרה"), run())
    }

    @Test
    fun searchWithinSearchNarrowsStepByStep() {
        val birthdays = SearchStep.Types(setOf(EventType.BIRTHDAY))
        assertEquals(listOf("דנה"), run(birthdays, SearchStep.RoundOnly))
        assertEquals(listOf("דנה"), run(birthdays, SearchStep.Age(50, 50)))
        assertEquals(listOf("דנה"), run(birthdays, SearchStep.Tags(setOf("משפחה"))))
        assertEquals(listOf("יוסי"), run(birthdays, SearchStep.GregorianMonths(setOf(11), "בנובמבר")))
        assertEquals(emptyList<String>(), run(birthdays, SearchStep.Age(80, null)))
    }

    @Test
    fun rangeStepsIntersectAndHebrewMonthFilters() {
        val nov = SearchStep.Range(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
        assertEquals(listOf("יוסי", "סבא"), run(nov).let { it.filter { t -> t == "יוסי" || t == "סבא" } })
        val earlyNov = SearchStep.Range(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10))
        assertEquals(listOf("יוסי"), run(nov, earlyNov))
        assertEquals(emptyList<String>(), run(SearchStep.Range(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2)), earlyNov))
        assertEquals(listOf("סבא"), run(SearchStep.HebrewMonths(setOf(8), "בחשוון"), SearchStep.Types(setOf(EventType.YAHRZEIT))))
    }

    @Test
    fun textStepAndArchivedSwitch() {
        assertEquals(listOf("אבי ושרה"), run(SearchStep.Text("שרה")))
        val archived = events.map { if (it.title == "יוסי") it.copy(archived = true) else it }
        assertTrue("יוסי" !in SearchEngine.run(archived, emptyList(), today, true).map { it.event.title })
        assertTrue("יוסי" in SearchEngine.run(archived, emptyList(), today, true, includeArchived = true).map { it.event.title })
    }

    @Test
    fun mitzvahEntriesBelongToTheMitzvahType() {
        val birth = HebrewDate(5774, 8, 20)
        val kid = SpecialEvent(type = EventType.BIRTHDAY, title = "נער", gender = Gender.MALE, origin = OriginDate(CalendarBasis.HEBREW, birth.year, birth.month, birth.day))
        val from = HebrewDate(5787, 8, 1).toLocalDate()
        val steps = listOf<SearchStep>(SearchStep.Range(from, from.plusDays(40)))
        val all = SearchEngine.run(listOf(kid), steps, from, true)
        assertEquals(2, all.size)
        assertEquals(1, SearchEngine.run(listOf(kid), steps + SearchStep.Types(setOf(EventType.BAR_BAT_MITZVAH)), from, true).size)
        assertEquals(1, SearchEngine.run(listOf(kid), steps + SearchStep.Types(setOf(EventType.BIRTHDAY)), from, true).size)
    }

    @Test
    fun codecRoundTrip() {
        val steps = listOf(
            SearchStep.Text("שם | עם; סימנים"),
            SearchStep.Types(setOf(EventType.BIRTHDAY, EventType.YAHRZEIT)),
            SearchStep.Tags(setOf("משפחה", "חברים")),
            SearchStep.Range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "השנה"),
            SearchStep.Range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)),
            SearchStep.HebrewMonths(setOf(12, 13), "באדר"),
            SearchStep.GregorianMonths(setOf(3), "במרץ"),
            SearchStep.Age(null, 17),
            SearchStep.Age(40, null),
            SearchStep.RoundOnly,
            SearchStep.ShabbatOnly,
        )
        assertEquals(steps, SearchCodec.decode(SearchCodec.encode(steps)))
        val saved = listOf(SavedSearch("חיפוש 1", steps), SavedSearch("אחר", emptyList()))
        assertEquals(saved, SearchCodec.decodeSaved(SearchCodec.encodeSaved(saved)))
        assertEquals(emptyList<SearchStep>(), SearchCodec.decode("garbage|x;Z|1"))
    }
}

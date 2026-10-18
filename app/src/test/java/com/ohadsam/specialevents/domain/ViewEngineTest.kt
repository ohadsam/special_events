package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.views.EventJump
import com.ohadsam.specialevents.domain.views.GroupBy
import com.ohadsam.specialevents.domain.views.GroupKey
import com.ohadsam.specialevents.domain.views.SortBy
import com.ohadsam.specialevents.domain.views.ViewEngine
import com.ohadsam.specialevents.domain.views.ViewFilter
import com.ohadsam.specialevents.domain.views.ViewSpec
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewEngineTest {
    private val today = LocalDate.of(2026, 10, 17)

    private fun ev(id: Long, title: String, type: EventType, year: Int?, month: Int, day: Int, tags: List<String> = emptyList()) = SpecialEvent(
        id = id,
        type = type,
        title = title,
        origin = OriginDate(CalendarBasis.GREGORIAN, year, month, day),
        tags = tags.map { Tag(name = it) },
        updatedAt = id * 10,
    )

    private val events = listOf(
        ev(1, "דנה לוי", EventType.BIRTHDAY, 1976, 10, 17, listOf("משפחה")), // turns 50 today: jubilee
        ev(2, "אבי ושרה", EventType.ANNIVERSARY, 2001, 10, 22, listOf("משפחה", "חברים")),
        ev(3, "יוסי", EventType.BIRTHDAY, 2010, 11, 20),
        ev(4, "אזכרה לסבא", EventType.YAHRZEIT, null, 12, 30),
    )
    private val items = EventTimeline.upcoming(events, today, inIsrael = true)

    private fun titles(spec: ViewSpec) = ViewEngine.apply(items, spec).flatMap { g -> g.items.map { it.event.title } }

    @Test
    fun filtersByTypeTagTextAndRange() {
        assertEquals(listOf("דנה לוי", "יוסי"), titles(ViewSpec(ViewFilter(types = setOf(EventType.BIRTHDAY)))))
        assertEquals(listOf("דנה לוי", "אבי ושרה"), titles(ViewSpec(ViewFilter(tags = setOf("משפחה")))))
        assertEquals(listOf("אבי ושרה"), titles(ViewSpec(ViewFilter(tags = setOf("חברים"), query = "שרה"))))
        assertEquals(listOf("אבי ושרה", "יוסי"), titles(ViewSpec(ViewFilter(from = LocalDate.of(2026, 10, 18), to = LocalDate.of(2026, 11, 30)))))
        assertTrue(ViewFilter().isActive.not() && ViewFilter(milestonesOnly = true).isActive)
    }

    @Test
    fun milestonesOnlyKeepsRoundNumbers() {
        val kept = titles(ViewSpec(ViewFilter(milestonesOnly = true)))
        assertTrue("דנה לוי" in kept)
        assertFalse("אזכרה לסבא" in kept)
    }

    @Test
    fun sortsByNameAndDirection() {
        assertEquals(listOf("אבי ושרה", "אזכרה לסבא", "דנה לוי", "יוסי"), titles(ViewSpec(sortBy = SortBy.NAME, groupBy = GroupBy.NONE)))
        assertEquals(listOf("יוסי", "דנה לוי", "אזכרה לסבא", "אבי ושרה"), titles(ViewSpec(sortBy = SortBy.NAME, ascending = false, groupBy = GroupBy.NONE)))
        assertEquals(listOf("אזכרה לסבא", "יוסי", "אבי ושרה", "דנה לוי"), titles(ViewSpec(sortBy = SortBy.UPDATED, ascending = false, groupBy = GroupBy.NONE)))
    }

    @Test
    fun numberSortKeepsMissingValuesLastInBothDirections() {
        val up = titles(ViewSpec(sortBy = SortBy.NUMBER, groupBy = GroupBy.NONE))
        val down = titles(ViewSpec(sortBy = SortBy.NUMBER, ascending = false, groupBy = GroupBy.NONE))
        assertEquals("אזכרה לסבא", up.last())
        assertEquals("אזכרה לסבא", down.last())
    }

    @Test
    fun groupsByMonthWithTimeOrderBothWays() {
        val asc = ViewEngine.apply(items, ViewSpec(groupBy = GroupBy.MONTH)).map { it.key }
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2026, 12)).map { GroupKey.Month(it) }, asc)
        assertEquals(asc.reversed(), ViewEngine.apply(items, ViewSpec(groupBy = GroupBy.MONTH, ascending = false)).map { it.key })
    }

    @Test
    fun tagGroupingPutsMultiTaggedInEachAndUntaggedLast() {
        val keys = ViewEngine.apply(items, ViewSpec(groupBy = GroupBy.TAG))
        assertEquals(listOf("חברים", "משפחה", null), keys.map { (it.key as GroupKey.Tag).name })
        assertEquals(2, keys.first { (it.key as GroupKey.Tag).name == "משפחה" }.items.size)
        assertEquals(2, keys.last().items.size)
    }

    @Test
    fun groupsByTypeAndAgeDecade() {
        assertEquals(listOf(EventType.BIRTHDAY, EventType.ANNIVERSARY, EventType.YAHRZEIT), ViewEngine.apply(items, ViewSpec(groupBy = GroupBy.TYPE)).map { (it.key as GroupKey.Type).type })
        val decades = ViewEngine.apply(items.filter { it.event.type == EventType.BIRTHDAY }, ViewSpec(groupBy = GroupBy.AGE_DECADE)).map { (it.key as GroupKey.AgeDecade).from }
        assertEquals(listOf(10, 50), decades)
    }

    @Test
    fun proximityGroupsMatchTheUpcomingList() {
        val keys = ViewEngine.apply(items, ViewSpec()).map { (it.key as GroupKey.Proximity).group }
        assertEquals(listOf(UpcomingGroup.TODAY, UpcomingGroup.THIS_WEEK, UpcomingGroup.LATER), keys)
    }

    @Test
    fun jumpFindsEventsByNameAndTheirNextOccurrence() {
        assertEquals(listOf("דנה לוי"), EventJump.matches(events, "דָּנָה").map { it.title })
        assertTrue(EventJump.matches(events, "   ").isEmpty())
        val next = EventJump.next(events[0], today, com.ohadsam.specialevents.calendar.DateRules())
        assertEquals(today, next?.date)
        assertEquals(today, EventJump.locate(next!!).date)
        assertNull(EventJump.next(events[0].copy(recurrence = Recurrence.ONCE), today.plusDays(1), com.ohadsam.specialevents.calendar.DateRules()))
    }

    @Test
    fun filtersAndGroupsByOwnerGroup() {
        val membership = mapOf(1L to setOf(10L), 2L to setOf(10L, 20L), 3L to setOf(20L))
        val onlyTen = ViewEngine.apply(items, ViewSpec(ViewFilter(groups = setOf(10L))), membership = membership).flatMap { g -> g.items.map { it.event.title } }
        assertEquals(listOf("דנה לוי", "אבי ושרה"), onlyTen)
        val grouped = ViewEngine.apply(items, ViewSpec(groupBy = GroupBy.GROUP), membership = membership, groupOrder = listOf(20L, 10L))
        assertEquals(listOf<Long?>(20L, 10L, null), grouped.map { (it.key as GroupKey.Group).id })
        assertEquals(2, grouped.first().items.size)
        assertEquals(1, grouped.last().items.size)
        assertTrue(ViewFilter(groups = setOf(1L)).isActive)
    }
}

package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.domain.checklist.Checklist
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.checklist.ChecklistKind
import com.ohadsam.specialevents.domain.checklist.ChecklistTemplates
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistTest {
    private val occurrence = LocalDate.of(2026, 11, 10)

    private fun task(title: String, daysBefore: Int? = null, done: Boolean = false, order: Int = 0) =
        ChecklistItem(id = order.toLong() + 1, title = title, daysBefore = daysBefore, done = done, sortOrder = order)

    private fun gift(amount: Int?, done: Boolean) = ChecklistItem(kind = ChecklistKind.GIFT, title = "מתנה", amount = amount, done = done)

    @Test
    fun progressCountsDoneItemsAndHandlesEmpty() {
        assertEquals(0f, Checklist.progress(emptyList()).fraction, 0f)
        assertFalse(Checklist.progress(emptyList()).complete)
        val p = Checklist.progress(listOf(task("א", done = true), task("ב"), task("ג", done = true), task("ד")))
        assertEquals(2, p.done)
        assertEquals(0.5f, p.fraction, 0f)
        assertTrue(Checklist.progress(listOf(task("א", done = true))).complete)
    }

    @Test
    fun giftSummarySplitsBudgetFromSpent() {
        val s = Checklist.gifts(listOf(gift(200, done = true), gift(150, done = false), gift(null, done = false), task("לא מתנה")))
        assertEquals(3, s.count)
        assertEquals(1, s.bought)
        assertEquals(200, s.spent)
        assertEquals(150, s.budget)
    }

    @Test
    fun dueDateCountsBackFromTheOccurrence() {
        assertEquals(LocalDate.of(2026, 11, 3), Checklist.dueDate(task("א", 7), occurrence))
        assertEquals(occurrence, Checklist.dueDate(task("ב", 0), occurrence))
        assertEquals(LocalDate.of(2026, 11, 11), Checklist.dueDate(task("מעקב", -1), occurrence))
        assertNull(Checklist.dueDate(task("ג"), occurrence))
    }

    @Test
    fun overdueOnlyWhenOpenDatedAndPastDue() {
        val item = task("א", 7)
        assertFalse(Checklist.isOverdue(item, occurrence, LocalDate.of(2026, 11, 3)))
        assertTrue(Checklist.isOverdue(item, occurrence, LocalDate.of(2026, 11, 4)))
        assertFalse(Checklist.isOverdue(item.copy(done = true), occurrence, LocalDate.of(2026, 12, 1)))
        assertFalse(Checklist.isOverdue(task("ללא תאריך"), occurrence, LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun orderedPutsOpenFirstEarliestDueFirstAndDoneLast() {
        val items = listOf(task("בוצע", 20, done = true, order = 0), task("בלי תאריך", null, order = 1), task("קרוב", 1, order = 2), task("רחוק", 14, order = 3))
        assertEquals(listOf("רחוק", "קרוב", "בלי תאריך", "בוצע"), Checklist.ordered(items).map { it.title })
    }

    @Test
    fun everyEventTypeHasATemplateWithTitles() {
        EventType.entries.forEach { type ->
            val list = ChecklistTemplates.forType(type)
            assertTrue(type.name, list.isNotEmpty() && list.all { it.title.isNotBlank() })
        }
        assertTrue(ChecklistTemplates.forType(EventType.BIRTHDAY).any { it.kind == ChecklistKind.GIFT })
    }
}

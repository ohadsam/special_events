package com.ohadsam.specialevents.domain.checklist

import java.time.LocalDate

/** A preparation step ([TASK]) or a present to buy ([GIFT]) for an event. */
enum class ChecklistKind { TASK, GIFT }

/**
 * [daysBefore] is the due date relative to the occurrence (null = no due date). [amount] is the budget of a gift in shekels, or what it
 * cost once [done] (bought). The same list serves every year of a yearly event; "start over" clears the marks.
 */
data class ChecklistItem(
    val id: Long = 0,
    val eventId: Long = 0,
    val kind: ChecklistKind = ChecklistKind.TASK,
    val title: String,
    val note: String? = null,
    val amount: Int? = null,
    val daysBefore: Int? = null,
    val done: Boolean = false,
    val sortOrder: Int = 0,
)

data class ChecklistProgress(val done: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
    val complete: Boolean get() = total > 0 && done == total
}

/** Money side of the gifts: everything planned, and what was actually bought. */
data class GiftSummary(val count: Int, val bought: Int, val budget: Int, val spent: Int)

object Checklist {
    fun progress(items: List<ChecklistItem>) = ChecklistProgress(items.count { it.done }, items.size)

    fun gifts(items: List<ChecklistItem>): GiftSummary {
        val gifts = items.filter { it.kind == ChecklistKind.GIFT }
        return GiftSummary(
            count = gifts.size,
            bought = gifts.count { it.done },
            budget = gifts.filter { !it.done }.sumOf { it.amount ?: 0 },
            spent = gifts.filter { it.done }.sumOf { it.amount ?: 0 },
        )
    }

    fun dueDate(item: ChecklistItem, occurrence: LocalDate): LocalDate? = item.daysBefore?.let { occurrence.minusDays(it.toLong()) }

    /** Not done and the due day has passed (the occurrence itself is the last chance, so after it everything open is overdue). */
    fun isOverdue(item: ChecklistItem, occurrence: LocalDate, today: LocalDate): Boolean {
        if (item.done) return false
        val due = dueDate(item, occurrence) ?: return false
        return today.isAfter(due)
    }

    /** Open items first (earliest due first, undated last), done items at the bottom, each group in the user's order. */
    fun ordered(items: List<ChecklistItem>): List<ChecklistItem> =
        items.sortedWith(compareBy<ChecklistItem> { it.done }.thenBy { it.daysBefore?.let { d -> -d } ?: Int.MAX_VALUE }.thenBy { it.sortOrder }.thenBy { it.id })
}

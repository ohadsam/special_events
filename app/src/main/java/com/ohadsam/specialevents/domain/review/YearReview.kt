package com.ohadsam.specialevents.domain.review

import com.ohadsam.specialevents.calendar.DateRules
import com.ohadsam.specialevents.data.GreetingSent
import com.ohadsam.specialevents.data.SendChannel
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.ItemKind
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.checklist.Checklist
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.checklist.GiftSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One person (or event title) and how many greetings were sent for them in the year. */
data class PersonCount(val name: String, val count: Int)

/** Numbers behind the "year in review" screen. Everything is derived, nothing is stored. */
data class YearReview(
    val year: Int,
    val occurrences: Int,
    val passed: Int,
    val greeted: Int,
    val byType: List<Pair<EventType, Int>>,
    val busiestMonth: Int?,
    val busiestMonthCount: Int,
    val greetingsSent: Int,
    val byChannel: List<Pair<SendChannel, Int>>,
    val topPeople: List<PersonCount>,
    val gifts: GiftSummary,
) {
    /** Share of the already-passed occurrences that were greeted, 0-100; null when none has passed yet. */
    val coveragePercent: Int? get() = if (passed == 0) null else greeted * PERCENT / passed

    val isEmpty: Boolean get() = occurrences == 0 && greetingsSent == 0

    private companion object {
        const val PERCENT = 100
    }
}

object YearReviewCalculator {
    private const val TOP_PEOPLE = 3

    fun compute(
        year: Int,
        events: List<SpecialEvent>,
        sent: List<GreetingSent>,
        checklist: List<ChecklistItem>,
        today: LocalDate,
        inIsrael: Boolean,
        rules: DateRules = DateRules(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): YearReview {
        val active = events.filter { !it.archived }
        val items = EventTimeline.between(active, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), today, inIsrael, rules)
        val occurrences = items.filter { it.kind == ItemKind.OCCURRENCE }
        val greetedKeys = sent.map { it.eventId to it.occurrenceDay }.toSet()
        val passedItems = occurrences.filter { !it.date.isAfter(today) }
        val sentInYear = sent.filter { Instant.ofEpochMilli(it.sentAt).atZone(zone).year == year }
        val byMonth = occurrences.groupingBy { it.date.monthValue }.eachCount()
        val busiest = byMonth.maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })
        val names = active.associate { it.id to (it.personName?.takeIf { n -> n.isNotBlank() } ?: it.title) }
        val activeIds = active.map { it.id }.toSet()
        return YearReview(
            year = year,
            occurrences = occurrences.size,
            passed = passedItems.size,
            greeted = passedItems.count { (it.event.id to it.date.toEpochDay()) in greetedKeys },
            byType = occurrences.groupingBy { it.event.type }.eachCount().toList().sortedWith(compareByDescending<Pair<EventType, Int>> { it.second }.thenBy { it.first.ordinal }),
            busiestMonth = busiest?.key,
            busiestMonthCount = busiest?.value ?: 0,
            greetingsSent = sentInYear.size,
            byChannel = sentInYear.groupingBy { it.channel }.eachCount().toList().sortedWith(compareByDescending<Pair<SendChannel, Int>> { it.second }.thenBy { it.first.ordinal }),
            topPeople = sentInYear.filter { it.eventId in activeIds }.groupingBy { names.getValue(it.eventId) }.eachCount()
                .map { PersonCount(it.key, it.value) }.sortedWith(compareByDescending<PersonCount> { it.count }.thenBy { it.name }).take(TOP_PEOPLE),
            gifts = Checklist.gifts(checklist.filter { it.eventId in activeIds }),
        )
    }
}

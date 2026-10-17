package com.ohadsam.specialevents.domain.reminders

import com.ohadsam.specialevents.calendar.HolyTimes
import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.domain.ItemKind
import com.ohadsam.specialevents.domain.UpcomingItem
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Shift { NONE, EARLIER, LATER }

/** A reminder resolved to a moment in time. [shift] tells whether it was moved because of Shabbat / Yom Tov. */
data class PlannedReminder(val rule: ReminderRule, val item: UpcomingItem, val fireAt: Instant, val shift: Shift)

/** Turns occurrences + rules into exact moments, moving or dropping those that would fall on Shabbat / Yom Tov. Pure; time zone and zmanim are inputs. */
object ReminderPlanner {
    /** Never fire closer than this to candle lighting / havdalah. */
    val HOLY_BUFFER: Duration = Duration.ofMinutes(30)

    fun plan(items: List<UpcomingItem>, rules: List<ReminderRule>, zone: ZoneId, holy: HolyTimes?): List<PlannedReminder> {
        val byEvent = rules.filter { it.enabled && it.channel == Channel.NOTIFICATION }.groupBy { it.eventId }
        val out = ArrayList<PlannedReminder>()
        for (item in items) {
            if (item.event.archived) continue
            val tier = tierOf(item)
            for (rule in byEvent[item.event.id].orEmpty()) {
                if (tier < rule.minTier) continue
                for ((at, shift) in withHolyRule(baseInstant(item.date, rule, zone), rule.shabbatPolicy, holy)) {
                    out.add(PlannedReminder(rule, item, at, shift))
                }
            }
        }
        return out.sortedWith(compareBy<PlannedReminder> { it.fireAt }.thenBy { it.rule.id })
    }

    /** Bar/bat mitzvah days always count as major milestones. */
    fun tierOf(item: UpcomingItem): Tier = if (item.kind == ItemKind.MITZVAH) Tier.MAJOR else item.milestone?.tier ?: Tier.NONE

    fun baseDate(occurrence: LocalDate, rule: ReminderRule): LocalDate = when (rule.offsetUnit) {
        OffsetUnit.DAYS -> occurrence.minusDays(rule.offsetValue.toLong())
        OffsetUnit.WEEKS -> occurrence.minusWeeks(rule.offsetValue.toLong())
        OffsetUnit.MONTHS -> occurrence.minusMonths(rule.offsetValue.toLong())
    }

    fun baseInstant(occurrence: LocalDate, rule: ReminderRule, zone: ZoneId): Instant =
        baseDate(occurrence, rule).atTime(rule.timeMinutes / MINUTES_IN_HOUR, rule.timeMinutes % MINUTES_IN_HOUR).atZone(zone).toInstant()

    /** The moment(s) to actually fire for a reminder that wanted [at]. */
    fun withHolyRule(at: Instant, policy: ShabbatPolicy, holy: HolyTimes?): List<Pair<Instant, Shift>> {
        val period = holy?.periodNear(at, HOLY_BUFFER) ?: return listOf(at to Shift.NONE)
        val before = period.start.minus(HOLY_BUFFER) to Shift.EARLIER
        val after = period.end.plus(HOLY_BUFFER) to Shift.LATER
        return when (policy) {
            ShabbatPolicy.BEFORE -> listOf(before)
            ShabbatPolicy.AFTER -> listOf(after)
            ShabbatPolicy.BOTH -> listOf(before, after)
            ShabbatPolicy.SKIP -> emptyList()
        }
    }

    private const val MINUTES_IN_HOUR = 60
}

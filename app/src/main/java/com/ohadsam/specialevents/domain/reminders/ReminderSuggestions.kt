package com.ohadsam.specialevents.domain.reminders

import com.ohadsam.specialevents.calendar.Holidays
import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import java.time.LocalDate

/** A reminder worth adding, with the reason shown to the user ([id] is stable so a dismissed suggestion can be remembered). */
data class Suggestion(val id: String, val reason: String, val rule: ReminderRule)

/**
 * Smart, offline suggestions for one event, based on its type, its next occurrence (round birthday? on Shabbat?) and what it
 * already has. Only gaps are suggested; accepting one just adds an ordinary rule.
 */
object ReminderSuggestions {
    private const val DAYS_IN_WEEK = 7
    private const val DAYS_IN_MONTH = 30
    private const val MORNING = 8 * 60
    private const val MID_MORNING = 9 * 60
    private const val TEN = 10 * 60
    private const val AFTERNOON = 16 * 60
    private const val PREP_MIN_DAYS = 3
    private const val MILESTONE_MIN_DAYS = 28
    private const val MITZVAH_SOON_DAYS = 3 * 366L
    private const val MITZVAH_MIN_DAYS = 85

    fun forEvent(event: SpecialEvent, rules: List<ReminderRule>, today: LocalDate, settings: AppSettings): List<Suggestion> {
        val active = rules.filter { it.enabled && it.channel == Channel.NOTIFICATION }
        val next = EventTimeline.forEvent(event, today, 1, settings.dateRules).firstOrNull()
        val out = ArrayList<Suggestion>()

        fun suggest(id: String, reason: String, value: Int, unit: OffsetUnit, time: Int, tier: Tier = Tier.NONE) {
            out.add(Suggestion(id, reason, ReminderRule(eventId = event.id, offsetValue = value, offsetUnit = unit, timeMinutes = time, minTier = tier, origin = RuleOrigin.SUGGESTED)))
        }

        if (active.none { daysBefore(it) == 0 }) {
            suggest("on-the-day", "אין תזכורת ביום האירוע עצמו", 0, OffsetUnit.DAYS, MORNING)
        }
        if (event.type == EventType.BIRTHDAY || event.type == EventType.ANNIVERSARY) {
            if (active.none { daysBefore(it) >= PREP_MIN_DAYS && it.minTier == Tier.NONE }) {
                suggest("week-before", "תזכורת שבוע מראש מאפשרת להכין מתנה וברכה", 1, OffsetUnit.WEEKS, TEN)
            }
        }
        if (event.type == EventType.YAHRZEIT && active.none { daysBefore(it) == 1 }) {
            suggest("yahrzeit-eve", "להדליק נר נשמה בערב שלפני — לפני השקיעה", 1, OffsetUnit.DAYS, AFTERNOON)
        }
        if (next?.milestone?.tier == Tier.MAJOR && active.none { daysBefore(it) >= MILESTONE_MIN_DAYS && it.minTier <= Tier.MAJOR }) {
            suggest("milestone-month", "יום עגול מתקרב — כדאי להיערך חודש מראש", 1, OffsetUnit.MONTHS, MID_MORNING, Tier.MAJOR)
        }
        EventTimeline.mitzvahInfo(event, settings.inIsrael, settings.dateRules)?.let { m ->
            val daysToMitzvah = java.time.temporal.ChronoUnit.DAYS.between(today, m.date)
            if (daysToMitzvah in 1..MITZVAH_SOON_DAYS && active.none { daysBefore(it) >= MITZVAH_MIN_DAYS && it.minTier <= Tier.MAJOR }) {
                suggest("mitzvah-prep", "בר/בת מצווה מתקרבים — הכנות לוקחות זמן", 3, OffsetUnit.MONTHS, MID_MORNING, Tier.MAJOR)
            }
        }
        if (next != null && (next.date.dayOfWeek == java.time.DayOfWeek.SATURDAY || Holidays.isYomTov(next.date, settings.inIsrael)) && active.none { daysBefore(it) == 1 && it.timeMinutes <= MID_MORNING }) {
            suggest("erev-holy", "האירוע הקרוב חל בשבת או ביום טוב — תזכורת בערב שלפני, מוקדם יותר", 1, OffsetUnit.DAYS, MID_MORNING)
        }
        return out
    }

    /** Approximate whole days between the reminder and the event (months count as 30). */
    fun daysBefore(rule: ReminderRule): Int = when (rule.offsetUnit) {
        OffsetUnit.DAYS -> rule.offsetValue
        OffsetUnit.WEEKS -> rule.offsetValue * DAYS_IN_WEEK
        OffsetUnit.MONTHS -> rule.offsetValue * DAYS_IN_MONTH
    }
}

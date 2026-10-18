package com.ohadsam.specialevents.domain.reminders

import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.domain.EventType

/** The reminders a new event starts with, by type (SPEC S5). They are ordinary rules the user can change or delete. */
object ReminderDefaults {
    private const val MORNING = 8 * 60
    private const val MID_MORNING = 9 * 60
    private const val AFTERNOON = 16 * 60
    private const val EARLY_EVENING = 18 * 60
    private const val EVENING = 20 * 60

    private fun rule(value: Int, unit: OffsetUnit, time: Int, tier: Tier = Tier.NONE) =
        ReminderRule(offsetValue = value, offsetUnit = unit, timeMinutes = time, minTier = tier, origin = RuleOrigin.DEFAULT)

    fun forType(type: EventType): List<ReminderRule> = when (type) {
        EventType.BIRTHDAY, EventType.ANNIVERSARY -> listOf(
            rule(0, OffsetUnit.DAYS, MORNING),
            rule(1, OffsetUnit.DAYS, EVENING),
            rule(7, OffsetUnit.DAYS, MID_MORNING, Tier.NOTABLE),
            rule(30, OffsetUnit.DAYS, MID_MORNING, Tier.MAJOR),
        )
        // The memorial candle is lit on the eve: a reminder the afternoon before (before sunset).
        EventType.YAHRZEIT -> listOf(
            rule(0, OffsetUnit.DAYS, MORNING),
            rule(1, OffsetUnit.DAYS, AFTERNOON),
        )
        EventType.BAR_BAT_MITZVAH -> listOf(
            rule(0, OffsetUnit.DAYS, MORNING),
            rule(1, OffsetUnit.WEEKS, MID_MORNING),
            rule(1, OffsetUnit.MONTHS, MID_MORNING),
            rule(3, OffsetUnit.MONTHS, MID_MORNING),
        )
        EventType.WORK, EventType.FAMILY, EventType.CUSTOM -> listOf(
            rule(0, OffsetUnit.DAYS, MORNING),
            rule(1, OffsetUnit.DAYS, EARLY_EVENING),
        )
    }
}

/** Short Hebrew description of a rule for lists ("יום לפני ב-20:00 · רק באבני דרך גדולות"). */
object ReminderLabels {
    fun describe(rule: ReminderRule): String {
        val time = "%02d:%02d".format(rule.timeMinutes / MINUTES_IN_HOUR, rule.timeMinutes % MINUTES_IN_HOUR)
        val tier = when (rule.minTier) {
            Tier.NONE -> ""
            Tier.NOTABLE -> " · רק באבני דרך"
            Tier.MAJOR -> " · רק באבני דרך גדולות"
        }
        val moment = when (rule.timeMode) {
            TimeMode.FIXED -> "ב-$time"
            TimeMode.NIGHTFALL -> "בצאת הכוכבים"
            TimeMode.MIDNIGHT -> "אחרי חצות"
        }
        val channel = when (rule.channel) {
            Channel.NOTIFICATION -> ""
            Channel.WHATSAPP -> " · ברכה בוואטסאפ"
            Channel.EMAIL -> " · ברכה במייל"
            Channel.SMS -> " · ברכה בהודעה"
        }
        return "${offsetText(rule.offsetValue, rule.offsetUnit)} $moment$tier$channel"
    }

    fun offsetText(value: Int, unit: OffsetUnit): String = when {
        value == 0 -> "ביום האירוע"
        unit == OffsetUnit.DAYS -> when (value) {
            1 -> "יום לפני"
            2 -> "יומיים לפני"
            else -> "$value ימים לפני"
        }
        unit == OffsetUnit.WEEKS -> when (value) {
            1 -> "שבוע לפני"
            2 -> "שבועיים לפני"
            else -> "$value שבועות לפני"
        }
        else -> when (value) {
            1 -> "חודש לפני"
            2 -> "חודשיים לפני"
            else -> "$value חודשים לפני"
        }
    }

    private const val MINUTES_IN_HOUR = 60
}

package com.ohadsam.specialevents.domain.gcal

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.calendar.hasPossibleDay
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent

/** What the tag in a calendar entry says about the event it came from. */
data class GcalTagData(val type: EventType, val basis: CalendarBasis, val year: Int?, val month: Int, val day: Int, val yearly: Boolean) {
    val origin: OriginDate get() = OriginDate(basis, year, month, day)
}

/**
 * The mark the app leaves in every calendar entry it writes, as the last line of the description: `[SE1:BIRTHDAY:G:1980:10:17:Y]`
 * (type, G/H = Gregorian/Hebrew, year or `-`, month, day, Y/O = yearly/one-time). Google keeps descriptions when it syncs, so the mark
 * survives on other devices and lets an import rebuild the event exactly instead of guessing it from the title.
 */
object GcalTag {
    private val TAG = Regex("""\[SE1:([A-Z_]+):([GH]):(-|\d{1,4}):(\d{1,2}):(\d{1,2}):([YO])]""")
    private const val MAX_MONTH_HEBREW = 13
    private const val MAX_MONTH_GREGORIAN = 12

    fun line(event: SpecialEvent): String {
        val o = event.origin
        val basis = if (o.basis == CalendarBasis.HEBREW) "H" else "G"
        val yearly = if (event.recurrence == Recurrence.YEARLY) "Y" else "O"
        return "[SE1:${event.type.name}:$basis:${o.year ?: "-"}:${o.month}:${o.day}:$yearly]"
    }

    /** The whole description: a human line (so people see where the entry came from) and the mark. */
    fun description(event: SpecialEvent, headline: String): String = headline + "\n" + line(event)

    /** Null when the text carries no valid mark (an entry of someone else's, or a mark that was damaged). */
    fun parse(description: String?): GcalTagData? {
        val m = description?.let(TAG::find) ?: return null
        val type = EventType.entries.firstOrNull { it.name == m.groupValues[1] } ?: return null
        val basis = if (m.groupValues[2] == "H") CalendarBasis.HEBREW else CalendarBasis.GREGORIAN
        val year = m.groupValues[3].toIntOrNull()
        val month = m.groupValues[4].toInt()
        val day = m.groupValues[5].toInt()
        val maxMonth = if (basis == CalendarBasis.HEBREW) MAX_MONTH_HEBREW else MAX_MONTH_GREGORIAN
        if (month !in 1..maxMonth || day !in 1..MAX_DAY) return null
        val data = GcalTagData(type, basis, year, month, day, m.groupValues[6] == "Y")
        return data.takeIf { data.origin.hasPossibleDay() }
    }

    /** The description without the mark and the headline line, for use as the event's notes. */
    fun userText(description: String?, headline: String): String? =
        description?.replace(TAG, "")?.lineSequence()?.filter { !it.trim().startsWith(headline.trim()) }?.joinToString("\n")?.trim()?.takeIf { it.isNotEmpty() }

    private const val MAX_DAY = 31
}

package com.ohadsam.specialevents.domain.groups

import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.ReminderDefaults
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.reminders.RuleOrigin

/**
 * A circle of people or occasions (family, friends, work...). It carries what its members share: look ([emoji], [colorArgb]), tags to
 * give them, a reminder template, a default greeting and channel, and extra recipients beyond the members themselves.
 */
data class EventGroup(
    val id: Long = 0,
    val name: String,
    val emoji: String? = null,
    val colorArgb: Int? = null,
    /** Set for the groups the app ships with; lets a restore recognise them under any name. */
    val builtInKey: String? = null,
    val sortOrder: Int = 0,
    val tags: List<String> = emptyList(),
    val defaultChannel: Channel? = null,
    /** Greeting template (built-in negative / custom positive id) used when a rule names none. */
    val defaultTemplateId: Long? = null,
    val reminderRules: List<ReminderRule> = emptyList(),
    /** Phone numbers or e-mail addresses that get the group's greetings besides the members. */
    val extraRecipients: List<String> = emptyList(),
)

/** One address a group greeting can go to. [label] is the member's name, or the raw address for extras. */
data class Recipient(val label: String, val phone: String? = null, val email: String? = null)

object GroupDefaults {
    private const val FAMILY_COLOR = 0xFFE57373.toInt()
    private const val FRIENDS_COLOR = 0xFF4DB6AC.toInt()
    private const val WORK_COLOR = 0xFF64B5F6.toInt()
    private const val PARTNER_COLOR = 0xFFF06292.toInt()
    private const val CHILDREN_COLOR = 0xFFFFB74D.toInt()
    private const val PARENTS_COLOR = 0xFFBA68C8.toInt()

    /** The starting groups; each begins with the reminders that fit its usual events and can be changed freely. */
    val BUILT_IN: List<EventGroup> = listOf(
        builtIn("family", "משפחה", "👪", FAMILY_COLOR, EventType.BIRTHDAY),
        builtIn("friends", "חברים", "🤝", FRIENDS_COLOR, EventType.BIRTHDAY),
        builtIn("work", "עבודה", "💼", WORK_COLOR, EventType.WORK),
        builtIn("partner", "בן/בת זוג", "💑", PARTNER_COLOR, EventType.ANNIVERSARY),
        builtIn("children", "ילדים", "🧒", CHILDREN_COLOR, EventType.BIRTHDAY),
        builtIn("parents", "הורים", "👵", PARENTS_COLOR, EventType.BIRTHDAY),
    )

    private fun builtIn(key: String, name: String, emoji: String, color: Int, basedOn: EventType) = EventGroup(
        name = name,
        emoji = emoji,
        colorArgb = color,
        builtInKey = key,
        sortOrder = 0,
        reminderRules = ReminderDefaults.forType(basedOn),
    )
}

object GroupLogic {
    /**
     * Whether a group's reminder template may replace an event's rules: only when none were changed by hand (a rule the user edited
     * is [RuleOrigin.USER] and is never overwritten).
     */
    fun mayApplyTemplate(currentRules: List<ReminderRule>): Boolean = currentRules.none { it.origin == RuleOrigin.USER }

    /** The group's rules as fresh, unattached rules ready to be saved for [eventId]. */
    fun templateFor(group: EventGroup, eventId: Long): List<ReminderRule> =
        group.reminderRules.map { it.copy(id = 0, eventId = eventId, origin = RuleOrigin.DEFAULT) }

    /** Which group's template an event gets when it belongs to several: the first by the user's order that has one. */
    fun groupWithTemplate(groups: List<EventGroup>): EventGroup? = groups.sortedBy { it.sortOrder }.firstOrNull { it.reminderRules.isNotEmpty() }

    /** A rule's own template wins; otherwise the first group that names one. */
    fun greetingTemplateId(ruleTemplateId: Long?, groups: List<EventGroup>): Long? =
        ruleTemplateId ?: groups.sortedBy { it.sortOrder }.firstNotNullOfOrNull { it.defaultTemplateId }

    /** Members with a phone or address, then the extras; the same number or address is listed once. */
    fun recipients(group: EventGroup, members: List<SpecialEvent>): List<Recipient> {
        val out = ArrayList<Recipient>()
        val seen = HashSet<String>()
        for (m in members.sortedBy { it.title }) {
            val phone = m.contactPhone?.filter { it.isDigit() || it == '+' }?.takeIf { it.count(Char::isDigit) >= MIN_DIGITS }
            val email = m.contactEmail?.trim()?.lowercase()?.takeIf { it.contains('@') }
            val key = phone ?: email ?: continue
            if (seen.add(key)) out += Recipient(m.personName ?: m.title, phone, email)
        }
        for (raw in group.extraRecipients) {
            val text = raw.trim()
            if (text.isEmpty()) continue
            if (text.contains('@')) {
                if (seen.add(text.lowercase())) out += Recipient(text, email = text)
            } else {
                val digits = text.filter { it.isDigit() || it == '+' }
                if (digits.count(Char::isDigit) >= MIN_DIGITS && seen.add(digits)) out += Recipient(text, phone = digits)
            }
        }
        return out
    }

    private const val MIN_DIGITS = 7
}

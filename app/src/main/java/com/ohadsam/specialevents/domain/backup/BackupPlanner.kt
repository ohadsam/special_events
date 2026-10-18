package com.ohadsam.specialevents.domain.backup

import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate

/** Decides what is new and what is already in the app when a backup is imported (pure, so it is testable). */
object BackupPlanner {
    /** Same kind of event with the same title on the same date counts as the same event. */
    fun key(e: SpecialEvent): String = listOf(e.type.name, TextSearch.normalize(e.title), e.origin.basis.name, e.origin.year, e.origin.month, e.origin.day).joinToString("|")

    fun templateKey(t: GreetingTemplate): String = TextSearch.normalize(t.name) + "|" + t.body.trim()

    fun preview(existing: List<SpecialEvent>, existingTemplates: List<GreetingTemplate>, incoming: BackupData, mode: ImportMode): ImportPreview {
        val known = if (mode == ImportMode.REPLACE) emptySet() else existing.map(::key).toSet()
        val seen = HashSet<String>()
        val fresh = incoming.events.count { seen.add(key(it)) && key(it) !in known }
        val knownTemplates = existingTemplates.map(::templateKey).toSet()
        return ImportPreview(
            newEvents = fresh,
            duplicateEvents = incoming.events.size - fresh,
            newTemplates = incoming.templates.map(::templateKey).distinct().count { it !in knownTemplates },
            existingEvents = existing.size,
        )
    }

    /** The part of [data] that belongs to the chosen events: their reminders, checklists and group memberships, and the groups still in use. */
    fun restrict(data: BackupData, eventIds: Set<Long>): BackupData {
        val members = data.members.filter { (e, _) -> e in eventIds }
        val usedGroups = members.map { it.second }.toSet()
        return data.copy(
            events = data.events.filter { it.id in eventIds },
            rules = data.rules.filter { it.eventId in eventIds },
            checklist = data.checklist.filter { it.eventId in eventIds },
            members = members,
            groups = data.groups.filter { it.id in usedGroups },
        )
    }

    /** Events a restore would add rather than duplicate (every event, when replacing). */
    fun defaultSelection(existing: List<SpecialEvent>, incoming: List<SpecialEvent>, mode: ImportMode): Set<Long> {
        if (mode == ImportMode.REPLACE) return incoming.map { it.id }.toSet()
        val known = existing.map(::key).toHashSet()
        return incoming.filter { known.add(key(it)) }.map { it.id }.toSet()
    }
}

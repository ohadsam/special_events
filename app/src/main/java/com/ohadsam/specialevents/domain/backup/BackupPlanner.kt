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
}

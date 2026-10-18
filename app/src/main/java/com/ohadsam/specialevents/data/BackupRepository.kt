package com.ohadsam.specialevents.data

import androidx.room.withTransaction
import com.ohadsam.specialevents.contacts.PhotoStore
import com.ohadsam.specialevents.data.db.AppDatabase
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.backup.BackupData
import com.ohadsam.specialevents.domain.backup.BackupPlanner
import com.ohadsam.specialevents.domain.backup.ImportMode
import com.ohadsam.specialevents.domain.backup.ImportPreview
import kotlinx.coroutines.flow.first

/** Reads everything for a backup and writes an imported backup back, all-or-nothing. */
class BackupRepository(
    private val db: AppDatabase,
    private val events: EventRepository,
    private val reminders: ReminderRepository,
    private val checklists: ChecklistRepository,
    private val greetings: GreetingRepository,
    private val groups: GroupRepository,
) {
    suspend fun snapshot(): BackupData = BackupData(
        events = events.allEvents.first(),
        rules = reminders.snapshot(),
        checklist = checklists.snapshot(),
        templates = greetings.customTemplates.first(),
        groups = groups.snapshot().first,
        members = groups.snapshot().second.map { it.eventId to it.groupId },
    )

    suspend fun preview(incoming: BackupData, mode: ImportMode): ImportPreview =
        BackupPlanner.preview(events.allEvents.first(), greetings.customTemplates.first(), incoming, mode)

    /** Returns how many events were added. In [ImportMode.REPLACE] the current events (and their reminders and checklists) are removed first. */
    suspend fun import(incoming: BackupData, mode: ImportMode): Int = db.withTransaction {
        val current = events.allEvents.first()
        if (mode == ImportMode.REPLACE) {
            current.forEach {
                events.delete(it.id)
                PhotoStore.delete(it.photoUri)
            }
        }
        val known = if (mode == ImportMode.REPLACE) HashSet<String>() else current.map(BackupPlanner::key).toHashSet()
        val idMap = HashMap<Long, Long>()
        var added = 0
        for (event in incoming.events) {
            if (!known.add(BackupPlanner.key(event))) continue
            idMap[event.id] = events.save(event.copy(id = 0, photoUri = null, contactLookupUri = null))
            added++
        }
        val groupIds = importGroups(incoming, idMap)
        incoming.rules.forEach { r ->
            idMap[r.eventId]?.let { reminders.save(r.copy(id = 0, eventId = it, toGroupIds = r.toGroupIds.mapNotNull(groupIds::get).toSet())) }
        }
        incoming.checklist.forEach { i -> idMap[i.eventId]?.let { checklists.save(i.copy(id = 0, eventId = it)) } }
        importTemplates(incoming, groupIds)
        added
    }

    /** Groups are matched to existing ones by their built-in key or name; members are linked only for events that were added. */
    private suspend fun importGroups(incoming: BackupData, eventIds: Map<Long, Long>): Map<Long, Long> {
        if (incoming.groups.isEmpty()) return emptyMap()
        val existing = groups.groups.first()
        val byKey = existing.mapNotNull { g -> g.builtInKey?.let { it to g.id } }.toMap()
        val byName = existing.associate { TextSearch.normalize(it.name) to it.id }
        val groupIds = HashMap<Long, Long>()
        for (g in incoming.groups) {
            groupIds[g.id] = g.builtInKey?.let(byKey::get) ?: byName[TextSearch.normalize(g.name)] ?: groups.save(g.copy(id = 0, defaultTemplateId = null))
        }
        groups.addMembers(incoming.members.mapNotNull { (e, g) -> eventIds[e]?.let { ne -> groupIds[g]?.let { ng -> ne to ng } } })
        return groupIds
    }

    /** New templates are added (known ones skipped); the groups they were made for are translated to this device's group ids. */
    private suspend fun importTemplates(incoming: BackupData, groupIds: Map<Long, Long>) {
        val known = greetings.customTemplates.first().map(BackupPlanner::templateKey).toHashSet()
        incoming.templates.forEach { t ->
            if (known.add(BackupPlanner.templateKey(t))) greetings.saveTemplate(t.copy(id = 0, groupIds = t.groupIds.mapNotNull(groupIds::get).toSet()))
        }
    }
}

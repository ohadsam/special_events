package com.ohadsam.specialevents.data

import androidx.room.withTransaction
import com.ohadsam.specialevents.contacts.PhotoStore
import com.ohadsam.specialevents.data.db.AppDatabase
import com.ohadsam.specialevents.domain.backup.BackupData
import com.ohadsam.specialevents.domain.backup.BackupPlanner
import com.ohadsam.specialevents.domain.backup.ImportMode
import com.ohadsam.specialevents.domain.backup.ImportPreview
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import kotlinx.coroutines.flow.first

/** Reads everything for a backup and writes an imported backup back, all-or-nothing. */
class BackupRepository(
    private val db: AppDatabase,
    private val events: EventRepository,
    private val reminders: ReminderRepository,
    private val checklists: ChecklistRepository,
    private val greetings: GreetingRepository,
) {
    suspend fun snapshot(): BackupData = BackupData(
        events = events.allEvents.first(),
        rules = reminders.snapshot(),
        checklist = checklists.snapshot(),
        templates = greetings.customTemplates.first(),
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
        incoming.rules.forEach { r -> idMap[r.eventId]?.let { reminders.save(r.copy(id = 0, eventId = it)) } }
        incoming.checklist.forEach { i -> idMap[i.eventId]?.let { checklists.save(i.copy(id = 0, eventId = it)) } }
        val knownTemplates = greetings.customTemplates.first().map(BackupPlanner::templateKey).toHashSet()
        incoming.templates.forEach { t: GreetingTemplate -> if (knownTemplates.add(BackupPlanner.templateKey(t))) greetings.saveTemplate(t.copy(id = 0)) }
        added
    }
}

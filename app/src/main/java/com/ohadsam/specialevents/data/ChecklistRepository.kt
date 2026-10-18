package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.AppDatabase
import com.ohadsam.specialevents.data.db.ChecklistItemEntity
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.checklist.ChecklistKind
import com.ohadsam.specialevents.domain.checklist.ChecklistTemplates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Checklist and gift items of all events; they die with their event (foreign-key cascade). */
class ChecklistRepository(db: AppDatabase) {
    private val dao = db.checklistDao()

    val all: Flow<List<ChecklistItem>> = dao.observeAll().map { rows -> rows.map(::toDomain) }

    fun forEvent(eventId: Long): Flow<List<ChecklistItem>> = all.map { items -> items.filter { it.eventId == eventId } }

    suspend fun save(item: ChecklistItem): Long {
        val entity = toEntity(item)
        return if (item.id == 0L) dao.insert(entity) else item.id.also { dao.update(entity) }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun setDone(item: ChecklistItem, done: Boolean) = dao.update(toEntity(item.copy(done = done)))

    /** Adds the starting list of [type], skipping titles the event already has. Returns how many were added. */
    suspend fun addTemplate(eventId: Long, type: EventType): Int {
        val existing = dao.forEvent(eventId)
        val titles = existing.map { it.title }.toSet()
        var order = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val fresh = ChecklistTemplates.forType(type).filter { it.title !in titles }.map { toEntity(it.copy(eventId = eventId, sortOrder = order++)) }
        dao.insertAll(fresh)
        return fresh.size
    }

    /** Unticks everything - for the next year of a yearly event. */
    suspend fun startOver(eventId: Long) = dao.clearDone(eventId)

    suspend fun snapshot(): List<ChecklistItem> = all.first()

    private fun toDomain(e: ChecklistItemEntity) = ChecklistItem(
        e.id, e.eventId, ChecklistKind.entries.firstOrNull { it.name == e.kind } ?: ChecklistKind.TASK, e.title, e.note, e.amount, e.daysBefore, e.done, e.sortOrder,
    )

    private fun toEntity(i: ChecklistItem) = ChecklistItemEntity(i.id, i.eventId, i.kind.name, i.title.trim(), i.note?.trim()?.ifEmpty { null }, i.amount, i.daysBefore, i.done, i.sortOrder)
}

package com.ohadsam.specialevents.data

import androidx.room.withTransaction
import com.ohadsam.specialevents.data.db.AppDatabase
import com.ohadsam.specialevents.data.db.EventTagCrossRef
import com.ohadsam.specialevents.data.db.TagEntity
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.Tag
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The only way UI and engines read/write events and tags (single source of truth over Room). */
class EventRepository(private val db: AppDatabase, private val clock: Clock = Clock.systemUTC()) {
    private val events = db.eventDao()
    private val tagDao = db.tagDao()

    /** All events including archived ones; callers filter (the timeline ignores archived). */
    val allEvents: Flow<List<SpecialEvent>> = events.observeAll().map { rows -> rows.map(EventMapper::toDomain) }

    val tags: Flow<List<Tag>> = tagDao.observeAll().map { rows -> rows.map(EventMapper::toDomain) }

    suspend fun get(id: Long): SpecialEvent? = events.get(id)?.let(EventMapper::toDomain)

    /** Inserts (id == 0) or updates, and replaces the event's tags (created by name when new). Returns the event id. */
    suspend fun save(event: SpecialEvent): Long = db.withTransaction {
        val now = clock.millis()
        val entity = EventMapper.toEntity(event, now)
        val id = if (event.id == 0L) events.insert(entity) else event.id.also { events.update(entity) }
        events.clearTags(id)
        val tagIds = event.tags.map { ensureTag(it).id }.distinct()
        events.insertTagRefs(tagIds.map { EventTagCrossRef(id, it) })
        id
    }

    suspend fun delete(id: Long) = events.delete(id)

    suspend fun setArchived(id: Long, archived: Boolean) {
        val current = events.get(id)?.event ?: return
        events.update(current.copy(archived = archived, updatedAt = clock.millis()))
    }

    suspend fun saveTag(tag: Tag): Tag = db.withTransaction {
        val existing = tagDao.findByName(tag.name.trim())
        if (tag.id != 0L) {
            tagDao.update(TagEntity(tag.id, tag.name.trim(), tag.colorArgb))
            tag
        } else {
            existing?.let(EventMapper::toDomain) ?: tag.copy(id = tagDao.insert(TagEntity(name = tag.name.trim(), colorArgb = tag.colorArgb)))
        }
    }

    suspend fun deleteTag(id: Long) = tagDao.delete(id)

    private suspend fun ensureTag(tag: Tag): Tag = tagDao.findByName(tag.name.trim())?.let(EventMapper::toDomain)
        ?: tag.copy(id = tagDao.insert(TagEntity(name = tag.name.trim(), colorArgb = tag.colorArgb)))
}

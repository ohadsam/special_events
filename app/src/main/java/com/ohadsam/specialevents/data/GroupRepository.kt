package com.ohadsam.specialevents.data

import androidx.room.withTransaction
import com.ohadsam.specialevents.data.db.AppDatabase
import com.ohadsam.specialevents.data.db.EventGroupEntity
import com.ohadsam.specialevents.data.db.EventGroupMember
import com.ohadsam.specialevents.domain.backup.BackupCodec
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.groups.GroupDefaults
import com.ohadsam.specialevents.domain.reminders.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Groups and who belongs to them. Memberships die with their event or group (foreign-key cascade). */
class GroupRepository(private val db: AppDatabase) {
    private val dao = db.groupDao()

    val groups: Flow<List<EventGroup>> = dao.observeGroups().map { rows -> rows.map(::toDomain) }

    /** event id -> ids of the groups it belongs to. */
    val membership: Flow<Map<Long, Set<Long>>> = dao.observeMembers().map { rows -> rows.groupBy({ it.eventId }, { it.groupId }).mapValues { it.value.toSet() } }

    suspend fun groupsOf(eventId: Long): List<EventGroup> = dao.groupsOf(eventId).map(::toDomain)

    suspend fun memberIds(groupId: Long): Set<Long> = dao.memberIds(groupId).toSet()

    suspend fun save(group: EventGroup): Long {
        val entity = toEntity(group)
        return if (group.id == 0L) dao.insert(entity.copy(sortOrder = nextOrder())) else group.id.also { dao.update(entity) }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun setMembers(groupId: Long, eventIds: Set<Long>) = db.withTransaction {
        dao.clearGroup(groupId)
        dao.insertMembers(eventIds.map { EventGroupMember(it, groupId) })
    }

    suspend fun setGroupsOf(eventId: Long, groupIds: Set<Long>) = db.withTransaction {
        dao.clearEvent(eventId)
        dao.insertMembers(groupIds.map { EventGroupMember(eventId, it) })
    }

    /** Adds memberships without touching existing ones (restore). */
    suspend fun addMembers(pairs: List<Pair<Long, Long>>) = dao.insertMembers(pairs.map { (e, g) -> EventGroupMember(e, g) })

    /** Adds the starting groups that are missing (by their built-in key); never touches groups the user edited. */
    suspend fun ensureBuiltIns() = db.withTransaction {
        val have = dao.groups().mapNotNull { it.builtInKey }.toSet()
        GroupDefaults.BUILT_IN.filter { it.builtInKey !in have }.forEach { dao.insert(toEntity(it).copy(sortOrder = nextOrder())) }
    }

    suspend fun snapshot(): Pair<List<EventGroup>, List<EventGroupMember>> = groups.first() to dao.observeMembers().first()

    private suspend fun nextOrder(): Int = (dao.groups().maxOfOrNull { it.sortOrder } ?: -1) + 1

    private fun toDomain(e: EventGroupEntity) = EventGroup(
        id = e.id,
        name = e.name,
        emoji = e.emoji,
        colorArgb = e.colorArgb,
        builtInKey = e.builtInKey,
        sortOrder = e.sortOrder,
        tags = lines(e.tags),
        defaultChannel = Channel.entries.firstOrNull { it.name == e.defaultChannel },
        defaultTemplateId = e.defaultTemplateId,
        reminderRules = if (e.reminderRules.isBlank()) emptyList() else BackupCodec.decodeRules(e.reminderRules),
        extraRecipients = lines(e.extraRecipients),
    )

    private fun toEntity(g: EventGroup) = EventGroupEntity(
        id = g.id,
        name = g.name.trim(),
        emoji = g.emoji?.trim()?.ifEmpty { null },
        colorArgb = g.colorArgb,
        builtInKey = g.builtInKey,
        sortOrder = g.sortOrder,
        tags = g.tags.joinToString("\n"),
        defaultChannel = g.defaultChannel?.name,
        defaultTemplateId = g.defaultTemplateId,
        reminderRules = if (g.reminderRules.isEmpty()) "" else BackupCodec.encodeRules(g.reminderRules),
        extraRecipients = g.extraRecipients.joinToString("\n"),
    )

    private fun lines(text: String) = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
}

package com.ohadsam.specialevents.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Transaction
    @Query("SELECT * FROM events ORDER BY id")
    fun observeAll(): Flow<List<EventWithTags>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): EventWithTags?

    @Insert
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM event_tags WHERE eventId = :eventId")
    suspend fun clearTags(eventId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagRefs(refs: List<EventTagCrossRef>)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_rules ORDER BY id")
    fun observeAll(): Flow<List<ReminderRuleEntity>>

    @Query("SELECT * FROM reminder_rules WHERE eventId = :eventId ORDER BY id")
    suspend fun forEvent(eventId: Long): List<ReminderRuleEntity>

    @Insert
    suspend fun insert(rule: ReminderRuleEntity): Long

    @Insert
    suspend fun insertAll(rules: List<ReminderRuleEntity>)

    @Update
    suspend fun update(rule: ReminderRuleEntity)

    @Query("DELETE FROM reminder_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM reminder_rules WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)
}

@Dao
interface GreetingDao {
    @Query("SELECT * FROM greeting_templates ORDER BY createdAt DESC")
    fun observeTemplates(): Flow<List<GreetingTemplateEntity>>

    @Insert
    suspend fun insertTemplate(t: GreetingTemplateEntity): Long

    @Update
    suspend fun updateTemplate(t: GreetingTemplateEntity)

    @Query("DELETE FROM greeting_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("SELECT * FROM greetings_sent WHERE eventId = :eventId ORDER BY sentAt DESC")
    fun observeSentFor(eventId: Long): Flow<List<GreetingSentEntity>>

    @Query("SELECT * FROM greetings_sent")
    fun observeAllSent(): Flow<List<GreetingSentEntity>>

    @Insert
    suspend fun insertSent(s: GreetingSentEntity): Long

    @Query("DELETE FROM greetings_sent WHERE id = :id")
    suspend fun deleteSent(id: Long)
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items ORDER BY eventId, sortOrder, id")
    fun observeAll(): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE eventId = :eventId ORDER BY sortOrder, id")
    suspend fun forEvent(eventId: Long): List<ChecklistItemEntity>

    @Insert
    suspend fun insert(item: ChecklistItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE checklist_items SET done = 0 WHERE eventId = :eventId")
    suspend fun clearDone(eventId: Long)
}

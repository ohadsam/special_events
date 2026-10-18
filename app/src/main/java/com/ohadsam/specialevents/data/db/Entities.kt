package com.ohadsam.specialevents.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Enums are stored by name (TEXT) so adding a value never needs a migration; unknown names fall back safely in `EventMapper`. */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val personName: String? = null,
    val gender: String? = null,
    val relation: String? = null,
    val basis: String,
    val recurrence: String,
    val originYear: Int? = null,
    val originMonth: Int,
    val originDay: Int,
    val afterSunset: Boolean = false,
    val timeMinutes: Int? = null,
    val notes: String? = null,
    val colorArgb: Int? = null,
    val iconKey: String? = null,
    val photoUri: String? = null,
    val contactLookupUri: String? = null,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    // Added in schema v3; kept last so a fresh install and an upgraded database have the same column order.
    val contactPhone: String? = null,
    val contactEmail: String? = null,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int? = null,
)

@Entity(
    tableName = "event_tags",
    primaryKeys = ["eventId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagId")],
)
data class EventTagCrossRef(val eventId: Long, val tagId: Long)

data class EventWithTags(
    @Embedded val event: EventEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(value = EventTagCrossRef::class, parentColumn = "eventId", entityColumn = "tagId"),
    )
    val tags: List<TagEntity>,
)

/** One reminder rule of an event; see `ReminderRule` in the domain for the meaning of the fields. */
@Entity(
    tableName = "reminder_rules",
    foreignKeys = [ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("eventId")],
)
data class ReminderRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val offsetValue: Int,
    val offsetUnit: String,
    val timeMinutes: Int,
    val channel: String,
    val shabbatPolicy: String,
    val profileKey: String,
    val minTier: String,
    val enabled: Boolean,
    val origin: String,
    // Added in schema v5; last, with a default, so an upgraded database and a fresh one are identical.
    @ColumnInfo(defaultValue = "FIXED") val timeMode: String = "FIXED",
    // Added in schema v6.
    @ColumnInfo(defaultValue = "0") val autoSend: Boolean = false,
    val templateId: Long? = null,
)

/** A greeting text the user wrote (the built-in ones live in code, see `GreetingCatalog`). */
@Entity(tableName = "greeting_templates")
data class GreetingTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val body: String,
    val forType: String?,
    val createdAt: Long,
)

/** "I greeted this event on this occurrence day by this channel" - the greeting history. */
@Entity(
    tableName = "greetings_sent",
    foreignKeys = [ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("eventId")],
)
data class GreetingSentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val occurrenceDay: Long,
    val channel: String,
    val sentAt: Long,
    val preview: String,
)

/** A checklist step or gift of an event; see `ChecklistItem` in the domain. */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("eventId")],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val kind: String,
    val title: String,
    val note: String?,
    val amount: Int?,
    val daysBefore: Int?,
    val done: Boolean,
    val sortOrder: Int,
)

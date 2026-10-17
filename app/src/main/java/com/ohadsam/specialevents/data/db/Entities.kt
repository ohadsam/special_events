package com.ohadsam.specialevents.data.db

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

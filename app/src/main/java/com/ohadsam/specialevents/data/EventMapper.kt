package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.data.db.EventEntity
import com.ohadsam.specialevents.data.db.EventWithTags
import com.ohadsam.specialevents.data.db.TagEntity
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.Tag

/** Storage ⇄ domain. Unknown enum names (e.g. from a newer backup) degrade to safe defaults instead of crashing. */
object EventMapper {
    fun toDomain(row: EventWithTags): SpecialEvent {
        val e = row.event
        val type = enumOrNull<EventType>(e.type) ?: EventType.CUSTOM
        return SpecialEvent(
            id = e.id,
            type = type,
            title = e.title,
            personName = e.personName,
            gender = e.gender?.let { enumOrNull<Gender>(it) },
            relation = e.relation,
            origin = OriginDate(enumOrNull<CalendarBasis>(e.basis) ?: type.defaultBasis, e.originYear, e.originMonth, e.originDay),
            recurrence = enumOrNull<Recurrence>(e.recurrence) ?: if (type.yearly) Recurrence.YEARLY else Recurrence.ONCE,
            afterSunset = e.afterSunset,
            timeMinutes = e.timeMinutes,
            notes = e.notes,
            colorArgb = e.colorArgb,
            iconKey = e.iconKey,
            photoUri = e.photoUri,
            contactLookupUri = e.contactLookupUri,
            contactPhone = e.contactPhone,
            contactEmail = e.contactEmail,
            archived = e.archived,
            tags = row.tags.map(::toDomain),
            createdAt = e.createdAt,
            updatedAt = e.updatedAt,
        )
    }

    fun toDomain(t: TagEntity) = Tag(t.id, t.name, t.colorArgb)

    /** [now] becomes `updatedAt` (and `createdAt` for a new event). */
    fun toEntity(event: SpecialEvent, now: Long): EventEntity = EventEntity(
        id = event.id,
        type = event.type.name,
        title = event.title,
        personName = event.personName,
        gender = event.gender?.name,
        relation = event.relation,
        basis = event.origin.basis.name,
        recurrence = event.recurrence.name,
        originYear = event.origin.year,
        originMonth = event.origin.month,
        originDay = event.origin.day,
        afterSunset = event.afterSunset,
        timeMinutes = event.timeMinutes,
        notes = event.notes,
        colorArgb = event.colorArgb,
        iconKey = event.iconKey,
        photoUri = event.photoUri,
        contactLookupUri = event.contactLookupUri,
        contactPhone = event.contactPhone,
        contactEmail = event.contactEmail,
        archived = event.archived,
        createdAt = if (event.createdAt == 0L) now else event.createdAt,
        updatedAt = now,
    )

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }
}

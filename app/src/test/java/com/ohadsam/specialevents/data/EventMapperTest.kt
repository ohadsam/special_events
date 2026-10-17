package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.data.db.EventWithTags
import com.ohadsam.specialevents.data.db.TagEntity
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.Tag
import org.junit.Assert.assertEquals
import org.junit.Test

class EventMapperTest {
    private val full = SpecialEvent(
        id = 7,
        type = EventType.ANNIVERSARY,
        title = "אבי ושרה",
        personName = "אבי",
        gender = Gender.MALE,
        relation = "אחי",
        origin = OriginDate(CalendarBasis.HEBREW, 5750, 12, 3),
        recurrence = Recurrence.YEARLY,
        afterSunset = true,
        timeMinutes = 20 * 60,
        notes = "הערה",
        colorArgb = 0xFF112233.toInt(),
        iconKey = "💍",
        photoUri = "content://p/1",
        contactLookupUri = "content://c/1",
        archived = true,
        tags = listOf(Tag(3, "משפחה", 5)),
        createdAt = 100,
        updatedAt = 200,
    )

    @Test
    fun roundTripKeepsEverythingExceptUpdatedAt() {
        val entity = EventMapper.toEntity(full, now = 999)
        val back = EventMapper.toDomain(EventWithTags(entity, listOf(TagEntity(3, "משפחה", 5))))
        assertEquals(full.copy(updatedAt = 999), back)
    }

    @Test
    fun newEventGetsCreatedAtFromNow() {
        val entity = EventMapper.toEntity(full.copy(id = 0, createdAt = 0), now = 555)
        assertEquals(555, entity.createdAt)
        assertEquals(555, entity.updatedAt)
    }

    @Test
    fun unknownEnumNamesFallBackSafely() {
        val entity = EventMapper.toEntity(full, 1).copy(type = "FUTURE_TYPE", basis = "LUNAR", recurrence = "WEEKLY", gender = "OTHER")
        val e = EventMapper.toDomain(EventWithTags(entity, emptyList()))
        assertEquals(EventType.CUSTOM, e.type)
        assertEquals(CalendarBasis.GREGORIAN, e.origin.basis)
        assertEquals(Recurrence.YEARLY, e.recurrence)
        assertEquals(null, e.gender)
    }
}

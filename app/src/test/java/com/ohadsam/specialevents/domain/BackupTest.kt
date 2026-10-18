package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.domain.backup.BackupCodec
import com.ohadsam.specialevents.domain.backup.BackupData
import com.ohadsam.specialevents.domain.backup.BackupError
import com.ohadsam.specialevents.domain.backup.BackupPlanner
import com.ohadsam.specialevents.domain.backup.DecodeResult
import com.ohadsam.specialevents.domain.backup.ImportMode
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.checklist.ChecklistKind
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.reminders.SoundProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private fun event(id: Long, title: String, month: Int = 10, day: Int = 17, basis: CalendarBasis = CalendarBasis.GREGORIAN) = SpecialEvent(
        id = id,
        type = EventType.BIRTHDAY,
        title = title,
        personName = "דנה \"הקטנה\" לוי",
        gender = Gender.FEMALE,
        origin = OriginDate(basis, 1980, month, day),
        notes = "שורה 1\nשורה 2 — עם גרשיים \" ותווים מיוחדים",
        contactPhone = "050-1234567",
        tags = listOf(Tag(name = "משפחה", colorArgb = -0xB5C5A8)),
    )

    private val rule = ReminderRule(
        eventId = 1,
        offsetValue = 7,
        channel = Channel.SMS,
        profileKey = "sound:content://media/x",
        minTier = Tier.MAJOR,
        autoSend = true,
        templateId = 55,
    )

    private val data = BackupData(
        events = listOf(event(1, "דנה לוי")),
        rules = listOf(rule),
        checklist = listOf(ChecklistItem(eventId = 1, kind = ChecklistKind.GIFT, title = "מתנה", amount = 150, daysBefore = 7, done = true, sortOrder = 2)),
        templates = listOf(GreetingTemplate(9, "שלי", "מזל טוב {שם}", EventType.BIRTHDAY)),
    )

    private fun decodeOk(text: String) = decode(text) as DecodeResult.Ok

    private fun decode(text: String) = BackupCodec.decode(text)

    @Test
    fun roundTripKeepsEventsChecklistAndTemplates() {
        val ok = decodeOk(BackupCodec.encode(data, "2026-10-18T10:00:00Z", "0.13.0"))
        assertEquals(0, ok.skipped)
        assertEquals("2026-10-18T10:00:00Z", ok.exportedAt)
        assertEquals(data.events.single().copy(id = 1), ok.data.events.single())
        assertEquals(data.checklist, ok.data.checklist)
        assertEquals("שלי", ok.data.templates.single().name)
        assertEquals(EventType.BIRTHDAY, ok.data.templates.single().forType)
    }

    @Test
    fun importedRulesCannotSendOnTheirOwnOrPointAtThisDevicesSounds() {
        val r = decodeOk(BackupCodec.encode(data, "t", "v")).data.rules.single()
        assertFalse(r.autoSend)
        assertNull(r.templateId)
        assertEquals(SoundProfile.DEFAULT_KEY, r.profileKey)
        assertEquals(Channel.SMS, r.channel)
        assertEquals(7, r.offsetValue)
        assertEquals(Tier.MAJOR, r.minTier)
    }

    @Test
    fun rejectsFilesThatAreNotBackups() {
        assertEquals(DecodeResult.Failure(BackupError.NOT_JSON), decode("hello"))
        assertEquals(DecodeResult.Failure(BackupError.WRONG_FORMAT), decode("""{"format":"other","version":1}"""))
        assertEquals(DecodeResult.Failure(BackupError.WRONG_FORMAT), decode("[1,2,3]".let { """{"x":$it}""" }))
        assertEquals(DecodeResult.Failure(BackupError.TOO_NEW), decode("""{"format":"special-events-backup","version":99}"""))
        assertEquals(DecodeResult.Failure(BackupError.TOO_BIG), decode("x".repeat(BackupCodec.MAX_CHARS + 1)))
    }

    @Test
    fun badEntriesAreSkippedAndCountedNotFatal() {
        val text = """
            {"format":"special-events-backup","version":1,"events":[
              {"id":1,"type":"BIRTHDAY","title":"טוב","basis":"GREGORIAN","month":3,"day":4},
              {"id":2,"type":"NOPE","title":"רע","basis":"GREGORIAN","month":3,"day":4},
              {"id":3,"type":"BIRTHDAY","title":"","basis":"GREGORIAN","month":3,"day":4},
              {"id":4,"type":"BIRTHDAY","title":"חודש 13 לועזי","basis":"GREGORIAN","month":13,"day":4},
              {"id":5,"type":"YAHRZEIT","title":"אדר ב","basis":"HEBREW","month":13,"day":14,"unknownField":true},
              "not an object"
            ]}
        """.trimIndent()
        val ok = decodeOk(text)
        assertEquals(listOf("טוב", "אדר ב"), ok.data.events.map { it.title })
        assertEquals(4, ok.skipped)
        assertEquals(Recurrence.YEARLY, ok.data.events.first().recurrence)
    }

    @Test
    fun textIsClampedAndOutOfRangeValuesCorrected() {
        val long = "א".repeat(1000)
        val text = """{"format":"special-events-backup","version":1,"events":[{"id":1,"type":"CUSTOM","title":"$long","basis":"GREGORIAN","month":1,"day":1,"timeMinutes":99999}],
            "rules":[{"eventId":1,"offsetValue":100000,"offsetUnit":"DAYS","timeMinutes":-5}]}"""
        val ok = decodeOk(text)
        assertEquals(200, ok.data.events.single().title.length)
        assertNull(ok.data.events.single().timeMinutes)
        assertEquals(365, ok.data.rules.single().offsetValue)
        assertEquals(0, ok.data.rules.single().timeMinutes)
    }

    @Test
    fun previewCountsDuplicatesByTypeTitleAndDate() {
        val existing = listOf(event(10, "דנה לוי"), event(11, "אחר", month = 1))
        val incoming = BackupData(listOf(event(1, " דנה  לוי".trim()), event(2, "חדש", month = 5), event(3, "חדש", month = 5)), emptyList(), emptyList(), listOf(GreetingTemplate(0, "שלי", "x")))
        val merge = BackupPlanner.preview(existing, emptyList(), incoming, ImportMode.MERGE)
        assertEquals(1, merge.newEvents)
        assertEquals(2, merge.duplicateEvents)
        assertEquals(1, merge.newTemplates)
        assertEquals(2, merge.existingEvents)
        val replace = BackupPlanner.preview(existing, emptyList(), incoming, ImportMode.REPLACE)
        assertEquals(2, replace.newEvents)
        assertTrue(BackupPlanner.key(event(1, "דנה לוי")) == BackupPlanner.key(event(99, "דנה לוי")))
    }
}

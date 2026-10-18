package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.backup.BackupCodec
import com.ohadsam.specialevents.domain.backup.BackupData
import com.ohadsam.specialevents.domain.backup.DecodeResult
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.greetings.RichText
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.share.CardField
import com.ohadsam.specialevents.domain.share.CardLabels
import com.ohadsam.specialevents.domain.share.PersonCard
import com.ohadsam.specialevents.domain.share.ShareField
import com.ohadsam.specialevents.domain.share.SharePlanner
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTest {
    private val dana = SpecialEvent(
        id = 1, type = EventType.BIRTHDAY, title = "דנה לוי", personName = "דנה לוי", origin = OriginDate(CalendarBasis.GREGORIAN, 1980, 10, 17),
        notes = "אוהבת שוקולד", colorArgb = 123, iconKey = "🎂", photoUri = "/files/p.jpg", contactLookupUri = "content://x", contactPhone = "050-1", contactEmail = "d@x.com",
        tags = listOf(Tag(name = "משפחה")),
    )
    private val avi = dana.copy(id = 2, type = EventType.ANNIVERSARY, title = "אבי ושרה", personName = null)
    private val group = EventGroup(id = 5, name = "משפחה")
    private val all = BackupData(
        events = listOf(dana, avi),
        rules = listOf(ReminderRule(eventId = 1, toGroupIds = setOf(5, 6)), ReminderRule(eventId = 2)),
        checklist = listOf(ChecklistItem(eventId = 1, title = "מתנה", done = true), ChecklistItem(eventId = 2, title = "פרחים")),
        templates = emptyList(),
        groups = listOf(group, EventGroup(id = 6, name = "עבודה")),
        members = listOf(1L to 5L, 2L to 6L),
    )

    @Test
    fun onlyChosenEventsAndFieldsLeave() {
        val shared = SharePlanner.build(setOf(1), all, emptySet())
        assertEquals(listOf(1L), shared.events.map { it.id })
        val e = shared.events.single()
        assertNull(e.notes)
        assertNull(e.contactPhone)
        assertNull(e.contactEmail)
        assertNull(e.iconKey)
        assertNull(e.colorArgb)
        assertTrue(e.tags.isEmpty())
        assertTrue(shared.rules.isEmpty() && shared.checklist.isEmpty() && shared.groups.isEmpty() && shared.members.isEmpty())
        assertEquals(dana.origin, e.origin)
    }

    @Test
    fun photoAndContactLinkNeverLeaveEvenWhenEverythingIsOn() {
        val e = SharePlanner.build(setOf(1), all, ShareField.entries.toSet()).events.single()
        assertNull(e.photoUri)
        assertNull(e.contactLookupUri)
        assertEquals("אוהבת שוקולד", e.notes)
        assertEquals("050-1", e.contactPhone)
        assertEquals("🎂", e.iconKey)
    }

    @Test
    fun checklistArrivesUntickedAndGroupsOnlyWithTheirMembers() {
        val shared = SharePlanner.build(setOf(1), all, setOf(ShareField.CHECKLIST, ShareField.GROUPS, ShareField.REMINDERS))
        assertEquals(listOf(false), shared.checklist.map { it.done })
        assertEquals(listOf(5L), shared.groups.map { it.id })
        assertEquals(listOf(1L to 5L), shared.members)
        assertEquals(setOf(5L), shared.rules.single().toGroupIds)
        val noGroups = SharePlanner.build(setOf(1), all, setOf(ShareField.REMINDERS))
        assertTrue(noGroups.rules.single().toGroupIds.isEmpty())
    }

    @Test
    fun shareFileIsTaggedAndReadableByTheNormalImporter() {
        val text = BackupCodec.encode(SharePlanner.build(setOf(1, 2), all, SharePlanner.DEFAULT_FIELDS), "t", "v", BackupCodec.KIND_SHARE)
        val ok = BackupCodec.decode(text) as DecodeResult.Ok
        assertEquals(BackupCodec.KIND_SHARE, ok.kind)
        assertEquals(2, ok.data.events.size)
        assertEquals(BackupCodec.KIND_BACKUP, (BackupCodec.decode(BackupCodec.encode(all, "t", "v")) as DecodeResult.Ok).kind)
    }

    private val labels = CardLabels(
        type = { if (it == EventType.BIRTHDAY) "יום הולדת" else "יום נישואין" },
        gregorian = { "${it.dayOfMonth}.${it.monthValue}.${it.year}" },
        age = { "בת $it" },
        years = { "$it שנים" },
        next = { "הקרוב: $it" },
        untitled = "תאריכים",
    )

    @Test
    fun cardShowsOnlyTheChosenDetailsAndFormatsPerTarget() {
        val item = EventTimeline.forEvent(dana, LocalDate.of(2026, 10, 18), 1).single()
        val spans = PersonCard.build(null, listOf(dana), mapOf(1L to item), setOf(CardField.TYPE, CardField.DATE_GREGORIAN, CardField.AGE), labels)
        assertEquals("דנה לוי\n\n🎂 יום הולדת — דנה לוי\n17.10.1980\nבת ${item.number}", RichText.plain(spans))
        assertTrue(RichText.whatsApp(spans).startsWith("*דנה לוי*"))
        assertTrue(RichText.html(spans).startsWith("<b>דנה לוי</b>"))
        val bare = PersonCard.build("הכרטיס שלי", listOf(dana), emptyMap(), setOf(CardField.NOTES, CardField.CONTACT, CardField.TAGS), labels)
        val text = RichText.plain(bare)
        assertTrue(text.startsWith("הכרטיס שלי"))
        assertTrue("אוהבת שוקולד" in text && "050-1 · d@x.com" in text && "#משפחה" in text)
        assertFalse("יום הולדת" in text)
    }

    @Test
    fun cardWithTwoEventsListsBothUnderOneHeading() {
        val text = RichText.plain(PersonCard.build("משפחה", listOf(dana, avi), emptyMap(), setOf(CardField.TYPE), labels))
        assertEquals("משפחה\n\n🎂 יום הולדת — דנה לוי\n\n🎂 יום נישואין — אבי ושרה", text)
        assertEquals("תאריכים", RichText.plain(PersonCard.build(null, emptyList(), emptyMap(), emptySet(), labels)))
    }
}

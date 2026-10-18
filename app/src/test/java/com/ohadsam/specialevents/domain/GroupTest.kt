package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.backup.BackupCodec
import com.ohadsam.specialevents.domain.backup.BackupData
import com.ohadsam.specialevents.domain.backup.DecodeResult
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.groups.GroupDefaults
import com.ohadsam.specialevents.domain.groups.GroupLogic
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.reminders.RuleOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupTest {
    private fun person(id: Long, name: String, phone: String? = null, email: String? = null) = SpecialEvent(
        id = id, type = EventType.BIRTHDAY, title = name, personName = name,
        origin = OriginDate(CalendarBasis.GREGORIAN, 1990, 1, 1), contactPhone = phone, contactEmail = email,
    )

    @Test
    fun builtInGroupsAreSixDistinctWithTemplates() {
        assertEquals(6, GroupDefaults.BUILT_IN.size)
        assertEquals(6, GroupDefaults.BUILT_IN.mapNotNull { it.builtInKey }.toSet().size)
        assertTrue(GroupDefaults.BUILT_IN.all { it.reminderRules.isNotEmpty() && it.emoji != null && it.colorArgb != null })
    }

    @Test
    fun templateIsNeverAppliedOverRulesTheUserEdited() {
        val own = ReminderRule(origin = RuleOrigin.USER)
        val auto = ReminderRule(origin = RuleOrigin.DEFAULT)
        assertTrue(GroupLogic.mayApplyTemplate(emptyList()))
        assertTrue(GroupLogic.mayApplyTemplate(listOf(auto)))
        assertFalse(GroupLogic.mayApplyTemplate(listOf(auto, own)))
        val rules = GroupLogic.templateFor(EventGroup(name = "x", reminderRules = listOf(own.copy(id = 5, eventId = 9))), eventId = 42)
        assertEquals(listOf(0L), rules.map { it.id })
        assertEquals(listOf(42L), rules.map { it.eventId })
        assertEquals(RuleOrigin.DEFAULT, rules.single().origin)
    }

    @Test
    fun greetingTemplateComesFromRuleThenGroupsInOrder() {
        val a = EventGroup(name = "א", sortOrder = 1, defaultTemplateId = 7)
        val b = EventGroup(name = "ב", sortOrder = 0, defaultTemplateId = 3)
        val none = EventGroup(name = "ג", sortOrder = 2)
        assertEquals(99L, GroupLogic.greetingTemplateId(99, listOf(a, b)))
        assertEquals(3L, GroupLogic.greetingTemplateId(null, listOf(a, b, none)))
        assertNull(GroupLogic.greetingTemplateId(null, listOf(none)))
        assertNull(GroupLogic.greetingTemplateId(null, emptyList()))
    }

    @Test
    fun recipientsCombineMembersAndExtrasWithoutDuplicates() {
        val group = EventGroup(name = "משפחה", extraRecipients = listOf("050-111 2233", "SAM@x.com", "sam@x.com", "אין", "0501112233"))
        val members = listOf(
            person(1, "דנה", phone = "050-1112233", email = "dana@x.com"),
            person(2, "יוסי", email = "Sam@X.com"),
            person(3, "בלי פרטים"),
        )
        val r = GroupLogic.recipients(group, members)
        assertEquals(listOf("דנה", "יוסי"), r.map { it.label })
        assertEquals("0501112233", r[0].phone)
        assertTrue(r.size == 2)
    }

    @Test
    fun groupsSurviveBackupAndRestore() {
        val g = EventGroup(
            id = 4, name = "חברים", emoji = "🤝", colorArgb = -0x123456, builtInKey = "friends", sortOrder = 1, tags = listOf("חברים"),
            defaultChannel = Channel.WHATSAPP, defaultTemplateId = 12, reminderRules = GroupDefaults.BUILT_IN.first().reminderRules, extraRecipients = listOf("050-1"),
        )
        val data = BackupData(listOf(person(1, "דנה")), emptyList(), emptyList(), emptyList(), listOf(g), listOf(1L to 4L))
        val ok = BackupCodec.decode(BackupCodec.encode(data, "t", "v")) as DecodeResult.Ok
        val back = ok.data.groups.single()
        assertEquals(g.copy(defaultTemplateId = null, reminderRules = back.reminderRules), back)
        assertEquals(g.reminderRules.size, back.reminderRules.size)
        assertEquals(listOf(1L to 4L), ok.data.members)
        assertTrue(back.reminderRules.none { it.autoSend })
    }

    @Test
    fun ruleListTextRoundTrips() {
        val rules = GroupDefaults.BUILT_IN.first().reminderRules
        assertEquals(rules.size, BackupCodec.decodeRules(BackupCodec.encodeRules(rules)).size)
        assertTrue(BackupCodec.decodeRules("garbage").isEmpty())
    }
}

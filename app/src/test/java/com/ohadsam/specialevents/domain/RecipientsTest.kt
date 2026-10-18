package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.backup.BackupCodec
import com.ohadsam.specialevents.domain.backup.BackupData
import com.ohadsam.specialevents.domain.backup.DecodeResult
import com.ohadsam.specialevents.domain.greetings.GreetingContext
import com.ohadsam.specialevents.domain.greetings.GreetingRenderer
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.groups.RecipientResolver
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipientsTest {
    private fun person(id: Long, name: String, phone: String? = null, email: String? = null) = SpecialEvent(
        id = id, type = EventType.BIRTHDAY, title = name, personName = name,
        origin = OriginDate(CalendarBasis.GREGORIAN, 1990, 1, 1), contactPhone = phone, contactEmail = email,
    )

    private val dana = person(1, "דנה", phone = "050-1111111")
    private val yossi = person(2, "יוסי", phone = "052-2222222")
    private val family = EventGroup(id = 7, name = "משפחה", extraRecipients = listOf("053-3333333"))
    private val members = mapOf(7L to listOf(dana, yossi))

    private fun resolve(rule: ReminderRule, event: SpecialEvent = dana, self: String = "054-4444444") =
        RecipientResolver.resolve(rule, event, listOf(family), { members[it].orEmpty() }, self, "")

    @Test
    fun defaultRuleGoesToThePersonAlone() {
        assertEquals(listOf("דנה"), resolve(ReminderRule()).map { it.label })
        assertTrue(resolve(ReminderRule(), event = person(3, "בלי פרטים")).isEmpty())
    }

    @Test
    fun personSelfGroupAndExtrasCombineInOrderWithoutDuplicates() {
        val rule = ReminderRule(toPerson = true, toSelf = true, toGroupIds = setOf(7), toExtra = listOf("050 111 1111", "055-5555555"))
        val r = resolve(rule)
        assertEquals(listOf("דנה", "אני", "יוסי", "053-3333333", "055-5555555"), r.map { it.label })
        assertEquals(r.size, r.map { it.key }.toSet().size)
    }

    @Test
    fun groupOnlyRuleSkipsThePersonAndUnknownGroups() {
        assertEquals(listOf("דנה", "יוסי", "053-3333333"), resolve(ReminderRule(toPerson = false, toGroupIds = setOf(7, 99))).map { it.label })
        assertTrue(resolve(ReminderRule(toPerson = false, toSelf = true), self = "").isEmpty())
    }

    @Test
    fun fanOutIsCapped() {
        val many = (1..100).map { "05000000%02d".format(it) + it }
        val r = resolve(ReminderRule(toPerson = false, toExtra = many))
        assertEquals(RecipientResolver.MAX_RECIPIENTS, r.size)
    }

    @Test
    fun recipientPlaceholderFillsEachCopyAndVanishesWhenUnknown() {
        val ctx = GreetingContext(name = "דנה")
        assertEquals("היי יוסי, מזל טוב לדנה", GreetingRenderer.render("היי {נמען}, מזל טוב ל{שם}", ctx.copy(recipient = "יוסי")))
        assertEquals("מזל טוב לדנה", GreetingRenderer.render("[[היי {נמען}, ]]מזל טוב ל{שם}", ctx))
    }

    @Test
    fun ruleRecipientsSurviveBackupExceptAutoSend() {
        val rule = ReminderRule(eventId = 1, toPerson = false, toSelf = true, toGroupIds = setOf(7), toExtra = listOf("050-1"), autoSend = true)
        val data = BackupData(listOf(dana), listOf(rule), emptyList(), emptyList())
        val back = (BackupCodec.decode(BackupCodec.encode(data, "t", "v")) as DecodeResult.Ok).data.rules.single()
        assertEquals(false, back.toPerson)
        assertEquals(true, back.toSelf)
        assertEquals(setOf(7L), back.toGroupIds)
        assertEquals(listOf("050-1"), back.toExtra)
        assertEquals(false, back.autoSend)
    }
}

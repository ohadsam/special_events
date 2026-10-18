package com.ohadsam.specialevents.domain.greetings

import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongAndAutoSendTest {
    @Test
    fun everyTypeHasSongIdeasAndUrlsAreEncoded() {
        EventType.entries.forEach { assertTrue(it.name, SongSuggestions.forEvent(it, null, null).isNotEmpty()) }
        val idea = SongSuggestions.forEvent(EventType.BIRTHDAY, null, null).first()
        assertTrue(idea.url.startsWith("https://www.youtube.com/results?search_query="))
        assertTrue(!idea.url.contains(" "))
    }

    @Test
    fun milestonesAndGenderPickMoreSpecificSongs() {
        assertEquals("שיר ליובל", SongSuggestions.forEvent(EventType.BIRTHDAY, null, "age:JUBILEE_50").first().label)
        assertEquals("שיר לחתונת זהב", SongSuggestions.forEvent(EventType.ANNIVERSARY, null, "gift:GOLD").first().label)
        assertEquals("שיר לבת מצווה", SongSuggestions.forEvent(EventType.BAR_BAT_MITZVAH, Gender.FEMALE, null).first().label)
    }

    @Test
    fun autoSendNeedsEveryCondition() {
        val ok = AutoSend.decide(autoSend = true, isSms = true, hasPermission = true, phone = "050-1234567", alreadyGreeted = false)
        assertEquals(AutoSendDecision.SEND, ok)
        assertEquals(AutoSendDecision.PROMPT_INSTEAD, AutoSend.decide(false, true, true, "0501234567", false))
        assertEquals(AutoSendDecision.PROMPT_INSTEAD, AutoSend.decide(true, false, true, "0501234567", false))
        assertEquals(AutoSendDecision.PROMPT_INSTEAD, AutoSend.decide(true, true, false, "0501234567", false))
        assertEquals(AutoSendDecision.PROMPT_INSTEAD, AutoSend.decide(true, true, true, null, false))
        assertEquals(AutoSendDecision.PROMPT_INSTEAD, AutoSend.decide(true, true, true, "12", false))
        assertEquals(AutoSendDecision.ALREADY_GREETED, AutoSend.decide(true, true, true, "0501234567", true))
    }

    @Test
    fun templateChoiceFallsBackToTheBestMatch() {
        val a = GreetingTemplate(-1, "a", "x")
        val b = GreetingTemplate(7, "b", "y")
        assertEquals(b, AutoSend.chooseTemplate(7, listOf(a, b)))
        assertEquals(a, AutoSend.chooseTemplate(99, listOf(a, b)))
        assertEquals(a, AutoSend.chooseTemplate(null, listOf(a, b)))
    }
}

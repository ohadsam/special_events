package com.ohadsam.specialevents.domain.greetings

import com.ohadsam.specialevents.calendar.AnniversaryGift
import com.ohadsam.specialevents.calendar.Milestones
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GreetingTest {
    private val full = GreetingContext(name = "דנה", age = 50, years = 25, relation = "אחות", hebrewDate = "ו׳ בחשוון", gregorianDate = "17.10.2026", parsha = "נח")

    @Test
    fun fillsPlaceholders() {
        assertEquals("מזל טוב דנה, בת 50!", GreetingRenderer.render("מזל טוב {שם}, בת {גיל}!", full))
        assertEquals("אחות · ו׳ בחשוון · 17.10.2026 · נח", GreetingRenderer.render("{יחס} · {תאריך_עברי} · {תאריך_לועזי} · {פרשה}", full))
    }

    @Test
    fun optionalSegmentsDisappearWhenAFactIsMissing() {
        val tpl = "מזל טוב {שם}[[ ליום הולדת {גיל}]]!"
        assertEquals("מזל טוב דנה ליום הולדת 50!", GreetingRenderer.render(tpl, full))
        assertEquals("מזל טוב דנה!", GreetingRenderer.render(tpl, full.copy(age = null)))
    }

    @Test
    fun missingPlaceholdersOutsideOptionalPartsVanishAndSpacingIsTidied() {
        assertEquals("שלום, ברכות!", GreetingRenderer.render("שלום {יחס} , ברכות !", full.copy(relation = null)))
        assertEquals("שורה אחת\n\nשורה שתיים", GreetingRenderer.render("שורה אחת\n\n\n\nשורה שתיים", full))
        assertEquals(setOf("גיל", "פרשה"), GreetingRenderer.missing("{שם} {גיל} {פרשה} {לא_ידוע}", full.copy(age = null, parsha = null)))
    }

    @Test
    fun whatsAppNumbers() {
        assertEquals("972501234567", PhoneNumbers.forWhatsApp("050-123-4567"))
        assertEquals("972501234567", PhoneNumbers.forWhatsApp("+972 50 123 4567"))
        assertEquals("972501234567", PhoneNumbers.forWhatsApp("00972501234567"))
        assertEquals("12125551234", PhoneNumbers.forWhatsApp("+1 (212) 555-1234"))
        assertEquals("972501234567", PhoneNumbers.forWhatsApp("972501234567"))
        assertEquals("447911123456", PhoneNumbers.forWhatsApp("07911 123456", defaultCountry = "44"))
        assertNull(PhoneNumbers.forWhatsApp("123"))
        assertNull(PhoneNumbers.forWhatsApp(null))
        assertEquals("+972501234567", PhoneNumbers.forDial("+972 (50) 123-4567"))
    }

    @Test
    fun everyEventTypeHasTemplatesAndTheyRenderWithoutLeftoverBraces() {
        EventType.entries.forEach { type ->
            assertTrue(type.name, GreetingCatalog.forEvent(type, null, null, emptyList()).isNotEmpty())
        }
        GreetingCatalog.BUILT_IN.forEach { tpl ->
            val text = GreetingRenderer.render(tpl.body, GreetingContext(name = "דנה"))
            assertTrue(tpl.name, !text.contains("{") && !text.contains("[[") && text.contains("דנה"))
        }
        assertEquals(GreetingCatalog.BUILT_IN.size, GreetingCatalog.BUILT_IN.map { it.id }.toSet().size)
    }

    @Test
    fun milestoneAndGenderTemplatesComeFirst() {
        val jubilee = GreetingCatalog.forEvent(EventType.BIRTHDAY, null, Milestones.forBirthday(50), emptyList()).first()
        assertEquals("יובל", jubilee.name)
        val gold = GreetingCatalog.forEvent(EventType.ANNIVERSARY, null, Milestones.forAnniversary(AnniversaryGift.GOLD.years), emptyList()).first()
        assertEquals("חתונת זהב", gold.name)
        assertEquals("בת מצווה", GreetingCatalog.forEvent(EventType.BAR_BAT_MITZVAH, Gender.FEMALE, null, emptyList()).first().name)
        val mine = GreetingTemplate(5, "שלי", "היי {שם}", EventType.BIRTHDAY)
        val list = GreetingCatalog.forEvent(EventType.BIRTHDAY, null, null, listOf(mine))
        assertEquals("שלי", list.first().name)
        assertTrue(list.indexOfFirst { it.milestone != null } > list.indexOfFirst { it.forType == EventType.BIRTHDAY && it.milestone == null })
    }
}

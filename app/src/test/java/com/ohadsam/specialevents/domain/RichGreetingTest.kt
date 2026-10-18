package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.domain.greetings.GreetingContext
import com.ohadsam.specialevents.domain.greetings.GreetingRenderer
import com.ohadsam.specialevents.domain.greetings.PlaceholderStyles
import com.ohadsam.specialevents.domain.greetings.RichText
import com.ohadsam.specialevents.domain.greetings.Span
import com.ohadsam.specialevents.domain.greetings.SpanStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichGreetingTest {
    private val ctx = GreetingContext(name = "דנה", age = 40)

    @Test
    fun spreadPutsTheSeparatorBetweenLettersButKeepsWordGaps() {
        assertEquals("ד-נ-ה", PlaceholderStyles.spread("דנה", "-"))
        assertEquals("ד.נ.ה ל.ו.י", PlaceholderStyles.spread("דנה לוי", "."))
        assertEquals("😀-🎂", PlaceholderStyles.spread("😀🎂", "-"))
    }

    @Test
    fun plainTextKeepsSpreadButDropsStyles() {
        assertEquals("מזל טוב ד-נ-ה!", GreetingRenderer.render("מזל טוב {שם|מפוזר:-|הדגש}!", ctx))
        assertEquals("מזל טוב דנה!", GreetingRenderer.render("מזל טוב {שם|הדגש|צבע:אדום}!", ctx))
        assertEquals("מזל טוב דנה!", GreetingRenderer.render("מזל טוב {שם}!", ctx))
    }

    @Test
    fun whatsAppGetsMarkersOnlyForWhatWhatsAppUnderstands() {
        val spans = GreetingRenderer.renderRich("מזל טוב {שם|הדגש}, בת {גיל|נטוי|קו_תחתון|צבע:כחול}!", ctx)
        assertEquals("מזל טוב *דנה*, בת _40_!", RichText.whatsApp(spans))
        assertEquals("מזל טוב דנה, בת 40!", RichText.plain(spans))
        assertEquals("*_דנה_*", RichText.whatsApp(listOf(Span("דנה", SpanStyle(bold = true, italic = true)))))
    }

    @Test
    fun whatsAppMarkersNeverHugSpaces() {
        assertEquals(" *דנה* ", RichText.whatsApp(listOf(Span(" דנה ", SpanStyle(bold = true)))))
        assertEquals("   ", RichText.whatsApp(listOf(Span("   ", SpanStyle(bold = true)))))
    }

    @Test
    fun htmlCarriesAllStylesAndEscapesText() {
        val html = RichText.html(GreetingRenderer.renderRich("שלום {שם|הדגש|קו_תחתון|צבע:אדום}\nמה נשמע", ctx))
        assertEquals("שלום <b><u><span style=\"color:#C62828\">דנה</span></u></b><br>מה נשמע", html)
        assertEquals("a &lt;b&gt; &amp; &quot;c&quot;", RichText.html(listOf(Span("a <b> & \"c\""))))
        assertFalse(RichText.html(listOf(Span("x", SpanStyle(color = "red; background:url(x)")))).contains("style"))
    }

    @Test
    fun hexColoursAreAcceptedAndBadOnesIgnored() {
        assertEquals("#112233", PlaceholderStyles.apply("x", listOf("צבע:#112233")).style.color)
        assertEquals(null, PlaceholderStyles.apply("x", listOf("צבע:#12")).style.color)
        assertEquals(null, PlaceholderStyles.apply("x", listOf("בלגן", "צבע:לא_קיים")).style.color)
    }

    @Test
    fun optionalPartsAndMissingUseThePlaceholderNameNotItsStyles() {
        val noAge = GreetingContext(name = "דנה")
        assertEquals("מזל טוב דנה!", GreetingRenderer.render("מזל טוב {שם}[[ ליום הולדת {גיל|הדגש}]]!", noAge))
        assertEquals(setOf("גיל"), GreetingRenderer.missing("{שם|הדגש} {גיל|מפוזר:-}", noAge))
    }

    @Test
    fun styledValuesNextToPunctuationStayTidy() {
        val spans = GreetingRenderer.renderRich("{שם|הדגש} , שלום", ctx)
        assertEquals("דנה, שלום", RichText.plain(spans))
        assertTrue(spans.first().style.bold)
    }

    @Test
    fun builderTokensRenderBackToTheChosenStyle() {
        val token = PlaceholderStyles.token("שם", PlaceholderStyles.Choice(spread = "-", bold = true, color = "אדום"))
        assertEquals("{שם|מפוזר:--|הדגש|צבע:אדום}".replace("--", "-"), token)
        val spans = GreetingRenderer.renderRich(token, ctx)
        assertEquals("ד-נ-ה", RichText.plain(spans))
        assertTrue(spans.single().style.bold)
        assertEquals("#C62828", spans.single().style.color)
        assertEquals("{גיל}", PlaceholderStyles.token("גיל", PlaceholderStyles.Choice()))
    }
}

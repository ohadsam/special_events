package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.domain.print.CardPage
import com.ohadsam.specialevents.domain.print.CardTheme
import com.ohadsam.specialevents.domain.print.ListSheet
import com.ohadsam.specialevents.domain.print.MonthLabels
import com.ohadsam.specialevents.domain.print.MonthSheetBuilder
import com.ohadsam.specialevents.domain.print.Paper
import com.ohadsam.specialevents.domain.print.PrintHtml
import com.ohadsam.specialevents.domain.print.PrintRow
import com.ohadsam.specialevents.domain.print.PrintSection
import com.ohadsam.specialevents.domain.print.PrintStyle
import com.ohadsam.specialevents.domain.print.YearLabels
import com.ohadsam.specialevents.domain.print.YearSheetBuilder
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintHtmlTest {
    private val style = PrintStyle(Paper.A5)

    @Test
    fun cardEscapesEverythingTheUserTypedAndKeepsLineBreaks() {
        val html = PrintHtml.card(CardPage("🎂", "מזל טוב <b>דנה</b>", "שורה 1\nשורה 2 & \"עוד\"", footer = "17 באוקטובר"), style)
        assertTrue(html.contains("dir=\"rtl\""))
        assertTrue(html.contains("size: A5"))
        assertTrue(html.contains("מזל טוב &lt;b&gt;דנה&lt;/b&gt;"))
        assertTrue(html.contains("שורה 1<br>שורה 2 &amp; &quot;עוד&quot;"))
        assertFalse(html.contains("<b>דנה</b>"))
        assertFalse(html.contains("<script"))
    }

    @Test
    fun cardOnlyAcceptsImageDataUrisAndRealColours() {
        val ok = PrintHtml.card(CardPage("🎉", "x", "y", photoDataUri = "data:image/jpeg;base64,AAAA"), style)
        assertTrue(ok.contains("class=\"photo\""))
        val bad = PrintHtml.card(CardPage("🎉", "x", "y", photoDataUri = "file:///etc/passwd"), style)
        assertFalse(bad.contains("passwd"))
        val css = PrintHtml.card(CardPage("🎉", "x", "y", theme = CardTheme.FESTIVE), PrintStyle(accent = "red; background:url(x)"))
        assertTrue(css.contains("#4A3AB8"))
        assertFalse(css.contains("url(x)"))
    }

    @Test
    fun listGroupsRowsUnderHeadingsInOrder() {
        val sheet = ListSheet(
            "ימי הולדת", "2026", listOf(PrintSection("אוקטובר", listOf(PrintRow("17.10", "דנה <x>", "יום הולדת", "גיל 46")))), listOf("תאריך", "שם", "סוג", "פרטים"),
        )
        val html = PrintHtml.list(sheet, PrintStyle())
        assertTrue(html.indexOf("אוקטובר") < html.indexOf("דנה"))
        assertTrue(html.contains("דנה &lt;x&gt;"))
        assertEquals(4, "<th>".toRegex().findAll(html).count())
    }

    @Test
    fun monthSheetIsAWeekGridWithEventsAndMarks() {
        val dana = SpecialEvent(id = 1, type = EventType.BIRTHDAY, title = "דנה", origin = OriginDate(CalendarBasis.GREGORIAN, 1980, 10, 17))
        val items = EventTimeline.between(listOf(dana), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 7), LocalDate.of(2026, 10, 1), true).groupBy { it.date }
        val labels = MonthLabels(
            weekdays = { listOf("א", "ב", "ג", "ד", "ה", "ו", "ש") },
            mark = { "חג" },
            entry = { "🎂 ${it.event.title}" },
            parsha = { "פרשת השבוע" },
        )
        val sheet = MonthSheetBuilder.build(LocalDate.of(2026, 10, 15), CalendarMode.DUAL, true, true, items, labels, "אוקטובר 2026", null)
        assertEquals(7, sheet.weeks.first().size)
        assertTrue(sheet.weeks.all { it.size == 7 })
        val day = sheet.weeks.flatten().first { it.date == LocalDate.of(2026, 10, 17) }
        assertEquals(listOf("🎂 דנה"), day.entries)
        assertTrue(day.shabbat)
        assertTrue("פרשת השבוע" in day.notes)
        assertEquals("17", day.primary)
        val html = PrintHtml.month(sheet, style)
        assertTrue(html.contains("🎂 דנה"))
        assertTrue(html.contains("class=\"shabbat\""))
        assertTrue(html.contains("אוקטובר 2026"))
    }

    @Test
    fun yearSheetHasTwelveMonthsWithMarkedDaysAndNotes() {
        val dana = SpecialEvent(id = 1, type = EventType.BIRTHDAY, title = "דנה <x>", origin = OriginDate(CalendarBasis.GREGORIAN, 1980, 10, 17))
        val items = EventTimeline.between(listOf(dana), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), true).groupBy { it.date }
        val labels = YearLabels(monthTitle = { "חודש $it" }, weekdays = { listOf("א", "ב", "ג", "ד", "ה", "ו", "ש") }, entry = { "🎂 ${it.event.title}" })
        val sheet = YearSheetBuilder.build(2026, true, true, items, labels, "לוח שנה 2026", null)
        assertEquals(12, sheet.months.size)
        assertTrue(sheet.months.all { m -> m.weeks.all { it.size == 7 } })
        val october = sheet.months[9]
        assertEquals(31, october.weeks.flatten().filterNotNull().size)
        assertTrue(october.weeks.flatten().filterNotNull().first { it.number == "17" }.marked)
        assertEquals(listOf("17 · 🎂 דנה <x>"), october.notes)
        assertTrue(sheet.months[0].notes.isEmpty())
        val html = PrintHtml.year(sheet, PrintStyle())
        assertTrue(html.contains("17 · 🎂 דנה &lt;x&gt;"))
        assertFalse(html.contains("<x>"))
        assertEquals(12, "<h2>".toRegex().findAll(html).count())
    }
}

package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.DateRules
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.calsync.CalendarImportPlanner
import com.ohadsam.specialevents.domain.calsync.DeviceEvent
import com.ohadsam.specialevents.domain.gcal.DesiredEntry
import com.ohadsam.specialevents.domain.gcal.ExistingEntry
import com.ohadsam.specialevents.domain.gcal.ExternalRename
import com.ohadsam.specialevents.domain.gcal.GcalMatcher
import com.ohadsam.specialevents.domain.gcal.GcalPlanner
import com.ohadsam.specialevents.domain.gcal.GcalTag
import com.ohadsam.specialevents.domain.gcal.GcalTagData
import com.ohadsam.specialevents.domain.gcal.GcalTitles
import com.ohadsam.specialevents.domain.gcal.ImportDecision
import com.ohadsam.specialevents.domain.gcal.MatchKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GcalTest {
    private val headline = CalendarImportPlanner.HEADLINE_PREFIX
    private val from = LocalDate.of(2026, 11, 26)

    private fun birthday(title: String = "דנה", month: Int = 10, day: Int = 17, year: Int? = 1980, mode: CalendarSyncMode = CalendarSyncMode.FOLLOW) =
        SpecialEvent(id = 1, type = EventType.BIRTHDAY, title = title, origin = OriginDate(CalendarBasis.GREGORIAN, year, month, day), calendarSync = mode)

    // ---- the mark

    @Test
    fun theMarkRoundTripsEveryField() {
        val hebrew = SpecialEvent(type = EventType.YAHRZEIT, title = "סבא", origin = OriginDate(CalendarBasis.HEBREW, null, 8, 30), recurrence = Recurrence.YEARLY)
        val parsed = GcalTag.parse(GcalTag.description(hebrew, headline))!!
        assertEquals(EventType.YAHRZEIT, parsed.type)
        assertEquals(CalendarBasis.HEBREW, parsed.basis)
        assertNull(parsed.year)
        assertEquals(8, parsed.month)
        assertEquals(30, parsed.day)
        assertTrue(parsed.yearly)
        val g = GcalTag.parse(GcalTag.line(birthday()))!!
        assertEquals(1980, g.year)
    }

    @Test
    fun foreignOrDamagedMarksAreRejected() {
        assertNull(GcalTag.parse(null))
        assertNull(GcalTag.parse("יום הולדת"))
        assertNull(GcalTag.parse("[SE1:NOPE:G:1980:10:17:Y]"))
        assertNull(GcalTag.parse("[SE1:BIRTHDAY:G:1980:13:17:Y]"))
        assertNull(GcalTag.parse("[SE1:BIRTHDAY:G:1980:4:31:Y]"))
        assertNotNull(GcalTag.parse("[SE1:BIRTHDAY:G:-:2:29:Y]"))
    }

    @Test
    fun userTextDropsTheHeadlineAndTheMark() {
        val text = "הערה שלי\n" + GcalTag.description(birthday(), headline)
        assertEquals("הערה שלי", GcalTag.userText(text, headline))
        assertNull(GcalTag.userText(GcalTag.description(birthday(), headline), headline))
    }

    // ---- what goes to the calendar

    @Test
    fun aGregorianBirthdayIsOneYearlyEntryAndHebrewOnesAreWrittenPerYear() {
        val greg = GcalPlanner.desired(listOf(birthday(mode = CalendarSyncMode.ON)), false, from, 3, DateRules(), headline)
        assertEquals(1, greg.size)
        assertEquals("FREQ=YEARLY", greg[0].rrule)
        assertEquals(LocalDate.of(1980, 10, 17), greg[0].date)
        val heb = SpecialEvent(id = 2, type = EventType.YAHRZEIT, title = "סבא", origin = OriginDate(CalendarBasis.HEBREW, 5700, 7, 10), calendarSync = CalendarSyncMode.ON)
        val years = GcalPlanner.desired(listOf(heb), false, from, 3, DateRules(), headline)
        assertEquals(3, years.size)
        assertTrue(years.all { it.rrule == null })
        assertEquals(3, years.map { it.date }.distinct().size)
    }

    @Test
    fun followUsesTheGlobalSwitchAndOffNeverSyncs() {
        val follow = birthday()
        val off = birthday(title = "רן", mode = CalendarSyncMode.OFF)
        assertTrue(GcalPlanner.desired(listOf(follow, off), false, from, 3, DateRules(), headline).isEmpty())
        val on = GcalPlanner.desired(listOf(follow, off), true, from, 3, DateRules(), headline)
        assertEquals(listOf("דנה"), on.map { it.title })
    }

    @Test
    fun aOneTimeEventInThePastIsNotWritten() {
        val past = SpecialEvent(type = EventType.CUSTOM, title = "טיול", origin = OriginDate(CalendarBasis.GREGORIAN, 2020, 5, 5), recurrence = Recurrence.ONCE, calendarSync = CalendarSyncMode.ON)
        val future = past.copy(title = "חתונה", origin = OriginDate(CalendarBasis.GREGORIAN, 2027, 5, 5))
        assertEquals(listOf("חתונה"), GcalPlanner.desired(listOf(past, future), false, from, 3, DateRules(), headline).map { it.title })
    }

    // ---- what changes in the calendar

    private fun existingOf(d: DesiredEntry, id: Long) = ExistingEntry(id, d.key, d.title, d.date)

    @Test
    fun anUpToDateCalendarNeedsNoChange() {
        val events = listOf(birthday(mode = CalendarSyncMode.ON))
        val want = GcalPlanner.desired(events, false, from, 3, DateRules(), headline)
        val diff = GcalPlanner.diff(events, false, want, want.mapIndexed { i, d -> existingOf(d, i + 1L) })
        assertTrue(diff.insert.isEmpty())
        assertTrue(diff.deleteIds.isEmpty())
        assertEquals(1, diff.unchanged)
    }

    @Test
    fun renamingAnEventReplacesItsEntry() {
        val old = birthday(title = "דנה", mode = CalendarSyncMode.ON)
        val oldEntry = GcalPlanner.desired(listOf(old), false, from, 3, DateRules(), headline).map { existingOf(it, 7) }
        val renamed = old.copy(title = "דנה כהן")
        val want = GcalPlanner.desired(listOf(renamed), false, from, 3, DateRules(), headline)
        val diff = GcalPlanner.diff(listOf(renamed), false, want, oldEntry)
        assertEquals(1, diff.insert.size)
        assertEquals(listOf(7L), diff.deleteIds)
    }

    @Test
    fun switchedOffArchivedAndDeletedEventsLeaveTheCalendarButFollowEventsStayWhenAutoSyncIsOff() {
        val kept = birthday(title = "א", mode = CalendarSyncMode.ON)
        val off = birthday(title = "ב", mode = CalendarSyncMode.OFF)
        val archived = birthday(title = "ג", mode = CalendarSyncMode.ON).copy(archived = true)
        val follow = birthday(title = "ד")
        val gone = birthday(title = "ה", mode = CalendarSyncMode.ON)
        val all = listOf(kept, off, archived, follow, gone)
        val entries = GcalPlanner.desired(all.map { it.copy(archived = false, calendarSync = CalendarSyncMode.ON) }, false, from, 3, DateRules(), headline)
        val existing = entries.mapIndexed { i, d -> existingOf(d, i + 1L) }
        val current = listOf(kept, off, archived, follow)
        val want = GcalPlanner.desired(current, false, from, 3, DateRules(), headline)
        val diff = GcalPlanner.diff(current, false, want, existing)
        // entries are in the order of `all`: א=1, ב=2, ג=3, ד=4, ה=5
        assertEquals(setOf(2L, 3L, 5L), diff.deleteIds.toSet())
        assertEquals(0, diff.insert.size)
    }

    @Test
    fun entriesWithoutOurMarkAreNeverTouchedAndDuplicatesAreCollapsed() {
        val events = listOf(birthday(mode = CalendarSyncMode.ON))
        val want = GcalPlanner.desired(events, false, from, 3, DateRules(), headline)
        val existing = listOf(existingOf(want[0], 1), existingOf(want[0], 2), ExistingEntry(3, null, "פגישה", from))
        val diff = GcalPlanner.diff(events, false, want, existing)
        assertEquals(listOf(2L), diff.deleteIds)
    }

    // ---- reading from the calendar

    @Test
    fun anEntryWithOurMarkBecomesExactlyTheEventItCameFrom() {
        val hebrew = SpecialEvent(type = EventType.YAHRZEIT, title = "סבא", origin = OriginDate(CalendarBasis.HEBREW, null, 8, 29), notes = "נר")
        val entry = DeviceEvent("גוגל", "סבא", LocalDate.of(2027, 11, 9), false, "נר\n" + GcalTag.description(hebrew, headline))
        val rebuilt = CalendarImportPlanner.toEvent(entry)
        assertEquals(EventType.YAHRZEIT, rebuilt.type)
        assertEquals(OriginDate(CalendarBasis.HEBREW, null, 8, 29), rebuilt.origin)
        assertEquals(Recurrence.YEARLY, rebuilt.recurrence)
        assertEquals("נר", rebuilt.notes)
    }

    // ---- finding duplicates and conflicts

    @Test
    fun theSameEventIsExactAndSimilarOnesAreLikelyOrConflicting() {
        val mine = listOf(birthday(title = "דנה כהן"))
        val exact = GcalMatcher.match(birthday(title = "דנה כהן"), mine)
        assertEquals(MatchKind.EXACT, exact.kind)
        val likely = GcalMatcher.match(birthday(title = "יום הולדת לדנה", year = null), mine)
        assertEquals(MatchKind.LIKELY, likely.kind)
        assertEquals(ImportDecision.MERGE, GcalMatcher.suggest(likely))
        val conflict = GcalMatcher.match(birthday(title = "דנה כהן", day = 18), mine)
        assertEquals(MatchKind.CONFLICT, conflict.kind)
        assertEquals(ImportDecision.IGNORE, GcalMatcher.suggest(conflict))
        assertTrue(conflict.differences.contains(com.ohadsam.specialevents.domain.gcal.Difference.DATE))
        assertEquals(MatchKind.NONE, GcalMatcher.match(birthday(title = "רן"), mine).kind)
    }

    @Test
    fun differentPeopleOnTheSameDayAreNotMatched() {
        assertFalse(GcalMatcher.sameName("דנה", "רונית"))
        assertTrue(GcalMatcher.sameName("דנה", "יום הולדת דנה כהן"))
        assertTrue(GcalMatcher.sameName("Dana's Birthday", "dana"))
    }

    @Test
    fun mergingTakesTheCalendarDateAndFillsGapsButKeepsTheRest() {
        val mine = birthday(title = "דנה", year = null).copy(id = 5, notes = null, colorArgb = 0x123456)
        val fromCal = birthday(title = "דנה כהן", day = 18, year = 1980).copy(notes = "מהיומן")
        val merged = GcalMatcher.merge(mine, fromCal)
        assertEquals(5L, merged.id)
        assertEquals("דנה כהן", merged.title)
        assertEquals(18, merged.origin.day)
        assertEquals(1980, merged.origin.year)
        assertEquals("מהיומן", merged.notes)
        assertEquals(0x123456, merged.colorArgb)
    }

    @Test
    fun reviewFlagsEverythingTheAppAlreadyHasAndKeepsMarkedEntriesEvenWhenOnlyRecurringIsOn() {
        val mine = listOf(birthday(title = "דנה"))
        val marked = DeviceEvent("גוגל", "אורי", LocalDate.of(2027, 3, 3), false, GcalTag.description(birthday(title = "אורי", month = 3, day = 3), headline))
        val plain = DeviceEvent("גוגל", "ישיבת צוות", LocalDate.of(2027, 3, 3), false, null)
        val dana = DeviceEvent("גוגל", "יום הולדת דנה", LocalDate.of(2026, 10, 17), true, null)
        val rows = CalendarImportPlanner.review(mine, listOf(marked, plain, dana), onlyRecurring = true)
        assertEquals(listOf("אורי", "יום הולדת דנה"), rows.map { it.event.title })
        assertEquals(ImportDecision.CREATE, rows[0].decision)
        assertTrue(rows[0].tagged)
        assertEquals(MatchKind.LIKELY, rows[1].match.kind)
    }

    // ---- two-way titles

    private fun entryFor(event: SpecialEvent, title: String) =
        ExistingEntry(1, null, title, LocalDate.of(2027, 10, 17), GcalTagData(event.type, event.origin.basis, event.origin.year, event.origin.month, event.origin.day, true))

    @Test
    fun calendarRenameIsTakenOverWhenTheAppTitleIsUnchanged() {
        val e = birthday("דנה")
        val renames = GcalPlanner.externalRenames(listOf(e), true, listOf(entryFor(e, "דנה כהן")), mapOf(e.id to "דנה"))
        assertEquals(listOf(ExternalRename(e.id, "דנה כהן")), renames)
    }

    @Test
    fun appRenameBeatsTheCalendarWhenBothChanged() {
        val e = birthday("דנה כהן")
        assertTrue(GcalPlanner.externalRenames(listOf(e), true, listOf(entryFor(e, "דני")), mapOf(e.id to "דנה")).isEmpty())
    }

    @Test
    fun noRenameWithoutAKnownLastTitleOrWhenTwoEventsShareTheDate() {
        val e = birthday("דנה")
        assertTrue(GcalPlanner.externalRenames(listOf(e), true, listOf(entryFor(e, "אחר")), emptyMap()).isEmpty())
        val twin = e.copy(id = 2, title = "רון")
        assertTrue(GcalPlanner.externalRenames(listOf(e, twin), true, listOf(entryFor(e, "אחר")), mapOf(1L to "דנה", 2L to "רון")).isEmpty())
    }

    @Test
    fun writtenTitlesRoundTripAndSkipEventsThatAreNotSynced() {
        val on = birthday("דנה", mode = CalendarSyncMode.ON)
        val off = birthday("רון", mode = CalendarSyncMode.OFF).copy(id = 2)
        val text = GcalTitles.encode(listOf(on, off), autoSync = false)
        assertEquals(mapOf(1L to "דנה"), GcalTitles.decode(text))
        assertEquals(emptyMap<Long, String>(), GcalTitles.decode(""))
    }
}

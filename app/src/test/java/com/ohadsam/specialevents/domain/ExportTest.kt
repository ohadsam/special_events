package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.backup.BackupCrypto
import com.ohadsam.specialevents.domain.backup.CsvExport
import com.ohadsam.specialevents.domain.backup.DecryptResult
import com.ohadsam.specialevents.domain.backup.ExportedDay
import com.ohadsam.specialevents.domain.backup.IcsExport
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTest {
    private fun event(title: String, notes: String? = null) =
        SpecialEvent(id = 7, type = EventType.BIRTHDAY, title = title, notes = notes, origin = OriginDate(CalendarBasis.GREGORIAN, 1980, 10, 17))

    @Test
    fun encryptedBackupRoundTripsAndHidesTheContent() {
        val secret = "{\"events\":[\"דנה לוי\"]}"
        val file = BackupCrypto.encrypt(secret, "סיסמה-123".toCharArray())
        assertTrue(BackupCrypto.isEncrypted(file))
        assertFalse(file.contains("דנה"))
        assertEquals(DecryptResult.Ok(secret), BackupCrypto.decrypt(file, "סיסמה-123".toCharArray()))
    }

    @Test
    fun wrongPasswordTamperedAndGarbageFilesAreRefused() {
        val file = BackupCrypto.encrypt("hello", "right-pass".toCharArray())
        assertEquals(DecryptResult.WrongPassword, BackupCrypto.decrypt(file, "wrong-pass".toCharArray()))
        val tampered = file.replace("\"data\":\"", "\"data\":\"AAAA")
        assertTrue(BackupCrypto.decrypt(tampered, "right-pass".toCharArray()) != DecryptResult.Ok("hello"))
        assertEquals(DecryptResult.Corrupt, BackupCrypto.decrypt("not json", "x".toCharArray()))
        assertEquals(DecryptResult.Corrupt, BackupCrypto.decrypt("""{"format":"special-events-encrypted","iterations":999999999999}""", "x".toCharArray()))
        assertFalse(BackupCrypto.isEncrypted("""{"format":"special-events-backup"}"""))
    }

    @Test
    fun twoEncryptionsOfTheSameTextDiffer() {
        val a = BackupCrypto.encrypt("same", "pw-123456".toCharArray())
        val b = BackupCrypto.encrypt("same", "pw-123456".toCharArray())
        assertTrue(a != b)
    }

    @Test
    fun icsHasCrlfAllDayEntriesAndEscapedText() {
        val ics = IcsExport.build(listOf(ExportedDay(event("דנה, לוי; ", notes = "שורה1\nשורה2"), LocalDate.of(2026, 10, 17), 46)), "20261018T100000Z", "0.13.0")
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261017\r\n"))
        assertTrue(ics.contains("DTEND;VALUE=DATE:20261018\r\n"))
        assertTrue(ics.contains("UID:event-7-2026-10-17@special-events"))
        val unfolded = ics.replace("\r\n ", "")
        assertTrue(unfolded.contains("SUMMARY:דנה\\, לוי\\;  (46)"))
        assertTrue(unfolded.contains("\\nשורה1\\nשורה2"))
    }

    @Test
    fun foldedLinesStayWithin75BytesAndKeepEveryCharacter() {
        val long = "א".repeat(120) + "🎂".repeat(10)
        val folded = IcsExport.fold("SUMMARY:$long")
        folded.split("\r\n").forEach { assertTrue(it.toByteArray().size <= 75) }
        assertEquals("SUMMARY:$long", folded.replace("\r\n ", ""))
    }

    @Test
    fun csvQuotesSeparatorsAndNeutralisesFormulas() {
        assertEquals("a", CsvExport.cell("a"))
        assertEquals("\"a,b\"", CsvExport.cell("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExport.cell("say \"hi\""))
        assertEquals("'=SUM(A1)", CsvExport.cell("=SUM(A1)"))
        assertEquals("+972501234567", CsvExport.cell("+972501234567"))
        val csv = CsvExport.build(listOf(event("דנה", notes = "שורה1\nשורה2")))
        assertTrue(csv.startsWith(CsvExport.BOM + CsvExport.HEADER.joinToString(",")))
        assertTrue(csv.contains("BIRTHDAY,דנה,,GREGORIAN,1980,10,17"))
        assertTrue(csv.contains("\"שורה1\nשורה2\""))
    }
}

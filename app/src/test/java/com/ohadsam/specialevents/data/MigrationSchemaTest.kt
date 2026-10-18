package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.Migrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Guards the hand-written migration SQL against drifting from what Room generates (the schema JSON is committed by CI after the first
 * build of a new version, so this test is skipped until that file exists).
 */
class MigrationSchemaTest {
    private fun createSqlOf(version: Int, table: String): Pair<String, List<String>>? {
        val dir = File("schemas/com.ohadsam.specialevents.data.db.AppDatabase")
        val file = File(dir, "$version.json").takeIf { it.exists() } ?: return null
        val text = file.readText()
        // The schema JSON is machine-written and regular; pull the entity block by its table name.
        val block = Regex("\"tableName\": \"$table\",\\s*\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").find(text) ?: return null
        val create = unescape(block.groupValues[1]).replace("\${TABLE_NAME}", table)
        val tail = text.substring(block.range.last)
        val end = tail.indexOf("\"foreignKeys\"").let { if (it < 0) tail.length else it }
        val indices = Regex("\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").findAll(tail.substring(0, end)).map { unescape(it.groupValues[1]).replace("\${TABLE_NAME}", table) }.toList()
        return create to indices
    }

    private fun unescape(s: String) = s.replace("\\\"", "\"").replace("\\\\", "\\")

    @Test
    fun reminderRulesMigrationMatchesRoomSchemaVersion2() {
        val schema = createSqlOf(2, "reminder_rules")
        assumeTrue("schemas/.../2.json not committed yet", schema != null)
        assertEquals(schema!!.first, Migrations.CREATE_REMINDER_RULES)
        assertEquals(listOf(Migrations.CREATE_REMINDER_RULES_INDEX), schema.second)
    }

    @Test
    fun eventsGainTheContactColumnsInVersion3() {
        val v2 = createSqlOf(2, "events")
        val v3 = createSqlOf(3, "events")
        assumeTrue("schemas/.../3.json not committed yet", v2 != null && v3 != null)
        val expected = v2!!.first.removeSuffix(")") + ", `contactPhone` TEXT, `contactEmail` TEXT)"
        assertEquals(expected, v3!!.first)
        assertTrue(Migrations.ADD_CONTACT_PHONE.endsWith("`contactPhone` TEXT") && Migrations.ADD_CONTACT_EMAIL.endsWith("`contactEmail` TEXT"))
    }
}

package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.Migrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class MigrationSchemaV7Test {
    private fun sql(version: Int, table: String): String? {
        val file = File("schemas/com.ohadsam.specialevents.data.db.AppDatabase/$version.json").takeIf { it.exists() } ?: return null
        val m = Regex("\"tableName\": \"$table\",\\s*\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").find(file.readText()) ?: return null
        return m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\").replace("\${TABLE_NAME}", table)
    }

    @Test
    fun checklistTableMatchesWhatRoomGenerates() {
        val generated = sql(7, "checklist_items")
        assumeTrue("schemas/.../7.json not committed yet", generated != null)
        assertEquals(generated, Migrations.CREATE_CHECKLIST_ITEMS)
    }

    @Test
    fun v7KeepsTheOlderTablesUnchanged() {
        val v6 = sql(6, "reminder_rules")
        val v7 = sql(7, "reminder_rules")
        assumeTrue("schemas not committed yet", v6 != null && v7 != null)
        assertEquals(v6, v7)
    }
}

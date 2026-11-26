package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.Migrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class MigrationSchemaV11Test {
    private fun createSql(version: Int, table: String): String? {
        val file = File("schemas/com.ohadsam.specialevents.data.db.AppDatabase/$version.json").takeIf { it.exists() } ?: return null
        val m = Regex("\"tableName\": \"$table\",\\s*\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").find(file.readText()) ?: return null
        return m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
    }

    @Test
    fun eventsGainTheCalendarSyncColumnAtTheEnd() {
        val v10 = createSql(10, "events")
        val v11 = createSql(11, "events")
        assumeTrue("schemas/.../11.json not committed yet", v10 != null && v11 != null)
        assertEquals(v10!!.removeSuffix(")") + ", `calendarSync` TEXT NOT NULL DEFAULT 'FOLLOW')", v11!!)
        assertEquals("ALTER TABLE `events` ADD COLUMN `calendarSync` TEXT NOT NULL DEFAULT 'FOLLOW'", Migrations.ADD_EVENT_CALENDAR_SYNC)
    }
}

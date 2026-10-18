package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.Migrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class MigrationSchemaV5Test {
    private fun createSql(version: Int): String? {
        val file = File("schemas/com.ohadsam.specialevents.data.db.AppDatabase/$version.json").takeIf { it.exists() } ?: return null
        val m = Regex("\"tableName\": \"reminder_rules\",\\s*\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").find(file.readText()) ?: return null
        return m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\").replace("\${TABLE_NAME}", "reminder_rules")
    }

    @Test
    fun ruleTimeModeColumnIsAppendedLikeRoomExpects() {
        val v4 = createSql(4)
        val v5 = createSql(5)
        assumeTrue("schemas/.../5.json not committed yet", v4 != null && v5 != null)
        val marker = ", FOREIGN KEY"
        val expected = v4!!.replaceFirst(marker, ", `timeMode` TEXT NOT NULL DEFAULT 'FIXED'$marker")
        assertEquals(expected, v5!!)
        assertEquals("ALTER TABLE `reminder_rules` ADD COLUMN `timeMode` TEXT NOT NULL DEFAULT 'FIXED'", Migrations.ADD_RULE_TIME_MODE)
    }
}

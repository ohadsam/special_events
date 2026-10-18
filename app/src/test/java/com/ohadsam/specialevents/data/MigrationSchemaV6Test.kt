package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.Migrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class MigrationSchemaV6Test {
    private fun createSql(version: Int): String? {
        val file = File("schemas/com.ohadsam.specialevents.data.db.AppDatabase/$version.json").takeIf { it.exists() } ?: return null
        val m = Regex("\"tableName\": \"reminder_rules\",\\s*\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").find(file.readText()) ?: return null
        return m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\").replace("\${TABLE_NAME}", "reminder_rules")
    }

    @Test
    fun autoSendColumnsAreAppendedLikeRoomExpects() {
        val v5 = createSql(5)
        val v6 = createSql(6)
        assumeTrue("schemas/.../6.json not committed yet", v5 != null && v6 != null)
        val marker = ", FOREIGN KEY"
        val expected = v5!!.replaceFirst(marker, ", `autoSend` INTEGER NOT NULL DEFAULT 0, `templateId` INTEGER$marker")
        assertEquals(expected, v6!!)
        assertEquals("ALTER TABLE `reminder_rules` ADD COLUMN `autoSend` INTEGER NOT NULL DEFAULT 0", Migrations.ADD_RULE_AUTO_SEND)
        assertEquals("ALTER TABLE `reminder_rules` ADD COLUMN `templateId` INTEGER", Migrations.ADD_RULE_TEMPLATE_ID)
    }
}

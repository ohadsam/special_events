package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.Migrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class MigrationSchemaV10Test {
    private fun createSql(version: Int, table: String): String? {
        val file = File("schemas/com.ohadsam.specialevents.data.db.AppDatabase/$version.json").takeIf { it.exists() } ?: return null
        val m = Regex("\"tableName\": \"$table\",\\s*\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").find(file.readText()) ?: return null
        return m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
    }

    @Test
    fun ruleAndHistoryColumnsAreAppendedLikeRoomExpects() {
        val rules8 = createSql(9, "reminder_rules")
        val rules10 = createSql(10, "reminder_rules")
        val sent9 = createSql(9, "greetings_sent")
        val sent10 = createSql(10, "greetings_sent")
        assumeTrue("schemas/.../10.json not committed yet", rules8 != null && rules10 != null && sent9 != null && sent10 != null)
        val marker = ", FOREIGN KEY"
        val columns = ", `toPerson` INTEGER NOT NULL DEFAULT 1, `toSelf` INTEGER NOT NULL DEFAULT 0, `toGroupIds` TEXT, `toExtra` TEXT"
        assertEquals(rules8!!.replaceFirst(marker, columns + marker), rules10!!)
        assertEquals(sent9!!.replaceFirst(marker, ", `recipient` TEXT$marker"), sent10!!)
        assertEquals("ALTER TABLE `reminder_rules` ADD COLUMN `toPerson` INTEGER NOT NULL DEFAULT 1", Migrations.ADD_RULE_TO_PERSON)
        assertEquals("ALTER TABLE `greetings_sent` ADD COLUMN `recipient` TEXT", Migrations.ADD_SENT_RECIPIENT)
    }
}

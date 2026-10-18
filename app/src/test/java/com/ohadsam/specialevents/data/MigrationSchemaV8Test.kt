package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.Migrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class MigrationSchemaV8Test {
    private fun sql(version: Int, table: String): String? {
        val file = File("schemas/com.ohadsam.specialevents.data.db.AppDatabase/$version.json").takeIf { it.exists() } ?: return null
        val m = Regex("\"tableName\": \"$table\",\\s*\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").find(file.readText()) ?: return null
        return m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\").replace("\${TABLE_NAME}", table)
    }

    @Test
    fun groupTablesMatchWhatRoomGenerates() {
        val groups = sql(8, "event_groups")
        val members = sql(8, "event_group_members")
        assumeTrue("schemas/.../8.json not committed yet", groups != null && members != null)
        assertEquals(groups, Migrations.CREATE_EVENT_GROUPS)
        assertEquals(members, Migrations.CREATE_EVENT_GROUP_MEMBERS)
    }
}

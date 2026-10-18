package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.Migrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class MigrationSchemaV9Test {
    private fun createSql(version: Int): String? {
        val file = File("schemas/com.ohadsam.specialevents.data.db.AppDatabase/$version.json").takeIf { it.exists() } ?: return null
        val m = Regex("\"tableName\": \"greeting_templates\",\\s*\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").find(file.readText()) ?: return null
        return m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
    }

    @Test
    fun templateColumnsAreAppendedLikeRoomExpects() {
        val v8 = createSql(8)
        val v9 = createSql(9)
        assumeTrue("schemas/.../9.json not committed yet", v8 != null && v9 != null)
        assertEquals(v8!!.removeSuffix(")") + ", `folder` TEXT, `tags` TEXT, `groupIds` TEXT)", v9!!)
        assertEquals("ALTER TABLE `greeting_templates` ADD COLUMN `folder` TEXT", Migrations.ADD_TEMPLATE_FOLDER)
        assertEquals("ALTER TABLE `greeting_templates` ADD COLUMN `tags` TEXT", Migrations.ADD_TEMPLATE_TAGS)
        assertEquals("ALTER TABLE `greeting_templates` ADD COLUMN `groupIds` TEXT", Migrations.ADD_TEMPLATE_GROUPS)
    }
}

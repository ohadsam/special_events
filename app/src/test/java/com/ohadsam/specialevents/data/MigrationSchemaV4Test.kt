package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.Migrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Version 4 tables must match what Room generated (see MigrationSchemaTest for why). */
class MigrationSchemaV4Test {
    private fun entityBlock(table: String): Pair<String, List<String>>? {
        val file = File("schemas/com.ohadsam.specialevents.data.db.AppDatabase/4.json").takeIf { it.exists() } ?: return null
        val text = file.readText()
        val m = Regex("\"tableName\": \"$table\",\\s*\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").find(text) ?: return null
        val create = m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\").replace("\${TABLE_NAME}", table)
        val tail = text.substring(m.range.last)
        val end = tail.indexOf("\"foreignKeys\"").let { if (it < 0) tail.length else it }
        val indices = Regex("\"createSql\": \"((?:[^\"\\\\]|\\\\.)*)\"").findAll(tail.substring(0, end)).map { it.groupValues[1].replace("\\\"", "\"").replace("\${TABLE_NAME}", table) }.toList()
        return create to indices
    }

    @Test
    fun greetingTablesMatchRoomSchemaVersion4() {
        val templates = entityBlock("greeting_templates")
        val sent = entityBlock("greetings_sent")
        assumeTrue("schemas/.../4.json not committed yet", templates != null && sent != null)
        assertEquals(templates!!.first, Migrations.CREATE_GREETING_TEMPLATES)
        assertEquals(sent!!.first, Migrations.CREATE_GREETINGS_SENT)
        assertEquals(listOf(Migrations.CREATE_GREETINGS_SENT_INDEX), sent.second)
    }
}

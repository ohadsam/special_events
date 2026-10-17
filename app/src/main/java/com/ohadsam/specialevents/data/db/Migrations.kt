package com.ohadsam.specialevents.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The SQL below must equal what Room generates for the new schema (`app/schemas/.../<version>.json`), or Room refuses to open the
 * database after the upgrade. `MigrationSchemaTest` compares them with the committed schema file.
 */
object Migrations {
    const val CREATE_REMINDER_RULES =
        "CREATE TABLE IF NOT EXISTS `reminder_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` INTEGER NOT NULL, " +
            "`offsetValue` INTEGER NOT NULL, `offsetUnit` TEXT NOT NULL, `timeMinutes` INTEGER NOT NULL, `channel` TEXT NOT NULL, " +
            "`shabbatPolicy` TEXT NOT NULL, `profileKey` TEXT NOT NULL, `minTier` TEXT NOT NULL, `enabled` INTEGER NOT NULL, " +
            "`origin` TEXT NOT NULL, FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    const val CREATE_REMINDER_RULES_INDEX = "CREATE INDEX IF NOT EXISTS `index_reminder_rules_eventId` ON `reminder_rules` (`eventId`)"

    val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_REMINDER_RULES)
            db.execSQL(CREATE_REMINDER_RULES_INDEX)
        }
    }

    val ALL: Array<Migration> = arrayOf(V1_TO_V2)
}

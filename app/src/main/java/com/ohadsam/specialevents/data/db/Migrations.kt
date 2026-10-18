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

    const val ADD_CONTACT_PHONE = "ALTER TABLE `events` ADD COLUMN `contactPhone` TEXT"
    const val ADD_CONTACT_EMAIL = "ALTER TABLE `events` ADD COLUMN `contactEmail` TEXT"

    val V2_TO_V3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(ADD_CONTACT_PHONE)
            db.execSQL(ADD_CONTACT_EMAIL)
        }
    }

    const val CREATE_GREETING_TEMPLATES =
        "CREATE TABLE IF NOT EXISTS `greeting_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
            "`body` TEXT NOT NULL, `forType` TEXT, `createdAt` INTEGER NOT NULL)"

    const val CREATE_GREETINGS_SENT =
        "CREATE TABLE IF NOT EXISTS `greetings_sent` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` INTEGER NOT NULL, " +
            "`occurrenceDay` INTEGER NOT NULL, `channel` TEXT NOT NULL, `sentAt` INTEGER NOT NULL, `preview` TEXT NOT NULL, " +
            "FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    const val CREATE_GREETINGS_SENT_INDEX = "CREATE INDEX IF NOT EXISTS `index_greetings_sent_eventId` ON `greetings_sent` (`eventId`)"

    val V3_TO_V4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_GREETING_TEMPLATES)
            db.execSQL(CREATE_GREETINGS_SENT)
            db.execSQL(CREATE_GREETINGS_SENT_INDEX)
        }
    }

    const val ADD_RULE_TIME_MODE = "ALTER TABLE `reminder_rules` ADD COLUMN `timeMode` TEXT NOT NULL DEFAULT 'FIXED'"

    val V4_TO_V5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(ADD_RULE_TIME_MODE)
        }
    }

    const val ADD_RULE_AUTO_SEND = "ALTER TABLE `reminder_rules` ADD COLUMN `autoSend` INTEGER NOT NULL DEFAULT 0"
    const val ADD_RULE_TEMPLATE_ID = "ALTER TABLE `reminder_rules` ADD COLUMN `templateId` INTEGER"

    val V5_TO_V6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(ADD_RULE_AUTO_SEND)
            db.execSQL(ADD_RULE_TEMPLATE_ID)
        }
    }

    val ALL: Array<Migration> = arrayOf(V1_TO_V2, V2_TO_V3, V3_TO_V4, V4_TO_V5, V5_TO_V6)
}

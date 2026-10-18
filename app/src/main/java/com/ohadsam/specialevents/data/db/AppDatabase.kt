package com.ohadsam.specialevents.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version 9 (v2 added reminder rules, v3 the contact phone/email of an event, v4 greeting templates and the greeting history, v5 the time mode of a reminder rule, v6 automatic sending, v7 checklist and gifts, v8 groups, v9 template folders/tags/groups). Any schema change = bump `version` + a `Migration` in the SAME commit (release builds never wipe data), and
 * commit the regenerated JSON files in `app/schemas`. Tables are added by the batch that needs them (reminders, greetings, ...).
 */
@Database(entities = [EventEntity::class, TagEntity::class, EventTagCrossRef::class, ReminderRuleEntity::class, GreetingTemplateEntity::class, GreetingSentEntity::class, ChecklistItemEntity::class, EventGroupEntity::class, EventGroupMember::class], version = 9, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    abstract fun tagDao(): TagDao

    abstract fun reminderDao(): ReminderDao

    abstract fun greetingDao(): GreetingDao

    abstract fun checklistDao(): ChecklistDao

    abstract fun groupDao(): GroupDao

    companion object {
        const val NAME = "special_events.db"

        fun create(context: Context): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, NAME).addMigrations(*Migrations.ALL).build()
    }
}

package com.ohadsam.specialevents.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version 1. Any schema change = bump `version` + a `Migration` in the SAME commit (release builds never wipe data), and
 * commit the regenerated JSON files in `app/schemas`. Tables are added by the batch that needs them (reminders, greetings, ...).
 */
@Database(entities = [EventEntity::class, TagEntity::class, EventTagCrossRef::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    abstract fun tagDao(): TagDao

    companion object {
        const val NAME = "special_events.db"

        fun create(context: Context): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}

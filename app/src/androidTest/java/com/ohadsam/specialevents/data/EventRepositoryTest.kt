package com.ohadsam.specialevents.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.data.db.AppDatabase
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on the emulator (ui-tests workflow): real Room, in-memory. */
@RunWith(AndroidJUnit4::class)
class EventRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: EventRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = EventRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun birthday(title: String, vararg tags: String) = SpecialEvent(
        type = EventType.BIRTHDAY,
        title = title,
        origin = OriginDate(CalendarBasis.GREGORIAN, 1980, 5, 4),
        tags = tags.map { Tag(name = it) },
    )

    @Test
    fun saveReadUpdateDelete() = runBlocking {
        val id = repo.save(birthday("דנה", "משפחה"))
        val saved = checkNotNull(repo.get(id))
        assertEquals("דנה", saved.title)
        assertEquals(listOf("משפחה"), saved.tags.map { it.name })
        repo.save(saved.copy(title = "דנה כהן", tags = emptyList()))
        assertEquals(emptyList<Tag>(), repo.get(id)?.tags)
        assertEquals(1, repo.allEvents.first().size)
        repo.delete(id)
        assertNull(repo.get(id))
    }

    @Test
    fun tagsAreSharedByNameIgnoringCase() = runBlocking {
        repo.save(birthday("א", "Work"))
        repo.save(birthday("ב", "work"))
        assertEquals(1, repo.tags.first().size)
    }
}

package com.ohadsam.specialevents.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ohadsam.specialevents.calendar.AdarInLeapYear
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {
    private lateinit var dir: File
    private lateinit var scope: CoroutineScope
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "settings-test-${System.nanoTime()}").apply { mkdirs() }
        scope = CoroutineScope(Dispatchers.IO + Job())
        repo = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(dir, "s.preferences_pb") }))
    }

    @After
    fun tearDown() {
        scope.cancel()
        dir.deleteRecursively()
    }

    @Test
    fun defaultsWhenNothingStored() = runBlocking {
        assertEquals(AppSettings(), repo.settings.first())
    }

    @Test
    fun updatesPersistAndResetRestoresDefaults() = runBlocking {
        repo.update { it.copy(calendarMode = CalendarMode.HEBREW, inIsrael = false, dateRules = it.dateRules.copy(adar = AdarInLeapYear.ADAR_I)) }
        val s = repo.settings.first()
        assertEquals(CalendarMode.HEBREW, s.calendarMode)
        assertEquals(false, s.inIsrael)
        assertEquals(AdarInLeapYear.ADAR_I, s.dateRules.adar)
        repo.reset()
        assertEquals(AppSettings(), repo.settings.first())
    }
}

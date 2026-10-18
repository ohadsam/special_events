package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.ui.help.HelpTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HelpTopicTest {
    @Test
    fun keysAreUniqueAndFindable() {
        assertEquals(HelpTopic.entries.size, HelpTopic.entries.map { it.key }.toSet().size)
        HelpTopic.entries.forEach { assertEquals(it, HelpTopic.byKey(it.key)) }
        assertNull(HelpTopic.byKey("nope"))
        assertNull(HelpTopic.byKey(null))
    }
}

class AppLockPolicyTest {
    private val sec = com.ohadsam.specialevents.security.AppLockPolicy

    @org.junit.Test
    fun doesNotLockWhenOffOrWhenThePhoneHasNoLock() {
        org.junit.Assert.assertFalse(sec.shouldLock(enabled = false, deviceSecure = true, backgroundedAtMs = null, nowMs = 0, timeoutSeconds = 0))
        org.junit.Assert.assertFalse(sec.shouldLock(enabled = true, deviceSecure = false, backgroundedAtMs = null, nowMs = 0, timeoutSeconds = 0))
    }

    @org.junit.Test
    fun locksOnColdStartAndAfterTheTimeout() {
        org.junit.Assert.assertTrue(sec.shouldLock(true, true, null, 5_000, 60))
        org.junit.Assert.assertFalse(sec.shouldLock(true, true, 10_000, 40_000, 60))
        org.junit.Assert.assertTrue(sec.shouldLock(true, true, 10_000, 70_000, 60))
        org.junit.Assert.assertTrue(sec.shouldLock(true, true, 10_000, 10_000, 0))
    }

    @org.junit.Test
    fun aClockThatWentBackwardsLocks() {
        org.junit.Assert.assertTrue(sec.shouldLock(true, true, 50_000, 10_000, 300))
    }
}

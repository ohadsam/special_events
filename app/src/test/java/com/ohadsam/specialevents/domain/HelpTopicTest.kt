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

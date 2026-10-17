package com.ohadsam.specialevents.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconSuggesterTest {
    @Test
    fun titleKeywordBeatsTypeDefault() {
        assertEquals("💍", IconSuggester.suggest(EventType.FAMILY, "חתונה של דנה", "👪"))
        assertEquals("🩺", IconSuggester.suggest(EventType.CUSTOM, "תור לרופא", "⭐"))
    }

    @Test
    fun fallsBackToTheTypeDefault() {
        assertEquals("🎂", IconSuggester.suggest(EventType.BIRTHDAY, "דנה", "🎂"))
    }

    @Test
    fun optionsStartWithTheSuggestionAndHaveNoDuplicates() {
        val options = IconSuggester.options(EventType.ANNIVERSARY, "חתונה", "💍")
        assertEquals("💍", options.first())
        assertEquals(options.size, options.toSet().size)
        assertTrue(options.size >= 5)
    }
}

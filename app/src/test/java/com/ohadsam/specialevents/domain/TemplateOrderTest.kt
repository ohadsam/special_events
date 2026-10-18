package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.domain.greetings.GreetingCatalog
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateOrderTest {
    private val a = GreetingTemplate(1, "א", "x")
    private val b = GreetingTemplate(2, "ב", "x", groupIds = setOf(10))
    private val c = GreetingTemplate(3, "ג", "x", groupIds = setOf(20))

    @Test
    fun templatesMadeForTheEventsGroupsComeFirstKeepingOtherOrder() {
        assertEquals(listOf(2L, 1L, 3L), GreetingCatalog.preferGroups(listOf(a, b, c), setOf(10)).map { it.id })
        assertEquals(listOf(2L, 3L, 1L), GreetingCatalog.preferGroups(listOf(a, b, c), setOf(10, 20)).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), GreetingCatalog.preferGroups(listOf(a, b, c), emptySet()).map { it.id })
    }
}

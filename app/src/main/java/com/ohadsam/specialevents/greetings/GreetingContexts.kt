package com.ohadsam.specialevents.greetings

import com.ohadsam.specialevents.calendar.ParshaNames
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.greetings.GreetingContext
import com.ohadsam.specialevents.ui.common.gregorianLong

/** Fills the placeholders of a greeting for one occurrence; shared by the greeting screen and the automatic sender. */
object GreetingContexts {
    fun of(event: SpecialEvent, item: UpcomingItem) = GreetingContext(
        name = event.personName ?: event.title,
        age = if (event.type == EventType.BIRTHDAY) item.number else null,
        years = if (event.type != EventType.BIRTHDAY) item.number else null,
        relation = event.relation,
        hebrewDate = item.hebrew.format(),
        gregorianDate = gregorianLong(item.date),
        parsha = item.mitzvah?.parsha?.let { ParshaNames.label(it) },
    )
}

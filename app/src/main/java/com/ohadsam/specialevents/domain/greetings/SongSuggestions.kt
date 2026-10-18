package com.ohadsam.specialevents.domain.greetings

import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender

/** A song idea: what to search for. Opened as a YouTube search, so no account, key or network code lives in the app. */
data class SongIdea(val label: String, val query: String) {
    val url: String get() = "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(query, "UTF-8")
}

object SongSuggestions {
    /** [milestone] is a key from `GreetingCatalog.milestoneKey` (e.g. "age:JUBILEE_50"). */
    fun forEvent(type: EventType, gender: Gender?, milestone: String?): List<SongIdea> = when (type) {
        EventType.BIRTHDAY -> listOfNotNull(
            if (milestone == "age:JUBILEE_50") SongIdea("שיר ליובל", "שיר ליום הולדת 50") else null,
            SongIdea("יום הולדת שמח", "יום הולדת שמח שיר"),
            SongIdea("שיר יום הולדת מרגש", "שיר יום הולדת מרגש"),
            SongIdea("Happy Birthday", "Happy Birthday song"),
        )
        EventType.ANNIVERSARY -> listOfNotNull(
            if (milestone?.startsWith("gift:") == true) SongIdea("שיר לחתונת ${giftName(milestone)}", "שיר לחתונת ${giftName(milestone)}") else null,
            SongIdea("שיר ליום נישואין", "שיר ליום נישואין"),
            SongIdea("שירי אהבה", "שירי אהבה רומנטיים בעברית"),
        )
        EventType.BAR_BAT_MITZVAH -> listOf(
            SongIdea(if (gender == Gender.FEMALE) "שיר לבת מצווה" else "שיר לבר מצווה", if (gender == Gender.FEMALE) "שיר לבת מצווה" else "שיר לבר מצווה"),
            SongIdea("שירי שמחה", "שירי שמחה חסידיים"),
        )
        EventType.YAHRZEIT -> listOf(SongIdea("אל מלא רחמים", "אל מלא רחמים"), SongIdea("שירי זיכרון", "שירי זיכרון והנצחה"))
        EventType.WORK -> listOf(SongIdea("שיר הצלחה", "שיר על הצלחה והתמדה"))
        EventType.FAMILY, EventType.CUSTOM -> listOf(SongIdea("שיר שמח", "שירי שמחה בעברית"))
    }

    private fun giftName(key: String) = when (key.removePrefix("gift:")) {
        "SILVER" -> "כסף"
        "GOLD" -> "זהב"
        "DIAMOND" -> "יהלום"
        else -> "נישואין"
    }
}

/** Whether a reminder may send its message on its own, and which template it uses. */
enum class AutoSendDecision { SEND, ALREADY_GREETED, PROMPT_INSTEAD }

object AutoSend {
    /**
     * Sending by itself is only allowed when the user asked for it on this rule, the permission is there, there is a phone number to
     * send to, and this occurrence was not greeted already; otherwise the user just gets the normal "time to greet" prompt.
     */
    fun decide(autoSend: Boolean, isSms: Boolean, hasPermission: Boolean, phone: String?, alreadyGreeted: Boolean): AutoSendDecision = when {
        !autoSend || !isSms || !hasPermission || PhoneNumbers.forDial(phone) == null -> AutoSendDecision.PROMPT_INSTEAD
        alreadyGreeted -> AutoSendDecision.ALREADY_GREETED
        else -> AutoSendDecision.SEND
    }

    /** The chosen template if it still exists, otherwise the best match for the event. */
    fun chooseTemplate(templateId: Long?, candidates: List<GreetingTemplate>): GreetingTemplate? =
        candidates.firstOrNull { it.id == templateId } ?: candidates.firstOrNull()
}

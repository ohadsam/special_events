package com.ohadsam.specialevents.domain

/** Suggests emoji icons for an event from its type and title (offline keyword table; the user can always pick another). */
object IconSuggester {
    private val KEYWORDS: List<Pair<List<String>, String>> = listOf(
        listOf("חתונה", "נישואין", "נשואין", "טבעת") to "💍",
        listOf("בר מצווה", "בת מצווה", "בר-מצווה", "בת-מצווה") to "📜",
        listOf("ברית", "בריתה", "תינוק", "לידה", "נולד") to "👶",
        listOf("אזכרה", "פטירה", "נר נשמה", "יארצייט") to "🕯️",
        listOf("טיול", "חופשה", "נסיעה", "טיסה") to "✈️",
        listOf("פגישה", "ישיבה", "ראיון") to "🤝",
        listOf("רופא", "בדיקה", "תור", "מרפאה") to "🩺",
        listOf("בוגר", "סיום", "תואר", "טקס") to "🎓",
        listOf("מסיבה", "חגיגה", "אירוע") to "🥳",
        listOf("חג", "פסח", "סוכות", "שבועות", "חנוכה", "פורים") to "🕎",
        listOf("עבודה", "משרד", "חברה") to "💼",
    )

    private val BY_TYPE: Map<EventType, List<String>> = mapOf(
        EventType.BIRTHDAY to listOf("🎂", "🎈", "🎁", "🥳", "🍰", "🎉", "👑", "🧁"),
        EventType.ANNIVERSARY to listOf("💍", "❤️", "💑", "🥂", "🌹", "💐", "💒", "✨"),
        EventType.YAHRZEIT to listOf("🕯️", "🕊️", "📖", "🙏", "🌿", "🕍", "✡️", "💙"),
        EventType.BAR_BAT_MITZVAH to listOf("📜", "🕍", "✡️", "🎉", "📖", "🎁", "🥳", "🌟"),
        EventType.WORK to listOf("💼", "🤝", "📅", "🏢", "💡", "🎯", "📊", "☕"),
        EventType.FAMILY to listOf("👪", "🏡", "🧸", "👶", "🍼", "🎒", "🌳", "❤️"),
        EventType.CUSTOM to listOf("⭐", "📌", "🎉", "🔔", "🎈", "🌈", "🎁", "📍"),
    )

    /** The best single guess: a keyword in the title wins over the type's default. */
    fun suggest(type: EventType, title: String, defaultForType: String): String {
        val t = TextSearch.normalize(title)
        return KEYWORDS.firstOrNull { (words, _) -> words.any { t.contains(TextSearch.normalize(it)) } }?.second ?: defaultForType
    }

    /** A short list to offer in the editor: the title match first, then the type's usual icons. */
    fun options(type: EventType, title: String, defaultForType: String): List<String> =
        (listOf(suggest(type, title, defaultForType)) + BY_TYPE.getValue(type)).distinct()
}

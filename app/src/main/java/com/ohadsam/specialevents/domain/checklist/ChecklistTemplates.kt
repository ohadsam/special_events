package com.ohadsam.specialevents.domain.checklist

import com.ohadsam.specialevents.domain.EventType

/** Starting lists per event type; the user adds, removes and renames freely afterwards. */
object ChecklistTemplates {
    private fun task(title: String, daysBefore: Int?) = ChecklistItem(title = title, daysBefore = daysBefore)

    private fun gift(title: String, daysBefore: Int?) = ChecklistItem(kind = ChecklistKind.GIFT, title = title, daysBefore = daysBefore)

    fun forType(type: EventType): List<ChecklistItem> = when (type) {
        EventType.BIRTHDAY -> listOf(gift("מתנה", 10), task("כרטיס ברכה", 3), task("לשלוח ברכה", 0))
        EventType.ANNIVERSARY -> listOf(gift("מתנה", 14), task("להזמין מסעדה", 10), task("פרחים", 1), task("לשלוח ברכה", 0))
        EventType.BAR_BAT_MITZVAH -> listOf(
            gift("מתנה", 14),
            task("לאשר הגעה", 21),
            task("סידור הסעה / לינה", 14),
            task("בגדים לאירוע", 7),
            task("שיק / מעטפה", 1),
            task("לשלוח ברכה", 0),
        )
        EventType.YAHRZEIT -> listOf(
            task("לתאם מניין לתפילה", 5),
            task("נר נשמה", 1),
            task("עלייה לתורה / לימוד לעילוי נשמה", 1),
            task("לעדכן את המשפחה", 3),
        )
        EventType.WORK -> listOf(task("להכין חומרים", 7), task("לשלוח הזמנה / תזכורת", 3), task("סיכום ומעקב", -1))
        EventType.FAMILY -> listOf(gift("מתנה", 10), task("תיאום עם המשפחה", 7), task("לשלוח ברכה", 0))
        EventType.CUSTOM -> listOf(task("להתכונן", 3))
    }
}

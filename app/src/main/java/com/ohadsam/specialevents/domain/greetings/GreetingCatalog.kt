package com.ohadsam.specialevents.domain.greetings

import com.ohadsam.specialevents.calendar.AnniversaryGift
import com.ohadsam.specialevents.calendar.Milestone
import com.ohadsam.specialevents.calendar.SpecialAge
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender

/**
 * A greeting text with placeholders (see [GreetingRenderer]). Built-in templates have negative ids; the user's own are stored in
 * the database. [forType] / [gender] / [milestone] only decide what is offered first.
 */
data class GreetingTemplate(
    val id: Long,
    val name: String,
    val body: String,
    val forType: EventType? = null,
    val gender: Gender? = null,
    /** e.g. "age:JUBILEE_50", "age:DECADE", "gift:SILVER"; null = ordinary. */
    val milestone: String? = null,
    val builtIn: Boolean = false,
    /** Where the user filed it, and how they labelled it; [groupIds] are the groups it is meant for (they are offered it first). */
    val folder: String? = null,
    val tags: List<String> = emptyList(),
    val groupIds: Set<Long> = emptySet(),
)

object GreetingCatalog {
    private var nextId = -1L

    private fun t(name: String, body: String, type: EventType?, gender: Gender? = null, milestone: String? = null) =
        GreetingTemplate(nextId--, name, body, type, gender, milestone, builtIn = true)

    val BUILT_IN: List<GreetingTemplate> = listOf(
        t("חמה", "מזל טוב {שם}![[ ליום הולדת {גיל}]] 🎂\nשתזכו לבריאות, לאושר ולשמחה, ולהגשים את כל החלומות. שנה מבורכת!", EventType.BIRTHDAY),
        t("קצרה", "יום הולדת שמח {שם}! 🎉 שיהיה לך יום מיוחד ושנה מלאה בדברים טובים.", EventType.BIRTHDAY),
        t("משפחתית", "{שם} היקר/ה,\nמאחלים לך יום הולדת שמח[[ ושנת ה-{גיל} מבורכת]]. אוהבים! ❤️", EventType.BIRTHDAY),
        t("מכובדת", "מזל טוב ליום הולדתך, {שם}. בברכת בריאות, נחת והצלחה בכל אשר תפנה.", EventType.BIRTHDAY),
        t("יובל", "{שם}, מזל טוב ליובל! 🎊 חמישים שנה של עשייה ונתינה — שתמשיכו מכוח אל כוח, בבריאות ובשמחה. עד 120!", EventType.BIRTHDAY, milestone = "age:JUBILEE_50"),
        t("יום הולדת עגול", "{שם}, מזל טוב לגיל {גיל}! 🎉 עשור חדש ובו עוד הרבה שמחות, בריאות ונחת. עד 120!", EventType.BIRTHDAY, milestone = "age:DECADE"),
        t("יום נישואין", "{שם}, מזל טוב ליום הנישואין! 💍[[ {שנים} שנים יחד]] — שתמשיכו לבנות ביחד בית של אהבה, שלווה ושמחה.", EventType.ANNIVERSARY),
        t("רומנטית", "מזל טוב {שם} 💕 עוד שנה ביחד, עוד שנה של אהבה. שתזכו לעוד רבות ובריאות.", EventType.ANNIVERSARY),
        t("חתונת כסף", "מזל טוב {שם} לחתונת הכסף! 🥈 עשרים וחמש שנות נישואין של אהבה, יציבות ובניין בית — ועוד רבות כאלה.", EventType.ANNIVERSARY, milestone = "gift:SILVER"),
        t("חתונת זהב", "מזל טוב {שם} לחתונת הזהב! 🥇 חמישים שנות נישואין — מופת לאהבה ולנאמנות. שתזכו לראות נחת מכל המשפחה.", EventType.ANNIVERSARY, milestone = "gift:GOLD"),
        t("חתונת יהלום", "מזל טוב {שם} לחתונת היהלום! 💎 שישים שנות נישואין — נדיר, מרגש ומעורר השראה. ברוכים תהיו.", EventType.ANNIVERSARY, milestone = "gift:DIAMOND"),
        t("זיכרון", "בהגיע יום הזיכרון[[ ה-{שנים}]] לפטירת {שם} ז״ל — יהי זכרו ברוך. מחבקים ומשתתפים איתכם בזיכרון.", EventType.YAHRZEIT),
        t("בר מצווה", "מזל טוב {שם} לרגל בר המצווה! 🎉 שתזכו לראות בו נחת, ושיגדל לתורה, לחופה ולמעשים טובים.", EventType.BAR_BAT_MITZVAH, Gender.MALE),
        t("בת מצווה", "מזל טוב {שם} לרגל בת המצווה! 🎉 שתזכו לראות בה נחת, ושתגדל לתורה, לחופה ולמעשים טובים.", EventType.BAR_BAT_MITZVAH, Gender.FEMALE),
        t("בר/בת מצווה", "מזל טוב {שם} לרגל בר/בת המצווה! 🎉 שתזכו לשמחה ולנחת, ולהמשך דרך מבורכת.", EventType.BAR_BAT_MITZVAH),
        t("תודה", "{שם} שלום, תודה על העבודה המשותפת. מאחל/ת המשך הצלחה!", EventType.WORK),
        t("מזל טוב מקצועי", "מזל טוב {שם}! שמחים בהצלחתך ומאחלים לך המשך דרך מוצלחת.", EventType.WORK),
        t("כללית", "{שם}, מחשבות טובות ובשורות טובות! 🌟 שיהיה לך המון אושר ובריאות.", null),
    )

    /** Keys a milestone can match, e.g. a 50th birthday -> "age:JUBILEE_50". */
    fun milestoneKey(type: EventType, milestone: Milestone?): String? = when {
        milestone == null -> null
        type == EventType.BIRTHDAY -> milestone.specialAge?.let { "age:${it.name}" }
        type == EventType.ANNIVERSARY -> milestone.gift?.let { "gift:${it.name}" }
        else -> null
    }

    /** Templates for an event, best first: milestone match, then the type and gender, then the rest; the user's own come before built-ins of the same rank. */
    fun forEvent(type: EventType, gender: Gender?, milestone: Milestone?, custom: List<GreetingTemplate>): List<GreetingTemplate> {
        val key = milestoneKey(type, milestone)
        fun rank(t: GreetingTemplate): Int = when {
            key != null && t.milestone == key -> 0
            t.milestone != null -> 9
            t.forType == type && (t.gender == null || t.gender == gender) -> 1
            t.forType == type -> 5
            t.forType == null -> 3
            else -> 7
        }
        return (custom + BUILT_IN).sortedBy { rank(it) }
    }

    /** Templates made for one of the event's groups come first; otherwise the order is kept. */
    fun preferGroups(templates: List<GreetingTemplate>, eventGroupIds: Set<Long>): List<GreetingTemplate> =
        if (eventGroupIds.isEmpty()) templates else templates.sortedBy { if (it.groupIds.any { g -> g in eventGroupIds }) 0 else 1 }

}

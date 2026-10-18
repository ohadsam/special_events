package com.ohadsam.specialevents.domain.greetings

/** The facts a greeting text can use. `null` means "not known", which makes the placeholders that need it disappear. */
data class GreetingContext(
    val name: String,
    val age: Int? = null,
    val years: Int? = null,
    val relation: String? = null,
    val hebrewDate: String? = null,
    val gregorianDate: String? = null,
    val parsha: String? = null,
)

/**
 * Fills a template. Placeholders: {שם} {גיל} {שנים} {יחס} {תאריך_עברי} {תאריך_לועזי} {פרשה}.
 * Text inside `[[ ... ]]` is optional: it is dropped entirely when any placeholder inside it is unknown, so one template reads well
 * both with and without an age ("מזל טוב {שם}[[ ליום הולדת {גיל}]]!").
 */
object GreetingRenderer {
    val PLACEHOLDERS = listOf("שם", "גיל", "שנים", "יחס", "תאריך_עברי", "תאריך_לועזי", "פרשה")
    private val OPTIONAL = Regex("\\[\\[(.*?)]]", RegexOption.DOT_MATCHES_ALL)
    private val VARIABLE = Regex("\\{([^{}]+)}")
    private val SPACE_BEFORE_PUNCTUATION = Regex("[ \\t]+([,.!?:;])")
    private val MANY_SPACES = Regex("[ \\t]{2,}")
    private val MANY_LINES = Regex("\\n{3,}")

    fun render(template: String, ctx: GreetingContext): String {
        val values = valuesOf(ctx)
        val withOptionals = OPTIONAL.replace(template) { m ->
            val inner = m.groupValues[1]
            if (VARIABLE.findAll(inner).all { values[it.groupValues[1]] != null }) inner else ""
        }
        return VARIABLE.replace(withOptionals) { m -> values[m.groupValues[1]].orEmpty() }
            .lines().joinToString("\n") { it.replace(MANY_SPACES, " ").replace(SPACE_BEFORE_PUNCTUATION, "$1").trim() }
            .replace(MANY_LINES, "\n\n")
            .trim()
    }

    /** Placeholders used by [template] that [ctx] cannot fill (shown as a hint in the editor). */
    fun missing(template: String, ctx: GreetingContext): Set<String> {
        val values = valuesOf(ctx)
        return VARIABLE.findAll(template).map { it.groupValues[1] }.filter { it in PLACEHOLDERS && values[it] == null }.toSet()
    }

    private fun valuesOf(ctx: GreetingContext): Map<String, String?> = mapOf(
        "שם" to ctx.name.takeIf { it.isNotBlank() },
        "גיל" to ctx.age?.toString(),
        "שנים" to ctx.years?.toString(),
        "יחס" to ctx.relation?.takeIf { it.isNotBlank() },
        "תאריך_עברי" to ctx.hebrewDate,
        "תאריך_לועזי" to ctx.gregorianDate,
        "פרשה" to ctx.parsha,
    )
}

/** Turns what a user typed into the digits WhatsApp's wa.me links want (international, no plus, no leading zero). */
object PhoneNumbers {
    /** [defaultCountry] digits without plus, e.g. "972". Returns null when there are too few digits to be a phone number. */
    fun forWhatsApp(raw: String?, defaultCountry: String = "972"): String? {
        val text = raw?.trim().orEmpty()
        var digits = text.filter { it.isDigit() }
        if (digits.length < MIN_DIGITS) return null
        digits = when {
            text.startsWith("+") -> digits
            digits.startsWith("00") -> digits.removePrefix("00")
            digits.startsWith("0") -> defaultCountry + digits.removePrefix("0")
            digits.startsWith(defaultCountry) -> digits
            else -> defaultCountry + digits
        }
        return digits.takeIf { it.length in MIN_DIGITS..MAX_DIGITS }
    }

    /** What to dial: the number as typed, digits and a leading plus only. */
    fun forDial(raw: String?): String? = raw?.filter { it.isDigit() || it == '+' }?.takeIf { it.count(Char::isDigit) >= MIN_DIGITS }

    private const val MIN_DIGITS = 7
    private const val MAX_DIGITS = 15
}

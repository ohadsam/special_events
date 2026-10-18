package com.ohadsam.specialevents.domain.greetings

/** How a piece of a greeting is dressed. [color] is "#RRGGBB" or null. */
data class SpanStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val color: String? = null,
) {
    val isPlain: Boolean get() = !bold && !italic && !underline && !strike && color == null
}

data class Span(val text: String, val style: SpanStyle = SpanStyle())

/**
 * The same greeting for different apps. WhatsApp understands *bold*, _italic_ and ~strike~ only; e-mail takes HTML; SMS and
 * everything else get plain text - so a style the target cannot show is simply left out, never printed as symbols.
 */
object RichText {
    private val WHITESPACE_EDGES = Regex("^(\\s*)(.*?)(\\s*)$", RegexOption.DOT_MATCHES_ALL)

    fun plain(spans: List<Span>): String = spans.joinToString("") { it.text }

    fun whatsApp(spans: List<Span>): String = spans.joinToString("") { span ->
        val s = span.style
        if (!(s.bold || s.italic || s.strike) || span.text.isBlank()) return@joinToString span.text
        // The markers must touch the text: spaces at the ends stay outside them.
        val m = WHITESPACE_EDGES.matchEntire(span.text) ?: return@joinToString span.text
        val (lead, core, trail) = m.destructured
        var out = core
        if (s.strike) out = "~$out~"
        if (s.italic) out = "_${out}_"
        if (s.bold) out = "*$out*"
        lead + out + trail
    }

    fun html(spans: List<Span>): String = spans.joinToString("") { span ->
        var out = escape(span.text).replace("\n", "<br>")
        val s = span.style
        if (s.color != null && COLOR.matches(s.color)) out = "<span style=\"color:${s.color}\">$out</span>"
        if (s.strike) out = "<s>$out</s>"
        if (s.underline) out = "<u>$out</u>"
        if (s.italic) out = "<i>$out</i>"
        if (s.bold) out = "<b>$out</b>"
        out
    }

    private val COLOR = Regex("#[0-9A-Fa-f]{6}")

    private fun escape(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}

/** One step applied to a placeholder's value: `{שם|מפוזר:-|הדגש}`. */
object PlaceholderStyles {
    val COLOR_NAMES = mapOf(
        "אדום" to "#C62828", "כחול" to "#1565C0", "ירוק" to "#2E7D32", "כתום" to "#EF6C00",
        "סגול" to "#6A1B9A", "ורוד" to "#C2185B", "אפור" to "#616161",
    )
    const val DEFAULT_SEPARATOR = "-"
    private const val MAX_SEPARATOR = 3

    /** [steps] are the parts after the first `|`. Unknown steps are ignored, so a typo shows the plain value instead of breaking the text. */
    fun apply(value: String, steps: List<String>): Span {
        var text = value
        var style = SpanStyle()
        for (raw in steps) {
            val step = raw.trim()
            val name = step.substringBefore(':')
            val arg = if (step.contains(':')) step.substringAfter(':') else null
            when (name) {
                "מפוזר" -> text = spread(text, arg?.takeIf { it.isNotEmpty() && it.length <= MAX_SEPARATOR } ?: DEFAULT_SEPARATOR)
                "הדגש" -> style = style.copy(bold = true)
                "נטוי" -> style = style.copy(italic = true)
                "קו_תחתון" -> style = style.copy(underline = true)
                "קו_חוצה" -> style = style.copy(strike = true)
                "צבע" -> colorOf(arg)?.let { style = style.copy(color = it) }
            }
        }
        return Span(text, style)
    }

    /** What the user chose in the placeholder builder. [spread] null = letters stay together; [color] is a name from [COLOR_NAMES] or "#RRGGBB". */
    data class Choice(
        val spread: String? = null,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strike: Boolean = false,
        val color: String? = null,
    )

    /** The placeholder text to insert, e.g. `{שם|מפוזר:-|הדגש}`. */
    fun token(name: String, c: Choice): String = buildString {
        append('{').append(name)
        c.spread?.let { append("|מפוזר:").append(it.take(MAX_SEPARATOR).ifEmpty { DEFAULT_SEPARATOR }) }
        if (c.bold) append("|הדגש")
        if (c.italic) append("|נטוי")
        if (c.underline) append("|קו_תחתון")
        if (c.strike) append("|קו_חוצה")
        c.color?.let { append("|צבע:").append(it) }
        append('}')
    }

    /** "דנה" with "-" becomes "ד-נ-ה"; spaces between words stay as they are. */
    fun spread(text: String, separator: String): String = text.split(' ').joinToString(" ") { word ->
        word.codePoints().toArray().joinToString(separator) { String(Character.toChars(it)) }
    }

    private fun colorOf(arg: String?): String? = arg?.trim()?.let { COLOR_NAMES[it] ?: it.takeIf { c -> Regex("#[0-9A-Fa-f]{6}").matches(c) } }
}

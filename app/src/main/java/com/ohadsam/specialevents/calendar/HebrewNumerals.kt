package com.ohadsam.specialevents.calendar

/** Hebrew letter numerals (gematria) for chapter/verse labels and search. */
object HebrewNumerals {
    private val ones = charArrayOf('א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט')
    private val tens = charArrayOf('י', 'כ', 'ל', 'מ', 'נ', 'ס', 'ע', 'פ', 'צ')
    private val hundreds = charArrayOf('ק', 'ר', 'ש', 'ת')

    private val values: Map<Char, Int> = buildMap {
        ones.forEachIndexed { i, c -> put(c, i + 1) }
        tens.forEachIndexed { i, c -> put(c, (i + 1) * 10) }
        hundreds.forEachIndexed { i, c -> put(c, (i + 1) * 100) }
        put('ך', 20)
        put('ם', 40)
        put('ן', 50)
        put('ף', 80)
        put('ץ', 90)
    }

    /** 1..999 → e.g. 15 → ט״ו, 119 → קי״ט, 30 → ל׳. 15 and 16 avoid the divine-name letter pairs. */
    fun toHebrew(n: Int, punctuation: Boolean = true): String {
        require(n in 1..999) { "n out of range: $n" }
        val sb = StringBuilder()
        var rest = n
        while (rest >= 400) {
            sb.append('ת')
            rest -= 400
        }
        if (rest >= 100) {
            sb.append(hundreds[rest / 100 - 1])
            rest %= 100
        }
        when (rest) {
            15 -> sb.append("טו")
            16 -> sb.append("טז")
            else -> {
                if (rest >= 10) sb.append(tens[rest / 10 - 1])
                if (rest % 10 != 0) sb.append(ones[rest % 10 - 1])
            }
        }
        if (!punctuation) return sb.toString()
        return if (sb.length == 1) "$sb׳" else sb.insert(sb.length - 1, '״').toString()
    }

    /** Parses "קל", "ק״ל", "קי'ט" → 130 / 130 / 119; null when empty or containing a non-numeral letter. */
    fun fromHebrew(input: String): Int? {
        var sum = 0
        var seen = false
        for (ch in input) {
            if (ch == '"' || ch == '\'' || ch == '״' || ch == '׳' || ch.isWhitespace()) continue
            sum += values[ch] ?: return null
            seen = true
        }
        return if (seen && sum > 0) sum else null
    }
}

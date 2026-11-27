package com.ohadsam.specialevents.domain.print

/**
 * The three decorative card borders of the brand set (A5 portrait art, 148 x 210 units; A4 has the same proportions), as inline SVG for
 * a CSS background. Only the main line takes the user's accent colour; the ribbon corners of the festive frame keep the brand colours.
 * [accent] must already be a validated colour (see PrintHtml).
 */
object PrintFrames {
    private const val OPEN = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 148 210">"""
    private const val RIBBON = "M49 31 C22 35 3 25 10 12 C19 -3 39 9 49 24 C56 4 80 -5 87 10 C96 27 72 36 49 31Z"

    private fun corner(x: Int, y: Int, dotX: Int, dotY: Int) =
        """<g transform="translate($x $y) scale(.2)"><path d="$RIBBON" fill="#FFB300"/></g><circle cx="$dotX" cy="$dotY" r="2" fill="#C2185B"/>"""

    private fun art(theme: CardTheme, accent: String): String = when (theme) {
        CardTheme.CLASSIC ->
            """$OPEN<path d="M8 8 H140 V202 H8Z" fill="none" stroke="$accent" stroke-width="1.5"/><path d="M12 12 H136 V198 H12Z" fill="none" stroke="#FFB300" stroke-width="1"/></svg>"""
        CardTheme.FESTIVE ->
            """$OPEN<path d="M8 8 H140 V202 H8Z" fill="none" stroke="$accent" stroke-width="1.5"/>${corner(12, 12, 24, 26)}${corner(118, 12, 130, 26)}${corner(12, 180, 24, 194)}${corner(118, 180, 130, 194)}</svg>"""
        CardTheme.MINIMAL ->
            """$OPEN<path d="M12 12 H136 M12 198 H136" fill="none" stroke="$accent" stroke-width="1.5"/></svg>"""
    }

    /** A `data:` URI for `url("...")` in CSS: quotes, angle brackets and `#` are escaped so it cannot end the CSS string early. */
    fun dataUri(theme: CardTheme, accent: String): String {
        val encoded = art(theme, accent).replace("\"", "'").replace("<", "%3C").replace(">", "%3E").replace("#", "%23")
        return "data:image/svg+xml;utf8,$encoded"
    }
}

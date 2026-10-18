package com.ohadsam.specialevents.domain.print

/**
 * HTML for the three printouts. Everything the user typed goes through [escape]; the page has no script and loads nothing from the
 * network, so it can be shown in a locked-down WebView, printed, or saved as PDF through the system print dialog.
 */
object PrintHtml {
    private val COLOR = Regex("#[0-9A-Fa-f]{6}")

    fun escape(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

    private fun text(s: String) = escape(s).replace("\n", "<br>")

    private fun accent(style: PrintStyle) = style.accent.takeIf { COLOR.matches(it) } ?: "#4A3AB8"

    private fun page(title: String, style: PrintStyle, css: String, body: String): String = """
        <!DOCTYPE html>
        <html lang="he" dir="rtl">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${escape(title)}</title>
        <style>
        @page { size: ${style.paper.css}; margin: 12mm; }
        * { box-sizing: border-box; }
        html, body { margin: 0; padding: 0; }
        body { font-family: ${style.font.stack}; color: #1b1b1f; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
        ${css.trimIndent()}
        </style>
        </head>
        <body>
        $body
        </body>
        </html>
    """.trimIndent()

    fun card(card: CardPage, style: PrintStyle): String {
        val ac = accent(style)
        val frame = when (card.theme) {
            CardTheme.CLASSIC -> "border: 3px double $ac; padding: 10mm;"
            CardTheme.FESTIVE -> "border: 6px solid $ac; border-radius: 8mm; background: linear-gradient(180deg, ${ac}22, #ffffff 55%); padding: 10mm;"
            CardTheme.MINIMAL -> "border-top: 2px solid $ac; border-bottom: 2px solid $ac; padding: 12mm 6mm;"
        }
        val photo = card.photoDataUri?.takeIf { it.startsWith("data:image/") }?.let { """<img class="photo" src="${escape(it)}" alt="">""" }.orEmpty()
        val css = """
            .card { min-height: 92vh; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; $frame }
            .emoji { font-size: 72pt; line-height: 1.1; }
            h1 { font-size: 30pt; margin: 6mm 0 4mm; color: $ac; }
            .message { font-size: 17pt; line-height: 1.6; max-width: 150mm; }
            .footer { margin-top: 10mm; font-size: 11pt; color: #55545c; }
            .photo { width: 42mm; height: 42mm; border-radius: 50%; object-fit: cover; border: 2px solid $ac; margin-bottom: 4mm; }
        """
        val body = """<div class="card">$photo<div class="emoji">${escape(card.emoji)}</div><h1>${text(card.headline)}</h1><div class="message">${text(card.message)}</div>${
            card.footer?.let { """<div class="footer">${text(it)}</div>""" }.orEmpty()
        }</div>"""
        return page(card.headline, style, css, body)
    }

    fun month(sheet: MonthSheet, style: PrintStyle): String {
        val ac = accent(style)
        val css = """
            h1 { text-align: center; color: $ac; margin: 0 0 1mm; font-size: 22pt; }
            .sub { text-align: center; color: #55545c; margin-bottom: 4mm; font-size: 11pt; }
            table { width: 100%; border-collapse: collapse; table-layout: fixed; }
            th { background: $ac; color: #fff; padding: 2mm; font-size: 10pt; }
            td { border: 1px solid #b9b8c1; vertical-align: top; height: 27mm; padding: 1mm 1.5mm; font-size: 8.5pt; }
            td.out { background: #f1f0f4; color: #8b8a93; }
            td.shabbat { background: #ecebf5; }
            .num { display: flex; justify-content: space-between; font-weight: bold; font-size: 11pt; }
            .num span.alt { font-weight: normal; font-size: 8pt; color: #66656e; }
            .entry { margin-top: 0.5mm; }
            .note { color: $ac; font-size: 7.5pt; margin-top: 0.5mm; }
        """
        val head = sheet.weekdays.joinToString("") { "<th>${escape(it)}</th>" }
        val rows = sheet.weeks.joinToString("") { week ->
            "<tr>" + week.joinToString("") { d ->
                val cls = listOfNotNull("out".takeIf { !d.inMonth }, "shabbat".takeIf { d.shabbat }).joinToString(" ")
                val alt = d.secondary?.let { """<span class="alt">${escape(it)}</span>""" }.orEmpty()
                "<td class=\"$cls\"><div class=\"num\"><span>${escape(d.primary)}</span>$alt</div>" +
                    d.entries.joinToString("") { """<div class="entry">${escape(it)}</div>""" } +
                    d.notes.joinToString("") { """<div class="note">${escape(it)}</div>""" } + "</td>"
            } + "</tr>"
        }
        val body = "<h1>${escape(sheet.title)}</h1>" + sheet.subtitle?.let { """<div class="sub">${escape(it)}</div>""" }.orEmpty() +
            "<table><thead><tr>$head</tr></thead><tbody>$rows</tbody></table>"
        return page(sheet.title, style, css, body)
    }

    fun year(sheet: YearSheet, style: PrintStyle): String {
        val ac = accent(style)
        val css = """
            h1 { text-align: center; color: $ac; margin: 0 0 1mm; font-size: 24pt; }
            .sub { text-align: center; color: #55545c; margin-bottom: 3mm; font-size: 10pt; }
            .grid { width: 100%; border-collapse: separate; border-spacing: 3mm 2mm; table-layout: fixed; }
            .grid > tbody > tr > td { vertical-align: top; padding: 0; }
            .box { page-break-inside: avoid; }
            h2 { font-size: 11pt; margin: 0 0 1mm; color: #fff; background: $ac; text-align: center; padding: 0.8mm; border-radius: 1mm; }
            table.m { width: 100%; border-collapse: collapse; table-layout: fixed; }
            table.m th { font-size: 6.5pt; color: #55545c; padding: 0.3mm 0; }
            table.m td { text-align: center; font-size: 7.5pt; height: 4.6mm; padding: 0; }
            td.sh { background: #ecebf5; }
            td.hol { color: $ac; font-weight: bold; }
            td.mk span { border: 1px solid $ac; border-radius: 50%; padding: 0 1mm; font-weight: bold; }
            .notes { font-size: 6.5pt; color: #333; margin-top: 0.8mm; min-height: 10mm; }
            .notes div { margin: 0.2mm 0; }
        """
        val head = sheet.weekdays.joinToString("") { "<th>${escape(it)}</th>" }
        fun box(m: YearMonthBox): String {
            val rows = m.weeks.joinToString("") { week ->
                "<tr>" + week.joinToString("") { d ->
                    if (d == null) {
                        "<td></td>"
                    } else {
                        val cls = listOfNotNull("sh".takeIf { d.shabbat }, "hol".takeIf { d.holiday }, "mk".takeIf { d.marked }).joinToString(" ")
                        "<td class=\"$cls\"><span>${escape(d.number)}</span></td>"
                    }
                } + "</tr>"
            }
            val notes = m.notes.joinToString("") { "<div>${escape(it)}</div>" }
            return """<div class="box"><h2>${escape(m.title)}</h2><table class="m"><thead><tr>$head</tr></thead><tbody>$rows</tbody></table><div class="notes">$notes</div></div>"""
        }
        val grid = sheet.months.chunked(YEAR_COLUMNS).joinToString("") { row -> "<tr>" + row.joinToString("") { "<td>${box(it)}</td>" } + "</tr>" }
        val body = "<h1>${escape(sheet.title)}</h1>" + sheet.subtitle?.let { """<div class="sub">${escape(it)}</div>""" }.orEmpty() + """<table class="grid"><tbody>$grid</tbody></table>"""
        return page(sheet.title, style, css, body)
    }

    private const val YEAR_COLUMNS = 3

    fun list(sheet: ListSheet, style: PrintStyle): String {
        val ac = accent(style)
        val css = """
            h1 { color: $ac; margin: 0 0 1mm; font-size: 22pt; }
            .sub { color: #55545c; margin-bottom: 4mm; font-size: 11pt; }
            h2 { font-size: 13pt; margin: 6mm 0 1.5mm; color: $ac; border-bottom: 1px solid $ac; padding-bottom: 1mm; page-break-after: avoid; }
            table { width: 100%; border-collapse: collapse; }
            th { text-align: right; font-size: 9pt; color: #55545c; padding: 1mm 2mm; }
            td { padding: 1.4mm 2mm; border-bottom: 1px solid #e3e2e8; font-size: 10.5pt; }
            tr { page-break-inside: avoid; }
        """
        val head = sheet.columns.joinToString("") { "<th>${escape(it)}</th>" }
        val sections = sheet.sections.joinToString("") { s ->
            (s.heading?.let { "<h2>${escape(it)}</h2>" }.orEmpty()) +
                "<table><thead><tr>$head</tr></thead><tbody>" +
                s.rows.joinToString("") { r -> "<tr><td>${escape(r.date)}</td><td><b>${escape(r.title)}</b></td><td>${escape(r.kind)}</td><td>${escape(r.detail)}</td></tr>" } +
                "</tbody></table>"
        }
        val body = "<h1>${escape(sheet.title)}</h1>" + sheet.subtitle?.let { """<div class="sub">${escape(it)}</div>""" }.orEmpty() + sections
        return page(sheet.title, style, css, body)
    }
}

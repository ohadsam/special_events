package com.ohadsam.specialevents.domain.print

import java.time.LocalDate

enum class Paper(val css: String, val mediaWidthMils: Int, val mediaHeightMils: Int) {
    A4("A4", 8_268, 11_693),
    A5("A5", 5_827, 8_268),
}

/** Fonts that exist on every Android device, so a printout looks the same everywhere. */
enum class PrintFont(val stack: String) {
    SANS("Roboto, 'Noto Sans Hebrew', 'Noto Sans', Arial, sans-serif"),
    SERIF("'Noto Serif Hebrew', 'Noto Serif', Georgia, 'Times New Roman', serif"),
    ROUNDED("'Arial Rounded MT Bold', 'Noto Sans Hebrew', Verdana, sans-serif"),
    SCRIPT("'Segoe Script', 'Comic Sans MS', 'Noto Sans Hebrew', cursive"),
}

enum class CardTheme { CLASSIC, FESTIVE, MINIMAL }

/** How a printout looks. [accent] is "#RRGGBB". */
data class PrintStyle(val paper: Paper = Paper.A4, val font: PrintFont = PrintFont.SANS, val accent: String = "#4A3AB8")

/** A greeting card page: big emoji, headline, the message, and a small line (dates) at the foot. */
data class CardPage(
    val emoji: String,
    val headline: String,
    val message: String,
    val footer: String? = null,
    /** A `data:` URI of a picture shown in a round frame; null = none. */
    val photoDataUri: String? = null,
    val theme: CardTheme = CardTheme.CLASSIC,
)

data class PrintDay(
    val date: LocalDate,
    val inMonth: Boolean,
    /** The day number as the sheet's main calendar shows it, and the other calendar's number (null when only one calendar is shown). */
    val primary: String,
    val secondary: String?,
    val shabbat: Boolean,
    val entries: List<String>,
    val notes: List<String>,
)

data class MonthSheet(val title: String, val subtitle: String?, val weekdays: List<String>, val weeks: List<List<PrintDay>>)

/** One day of a year-sheet month; [marked] = something is planned, [holiday] = a Jewish holiday or fast. */
data class YearDay(val number: String, val shabbat: Boolean, val marked: Boolean, val holiday: Boolean)

/** [weeks] hold null for the blank cells before the 1st and after the last day. */
data class YearMonthBox(val title: String, val weeks: List<List<YearDay?>>, val notes: List<String>)

data class YearSheet(val title: String, val subtitle: String?, val weekdays: List<String>, val months: List<YearMonthBox>)

data class PrintRow(val date: String, val title: String, val kind: String, val detail: String)

data class PrintSection(val heading: String?, val rows: List<PrintRow>)

data class ListSheet(val title: String, val subtitle: String?, val sections: List<PrintSection>, val columns: List<String>)

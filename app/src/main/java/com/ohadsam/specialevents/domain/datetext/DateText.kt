package com.ohadsam.specialevents.domain.datetext

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.EditorDate
import java.time.DayOfWeek
import java.time.LocalDate

enum class ParseWarning {
    /** The weekday written does not fit the date; [ParsedDate.actualWeekday] says what it really was. */
    WEEKDAY_MISMATCH,

    /** Several years fit (weekday given, year not): the user must pick one of [ParsedDate.candidates]. */
    AMBIGUOUS_YEAR,

    /** A two-digit year was read as 19xx/20xx. */
    TWO_DIGIT_YEAR,

    /** "Adar" in a leap year was read as Adar II. */
    ADAR_READ_AS_SECOND,

    /** The age could not be turned into a year (e.g. a 29 February birthday in a non-leap year). */
    AGE_UNRESOLVED,
}

/** What was understood from a free-text date. [year] null = unknown. Months of a Hebrew date use the app's numbering (Nisan = 1, Adar II = 13). */
data class ParsedDate(
    val basis: CalendarBasis,
    val year: Int?,
    val month: Int,
    val day: Int,
    val afterSunset: Boolean = false,
    val age: Int? = null,
    val weekday: DayOfWeek? = null,
    val actualWeekday: DayOfWeek? = null,
    /** Possible years, nearest first, when a weekday was given without a year. */
    val candidates: List<Int> = emptyList(),
    val warnings: Set<ParseWarning> = emptySet(),
) {
    /** True while several years fit and the user still has to choose one. */
    val needsYear: Boolean get() = candidates.size > 1

    fun origin() = OriginDate(basis, year, month, day)

    fun withYear(chosen: Int): ParsedDate = copy(year = chosen, candidates = emptyList(), warnings = warnings - ParseWarning.AMBIGUOUS_YEAR)

    /** The editor's two-calendar date for this; with an unknown year a reference year is used for display only. */
    fun toEditorDate(today: LocalDate): EditorDate = EditorDate.fromOrigin(origin(), afterSunset, today)
}

enum class DateParseError { EMPTY, NO_MONTH, NO_DAY, INVALID_DATE }

sealed interface DateParse {
    data class Ok(val date: ParsedDate) : DateParse

    data class Failure(val error: DateParseError) : DateParse
}

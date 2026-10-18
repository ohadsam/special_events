package com.ohadsam.specialevents.ui.calendar

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.CalendarMark
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.MarkKind
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.domain.CalendarData
import com.ohadsam.specialevents.domain.CalendarLevel
import com.ohadsam.specialevents.ui.common.dateLabel
import com.ohadsam.specialevents.ui.common.gregorianLong
import com.ohadsam.specialevents.ui.common.gregorianMonthYear
import com.ohadsam.specialevents.ui.common.gregorianShort
import com.ohadsam.specialevents.ui.common.gregorianSpanLabel
import com.ohadsam.specialevents.ui.common.hebrewMonthYear
import com.ohadsam.specialevents.ui.common.hebrewSpanLabel
import com.ohadsam.specialevents.ui.common.weekdayName
import java.time.LocalDate

@StringRes
fun MarkKind.labelRes(): Int = when (this) {
    MarkKind.ROSH_HASHANA -> R.string.mark_rosh_hashana
    MarkKind.TZOM_GEDALIAH -> R.string.mark_tzom_gedaliah
    MarkKind.YOM_KIPPUR -> R.string.mark_yom_kippur
    MarkKind.SUKKOT -> R.string.mark_sukkot
    MarkKind.SHMINI_ATZERET -> R.string.mark_shmini_atzeret
    MarkKind.SIMCHAT_TORAH -> R.string.mark_simchat_torah
    MarkKind.CHANUKAH -> R.string.mark_chanukah
    MarkKind.TENTH_OF_TEVET -> R.string.mark_tenth_of_tevet
    MarkKind.TU_BISHVAT -> R.string.mark_tu_bishvat
    MarkKind.PURIM_KATAN -> R.string.mark_purim_katan
    MarkKind.TAANIT_ESTHER -> R.string.mark_taanit_esther
    MarkKind.PURIM -> R.string.mark_purim
    MarkKind.SHUSHAN_PURIM -> R.string.mark_shushan_purim
    MarkKind.PESACH -> R.string.mark_pesach
    MarkKind.PESACH_SHENI -> R.string.mark_pesach_sheni
    MarkKind.LAG_BAOMER -> R.string.mark_lag_baomer
    MarkKind.SHAVUOT -> R.string.mark_shavuot
    MarkKind.SEVENTEENTH_OF_TAMMUZ -> R.string.mark_seventeenth_of_tammuz
    MarkKind.TISHA_BAV -> R.string.mark_tisha_bav
    MarkKind.TU_BAV -> R.string.mark_tu_bav
    MarkKind.ROSH_CHODESH -> R.string.mark_rosh_chodesh
}

/** The string resource for a mark and, for "day N of Chanukah", the number that goes into it. */
private fun markSpec(mark: CalendarMark): Pair<Int, Int?> = when (mark.kind) {
    MarkKind.CHANUKAH -> R.string.mark_chanukah_day to mark.day
    MarkKind.SUKKOT -> sukkotRes(mark) to null
    MarkKind.PESACH -> (if (mark.yomTov) R.string.mark_pesach else R.string.mark_pesach_chol) to null
    else -> mark.kind.labelRes() to null
}

private fun sukkotRes(mark: CalendarMark): Int = when {
    mark.yomTov -> R.string.mark_sukkot
    mark.day == SUKKOT_LAST_DAY -> R.string.mark_hoshana_raba
    else -> R.string.mark_sukkot_chol
}

@Composable
fun markText(mark: CalendarMark): String = markText(LocalContext.current, mark)

/** The same text outside a composition (printing, notifications). */
fun markText(context: Context, mark: CalendarMark): String = markSpec(mark).let { (res, arg) -> if (arg != null) context.getString(res, arg) else context.getString(res) }

private const val SUKKOT_LAST_DAY = 7

/** Yom Tov in the primary colour, fasts in the error colour, Rosh Chodesh quiet, everything else tertiary. */
@Composable
fun markColor(mark: CalendarMark): Color = when {
    mark.fast -> MaterialTheme.colorScheme.error
    mark.yomTov -> MaterialTheme.colorScheme.primary
    mark.kind == MarkKind.ROSH_CHODESH -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.tertiary
}

@Composable
fun MarkChip(mark: CalendarMark) {
    val color = markColor(mark)
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.12f)) {
        Text(markText(mark), style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

data class Titles(val title: String, val subtitle: String?)

/** Heading of the period on screen, in the user's calendar mode (a second line shows the other calendar in dual mode). */
@Composable
fun periodTitles(level: CalendarLevel, anchor: LocalDate, mode: CalendarMode, weekStartsSunday: Boolean): Titles {
    val hebrew = HebrewCalendar.fromLocalDate(anchor)
    val hebrewPrimary = mode == CalendarMode.HEBREW
    return when (level) {
        CalendarLevel.AGENDA -> Titles(stringResource(R.string.calendar_level_agenda), null)
        CalendarLevel.DAY -> Titles(dateLabel(anchor, hebrew, mode, long = true), weekdayName(anchor))
        CalendarLevel.WEEK -> {
            val start = CalendarData.weekStart(anchor, weekStartsSunday)
            val end = start.plusDays(6)
            Titles("${gregorianShort(start)} – ${gregorianShort(end)}", if (mode == CalendarMode.DUAL) hebrewSpanLabel(start, end) else null)
        }
        CalendarLevel.MONTH -> {
            val (first, length) = CalendarData.monthSpan(anchor, hebrewPrimary)
            val last = first.plusDays(length - 1L)
            when (mode) {
                CalendarMode.GREGORIAN -> Titles(gregorianMonthYear(anchor), null)
                CalendarMode.HEBREW -> Titles(hebrewMonthYear(hebrew), gregorianSpanLabel(first, last))
                CalendarMode.DUAL -> Titles(gregorianMonthYear(anchor), hebrewSpanLabel(first, last))
            }
        }
        CalendarLevel.YEAR -> when (mode) {
            CalendarMode.GREGORIAN -> Titles(anchor.year.toString(), null)
            CalendarMode.HEBREW -> Titles(HebrewCalendar.yearLabel(hebrew.year), null)
            CalendarMode.DUAL -> Titles(anchor.year.toString(), HebrewCalendar.yearLabel(hebrew.year))
        }
    }
}

@Composable
fun levelLabel(level: CalendarLevel): String = stringResource(
    when (level) {
        CalendarLevel.AGENDA -> R.string.calendar_level_agenda
        CalendarLevel.DAY -> R.string.calendar_level_day
        CalendarLevel.WEEK -> R.string.calendar_level_week
        CalendarLevel.MONTH -> R.string.calendar_level_month
        CalendarLevel.YEAR -> R.string.calendar_level_year
    },
)

/** Weekday + long Gregorian date, used by day sections. */
fun longDateLine(date: LocalDate): String = "${weekdayName(date)}, ${gregorianLong(date)}"

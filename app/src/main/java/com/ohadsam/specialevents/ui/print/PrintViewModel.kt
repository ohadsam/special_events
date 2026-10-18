package com.ohadsam.specialevents.ui.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewNumerals
import com.ohadsam.specialevents.calendar.Parsha
import com.ohadsam.specialevents.calendar.ParshaNames
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.domain.CalendarData
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.greetings.GreetingCatalog
import com.ohadsam.specialevents.domain.greetings.GreetingRenderer
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.print.CardPage
import com.ohadsam.specialevents.domain.print.CardTheme
import com.ohadsam.specialevents.domain.print.ListSheet
import com.ohadsam.specialevents.domain.print.MonthLabels
import com.ohadsam.specialevents.domain.print.MonthSheetBuilder
import com.ohadsam.specialevents.domain.print.Paper
import com.ohadsam.specialevents.domain.print.PrintFont
import com.ohadsam.specialevents.domain.print.PrintHtml
import com.ohadsam.specialevents.domain.print.PrintRow
import com.ohadsam.specialevents.domain.print.PrintSection
import com.ohadsam.specialevents.domain.print.PrintStyle
import com.ohadsam.specialevents.domain.views.GroupBy
import com.ohadsam.specialevents.domain.views.SortBy
import com.ohadsam.specialevents.domain.views.ViewEngine
import com.ohadsam.specialevents.domain.views.ViewFilter
import com.ohadsam.specialevents.domain.views.ViewSpec
import com.ohadsam.specialevents.greetings.GreetingContexts
import com.ohadsam.specialevents.print.PrintOutput
import com.ohadsam.specialevents.ui.calendar.markText
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.common.gregorianLong
import com.ohadsam.specialevents.ui.common.gregorianMonthYear
import com.ohadsam.specialevents.ui.common.gregorianShort
import com.ohadsam.specialevents.ui.common.hebrewMonthYear
import com.ohadsam.specialevents.ui.common.labelRes
import com.ohadsam.specialevents.ui.upcoming.groupTitleText
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PrintKind { CARD, MONTH, LIST }

data class PrintState(
    val loading: Boolean = true,
    val kind: PrintKind = PrintKind.CARD,
    val style: PrintStyle = PrintStyle(),
    val events: List<SpecialEvent> = emptyList(),
    val groups: List<EventGroup> = emptyList(),
    val allTags: List<String> = emptyList(),
    // greeting card
    val cardEventId: Long? = null,
    val templates: List<GreetingTemplate> = emptyList(),
    val templateId: Long? = null,
    val headline: String = "",
    val theme: CardTheme = CardTheme.CLASSIC,
    val includePhoto: Boolean = true,
    val includeDates: Boolean = true,
    // month sheet
    val month: LocalDate = LocalDate.now().withDayOfMonth(1),
    val showMarks: Boolean = true,
    // list
    val year: Int = LocalDate.now().year,
    val filter: ViewFilter = ViewFilter(),
    val groupBy: GroupBy = GroupBy.MONTH,
    val html: String = "",
    val jobName: String = "",
    val busy: Boolean = false,
    val message: Int? = null,
)

/** Builds the page for the chosen printout (card, month sheet or list) whenever an option changes. */
class PrintViewModel(private val container: AppContainer, private val initialEventId: Long?) : ViewModel() {
    private val _state = MutableStateFlow(PrintState())
    val state: StateFlow<PrintState> = _state.asStateFlow()
    private val context get() = container.appContext
    private var settings = AppSettings()
    private var membership: Map<Long, Set<Long>> = emptyMap()

    init {
        viewModelScope.launch {
            settings = container.settings.settings.first()
            membership = container.groups.membership.first()
            val events = container.events.allEvents.first().filter { !it.archived }
            val groups = container.groups.groups.first()
            val templates = GreetingCatalog.BUILT_IN + container.greetings.customTemplates.first()
            val chosen = initialEventId?.takeIf { id -> events.any { it.id == id } } ?: events.firstOrNull()?.id
            _state.update {
                it.copy(
                    loading = false,
                    kind = if (initialEventId != null) PrintKind.CARD else PrintKind.MONTH,
                    events = events,
                    groups = groups,
                    allTags = events.flatMap { e -> e.tags.map { t -> t.name } }.distinct().sorted(),
                    cardEventId = chosen,
                    templates = templates,
                    style = it.style.copy(paper = if (initialEventId != null) Paper.A5 else Paper.A4),
                )
            }
            rebuild()
        }
    }

    fun setKind(kind: PrintKind) = change { it.copy(kind = kind, style = it.style.copy(paper = if (kind == PrintKind.CARD) Paper.A5 else Paper.A4)) }

    fun setPaper(paper: Paper) = change { it.copy(style = it.style.copy(paper = paper)) }

    fun setFont(font: PrintFont) = change { it.copy(style = it.style.copy(font = font)) }

    fun setAccent(hex: String) = change { it.copy(style = it.style.copy(accent = hex)) }

    fun setCardEvent(id: Long) = change { it.copy(cardEventId = id, headline = "") }

    fun setTemplate(id: Long?) = change { it.copy(templateId = id) }

    fun setHeadline(text: String) = change { it.copy(headline = text) }

    fun setTheme(theme: CardTheme) = change { it.copy(theme = theme) }

    fun setIncludePhoto(on: Boolean) = change { it.copy(includePhoto = on) }

    fun setIncludeDates(on: Boolean) = change { it.copy(includeDates = on) }

    fun stepMonth(delta: Int) = change { it.copy(month = it.month.plusMonths(delta.toLong())) }

    fun setShowMarks(on: Boolean) = change { it.copy(showMarks = on) }

    fun stepYear(delta: Int) = change { it.copy(year = it.year + delta) }

    fun toggleType(type: EventType) = change { s -> s.copy(filter = s.filter.copy(types = if (type in s.filter.types) s.filter.types - type else s.filter.types + type)) }

    fun toggleGroup(id: Long) = change { s -> s.copy(filter = s.filter.copy(groups = if (id in s.filter.groups) s.filter.groups - id else s.filter.groups + id)) }

    fun toggleTag(tag: String) = change { s -> s.copy(filter = s.filter.copy(tags = if (tag in s.filter.tags) s.filter.tags - tag else s.filter.tags + tag)) }

    fun setGroupBy(by: GroupBy) = change { it.copy(groupBy = by) }

    fun messageShown() = _state.update { it.copy(message = null) }

    /** Writes the page as a picture into the cache and hands it over; reports in [message] when the device could not draw it. */
    fun shareImage(onReady: (java.io.File) -> Unit) {
        _state.update { it.copy(busy = true) }
        PrintOutput.renderToPng(context, _state.value.html, java.io.File(context.cacheDir, "share")) { file ->
            _state.update { it.copy(busy = false, message = if (file == null) R.string.print_image_failed else null) }
            file?.let(onReady)
        }
    }

    private fun change(transform: (PrintState) -> PrintState) {
        _state.update(transform)
        rebuild()
    }

    private fun rebuild() {
        val s = _state.value
        if (s.loading) return
        viewModelScope.launch {
            val (html, job) = withContext(Dispatchers.Default) {
                when (s.kind) {
                    PrintKind.CARD -> cardHtml(s)
                    PrintKind.MONTH -> monthHtml(s)
                    PrintKind.LIST -> listHtml(s)
                }
            }
            _state.update { it.copy(html = html, jobName = job) }
        }
    }

    private fun cardHtml(s: PrintState): Pair<String, String> {
        val event = s.events.firstOrNull { it.id == s.cardEventId } ?: return PrintHtml.card(CardPage("🎉", context.getString(R.string.print_card_empty), ""), s.style) to context.getString(R.string.print_card_empty)
        val today = LocalDate.now()
        val item = EventTimeline.forEvent(event, today, 1, settings.dateRules).firstOrNull()
        val template = s.templates.firstOrNull { it.id == s.templateId } ?: GreetingCatalog.forEvent(event.type, event.gender, item?.milestone, emptyList()).firstOrNull()
        val message = if (template != null && item != null) GreetingRenderer.render(template.body, GreetingContexts.of(event, item)) else ""
        val name = event.personName ?: event.title
        val headline = s.headline.ifBlank { context.getString(R.string.print_card_headline, name) }
        val footer = if (s.includeDates && item != null) "${gregorianLong(item.date)} · ${item.hebrew.format()}" else null
        val accent = event.colorArgb?.let { "#%06X".format(it and 0xFFFFFF) } ?: s.style.accent
        val page = CardPage(
            emoji = event.iconKey ?: event.type.defaultEmoji(),
            headline = headline,
            message = message,
            footer = footer,
            photoDataUri = if (s.includePhoto) PrintOutput.photoDataUri(event.photoUri) else null,
            theme = s.theme,
        )
        return PrintHtml.card(page, s.style.copy(accent = accent)) to headline
    }

    private fun monthHtml(s: PrintState): Pair<String, String> {
        val first = s.month
        val gridFirst = CalendarData.monthGrid(first, settings.calendarMode == CalendarMode.HEBREW, settings.weekStartsOnSunday).let { it.first().first().date to it.last().last().date }
        val items = EventTimeline.between(s.events, gridFirst.first, gridFirst.second, LocalDate.now(), settings.inIsrael, settings.dateRules).groupBy { it.date }
        val labels = MonthLabels(
            weekdays = { sundayFirst -> weekdayLetters(sundayFirst) },
            mark = { markText(context, it) },
            entry = { item -> (item.event.iconKey ?: item.event.type.defaultEmoji()) + " " + item.event.title + (item.number?.let { " ($it)" } ?: "") },
            parsha = { date -> Parsha.ofShabbat(date, settings.inIsrael)?.let { context.getString(R.string.calendar_parsha, ParshaNames.label(it)) } },
        )
        val title = if (settings.calendarMode == CalendarMode.HEBREW) hebrewMonthYear(HebrewCalendar.fromLocalDate(first.plusDays(MID_MONTH))) else gregorianMonthYear(first)
        val subtitle = if (settings.calendarMode == CalendarMode.DUAL) hebrewSpan(first) else null
        val sheet = MonthSheetBuilder.build(first, settings.calendarMode, settings.weekStartsOnSunday, settings.inIsrael, items, labels, title, subtitle, s.showMarks)
        return PrintHtml.month(sheet, s.style) to title
    }

    private fun hebrewSpan(first: LocalDate): String {
        val a = hebrewMonthYear(HebrewCalendar.fromLocalDate(first))
        val b = hebrewMonthYear(HebrewCalendar.fromLocalDate(first.withDayOfMonth(first.lengthOfMonth())))
        return if (a == b) a else "$a – $b"
    }

    private fun weekdayLetters(sundayFirst: Boolean): List<String> {
        val letters = (1..DAYS_IN_WEEK).map { HebrewNumerals.toHebrew(it) }
        return if (sundayFirst) letters else letters.drop(1) + letters.first()
    }

    private fun listHtml(s: PrintState): Pair<String, String> {
        val from = LocalDate.of(s.year, 1, 1)
        val to = LocalDate.of(s.year, 12, 31)
        val items = EventTimeline.between(s.events, from, to, LocalDate.now(), settings.inIsrael, settings.dateRules)
        val spec = ViewSpec(s.filter, s.groupBy, SortBy.DATE, true)
        val names = s.groups.associate { it.id to listOfNotNull(it.emoji, it.name).joinToString(" ") }
        val sections = ViewEngine.apply(items, spec, settings.weekStartsOnSunday, membership, s.groups.map { it.id }).map { g ->
            PrintSection(groupTitleText(context, g.key, names).takeIf { s.groupBy != GroupBy.NONE }, g.items.map(::rowOf))
        }
        val title = context.getString(R.string.print_list_title, s.year)
        val sheet = ListSheet(
            title,
            context.getString(R.string.print_list_count, sections.sumOf { it.rows.size }),
            sections,
            listOf(R.string.print_col_date, R.string.print_col_name, R.string.print_col_kind, R.string.print_col_detail).map(context::getString),
        )
        return PrintHtml.list(sheet, s.style) to title
    }

    private fun rowOf(item: UpcomingItem): PrintRow {
        val detail = item.number?.let { if (item.event.type == EventType.BIRTHDAY) context.getString(R.string.share_card_age, it) else context.getString(R.string.share_card_years, it) }.orEmpty()
        return PrintRow(gregorianShort(item.date) + " · " + item.hebrew.format(withYear = false), item.event.title, context.getString(item.event.type.labelRes()), detail)
    }

    class Factory(private val container: AppContainer, private val eventId: Long?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PrintViewModel(container, eventId) as T
    }

    private companion object {
        const val MID_MONTH = 14L
        const val DAYS_IN_WEEK = 7
    }
}

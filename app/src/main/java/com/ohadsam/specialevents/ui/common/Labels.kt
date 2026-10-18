package com.ohadsam.specialevents.ui.common

import androidx.annotation.StringRes
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.AdarInLeapYear
import com.ohadsam.specialevents.calendar.AnniversaryGift
import com.ohadsam.specialevents.calendar.LeapDay
import com.ohadsam.specialevents.calendar.ShortMonthThirtieth
import com.ohadsam.specialevents.calendar.TzeitMethod
import com.ohadsam.specialevents.calendar.SpecialAge
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.data.prefs.ThemeMode
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.TypeFilter
import com.ohadsam.specialevents.domain.UpcomingGroup
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.views.GroupBy
import com.ohadsam.specialevents.domain.views.SortBy

@StringRes
fun EventType.labelRes(): Int = when (this) {
    EventType.BIRTHDAY -> R.string.type_birthday
    EventType.ANNIVERSARY -> R.string.type_anniversary
    EventType.YAHRZEIT -> R.string.type_yahrzeit
    EventType.BAR_BAT_MITZVAH -> R.string.type_bar_bat_mitzvah
    EventType.WORK -> R.string.type_work
    EventType.FAMILY -> R.string.type_family
    EventType.CUSTOM -> R.string.type_custom
}

/** Default emoji icon of a type; the user can pick another one per event. */
fun EventType.defaultEmoji(): String = when (this) {
    EventType.BIRTHDAY -> "🎂"
    EventType.ANNIVERSARY -> "💍"
    EventType.YAHRZEIT -> "🕯️"
    EventType.BAR_BAT_MITZVAH -> "📜"
    EventType.WORK -> "💼"
    EventType.FAMILY -> "👪"
    EventType.CUSTOM -> "⭐"
}

@StringRes
fun TypeFilter.labelRes(): Int = when (this) {
    TypeFilter.ALL -> R.string.filter_all
    TypeFilter.BIRTHDAYS -> R.string.filter_birthdays
    TypeFilter.ANNIVERSARIES -> R.string.filter_anniversaries
    TypeFilter.YAHRZEITS -> R.string.filter_yahrzeits
    TypeFilter.OTHER -> R.string.filter_other
}

@StringRes
fun UpcomingGroup.labelRes(): Int = when (this) {
    UpcomingGroup.TODAY -> R.string.group_today
    UpcomingGroup.THIS_WEEK -> R.string.group_week
    UpcomingGroup.THIS_MONTH -> R.string.group_month
    UpcomingGroup.LATER -> R.string.group_later
}

@StringRes
fun AnniversaryGift.labelRes(): Int = when (this) {
    AnniversaryGift.PAPER -> R.string.gift_paper
    AnniversaryGift.COTTON -> R.string.gift_cotton
    AnniversaryGift.LEATHER -> R.string.gift_leather
    AnniversaryGift.FLOWERS -> R.string.gift_flowers
    AnniversaryGift.WOOD -> R.string.gift_wood
    AnniversaryGift.SUGAR -> R.string.gift_sugar
    AnniversaryGift.WOOL -> R.string.gift_wool
    AnniversaryGift.BRONZE -> R.string.gift_bronze
    AnniversaryGift.POTTERY -> R.string.gift_pottery
    AnniversaryGift.TIN -> R.string.gift_tin
    AnniversaryGift.STEEL -> R.string.gift_steel
    AnniversaryGift.SILK -> R.string.gift_silk
    AnniversaryGift.LACE -> R.string.gift_lace
    AnniversaryGift.IVORY -> R.string.gift_ivory
    AnniversaryGift.CRYSTAL -> R.string.gift_crystal
    AnniversaryGift.CHINA -> R.string.gift_china
    AnniversaryGift.SILVER -> R.string.gift_silver
    AnniversaryGift.PEARL -> R.string.gift_pearl
    AnniversaryGift.CORAL -> R.string.gift_coral
    AnniversaryGift.RUBY -> R.string.gift_ruby
    AnniversaryGift.SAPPHIRE -> R.string.gift_sapphire
    AnniversaryGift.GOLD -> R.string.gift_gold
    AnniversaryGift.EMERALD -> R.string.gift_emerald
    AnniversaryGift.DIAMOND -> R.string.gift_diamond
    AnniversaryGift.PLATINUM -> R.string.gift_platinum
}

@StringRes
fun SpecialAge.labelRes(): Int = when (this) {
    SpecialAge.FIRST -> R.string.age_first
    SpecialAge.ADULT_18 -> R.string.age_adult
    SpecialAge.AGE_21 -> R.string.age_21
    SpecialAge.DECADE -> R.string.age_decade
    SpecialAge.JUBILEE_50 -> R.string.age_jubilee
    SpecialAge.LONG_LIFE_120 -> R.string.age_120
}

@StringRes
fun CalendarMode.labelRes(): Int = when (this) {
    CalendarMode.HEBREW -> R.string.mode_hebrew
    CalendarMode.GREGORIAN -> R.string.mode_gregorian
    CalendarMode.DUAL -> R.string.mode_dual
}

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
fun AdarInLeapYear.labelRes(): Int = when (this) {
    AdarInLeapYear.ADAR_II -> R.string.rule_adar_two
    AdarInLeapYear.ADAR_I -> R.string.rule_adar_one
}

@StringRes
fun ShortMonthThirtieth.labelRes(): Int = when (this) {
    ShortMonthThirtieth.DAY_BEFORE -> R.string.rule_thirtieth_before
    ShortMonthThirtieth.NEXT_DAY -> R.string.rule_thirtieth_next
}

@StringRes
fun LeapDay.labelRes(): Int = when (this) {
    LeapDay.FEB_28 -> R.string.rule_leap_feb28
    LeapDay.MAR_1 -> R.string.rule_leap_mar1
}

@StringRes
fun TzeitMethod.labelRes(): Int = when (this) {
    TzeitMethod.DEGREES_8_5 -> R.string.tzeit_8_5
    TzeitMethod.DEGREES_7_083 -> R.string.tzeit_7_083
    TzeitMethod.MINUTES_42 -> R.string.tzeit_42
    TzeitMethod.MINUTES_72 -> R.string.tzeit_72
}

@StringRes
fun GroupBy.labelRes(): Int = when (this) {
    GroupBy.PROXIMITY -> R.string.groupby_proximity
    GroupBy.DAY -> R.string.groupby_day
    GroupBy.WEEK -> R.string.groupby_week
    GroupBy.MONTH -> R.string.groupby_month
    GroupBy.HEBREW_MONTH -> R.string.groupby_hebrew_month
    GroupBy.TYPE -> R.string.groupby_type
    GroupBy.TAG -> R.string.groupby_tag
    GroupBy.GROUP -> R.string.groupby_group
    GroupBy.AGE_DECADE -> R.string.groupby_age
    GroupBy.NONE -> R.string.groupby_none
}

@StringRes
fun SortBy.labelRes(): Int = when (this) {
    SortBy.DATE -> R.string.sortby_date
    SortBy.NAME -> R.string.sortby_name
    SortBy.TYPE -> R.string.sortby_type
    SortBy.NUMBER -> R.string.sortby_number
    SortBy.UPDATED -> R.string.sortby_updated
    SortBy.CREATED -> R.string.sortby_created
}

@StringRes
fun Channel.labelRes(): Int = when (this) {
    Channel.NOTIFICATION -> R.string.channel_notification
    Channel.WHATSAPP -> R.string.channel_whatsapp
    Channel.EMAIL -> R.string.channel_email
    Channel.SMS -> R.string.channel_sms
}

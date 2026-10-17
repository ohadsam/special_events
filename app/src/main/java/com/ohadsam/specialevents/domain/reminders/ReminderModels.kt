package com.ohadsam.specialevents.domain.reminders

import com.ohadsam.specialevents.calendar.Tier

enum class OffsetUnit { DAYS, WEEKS, MONTHS }

/** How a reminder reaches the user. Only [NOTIFICATION] is delivered by the reminder engine; the others are handled by greetings (batch 8). */
enum class Channel { NOTIFICATION, WHATSAPP, EMAIL, SMS }

/** What to do with a reminder that would fire during Shabbat / Yom Tov: move it before candle lighting, after havdalah, both, or drop it. */
enum class ShabbatPolicy { BEFORE, AFTER, BOTH, SKIP }

enum class RuleOrigin { DEFAULT, SUGGESTED, USER }

/**
 * "[offsetValue] [offsetUnit] before the occurrence, at [timeMinutes] of the day" (offset 0 = on the day itself).
 * [minTier] limits it to milestones: a rule with MAJOR fires only for round birthdays, silver/gold anniversaries, bar/bat mitzvah...
 * [profileKey] picks the sound/vibration profile (see `SoundProfile`).
 */
data class ReminderRule(
    val id: Long = 0,
    val eventId: Long = 0,
    val offsetValue: Int = 0,
    val offsetUnit: OffsetUnit = OffsetUnit.DAYS,
    val timeMinutes: Int = DEFAULT_TIME_MINUTES,
    val channel: Channel = Channel.NOTIFICATION,
    val shabbatPolicy: ShabbatPolicy = ShabbatPolicy.BEFORE,
    val profileKey: String = SoundProfile.DEFAULT_KEY,
    val minTier: Tier = Tier.NONE,
    val enabled: Boolean = true,
    val origin: RuleOrigin = RuleOrigin.DEFAULT,
) {
    companion object {
        const val DEFAULT_TIME_MINUTES = 8 * 60
    }
}

/**
 * Sound and vibration of a notification. Android fixes both per notification channel, so each distinct profile gets its own channel.
 * Keys: `default`, `vibrate` (no sound), `silent`, or `sound:<uri>` for a custom tone.
 */
data class SoundProfile(val key: String, val soundUri: String?, val sound: Boolean, val vibrate: Boolean) {
    companion object {
        const val DEFAULT_KEY = "default"
        const val VIBRATE_KEY = "vibrate"
        const val SILENT_KEY = "silent"
        private const val CUSTOM_PREFIX = "sound:"

        val BUILT_IN = listOf(
            SoundProfile(DEFAULT_KEY, null, sound = true, vibrate = true),
            SoundProfile(VIBRATE_KEY, null, sound = false, vibrate = true),
            SoundProfile(SILENT_KEY, null, sound = false, vibrate = false),
        )

        fun fromKey(key: String): SoundProfile = BUILT_IN.firstOrNull { it.key == key }
            ?: if (key.startsWith(CUSTOM_PREFIX)) SoundProfile(key, key.removePrefix(CUSTOM_PREFIX), sound = true, vibrate = true) else BUILT_IN.first()

        fun custom(uri: String) = fromKey(CUSTOM_PREFIX + uri)
    }
}

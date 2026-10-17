package com.ohadsam.specialevents.calendar

import com.ohadsam.specialevents.location.GeoPlace
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** From candle lighting on the eve to havdalah at the end of the last consecutive Shabbat / Yom Tov day. */
data class HolyPeriod(val start: Instant, val end: Instant, val firstDay: LocalDate, val lastDay: LocalDate)

/** How the end of Shabbat / Yom Tov ("צאת") is computed. */
enum class TzeitMethod(val zenith: Double?, val minutesAfterSunset: Int?) {
    DEGREES_8_5(98.5, null),
    DEGREES_7_083(97.083, null),
    MINUTES_42(null, 42),
    MINUTES_72(null, 72),
}

interface HolyTimes {
    /** The holy period whose [start − buffer, end + buffer] window contains [instant], if any. */
    fun periodNear(instant: Instant, buffer: Duration): HolyPeriod?
}

/** Candle lighting (sunset − place.candleLightingMinutes) and havdalah, computed offline for [place]. */
class ZmanimHolyTimes(private val place: GeoPlace, private val tzeit: TzeitMethod = TzeitMethod.DEGREES_8_5) : HolyTimes {
    private fun isHoly(d: LocalDate) = Holidays.isHolyDay(d, place.inIsrael)

    fun sunset(date: LocalDate): Instant? = SunCalc.sunset(date, place.latitude, place.longitude, place.zone)

    fun candleLighting(date: LocalDate): Instant? = sunset(date)?.minus(Duration.ofMinutes(place.candleLightingMinutes.toLong()))

    fun havdalah(date: LocalDate): Instant? {
        val sunset = sunset(date) ?: return null
        val deg = tzeit.zenith
        return if (deg != null) {
            SunCalc.event(date, place.latitude, place.longitude, place.zone, deg, rising = false) ?: sunset.plus(Duration.ofMinutes(DEFAULT_TZEIT_MIN))
        } else {
            sunset.plus(Duration.ofMinutes(tzeit.minutesAfterSunset!!.toLong()))
        }
    }

    private fun periodStartingOn(first: LocalDate): HolyPeriod? {
        var last = first
        while (isHoly(last.plusDays(1))) last = last.plusDays(1)
        val start = candleLighting(first.minusDays(1)) ?: return null
        val end = havdalah(last) ?: return null
        return HolyPeriod(start, end, first, last)
    }

    override fun periodNear(instant: Instant, buffer: Duration): HolyPeriod? {
        val day = instant.atZone(place.zone).toLocalDate()
        // A run of holy days is at most 3 long, so a window can only involve runs that began within the previous 3 days
        // or begin tomorrow (whose eve — today — is inside the window).
        for (back in 3 downTo -1) {
            val first = day.minusDays(back.toLong())
            if (isHoly(first) && !isHoly(first.minusDays(1))) {
                val p = periodStartingOn(first) ?: continue
                if (!instant.isBefore(p.start.minus(buffer)) && instant.isBefore(p.end.plus(buffer))) return p
            }
        }
        return null
    }

    private companion object {
        const val DEFAULT_TZEIT_MIN = 42L
    }
}

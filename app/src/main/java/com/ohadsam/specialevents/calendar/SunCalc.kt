package com.ohadsam.specialevents.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Sunrise/sunset and twilight times (US Naval Observatory "Almanac for Computers" algorithm, accurate to about a minute at
 * mid-latitudes). Sea-level; no elevation adjustment, matching common candle-lighting practice.
 */
object SunCalc {
    const val SUNSET_ZENITH = 90.8333

    private fun rad(d: Double) = Math.toRadians(d)

    private fun deg(r: Double) = Math.toDegrees(r)

    private fun norm(x: Double, mod: Double) = ((x % mod) + mod) % mod

    /** The instant on local [date] when the sun crosses [zenith] degrees, or null (polar day/night). */
    fun event(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId, zenith: Double, rising: Boolean): Instant? {
        val lngHour = longitude / 15.0
        val n = date.dayOfYear
        val t = n + ((if (rising) 6.0 else 18.0) - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        val l = norm(m + 1.916 * sin(rad(m)) + 0.020 * sin(rad(2 * m)) + 282.634, 360.0)
        var ra = norm(deg(atan(0.91764 * tan(rad(l)))), 360.0)
        ra += floor(l / 90.0) * 90.0 - floor(ra / 90.0) * 90.0
        ra /= 15.0
        val sinDec = 0.39782 * sin(rad(l))
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(rad(zenith)) - sinDec * sin(rad(latitude))) / (cosDec * cos(rad(latitude)))
        if (cosH > 1 || cosH < -1) return null
        val h = (if (rising) 360.0 - deg(acos(cosH)) else deg(acos(cosH))) / 15.0
        val localMeanTime = h + ra - 0.06571 * t - 6.622
        val utHours = norm(localMeanTime - lngHour, 24.0)
        // The UT clock time may belong to the previous/next UTC day relative to the local date; pick the one landing on [date].
        for (offset in -1L..1L) {
            val instant = date.plusDays(offset).atStartOfDay(ZoneOffset.UTC).toInstant().plusMillis((utHours * 3_600_000).toLong())
            if (instant.atZone(zone).toLocalDate() == date) return instant
        }
        return null
    }

    fun sunset(date: LocalDate, lat: Double, lon: Double, zone: ZoneId): Instant? = event(date, lat, lon, zone, SUNSET_ZENITH, rising = false)

    fun sunrise(date: LocalDate, lat: Double, lon: Double, zone: ZoneId): Instant? = event(date, lat, lon, zone, SUNSET_ZENITH, rising = true)
}

package com.ohadsam.specialevents.location

import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPlace(
    val id: String,
    val nameHe: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    val zoneId: String,
    val inIsrael: Boolean,
    val candleLightingMinutes: Int,
) {
    val zone: ZoneId get() = ZoneId.of(zoneId)
}

/** Built-in places for manual selection; the first entry (Jerusalem) is the last-resort default. */
object Places {
    private const val IL = "Asia/Jerusalem"
    private const val DEFAULT_CANDLES = 18
    private const val JERUSALEM_CANDLES = 40

    private fun il(id: String, he: String, lat: Double, lon: Double, elev: Double = 0.0, candles: Int = DEFAULT_CANDLES) =
        GeoPlace(id, he, lat, lon, elev, IL, true, candles)

    private fun world(id: String, he: String, lat: Double, lon: Double, zone: String) =
        GeoPlace(id, he, lat, lon, 0.0, zone, false, DEFAULT_CANDLES)

    val JERUSALEM = il("jerusalem", "ירושלים", 31.7683, 35.2137, 754.0, JERUSALEM_CANDLES)

    val ALL: List<GeoPlace> = listOf(
        JERUSALEM,
        il("tel_aviv", "תל אביב", 32.0853, 34.7818),
        il("haifa", "חיפה", 32.7940, 34.9896),
        il("beer_sheva", "באר שבע", 31.2530, 34.7915),
        il("petah_tikva", "פתח תקווה", 32.0840, 34.8878),
        il("bnei_brak", "בני ברק", 32.0807, 34.8338),
        il("ramat_gan", "רמת גן", 32.0684, 34.8248),
        il("holon", "חולון", 32.0158, 34.7874),
        il("rishon", "ראשון לציון", 31.9730, 34.7925),
        il("rehovot", "רחובות", 31.8928, 34.8113),
        il("ashdod", "אשדוד", 31.8044, 34.6553),
        il("ashkelon", "אשקלון", 31.6688, 34.5743),
        il("netanya", "נתניה", 32.3215, 34.8532),
        il("modiin", "מודיעין", 31.8969, 35.0104),
        il("beit_shemesh", "בית שמש", 31.7470, 34.9881),
        il("elad", "אלעד", 32.0520, 34.9510),
        il("beitar_illit", "ביתר עילית", 31.6951, 35.1163),
        il("tiberias", "טבריה", 32.7959, 35.5310),
        il("safed", "צפת", 32.9646, 35.4960),
        il("afula", "עפולה", 32.6078, 35.2897),
        il("hebron", "חברון", 31.5326, 35.0998),
        il("netivot", "נתיבות", 31.4217, 34.5886),
        il("eilat", "אילת", 29.5577, 34.9519),
        world("new_york", "ניו יורק", 40.7128, -74.0060, "America/New_York"),
        world("los_angeles", "לוס אנג׳לס", 34.0522, -118.2437, "America/Los_Angeles"),
        world("miami", "מיאמי", 25.7617, -80.1918, "America/New_York"),
        world("chicago", "שיקגו", 41.8781, -87.6298, "America/Chicago"),
        world("toronto", "טורונטו", 43.6532, -79.3832, "America/Toronto"),
        world("montreal", "מונטריאול", 45.5019, -73.5674, "America/Toronto"),
        world("mexico_city", "מקסיקו סיטי", 19.4326, -99.1332, "America/Mexico_City"),
        world("buenos_aires", "בואנוס איירס", -34.6037, -58.3816, "America/Argentina/Buenos_Aires"),
        world("london", "לונדון", 51.5074, -0.1278, "Europe/London"),
        world("manchester", "מנצ׳סטר", 53.4808, -2.2426, "Europe/London"),
        world("paris", "פריז", 48.8566, 2.3522, "Europe/Paris"),
        world("antwerp", "אנטוורפן", 51.2194, 4.4025, "Europe/Brussels"),
        world("zurich", "ציריך", 47.3769, 8.5417, "Europe/Zurich"),
        world("moscow", "מוסקבה", 55.7558, 37.6173, "Europe/Moscow"),
        world("johannesburg", "יוהנסבורג", -26.2041, 28.0473, "Africa/Johannesburg"),
        world("melbourne", "מלבורן", -37.8136, 144.9631, "Australia/Melbourne"),
        world("sydney", "סידני", -33.8688, 151.2093, "Australia/Sydney"),
    )

    fun byId(id: String?): GeoPlace? = ALL.firstOrNull { it.id == id }

    fun search(query: String): List<GeoPlace> {
        val q = query.trim()
        return if (q.isEmpty()) ALL else ALL.filter { it.nameHe.contains(q) || it.id.contains(q, ignoreCase = true) }
    }

    /** Great-circle distance in km (haversine). */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lon2 - lon1)
        val a = sin(dp / 2).pow(2) + cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}

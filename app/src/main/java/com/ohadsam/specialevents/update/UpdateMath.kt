package com.ohadsam.specialevents.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/** What `version.json` on the GitHub release `latest` says about the newest build. */
data class RemoteVersion(val versionName: String, val versionCode: Int, val sha256: String, val apkUrl: String, val notes: String)

/**
 * Rules of the in-app updater, as pure functions: where updates may come from, how the release manifest is read and checked,
 * and when a build counts as newer. The network, the download and the installer live in :app.
 */
object UpdateMath {
    const val RELEASE_PAGE = "https://github.com/ohadsam/special_events/releases/tag/latest"
    const val VERSION_URL = "https://github.com/ohadsam/special_events/releases/download/latest/version.json"
    const val DEFAULT_APK_URL = "https://github.com/ohadsam/special_events/releases/download/latest/SpecialEvents.apk"
    const val MAX_MANIFEST_BYTES = 16 * 1024
    const val MAX_APK_BYTES = 150L * 1024 * 1024

    private val ALLOWED_HOST_SUFFIXES = listOf("github.com", "githubusercontent.com")
    private val json = Json { ignoreUnknownKeys = true; isLenient = false }
    private val HEX64 = Regex("^[0-9a-fA-F]{64}$")

    /** HTTPS only, no credentials in the URL, host is github.com or *.githubusercontent.com (redirects of release downloads). */
    fun validUrl(url: String): Boolean {
        if (url.length > 400 || !url.startsWith("https://")) return false
        val rest = url.removePrefix("https://")
        val host = rest.substringBefore('/').substringBefore('?')
        if (host.isEmpty() || '@' in host || ':' in host || ' ' in host) return false
        return ALLOWED_HOST_SUFFIXES.any { host == it || host.endsWith(".$it") }
    }

    /** Null for anything that is not a complete, plausible manifest (never throws). */
    fun parse(body: String): RemoteVersion? {
        if (body.length > MAX_MANIFEST_BYTES) return null
        val o = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val name = o.str("versionName")?.takeIf { it.length in 1..30 && it.all { c -> c.isLetterOrDigit() || c in ".-+_" } } ?: return null
        val code = (o["versionCode"] as? JsonPrimitive)?.intOrNull?.takeIf { it in 1..2_000_000_000 } ?: return null
        val sha = o.str("sha256")?.takeIf { HEX64.matches(it) }?.lowercase() ?: return null
        val apk = o.str("apk")?.takeIf { validUrl(it) } ?: DEFAULT_APK_URL
        val notes = (o.str("notes") ?: "").filter { !it.isISOControl() }.take(400)
        return RemoteVersion(name, code, sha, apk, notes)
    }

    fun isNewer(installedCode: Long, remoteCode: Int): Boolean = remoteCode.toLong() > installedCode

    fun sha256Matches(expectedHex: String, actualHex: String): Boolean = expectedHex.equals(actualHex, ignoreCase = true) && HEX64.matches(actualHex)

    private fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.contentOrNull
}


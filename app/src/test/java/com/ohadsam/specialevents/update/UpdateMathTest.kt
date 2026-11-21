package com.ohadsam.specialevents.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateMathTest {
    private val sha = "a".repeat(64)
    private fun manifest(extra: String = "") = """{"versionName":"0.2.0","versionCode":250,"sha256":"$sha","apk":"https://github.com/ohadsam/special_events/releases/download/latest/SpecialEvents.apk","notes":"Pocket watch"$extra}"""

    @Test fun parsesAGoodManifest() {
        val v = requireNotNull(UpdateMath.parse(manifest()))
        assertEquals("0.2.0", v.versionName); assertEquals(250, v.versionCode); assertEquals("Pocket watch", v.notes)
    }

    @Test fun rejectsBrokenOrHostileManifests() {
        for (bad in listOf("", "{", "[]", manifest().replace("250", "0"), manifest().replace(sha, "xyz"),
            manifest().replace("0.2.0", "<script>"), manifest().replace("\"versionName\":\"0.2.0\",", ""), "x".repeat(20_000))) assertNull(bad.take(40), UpdateMath.parse(bad))
    }

    @Test fun foreignApkUrlFallsBackToTheOfficialOne() {
        val v = UpdateMath.parse(manifest().replace("github.com/ohadsam", "evil.example.com/ohadsam"))!!
        assertEquals(UpdateMath.DEFAULT_APK_URL, v.apkUrl)
    }

    @Test fun onlyGithubHttpsUrlsAreAllowed() {
        assertTrue(UpdateMath.validUrl("https://github.com/ohadsam/x/releases/download/latest/a.apk"))
        assertTrue(UpdateMath.validUrl("https://objects.githubusercontent.com/github-production/abc?X=1"))
        assertTrue(UpdateMath.validUrl("https://release-assets.githubusercontent.com/a/b"))
        for (bad in listOf("http://github.com/a", "https://github.com.evil.com/a", "https://evilgithub.com/a", "https://user@github.com/a", "https://github.com:8443/a", "https://evil.com/github.com", "ftp://github.com/a", "", "https://"))
            assertFalse(bad, UpdateMath.validUrl(bad))
    }

    @Test fun versionOrderAndHashCheck() {
        assertTrue(UpdateMath.isNewer(249, 250)); assertFalse(UpdateMath.isNewer(250, 250)); assertFalse(UpdateMath.isNewer(251, 250))
        assertTrue(UpdateMath.sha256Matches(sha, sha.uppercase()))
        assertFalse(UpdateMath.sha256Matches(sha, "b".repeat(64))); assertFalse(UpdateMath.sha256Matches("short", "short"))
    }

}

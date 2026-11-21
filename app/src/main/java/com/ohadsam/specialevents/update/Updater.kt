package com.ohadsam.specialevents.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * In-app update: read the release manifest, download the APK, verify it (hash, package, higher version, SAME signing key),
 * then hand it to the system installer as an update. All rules (allowed hosts, version order) are in :core UpdateMath.
 * Every function blocks: call them from a background thread.
 */
object Updater {
    sealed interface Check {
        data class UpToDate(val installed: String) : Check
        data class Available(val remote: RemoteVersion) : Check
        data class Failed(val reason: String) : Check
    }

    const val ACTION_INSTALL_RESULT = "com.ohadsam.specialevents.INSTALL_RESULT"
    private const val PREFS = "update"

    fun installedInfo(ctx: Context): PackageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    fun installedCode(ctx: Context): Long = PackageInfoCompat.getLongVersionCode(installedInfo(ctx))
    fun installedName(ctx: Context): String = installedInfo(ctx).versionName ?: ""

    fun check(ctx: Context): Check {
        val body = fetchText(UpdateMath.VERSION_URL, UpdateMath.MAX_MANIFEST_BYTES) ?: return Check.Failed("network")
        val remote = UpdateMath.parse(body) ?: return Check.Failed("manifest")
        return if (UpdateMath.isNewer(installedCode(ctx), remote.versionCode)) Check.Available(remote) else Check.UpToDate(installedName(ctx))
    }

    /** Downloads to the cache and returns the file only when the SHA-256 matches the manifest; null otherwise. */
    fun download(ctx: Context, remote: RemoteVersion, onProgress: (Int) -> Unit): File? {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        val dest = File(dir, "SpecialEvents.apk").also { it.delete() }
        val conn = open(remote.apkUrl) ?: return null
        return try {
            val total = conn.contentLengthLong
            if (total > UpdateMath.MAX_APK_BYTES) return null
            val md = MessageDigest.getInstance("SHA-256")
            var done = 0L; var lastPct = -1
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        done += n
                        if (done > UpdateMath.MAX_APK_BYTES) { dest.delete(); return null }
                        md.update(buf, 0, n); out.write(buf, 0, n)
                        if (total > 0) { val pct = (done * 100 / total).toInt(); if (pct != lastPct) { lastPct = pct; onProgress(pct) } }
                    }
                }
            }
            val hex = md.digest().joinToString("") { "%02x".format(it) }
            if (!UpdateMath.sha256Matches(remote.sha256, hex)) { dest.delete(); null } else dest
        } catch (_: Exception) { dest.delete(); null } finally { conn.disconnect() }
    }

    /** Null when the file may be installed over this app; otherwise a short reason code. */
    fun verify(ctx: Context, file: File): String? {
        val pm = ctx.packageManager
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(file.path, flags) ?: return "unreadable"
        if (archive.packageName != ctx.packageName) return "package"
        if (!UpdateMath.isNewer(installedCode(ctx), PackageInfoCompat.getLongVersionCode(archive).toInt())) return "not_newer"
        val installed = pm.getPackageInfo(ctx.packageName, flags)
        return if (signers(archive).isNotEmpty() && signers(archive) == signers(installed)) null else "signature"
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> =
        (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)?.map { it.toCharsString() }?.toSet() ?: emptySet()

    fun canInstall(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    /** Streams the APK into a PackageInstaller session as an UPDATE of this app; the result arrives in InstallReceiver. */
    fun install(ctx: Context, file: File): Boolean = try {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply { setAppPackageName(ctx.packageName) }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            file.inputStream().use { input -> session.openWrite("SpecialEvents.apk", 0, file.length()).use { out -> input.copyTo(out); session.fsync(out) } }
            val intent = Intent(ctx, InstallReceiver::class.java).setAction(ACTION_INSTALL_RESULT).setPackage(ctx.packageName)
            val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            session.commit(PendingIntent.getBroadcast(ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutable).intentSender)
        }
        true
    } catch (_: Exception) { false }

    fun lastInstallStatus(ctx: Context): String? = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("status", null)
    fun setInstallStatus(ctx: Context, status: String) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("status", status).apply() }

    private fun fetchText(url: String, maxBytes: Int): String? {
        val c = open(url) ?: return null
        return try {
            c.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(4096)
                while (true) { val n = input.read(buf); if (n < 0) break; out.write(buf, 0, n); if (out.size() > maxBytes) return null }
                out.toString("UTF-8")
            }
        } catch (_: Exception) { null } finally { c.disconnect() }
    }

    /** GET with manual redirects; every hop must pass UpdateMath.validUrl. Null on any problem. */
    private fun open(start: String): HttpURLConnection? {
        var url = start
        repeat(6) {
            if (!UpdateMath.validUrl(url)) return null
            val c = try { URL(url).openConnection() as HttpURLConnection } catch (_: Exception) { return null }
            c.instanceFollowRedirects = false; c.connectTimeout = 15_000; c.readTimeout = 30_000
            c.setRequestProperty("Accept", "*/*")
            val code = try { c.responseCode } catch (_: Exception) { c.disconnect(); return null }
            if (code in 300..399) {
                val loc = c.getHeaderField("Location"); c.disconnect()
                url = try { URL(URL(url), loc ?: return null).toString() } catch (_: Exception) { return null }
                return@repeat
            }
            if (code == 200) return c
            c.disconnect(); return null
        }
        return null
    }
}

package com.ohadsam.specialevents.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Files handed to other apps live in the cache and are shared through the FileProvider, so no storage permission is involved. */
object BackupFiles {
    private const val BACKUP_DIR = "backup"
    private const val SHARE_DIR = "share"

    fun fileName(prefix: String, extension: String, date: java.time.LocalDate) = "$prefix-$date.$extension"

    fun backupFile(context: Context, name: String): File = fresh(context, BACKUP_DIR, name)

    fun apkFile(context: Context, name: String): File = fresh(context, SHARE_DIR, name)

    fun uriFor(context: Context, file: File): Uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)

    /** Only one shared file per folder is kept, so the cache never fills up with old copies. */
    private fun fresh(context: Context, dir: String, name: String): File {
        val folder = File(context.cacheDir, dir).apply { mkdirs() }
        folder.listFiles()?.forEach { it.delete() }
        return File(folder, name)
    }

    /** True when the installed app consists of several APK files (a bundle), which cannot be shared as one installable file. */
    fun isSplitInstall(context: Context): Boolean = !context.applicationInfo.splitSourceDirs.isNullOrEmpty()

    fun installedApk(context: Context): File = File(context.applicationInfo.sourceDir)
}

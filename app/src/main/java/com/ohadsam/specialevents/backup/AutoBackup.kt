package com.ohadsam.specialevents.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ohadsam.specialevents.BuildConfig
import com.ohadsam.specialevents.SpecialEventsApp
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.backup.BackupCodec
import com.ohadsam.specialevents.domain.backup.BackupRetention
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/** Writes a backup into the folder the user chose (Storage Access Framework, so no storage permission) and keeps only the newest copies. */
object AutoBackup {
    private const val WORK_NAME = "auto-backup"

    const val ERROR_NO_FOLDER = "no-folder"
    const val ERROR_WRITE = "write"
    const val ERROR_PERMISSION = "permission"

    /** Makes WorkManager match the settings: a periodic job when a folder and an interval are set, nothing otherwise. */
    fun schedule(context: Context, settings: AppSettings) {
        val work = WorkManager.getInstance(context)
        if (settings.autoBackupFolder.isBlank() || settings.autoBackupDays <= 0) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(settings.autoBackupDays.toLong(), TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Runs one backup now. Returns "" on success, otherwise one of the ERROR_ codes; the result is also stored in the settings. */
    suspend fun runNow(context: Context): String {
        val container = (context.applicationContext as SpecialEventsApp).container
        val settings = container.settings.settings.first()
        val error = write(context, container.backups.snapshot().let { BackupCodec.encode(it, Instant.now().toString(), BuildConfig.VERSION_NAME) }, settings)
        container.settings.update { it.copy(lastBackupAt = if (error.isEmpty()) System.currentTimeMillis() else it.lastBackupAt, lastBackupError = error) }
        container.logger.i("AutoBackup", if (error.isEmpty()) "backup written" else "backup failed: $error")
        return error
    }

    private fun write(context: Context, text: String, settings: AppSettings): String {
        val tree = settings.autoBackupFolder.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return ERROR_NO_FOLDER
        val resolver = context.contentResolver
        return try {
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val created = DocumentsContract.createDocument(resolver, parent, "application/json", BackupRetention.fileName(LocalDate.now())) ?: return ERROR_WRITE
            resolver.openOutputStream(created, "wt")?.use { it.write(text.toByteArray()) } ?: return ERROR_WRITE
            prune(context, tree, settings.autoBackupKeep)
            ""
        } catch (e: SecurityException) {
            ERROR_PERMISSION
        } catch (e: IOException) {
            ERROR_WRITE
        } catch (e: IllegalArgumentException) {
            ERROR_NO_FOLDER
        }
    }

    private fun prune(context: Context, tree: Uri, keep: Int) {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val byName = LinkedHashMap<String, String>()
        resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) byName[c.getString(1).orEmpty()] = c.getString(0).orEmpty()
        }
        BackupRetention.toDelete(byName.keys.toList(), keep).forEach { name ->
            byName[name]?.let { id -> runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) } }
        }
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = if (AutoBackup.runNow(applicationContext).isEmpty()) Result.success() else Result.retry()
}

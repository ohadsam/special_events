package com.ohadsam.specialevents.ui.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.BuildConfig
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.backup.AutoBackup
import com.ohadsam.specialevents.backup.BackupFiles
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.backup.BackupCodec
import com.ohadsam.specialevents.domain.backup.BackupCrypto
import com.ohadsam.specialevents.domain.backup.CsvExport
import com.ohadsam.specialevents.domain.backup.CsvField
import com.ohadsam.specialevents.domain.backup.CsvImport
import com.ohadsam.specialevents.domain.backup.CsvMapping
import com.ohadsam.specialevents.domain.backup.CsvParser
import com.ohadsam.specialevents.domain.backup.DecryptResult
import com.ohadsam.specialevents.domain.backup.ExportDays
import com.ohadsam.specialevents.domain.backup.IcsExport
import com.ohadsam.specialevents.domain.backup.BackupData
import com.ohadsam.specialevents.domain.backup.BackupError
import com.ohadsam.specialevents.domain.backup.BackupPlanner
import com.ohadsam.specialevents.domain.backup.DecodeResult
import com.ohadsam.specialevents.domain.backup.ImportMode
import com.ohadsam.specialevents.domain.backup.ImportPreview
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())

/** A file the user picked, read and checked, waiting for a decision about how to import it. */
data class PendingImport(
    val data: BackupData,
    val skipped: Int,
    val mode: ImportMode,
    val preview: ImportPreview,
    /** "backup" (my own file) or "share" (events someone sent me): the dialog words itself accordingly. */
    val kind: String = BackupCodec.KIND_BACKUP,
    /** Events of the file the user wants to take in (ids as numbered in the file). */
    val selected: Set<Long> = emptySet(),
    /** Events of the file that already exist here. */
    val existing: Set<Long> = emptySet(),
)

/** What the user is saving to a file. */
enum class ExportKind(val extension: String, val mime: String) {
    BACKUP("json", "application/json"),
    ENCRYPTED("sevents", "application/octet-stream"),
    ICS("ics", "text/calendar"),
    CSV("csv", "text/csv"),
}

/** A spreadsheet that was read and waits for the user to confirm which column is which. */
data class CsvDraft(val header: List<String>, val rows: List<List<String>>, val mapping: CsvMapping)

data class BackupState(
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val pending: PendingImport? = null,
    /** An encrypted file was chosen and waits for its password. */
    val needPassword: Boolean = false,
    val csv: CsvDraft? = null,
)

class BackupViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state
    private val context: Context get() = container.appContext

    /** Folder, interval and last result of the automatic backup. */
    val settings: StateFlow<AppSettings> = container.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Remembers the folder the user picked; the permission survives restarts so the background job can write there. */
    fun setFolder(uri: Uri) {
        val ok = runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }.isSuccess
        if (!ok) {
            _state.update { it.copy(message = UiMessage(R.string.backup_auto_err_permission)) }
            return
        }
        viewModelScope.launch { container.settings.update { it.copy(autoBackupFolder = uri.toString(), lastBackupError = "") } }
    }

    fun setAutoDays(days: Int) {
        viewModelScope.launch { container.settings.update { it.copy(autoBackupDays = days) } }
    }

    fun setAutoKeep(keep: Int) {
        viewModelScope.launch { container.settings.update { it.copy(autoBackupKeep = keep) } }
    }

    /** One backup into the chosen folder right now. */
    fun backupNow() = launchBusy {
        val error = AutoBackup.runNow(context)
        _state.update { it.copy(message = UiMessage(if (error.isEmpty()) R.string.backup_auto_done else R.string.backup_auto_failed)) }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    /** Held only in memory while an encrypted file waits for its password. */
    private var encryptedText: String? = null

    fun suggestedName(kind: ExportKind): String = BackupFiles.fileName("special-events", kind.extension, LocalDate.now())

    /** Writes the chosen kind of file into the place the user picked in the system picker. [password] is only used for [ExportKind.ENCRYPTED]. */
    fun exportTo(uri: Uri, kind: ExportKind, password: CharArray?) = runWithMessage(R.string.backup_saved) {
        val text = contentFor(kind, password)
        withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("no stream") }
    }

    /** Writes a backup to the cache and hands the file to [onReady] (on the main thread) for sharing. */
    fun prepareShare(kind: ExportKind, password: CharArray?, onReady: (File) -> Unit) = launchBusy {
        val text = contentFor(kind, password)
        val file = withContext(Dispatchers.IO) { BackupFiles.backupFile(context, suggestedName(kind)).also { it.writeText(text) } }
        onReady(file)
    }

    private suspend fun contentFor(kind: ExportKind, password: CharArray?): String = withContext(Dispatchers.Default) {
        val today = LocalDate.now()
        when (kind) {
            ExportKind.BACKUP -> encodeNow()
            ExportKind.ENCRYPTED -> BackupCrypto.encrypt(encodeNow(), requireNotNull(password) { "password" })
            ExportKind.CSV -> CsvExport.build(container.events.allEvents.first())
            ExportKind.ICS -> {
                val days = ExportDays.of(container.events.allEvents.first(), today, ICS_YEARS, container.settings.settings.first().dateRules)
                IcsExport.build(days, ICS_STAMP.format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)), BuildConfig.VERSION_NAME)
            }
        }
    }

    fun prepareApk(onReady: (File) -> Unit) {
        if (BackupFiles.isSplitInstall(context)) {
            _state.update { it.copy(message = UiMessage(R.string.share_app_split)) }
            return
        }
        launchBusy {
            val file = withContext(Dispatchers.IO) {
                BackupFiles.apkFile(context, "SpecialEvents-v${BuildConfig.VERSION_NAME}.apk").also { BackupFiles.installedApk(context).copyTo(it, overwrite = true) }
            }
            onReady(file)
        }
    }

    /** Reads the chosen file (size-limited) and shows what importing it would do. */
    fun load(uri: Uri) = launchBusy {
        val text = withContext(Dispatchers.IO) { readLimited(uri) }
        if (text != null && BackupCrypto.isEncrypted(text)) {
            encryptedText = text
            _state.update { it.copy(needPassword = true) }
        } else {
            showPreview(text)
        }
    }

    /** Reads a spreadsheet and proposes which column is the name, the date, and so on. */
    fun loadCsv(uri: Uri) = launchBusy {
        val rows = withContext(Dispatchers.IO) { readLimited(uri) }?.let(CsvParser::parse).orEmpty()
        if (rows.size < 2) {
            _state.update { it.copy(message = UiMessage(R.string.csv_err_empty)) }
        } else {
            _state.update { it.copy(csv = CsvDraft(rows.first(), rows.drop(1), CsvImport.guessMapping(rows.first()))) }
        }
    }

    fun setCsvColumn(field: CsvField, column: Int?) = _state.update { s ->
        s.copy(csv = s.csv?.let { d -> d.copy(mapping = CsvMapping(if (column == null) d.mapping.columns - field else d.mapping.columns + (field to column))) })
    }

    fun cancelCsv() = _state.update { it.copy(csv = null) }

    /** Turns the rows into events (unreadable rows are counted, not guessed) and shows the usual import preview. */
    fun confirmCsv() {
        val draft = _state.value.csv ?: return
        _state.update { it.copy(csv = null) }
        launchBusy {
            val result = withContext(Dispatchers.Default) { CsvImport.toEvents(draft.rows, draft.mapping, LocalDate.now()) }
            if (result.events.isEmpty()) {
                _state.update { it.copy(message = UiMessage(R.string.csv_err_none)) }
            } else {
                showPending(BackupData(result.events, emptyList(), emptyList(), emptyList()), result.skippedRows, BackupCodec.KIND_CSV)
            }
        }
    }

    /** Tries the password on the waiting encrypted file; a wrong one just asks again. */
    fun submitPassword(password: CharArray) = launchBusy {
        val text = encryptedText ?: return@launchBusy
        when (val result = withContext(Dispatchers.Default) { BackupCrypto.decrypt(text, password) }) {
            is DecryptResult.Ok -> {
                encryptedText = null
                _state.update { it.copy(needPassword = false) }
                showPreview(result.text)
            }
            DecryptResult.WrongPassword -> _state.update { it.copy(message = UiMessage(R.string.backup_err_password)) }
            DecryptResult.Corrupt -> {
                encryptedText = null
                _state.update { it.copy(needPassword = false, message = UiMessage(R.string.backup_err_format)) }
            }
        }
    }

    fun cancelPassword() {
        encryptedText = null
        _state.update { it.copy(needPassword = false) }
    }

    private suspend fun showPreview(text: String?) {
        when (val result = text?.let(BackupCodec::decode) ?: DecodeResult.Failure(BackupError.TOO_BIG)) {
            is DecodeResult.Failure -> _state.update { it.copy(message = UiMessage(errorText(result.error))) }
            is DecodeResult.Ok -> showPending(result.data, result.skipped, result.kind)
        }
    }

    private suspend fun showPending(data: BackupData, skipped: Int, kind: String) {
        val here = container.events.allEvents.first()
        val known = here.map(BackupPlanner::key).toSet()
        val pending = PendingImport(
            data, skipped, ImportMode.MERGE, container.backups.preview(data, ImportMode.MERGE), kind,
            selected = BackupPlanner.defaultSelection(here, data.events, ImportMode.MERGE),
            existing = data.events.filter { BackupPlanner.key(it) in known }.map { it.id }.toSet(),
        )
        _state.update { it.copy(pending = pending) }
    }

    fun setMode(mode: ImportMode) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            val preview = container.backups.preview(pending.data, mode)
            val selected = BackupPlanner.defaultSelection(container.events.allEvents.first(), pending.data.events, mode)
            _state.update { s -> s.copy(pending = s.pending?.copy(mode = mode, preview = preview, selected = selected)) }
        }
    }

    fun cancelImport() = _state.update { it.copy(pending = null) }

    fun toggleImportEvent(id: Long) = _state.update { s ->
        s.copy(pending = s.pending?.let { p -> p.copy(selected = if (id in p.selected) p.selected - id else p.selected + id) })
    }

    fun confirmImport() {
        val pending = _state.value.pending ?: return
        _state.update { it.copy(pending = null) }
        launchBusy {
            val added = container.backups.import(BackupPlanner.restrict(pending.data, pending.selected), pending.mode, defaultReminders = pending.kind != BackupCodec.KIND_BACKUP)
            container.logger.i("Backup", "imported $added events (${pending.mode})")
            _state.update { it.copy(message = UiMessage(R.string.backup_imported, listOf(added))) }
        }
    }

    private suspend fun encodeNow(): String = BackupCodec.encode(container.backups.snapshot(), Instant.now().toString(), BuildConfig.VERSION_NAME)

    private fun readLimited(uri: Uri): String? = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        val buffer = CharArray(BackupCodec.MAX_CHARS + 1)
        var used = 0
        while (used < buffer.size) {
            val n = reader.read(buffer, used, buffer.size - used)
            if (n < 0) break
            used += n
        }
        if (used > BackupCodec.MAX_CHARS) null else String(buffer, 0, used)
    }

    @StringRes
    private fun errorText(error: BackupError) = when (error) {
        BackupError.NOT_JSON, BackupError.WRONG_FORMAT -> R.string.backup_err_format
        BackupError.TOO_NEW -> R.string.backup_err_new
        BackupError.TOO_BIG -> R.string.backup_err_big
    }

    private fun runWithMessage(@StringRes success: Int, block: suspend () -> Unit) = launchBusy {
        block()
        _state.update { it.copy(message = UiMessage(success)) }
    }

    /** The file picker came back after the password was lost (the screen was recreated while it was open). */
    fun passwordLost() = _state.update { it.copy(message = UiMessage(R.string.backup_err_need_password)) }

    private fun launchBusy(block: suspend () -> Unit) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: java.io.IOException) {
                container.logger.w("Backup", "failed", e)
                _state.update { it.copy(message = UiMessage(R.string.backup_err_io)) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // A bad file, a revoked folder permission or a parser error must end in a message, not a crash.
                container.logger.w("Backup", "failed", e)
                _state.update { it.copy(message = UiMessage(R.string.backup_err_io)) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        const val ICS_YEARS = 10
        val ICS_STAMP: java.time.format.DateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BackupViewModel(container) as T
    }
}

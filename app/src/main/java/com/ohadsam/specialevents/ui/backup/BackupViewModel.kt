package com.ohadsam.specialevents.ui.backup

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.BuildConfig
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.backup.BackupFiles
import com.ohadsam.specialevents.domain.backup.BackupCodec
import com.ohadsam.specialevents.domain.backup.BackupData
import com.ohadsam.specialevents.domain.backup.BackupError
import com.ohadsam.specialevents.domain.backup.DecodeResult
import com.ohadsam.specialevents.domain.backup.ImportMode
import com.ohadsam.specialevents.domain.backup.ImportPreview
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())

/** A file the user picked, read and checked, waiting for a decision about how to import it. */
data class PendingImport(val data: BackupData, val skipped: Int, val mode: ImportMode, val preview: ImportPreview)

data class BackupState(val busy: Boolean = false, val message: UiMessage? = null, val pending: PendingImport? = null)

class BackupViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state
    private val context: Context get() = container.appContext

    fun messageShown() = _state.update { it.copy(message = null) }

    fun suggestedName(): String = BackupFiles.fileName("special-events", "json", LocalDate.now())

    /** Writes a backup into the file the user chose in the system picker. */
    fun exportTo(uri: Uri) = runWithMessage(R.string.backup_saved) {
        val text = encodeNow()
        withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("no stream") }
    }

    /** Writes a backup to the cache and hands the file to [onReady] (on the main thread) for sharing. */
    fun prepareShare(onReady: (File) -> Unit) = launchBusy {
        val text = encodeNow()
        val file = withContext(Dispatchers.IO) { BackupFiles.backupFile(context, suggestedName()).also { it.writeText(text) } }
        onReady(file)
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
        when (val result = text?.let(BackupCodec::decode) ?: DecodeResult.Failure(BackupError.TOO_BIG)) {
            is DecodeResult.Failure -> _state.update { it.copy(message = UiMessage(errorText(result.error))) }
            is DecodeResult.Ok -> _state.update {
                it.copy(pending = PendingImport(result.data, result.skipped, ImportMode.MERGE, container.backups.preview(result.data, ImportMode.MERGE)))
            }
        }
    }

    fun setMode(mode: ImportMode) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            val preview = container.backups.preview(pending.data, mode)
            _state.update { s -> s.copy(pending = s.pending?.copy(mode = mode, preview = preview)) }
        }
    }

    fun cancelImport() = _state.update { it.copy(pending = null) }

    fun confirmImport() {
        val pending = _state.value.pending ?: return
        _state.update { it.copy(pending = null) }
        launchBusy {
            val added = container.backups.import(pending.data, pending.mode)
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

    private fun launchBusy(block: suspend () -> Unit) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: java.io.IOException) {
                container.logger.w("Backup", "failed", e)
                _state.update { it.copy(message = UiMessage(R.string.backup_err_io)) }
            } catch (e: IllegalStateException) {
                container.logger.w("Backup", "failed", e)
                _state.update { it.copy(message = UiMessage(R.string.backup_err_io)) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BackupViewModel(container) as T
    }
}

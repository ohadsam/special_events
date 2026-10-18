package com.ohadsam.specialevents.greetings

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Records a short AAC voice greeting into the cache folder (deleted by the system when space is needed) and plays it back for a check. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var target: File? = null

    val isRecording: Boolean get() = recorder != null

    /** Returns false when the microphone is busy or unavailable. */
    fun start(): Boolean {
        stopPlayback()
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // one draft at a time
        val file = File(dir, "greeting-${System.currentTimeMillis()}.m4a")
        val r = newRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(BIT_RATE)
            r.setAudioSamplingRate(SAMPLE_RATE)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            target = file
            true
        } catch (e: java.io.IOException) {
            r.release()
            false
        } catch (e: IllegalStateException) {
            r.release()
            false
        }
    }

    /** Stops and returns the recording, or null if it was too short to be valid. */
    fun stop(): File? {
        val r = recorder ?: return null
        recorder = null
        val file = target
        return try {
            r.stop()
            file
        } catch (e: RuntimeException) {
            file?.delete() // stop() throws when nothing was captured
            null
        } finally {
            r.release()
        }
    }

    fun play(file: File, onDone: () -> Unit): Boolean {
        stopPlayback()
        return try {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    onDone()
                }
                prepare()
                start()
            }
            true
        } catch (e: java.io.IOException) {
            stopPlayback()
            false
        }
    }

    fun stopPlayback() {
        player?.release()
        player = null
    }

    fun release() {
        if (recorder != null) stop()
        stopPlayback()
    }

    fun discard() {
        release()
        File(context.cacheDir, DIR).listFiles()?.forEach { it.delete() }
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    companion object {
        const val DIR = "voice"
        private const val BIT_RATE = 64_000
        private const val SAMPLE_RATE = 44_100
    }
}

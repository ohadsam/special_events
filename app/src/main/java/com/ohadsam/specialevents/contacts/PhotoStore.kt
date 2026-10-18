package com.ohadsam.specialevents.contacts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

/** Event photos live in the app's private storage (downscaled JPEG), so they survive the picker's temporary access and stay private. */
object PhotoStore {
    private const val MAX_SIDE = 512
    private const val QUALITY = 85
    private const val CACHE_ENTRIES = 80
    private val cache = LruCache<String, Bitmap>(CACHE_ENTRIES)

    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    /** Copies a picked image (gallery / contact) into private storage; returns a `file:` URI string, or null if it cannot be read. */
    fun saveFrom(context: Context, source: Uri): String? = runCatching {
        val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() } ?: return null
        save(context, bytes)
    }.getOrNull()

    fun save(context: Context, bytes: ByteArray): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val rotated = rotate(decoded, orientationOf(bytes))
        val scale = MAX_SIDE.toFloat() / maxOf(rotated.width, rotated.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(rotated, (rotated.width * scale).toInt(), (rotated.height * scale).toInt(), true) else rotated
        val file = File(dir(context), "${UUID.randomUUID()}.jpg")
        file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        Uri.fromFile(file).toString()
    }.getOrNull()

    /** A small decoded copy for lists and avatars, cached. */
    fun thumb(uri: String, sidePx: Int): Bitmap? {
        val key = "$uri@$sidePx"
        cache.get(key)?.let { return it }
        val path = Uri.parse(uri).path ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= sidePx) sample *= 2
        val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        cache.put(key, bmp)
        return bmp
    }

    fun delete(uri: String?) {
        val path = uri?.let { Uri.parse(it).path } ?: return
        runCatching { File(path).delete() }
    }

    private fun orientationOf(bytes: ByteArray): Int =
        runCatching { ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            .getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun rotate(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    }
}

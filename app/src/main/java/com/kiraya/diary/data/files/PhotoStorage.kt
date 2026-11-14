package com.kiraya.diary.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import com.kiraya.diary.core.coroutines.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps photos (CNIC, agreements) in the app's private files folder.
 * The database stores only the file name.
 */
@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val dir: File get() = File(context.filesDir, DIR).apply { mkdirs() }

    fun file(name: String): File = File(dir, name)

    /** A new empty file in the cache for the camera to write into. */
    fun newCameraFile(): File {
        val camDir = File(context.cacheDir, "camera").apply { mkdirs() }
        return File(camDir, "cam_${UUID.randomUUID()}.jpg")
    }

    /** Copies an image into app storage, scaled down and rotated upright. Returns the file name. */
    suspend fun import(uri: Uri, prefix: String): String? = withContext(io) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@runCatching null

            val rotation = context.contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            val bitmap = if (rotation != 0f) {
                Bitmap.createBitmap(
                    decoded, 0, 0, decoded.width, decoded.height,
                    Matrix().apply { postRotate(rotation) }, true,
                ).also { if (it != decoded) decoded.recycle() }
            } else {
                decoded
            }

            val name = "${prefix}_${UUID.randomUUID()}.jpg"
            file(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            bitmap.recycle()
            name
        }.getOrNull()
    }

    suspend fun delete(name: String?) {
        if (name.isNullOrBlank()) return
        withContext(io) { file(name).delete() }
    }

    /** Loads a small bitmap for showing on screen. */
    suspend fun loadBitmap(name: String, maxSide: Int): Bitmap? = withContext(io) {
        val f = file(name)
        if (!f.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) {
            sample *= 2
        }
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    companion object {
        const val DIR = "photos"
        private const val MAX_SIDE = 1600
        private const val QUALITY = 85
    }
}

package com.weathersnap.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CompressionResult(
    val compressedFile: File,
    val originalBytes: Long,
    val compressedBytes: Long,
)

@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun compress(
        source: File,
        maxEdgePx: Int = 1600,
        quality: Int = 70,
    ): CompressionResult = withContext(Dispatchers.IO) {
        val originalBytes = source.length()

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)

        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: error("Failed to decode captured image")

        val rotated = applyExifRotation(source, decoded)
        val scaled = scaleToMaxEdge(rotated, maxEdgePx)

        val outDir = File(context.cacheDir, CAPTURES_DIR).apply { mkdirs() }
        val outFile = File(outDir, "compressed_${UUID.randomUUID()}.jpg")
        FileOutputStream(outFile).use { os ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, os)
        }
        if (scaled !== decoded) scaled.recycle()
        if (rotated !== decoded) rotated.recycle()
        decoded.recycle()

        CompressionResult(outFile, originalBytes, outFile.length())
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        val longestEdge = maxOf(width, height)
        while (longestEdge / sample > maxEdge * 2) sample *= 2
        return sample
    }

    private fun applyExifRotation(source: File, bitmap: Bitmap): Bitmap {
        val exif = runCatching { ExifInterface(source.absolutePath) }.getOrNull() ?: return bitmap
        val degrees = when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longest
        val targetW = (bitmap.width * ratio).toInt()
        val targetH = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    companion object { const val CAPTURES_DIR = "captures" }
}

package com.example.data.coach.food

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object FoodImageProcessor {

    /**
     * Creates a temporary image file in the application's cache directory
     * and returns a FileProvider content URI suitable for Camera TakePicture.
     */
    fun createTempImageUri(context: Context): Pair<Uri, File> {
        val cacheFolder = File(context.cacheDir, "camera_photos").apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File.createTempFile(
            "motioniq_photo_${System.currentTimeMillis()}_",
            ".jpg",
            cacheFolder
        ).apply {
            createNewFile()
            deleteOnExit()
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, tempFile)
        return Pair(uri, tempFile)
    }

    /**
     * Safely detects the MIME type of the given image URI.
     */
    fun getMimeType(context: Context, uri: Uri, bytes: ByteArray? = null): String {
        val resolverType = try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }

        if (!resolverType.isNullOrBlank() && resolverType != "application/octet-stream") {
            return resolverType
        }

        if (bytes != null && bytes.size >= 4) {
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
                return "image/jpeg"
            }
            if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
                return "image/png"
            }
            if (bytes.size >= 12 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
                && bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()) {
                return "image/webp"
            }
        }

        // Infer from file extension or fallback to image/jpeg
        val path = uri.path?.lowercase() ?: ""
        return when {
            path.endsWith(".png") -> "image/png"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".heic") || path.endsWith(".heif") -> "image/heic"
            path.endsWith(".gif") -> "image/gif"
            else -> "image/jpeg"
        }
    }

    /**
     * Safely reads raw bytes from any content:// or file:// URI.
     */
    fun readImageBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            }
        } catch (e: Exception) {
            try {
                if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                    val filePath = uri.path ?: ""
                    val file = File(filePath)
                    if (file.exists() && file.canRead()) {
                        file.readBytes()
                    } else null
                } else null
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Safely loads and scales a Bitmap from a content URI with memory protection,
     * aspect ratio preservation, and automatic EXIF orientation normalization.
     */
    fun loadScaledBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            val bytes = readImageBytes(context, uri) ?: return null
            if (bytes.isEmpty()) return null

            // 1. Decode bounds only to determine inSampleSize
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return null

            // Calculate sample size (power of 2)
            var sampleSize = 1
            var halfWidth = rawWidth / 2
            var halfHeight = rawHeight / 2
            while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            // 2. Decode sampled bitmap
            val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

            // 3. Apply EXIF orientation
            val orientation = getExifOrientationFromBytes(context, bytes, uri)
            val orientedBitmap = rotateBitmapIfRequired(decodedBitmap, orientation)

            // 4. Resize strictly within maxDimension while preserving aspect ratio
            val currentWidth = orientedBitmap.width
            val currentHeight = orientedBitmap.height
            if (currentWidth > maxDimension || currentHeight > maxDimension) {
                val scale = min(maxDimension.toFloat() / currentWidth, maxDimension.toFloat() / currentHeight)
                val targetWidth = (currentWidth * scale).toInt().coerceAtLeast(1)
                val targetHeight = (currentHeight * scale).toInt().coerceAtLeast(1)

                val scaledBitmap = Bitmap.createScaledBitmap(orientedBitmap, targetWidth, targetHeight, true)
                if (scaledBitmap != orientedBitmap) {
                    orientedBitmap.recycle()
                }
                scaledBitmap
            } else {
                orientedBitmap
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            // Secondary low-memory emergency fallback
            try {
                val bytes = readImageBytes(context, uri) ?: return null
                val fallbackOptions = BitmapFactory.Options().apply {
                    inSampleSize = 4
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, fallbackOptions)
            } catch (fallbackError: Exception) {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a Bitmap into a Base64 encoded JPEG string.
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Converts a Bitmap into a compressed byte array.
     */
    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), outputStream)
        return outputStream.toByteArray()
    }

    private fun getExifOrientationFromBytes(context: Context, bytes: ByteArray, uri: Uri): Int {
        return try {
            val byteStream = java.io.ByteArrayInputStream(bytes)
            val exif = ExifInterface(byteStream)
            val orient = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            if (orient != ExifInterface.ORIENTATION_UNDEFINED) {
                orient
            } else {
                ExifInterface.ORIENTATION_NORMAL
            }
        } catch (e: Exception) {
            getExifOrientation(context, uri)
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            // First try reading directly from stream
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orient = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                if (orient != ExifInterface.ORIENTATION_UNDEFINED) {
                    return orient
                }
            }

            // Fallback: Copy to temporary cache file for robust EXIF inspection
            val tempFile = File.createTempFile("exif_check_", ".tmp", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val exif = ExifInterface(tempFile.absolutePath)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1.0f, 1.0f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1.0f, 1.0f)
            }
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Cleans up old food analysis cache files older than 1 hour.
     */
    fun cleanOldCacheFiles(context: Context) {
        try {
            val threshold = System.currentTimeMillis() - 3600000
            val cacheFolder = File(context.cacheDir, "camera_photos")
            if (cacheFolder.exists()) {
                cacheFolder.listFiles()?.forEach { file ->
                    if (file.lastModified() < threshold) {
                        file.delete()
                    }
                }
            }
            context.cacheDir.listFiles()?.forEach { file ->
                if ((file.name.startsWith("motioniq_photo_") || file.name.startsWith("food_capture_")) && file.lastModified() < threshold) {
                    file.delete()
                }
            }
        } catch (ignored: Exception) {}
    }
}

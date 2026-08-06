package com.shadow.calorietracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

data class ReviewBundleMedia(
    val bundlePath: String,
    val role: String,
    val ownerId: String?,
    val mimeType: String,
    val byteLength: Long,
    val width: Int,
    val height: Int,
    val sha256: String,
    val file: File,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("path", bundlePath)
        .put("role", role)
        .put("ownerId", ownerId ?: JSONObject.NULL)
        .put("mimeType", mimeType)
        .put("byteLength", byteLength)
        .put("width", width)
        .put("height", height)
        .put("sha256", sha256)
}

/** Re-encodes review media so exported files cannot retain EXIF, XMP, GPS, or device metadata. */
class ReviewBundleMediaSanitizer(context: Context) : Closeable {
    private val directory = File(context.cacheDir, "review-export-${UUID.randomUUID()}").apply { mkdirs() }

    fun sanitize(
        source: File,
        bundlePathWithoutExtension: String,
        role: String,
        ownerId: String? = null,
    ): ReviewBundleMedia? = runCatching {
        require(source.isFile)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        var sampleSize = 1
        while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > MAX_EDGE_PIXELS) {
            sampleSize *= 2
        }
        val decoded = requireNotNull(
            BitmapFactory.decodeFile(source.absolutePath, BitmapFactory.Options().apply { inSampleSize = sampleSize }),
        )
        val oriented = decoded.applyExifOrientation(source)
        val scaled = if (maxOf(oriented.width, oriented.height) > MAX_EDGE_PIXELS) {
            val scale = MAX_EDGE_PIXELS.toFloat() / maxOf(oriented.width, oriented.height)
            Bitmap.createScaledBitmap(
                oriented,
                (oriented.width * scale).toInt().coerceAtLeast(1),
                (oriented.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else oriented
        val flattened = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888).also { target ->
            Canvas(target).apply {
                drawColor(Color.WHITE)
                drawBitmap(scaled, 0f, 0f, null)
            }
        }
        val output = File(directory, "${UUID.randomUUID()}.jpg")
        output.outputStream().buffered().use { stream ->
            check(flattened.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream))
        }
        val result = ReviewBundleMedia(
            bundlePath = "$bundlePathWithoutExtension.jpg",
            role = role,
            ownerId = ownerId,
            mimeType = "image/jpeg",
            byteLength = output.length(),
            width = flattened.width,
            height = flattened.height,
            sha256 = output.sha256(),
            file = output,
        )
        listOf(flattened, scaled, oriented, decoded).distinctBy(System::identityHashCode).forEach(Bitmap::recycle)
        result
    }.getOrNull()

    override fun close() {
        directory.listFiles().orEmpty().forEach(File::delete)
        directory.delete()
    }

    private fun Bitmap.applyExifOrientation(source: File): Bitmap {
        val orientation = runCatching {
            ExifInterface(source).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        return if (matrix.isIdentity) this else Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun File.sha256(): String = inputStream().buffered().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_EDGE_PIXELS = 2_048
        const val JPEG_QUALITY = 90
    }
}

package com.shadow.calorietracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage

/** Decodes large camera images at a bounded size before handing them to ML Kit. */
internal object OcrInputImageLoader {
    data class Loaded(val image: InputImage, private val bitmap: Bitmap) : AutoCloseable {
        override fun close() = bitmap.recycle()
    }

    fun load(context: Context, uri: Uri, maxDimension: Int = 2_048): Loaded {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        requireNotNull(resolver.openInputStream(uri)).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unable to decode image bounds" }

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) sampleSize *= 2
        val decoded = requireNotNull(resolver.openInputStream(uri)).use { input ->
            BitmapFactory.decodeStream(
                input,
                null,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            )
        } ?: error("Unable to decode image")

        val rotation = runCatching {
            requireNotNull(resolver.openInputStream(uri)).use { ExifInterface(it).rotationDegrees }
        }.getOrDefault(0)
        val oriented = if (rotation == 0) {
            decoded
        } else {
            Bitmap.createBitmap(
                decoded,
                0,
                0,
                decoded.width,
                decoded.height,
                Matrix().apply { postRotate(rotation.toFloat()) },
                true,
            ).also { decoded.recycle() }
        }
        return Loaded(InputImage.fromBitmap(oriented, 0), oriented)
    }
}

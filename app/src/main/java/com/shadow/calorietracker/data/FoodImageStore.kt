package com.shadow.calorietracker.data

import android.content.Context
import android.net.Uri
import com.shadow.calorietracker.model.FoodImage
import com.shadow.calorietracker.model.FoodImageSource
import java.io.File
import java.util.UUID

class FoodImageStore(private val context: Context) {
    fun import(uri: Uri): FoodImage {
        val directory = File(context.filesDir, "food-images").apply { mkdirs() }
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val destination = File(directory, "food-${UUID.randomUUID()}.$extension")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open selected image" }
            destination.outputStream().use(input::copyTo)
        }
        return FoodImage(source = FoodImageSource.LOCAL, localPath = destination.absolutePath)
    }
}

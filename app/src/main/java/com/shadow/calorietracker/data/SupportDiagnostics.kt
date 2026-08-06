package com.shadow.calorietracker.data

import android.content.Context
import android.net.Uri
import android.os.Build
import com.shadow.calorietracker.BuildConfig
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

data class SupportExportResult(val diagnosticCount: Int, val hasFeedback: Boolean)

class SupportDiagnosticStore(private val context: Context) {
    private val stagingDirectory = File(context.cacheDir, "diagnostic-staging")
    private val flaggedDirectory = File(context.filesDir, "flagged-scan-diagnostics")

    fun stage(source: Uri, kind: String): File {
        stagingDirectory.mkdirs()
        val extension = when (context.contentResolver.getType(source)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val destination = File(stagingDirectory, "${kind.safeName()}-${UUID.randomUUID()}.$extension")
        requireNotNull(context.contentResolver.openInputStream(source)).use { input ->
            destination.outputStream().use(input::copyTo)
        }
        return destination
    }

    fun discard(path: String?) {
        path?.let(::File)?.takeIf { it.parentFile == stagingDirectory }?.delete()
    }

    fun flag(stagedPath: String, kind: String, failure: String): Boolean {
        val staged = File(stagedPath)
        if (!staged.isFile || staged.parentFile != stagingDirectory) return false
        flaggedDirectory.mkdirs()
        val id = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
        val image = File(flaggedDirectory, "$id.${staged.extension.ifBlank { "jpg" }}")
        staged.copyTo(image)
        val metadata = JSONObject()
            .put("id", id)
            .put("kind", kind)
            .put("failure", failure)
            .put("flaggedAtEpochMillis", System.currentTimeMillis())
            .put("image", image.name)
        File(flaggedDirectory, "$id.json").writeText(metadata.toString(2))
        staged.delete()
        return true
    }

    fun count(): Int = flaggedDirectory.listFiles { file -> file.extension == "json" }?.size ?: 0

    fun clear() {
        flaggedDirectory.listFiles()?.forEach(File::delete)
    }

    fun appendTo(zip: ZipOutputStream, feedback: String): SupportExportResult {
        val reports = flaggedDirectory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .sortedBy(File::getName)
            .mapNotNull { metadataFile ->
                runCatching { JSONObject(metadataFile.readText()) }.getOrNull()?.let { metadata ->
                    val image = File(flaggedDirectory, metadata.optString("image"))
                    if (image.isFile) metadata to image else null
                }
            }
        val trimmedFeedback = feedback.trim()
        val manifest = JSONObject()
            .put("schema", "calorie-quick-support-bundle")
            .put("schemaVersion", 1)
            .put("generatedAtEpochMillis", System.currentTimeMillis())
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("appVersionCode", BuildConfig.VERSION_CODE)
            .put("androidSdk", Build.VERSION.SDK_INT)
            .put("deviceManufacturer", Build.MANUFACTURER)
            .put("deviceModel", Build.MODEL)
            .put("feedback", trimmedFeedback.takeIf(String::isNotEmpty) ?: JSONObject.NULL)
            .put("diagnostics", JSONArray().apply {
                reports.forEachIndexed { index, (metadata, image) ->
                    put(JSONObject(metadata.toString()).put("bundlePath", "images/$index-${image.name}"))
                }
            })
        zip.putNextEntry(ZipEntry("support/support.json"))
        zip.write(manifest.toString(2).toByteArray())
        zip.closeEntry()
        reports.forEachIndexed { index, (_, image) ->
            zip.putNextEntry(ZipEntry("support/images/$index-${image.name}"))
            image.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        return SupportExportResult(reports.size, trimmedFeedback.isNotEmpty())
    }
}

private fun String.safeName(): String = replace(Regex("[^A-Za-z0-9_-]"), "_")

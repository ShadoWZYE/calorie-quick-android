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

data class FeedbackMessage(
    val id: String,
    val text: String,
    val createdAtEpochMillis: Long,
)

class SupportDiagnosticStore(private val context: Context) {
    private val stagingDirectory = File(context.cacheDir, "diagnostic-staging")
    private val flaggedDirectory = File(context.filesDir, "flagged-scan-diagnostics")
    private val feedbackPreferences = context.getSharedPreferences("support-feedback", Context.MODE_PRIVATE)

    fun feedbackMessages(): List<FeedbackMessage> = runCatching {
        val stored = JSONArray(feedbackPreferences.getString("messages", "[]"))
        buildList {
            repeat(stored.length()) { index ->
                val item = stored.getJSONObject(index)
                add(
                    FeedbackMessage(
                        id = item.getString("id"),
                        text = item.getString("text"),
                        createdAtEpochMillis = item.getLong("createdAtEpochMillis"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun addFeedback(text: String): List<FeedbackMessage> {
        val normalized = text.trim().take(2_000)
        if (normalized.isEmpty()) return feedbackMessages()
        return (feedbackMessages() + FeedbackMessage(
            id = UUID.randomUUID().toString(),
            text = normalized,
            createdAtEpochMillis = System.currentTimeMillis(),
        )).also(::saveFeedbackMessages)
    }

    fun deleteFeedback(id: String): List<FeedbackMessage> = feedbackMessages()
        .filterNot { it.id == id }
        .also(::saveFeedbackMessages)

    private fun saveFeedbackMessages(messages: List<FeedbackMessage>) {
        val json = JSONArray().apply {
            messages.forEach { message ->
                put(
                    JSONObject()
                        .put("id", message.id)
                        .put("text", message.text)
                        .put("createdAtEpochMillis", message.createdAtEpochMillis),
                )
            }
        }
        feedbackPreferences.edit().putString("messages", json.toString()).apply()
    }

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

    fun appendTo(
        zip: ZipOutputStream,
        feedback: String = "",
        includeDiagnostics: Boolean = true,
        includeFeedback: Boolean = true,
    ): SupportExportResult {
        val reports = if (includeDiagnostics) flaggedDirectory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .sortedBy(File::getName)
            .mapNotNull { metadataFile ->
                runCatching { JSONObject(metadataFile.readText()) }.getOrNull()?.let { metadata ->
                    val image = File(flaggedDirectory, metadata.optString("image"))
                    if (image.isFile) metadata to image else null
                }
            } else emptyList()
        val trimmedFeedback = feedback.trim()
        val feedbackThread = if (includeFeedback) feedbackMessages().let { messages ->
            if (trimmedFeedback.isEmpty()) messages else messages + FeedbackMessage(
                id = UUID.randomUUID().toString(),
                text = trimmedFeedback,
                createdAtEpochMillis = System.currentTimeMillis(),
            )
        } else emptyList()
        val manifest = JSONObject()
            .put("schema", "calorie-quick-support-bundle")
            .put("schemaVersion", 2)
            .put("generatedAtEpochMillis", System.currentTimeMillis())
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("appVersionCode", BuildConfig.VERSION_CODE)
            .put("androidSdk", Build.VERSION.SDK_INT)
            .put("deviceManufacturer", Build.MANUFACTURER)
            .put("deviceModel", Build.MODEL)
            .put("feedback", trimmedFeedback.takeIf(String::isNotEmpty) ?: JSONObject.NULL)
            .put("feedbackThread", JSONArray().apply {
                feedbackThread.forEach { message ->
                    put(
                        JSONObject()
                            .put("id", message.id)
                            .put("text", message.text)
                            .put("createdAtEpochMillis", message.createdAtEpochMillis),
                    )
                }
            })
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
        return SupportExportResult(reports.size, feedbackThread.isNotEmpty())
    }
}

private fun String.safeName(): String = replace(Regex("[^A-Za-z0-9_-]"), "_")

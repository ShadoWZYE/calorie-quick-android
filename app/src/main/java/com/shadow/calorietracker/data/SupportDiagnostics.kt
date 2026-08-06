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

data class SupportExportResult(
    val diagnosticCount: Int,
    val freezeCount: Int,
    val crashCount: Int,
    val hasFeedback: Boolean,
)

data class FeedbackMessage(
    val id: String,
    val text: String,
    val createdAtEpochMillis: Long,
    val imageLocalPath: String? = null,
)

class SupportDiagnosticStore(private val context: Context) {
    private val stagingDirectory = File(context.cacheDir, "diagnostic-staging")
    private val flaggedDirectory = File(context.filesDir, "flagged-scan-diagnostics")
    private val freezeDirectory = File(context.filesDir, "ui-freeze-diagnostics")
    private val crashDirectory = File(context.filesDir, "crash-diagnostics")
    private val feedbackImageDirectory = File(context.filesDir, "feedback-images")
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
                        imageLocalPath = (item.opt("imageLocalPath") as? String)?.takeIf(String::isNotBlank),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun addFeedback(text: String, image: Uri? = null): List<FeedbackMessage> {
        val normalized = text.trim().take(2_000)
        if (normalized.isEmpty() && image == null) return feedbackMessages()
        val id = UUID.randomUUID().toString()
        val imagePath = image?.let { importFeedbackImage(it, id) }
        return (feedbackMessages() + FeedbackMessage(
            id = id,
            text = normalized,
            createdAtEpochMillis = System.currentTimeMillis(),
            imageLocalPath = imagePath,
        )).also(::saveFeedbackMessages)
    }

    fun deleteFeedback(id: String): List<FeedbackMessage> {
        val messages = feedbackMessages()
        messages.firstOrNull { it.id == id }?.imageLocalPath?.let { runCatching { File(it).delete() } }
        return messages.filterNot { it.id == id }.also(::saveFeedbackMessages)
    }

    fun replaceFeedbackMessages(messages: List<FeedbackMessage>) {
        val retained = messages.mapNotNull(FeedbackMessage::imageLocalPath).toSet()
        feedbackMessages().mapNotNull(FeedbackMessage::imageLocalPath)
            .filterNot(retained::contains)
            .forEach { runCatching { File(it).delete() } }
        saveFeedbackMessages(messages)
    }

    private fun importFeedbackImage(source: Uri, id: String): String {
        feedbackImageDirectory.mkdirs()
        val extension = when (context.contentResolver.getType(source)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val destination = File(feedbackImageDirectory, "feedback-$id.$extension")
        requireNotNull(context.contentResolver.openInputStream(source)).use { input ->
            destination.outputStream().use(input::copyTo)
        }
        return destination.absolutePath
    }

    private fun saveFeedbackMessages(messages: List<FeedbackMessage>) {
        val json = JSONArray().apply {
            messages.forEach { message ->
                put(
                    JSONObject()
                        .put("id", message.id)
                        .put("text", message.text)
                        .put("createdAtEpochMillis", message.createdAtEpochMillis)
                        .put("imageLocalPath", message.imageLocalPath ?: JSONObject.NULL),
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

    @Synchronized
    fun recordUiFreeze(stalledForMillis: Long, threadState: String, stackTrace: String) {
        freezeDirectory.mkdirs()
        val detectedAt = System.currentTimeMillis()
        val id = "$detectedAt-${UUID.randomUUID()}"
        val report = JSONObject()
            .put("id", id)
            .put("detectedAtEpochMillis", detectedAt)
            .put("stalledForMillis", stalledForMillis)
            .put("threadState", threadState)
            .put("mainThreadStackTrace", stackTrace.take(32_000))
        File(freezeDirectory, "$id.json").writeText(report.toString(2))
        freezeDirectory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .sortedByDescending(File::lastModified)
            .drop(MAX_FREEZE_REPORTS)
            .forEach(File::delete)
    }

    fun freezeCount(): Int = freezeDirectory.listFiles { file -> file.extension == "json" }?.size ?: 0

    fun clearFreezes() {
        freezeDirectory.listFiles()?.forEach(File::delete)
    }

    @Synchronized
    fun recordCrash(threadName: String, throwable: Throwable) {
        crashDirectory.mkdirs()
        val detectedAt = System.currentTimeMillis()
        val id = "$detectedAt-${UUID.randomUUID()}"
        val report = JSONObject()
            .put("id", id)
            .put("detectedAtEpochMillis", detectedAt)
            .put("threadName", threadName.take(200))
            .put("exceptionType", throwable.javaClass.name)
            .put("message", throwable.message?.take(2_000) ?: JSONObject.NULL)
            .put("stackTrace", throwable.stackTraceToString().take(64_000))
        File(crashDirectory, "$id.json").writeText(report.toString(2))
        crashDirectory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .sortedByDescending(File::lastModified)
            .drop(MAX_CRASH_REPORTS)
            .forEach(File::delete)
    }

    fun crashCount(): Int = crashDirectory.listFiles { file -> file.extension == "json" }?.size ?: 0

    fun clearCrashes() {
        crashDirectory.listFiles()?.forEach(File::delete)
    }

    fun appendTo(
        zip: ZipOutputStream,
        feedback: String = "",
        includeDiagnostics: Boolean = true,
        includeFreezeReports: Boolean = true,
        includeCrashReports: Boolean = true,
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
        val freezeReports = if (includeFreezeReports) freezeDirectory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .sortedBy(File::getName)
            .mapNotNull { file -> runCatching { JSONObject(file.readText()) }.getOrNull() }
        else emptyList()
        val crashReports = if (includeCrashReports) crashDirectory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .sortedBy(File::getName)
            .mapNotNull { file -> runCatching { JSONObject(file.readText()) }.getOrNull() }
        else emptyList()
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
            .put("schemaVersion", 5)
            .put("generatedAtEpochMillis", System.currentTimeMillis())
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("appVersionCode", BuildConfig.VERSION_CODE)
            .put("androidSdk", Build.VERSION.SDK_INT)
            .put("deviceManufacturer", Build.MANUFACTURER)
            .put("deviceModel", Build.MODEL)
            .put("feedback", trimmedFeedback.takeIf(String::isNotEmpty) ?: JSONObject.NULL)
            .put("feedbackThread", JSONArray().apply {
                feedbackThread.forEachIndexed { index, message ->
                    val image = message.imageLocalPath?.let(::File)?.takeIf(File::isFile)
                    put(
                        JSONObject()
                            .put("id", message.id)
                            .put("text", message.text)
                            .put("createdAtEpochMillis", message.createdAtEpochMillis)
                            .put(
                                "imageBundlePath",
                                image?.let { "feedback-images/$index-${it.name.safeName()}" } ?: JSONObject.NULL,
                            ),
                    )
                }
            })
            .put("diagnostics", JSONArray().apply {
                reports.forEachIndexed { index, (metadata, image) ->
                    put(JSONObject(metadata.toString()).put("bundlePath", "images/$index-${image.name}"))
                }
            })
            .put("uiFreezes", JSONArray(freezeReports))
            .put("crashes", JSONArray(crashReports))
        zip.putNextEntry(ZipEntry("support/support.json"))
        zip.write(manifest.toString(2).toByteArray())
        zip.closeEntry()
        reports.forEachIndexed { index, (_, image) ->
            zip.putNextEntry(ZipEntry("support/images/$index-${image.name}"))
            image.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        feedbackThread.forEachIndexed { index, message ->
            val image = message.imageLocalPath?.let(::File)?.takeIf(File::isFile) ?: return@forEachIndexed
            zip.putNextEntry(ZipEntry("support/feedback-images/$index-${image.name.safeName()}"))
            image.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        return SupportExportResult(reports.size, freezeReports.size, crashReports.size, feedbackThread.isNotEmpty())
    }

    private companion object {
        const val MAX_FREEZE_REPORTS = 20
        const val MAX_CRASH_REPORTS = 20
    }
}

private fun String.safeName(): String = replace(Regex("[^A-Za-z0-9_-]"), "_")

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
    val feedbackCount: Int = 0,
    val performanceCount: Int = 0,
    val manifest: JSONObject? = null,
    val media: List<ReviewBundleMedia> = emptyList(),
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
    private val performanceDirectory = File(context.filesDir, "performance-diagnostics")
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

    fun performanceCount(): Int = performanceDirectory.listFiles { file -> file.extension == "json" }?.size ?: 0

    fun clearPerformanceDiagnostics() {
        performanceDirectory.listFiles()?.forEach(File::delete)
    }

    @Synchronized
    fun recordNetworkOperation(
        operation: String,
        durationMillis: Long,
        outcome: String,
        resultCount: Int,
        queryLength: Int,
    ) {
        performanceDirectory.mkdirs()
        val occurredAt = System.currentTimeMillis()
        val id = "$occurredAt-${UUID.randomUUID()}"
        File(performanceDirectory, "$id.json").writeText(
            JSONObject()
                .put("id", id)
                .put("operation", operation.take(80))
                .put("occurredAtEpochMillis", occurredAt)
                .put("durationMillis", durationMillis.coerceAtLeast(0))
                .put("outcome", outcome.take(80))
                .put("resultCount", resultCount.coerceAtLeast(0))
                .put("queryLength", queryLength.coerceAtLeast(0))
                .toString(2),
        )
        performanceDirectory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .sortedByDescending(File::lastModified)
            .drop(MAX_PERFORMANCE_REPORTS)
            .forEach(File::delete)
    }

    fun appendTo(
        zip: ZipOutputStream,
        mediaSanitizer: ReviewBundleMediaSanitizer,
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
        val performanceReports = if (includeDiagnostics) performanceDirectory.listFiles { file -> file.extension == "json" }
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
        val diagnosticMedia = reports.mapIndexedNotNull { index, (metadata, image) ->
            mediaSanitizer.sanitize(
                source = image,
                bundlePathWithoutExtension = "support/images/$index-${metadata.optString("id").safeName()}",
                role = "failed-scan",
                ownerId = metadata.optString("id").takeIf(String::isNotBlank),
            )
        }
        val feedbackMedia = feedbackThread.mapIndexedNotNull { index, message ->
            val image = message.imageLocalPath?.let(::File)?.takeIf(File::isFile) ?: return@mapIndexedNotNull null
            mediaSanitizer.sanitize(
                source = image,
                bundlePathWithoutExtension = "support/feedback-images/$index-${message.id.safeName()}",
                role = "feedback-context",
                ownerId = message.id,
            )
        }
        val diagnosticMediaByOwner = diagnosticMedia.associateBy(ReviewBundleMedia::ownerId)
        val feedbackMediaByOwner = feedbackMedia.associateBy(ReviewBundleMedia::ownerId)
        val manifest = JSONObject()
            .put("schema", "calorie-quick-support-bundle")
            .put("schemaVersion", 7)
            .put("generatedAtEpochMillis", System.currentTimeMillis())
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("appVersionCode", BuildConfig.VERSION_CODE)
            .put("buildId", BuildConfig.BUILD_ID)
            .put("sourceCommit", BuildConfig.SOURCE_COMMIT)
            .put("sourceDirty", BuildConfig.SOURCE_DIRTY)
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
                                feedbackMediaByOwner[message.id]?.bundlePath ?: JSONObject.NULL,
                            )
                            .put("imageSanitizationFailed", image != null && feedbackMediaByOwner[message.id] == null),
                    )
                }
            })
            .put("diagnostics", JSONArray().apply {
                reports.forEachIndexed { index, (metadata, image) ->
                    val ownerId = metadata.optString("id").takeIf(String::isNotBlank)
                    put(
                        JSONObject(metadata.toString())
                            .put("bundlePath", diagnosticMediaByOwner[ownerId]?.bundlePath ?: JSONObject.NULL)
                            .put("imageSanitizationFailed", diagnosticMediaByOwner[ownerId] == null),
                    )
                }
            })
            .put("uiFreezes", JSONArray(freezeReports))
            .put("crashes", JSONArray(crashReports))
            .put("performance", JSONArray(performanceReports))
        zip.putNextEntry(ZipEntry("support/support.json"))
        zip.write(manifest.toString(2).toByteArray())
        zip.closeEntry()
        (diagnosticMedia + feedbackMedia).forEach { media ->
            zip.putNextEntry(ZipEntry(media.bundlePath))
            media.file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        return SupportExportResult(
            diagnosticCount = reports.size,
            freezeCount = freezeReports.size,
            crashCount = crashReports.size,
            hasFeedback = feedbackThread.isNotEmpty(),
            feedbackCount = feedbackThread.size,
            performanceCount = performanceReports.size,
            manifest = manifest,
            media = diagnosticMedia + feedbackMedia,
        )
    }

    private companion object {
        const val MAX_FREEZE_REPORTS = 20
        const val MAX_CRASH_REPORTS = 20
        const val MAX_PERFORMANCE_REPORTS = 50
    }
}

private fun String.safeName(): String = replace(Regex("[^A-Za-z0-9_-]"), "_")

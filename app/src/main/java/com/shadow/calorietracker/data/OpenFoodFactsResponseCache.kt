package com.shadow.calorietracker.data

import android.content.Context
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

data class CachedOpenFoodFactsResponse(
    val file: File,
    val response: JSONObject,
    val cachedAtEpochMillis: Long,
)

class OpenFoodFactsResponseCache(context: Context) {
    private val directory = File(context.filesDir, "open-food-facts-cache").apply { mkdirs() }

    @Synchronized
    fun readFresh(requestUrl: String, locale: String, now: Long = System.currentTimeMillis()): JSONObject? =
        read(requestUrl, locale)?.takeIf { now - it.cachedAtEpochMillis <= FRESH_FOR_MILLIS }?.response

    @Synchronized
    fun readAny(requestUrl: String, locale: String): JSONObject? = read(requestUrl, locale)?.response

    @Synchronized
    fun write(requestUrl: String, locale: String, requestKind: String, response: JSONObject) {
        val key = key(requestUrl, locale)
        val destination = File(directory, "$key.json")
        val temporary = File(directory, "$key.tmp")
        temporary.writeText(
            JSONObject()
                .put("schema", "calorie-quick-open-food-facts-response")
                .put("schemaVersion", 1)
                .put("key", key)
                .put("requestKind", requestKind)
                .put("locale", locale.take(20))
                .put("cachedAtEpochMillis", System.currentTimeMillis())
                .put("response", JSONObject(response.toString()))
                .toString(),
        )
        temporary.copyTo(destination, overwrite = true)
        temporary.delete()
    }

    @Synchronized
    fun entries(): List<File> = directory.listFiles { file -> file.extension == "json" }
        .orEmpty()
        .sortedBy(File::getName)

    private fun read(requestUrl: String, locale: String): CachedOpenFoodFactsResponse? {
        val file = File(directory, "${key(requestUrl, locale)}.json")
        if (!file.isFile) return null
        return runCatching {
            val wrapper = JSONObject(file.readText())
            CachedOpenFoodFactsResponse(
                file = file,
                response = wrapper.getJSONObject("response"),
                cachedAtEpochMillis = wrapper.getLong("cachedAtEpochMillis"),
            )
        }.getOrNull()
    }

    private fun key(requestUrl: String, locale: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$locale\n$requestUrl".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val FRESH_FOR_MILLIS = 30L * 24 * 60 * 60 * 1_000
    }
}

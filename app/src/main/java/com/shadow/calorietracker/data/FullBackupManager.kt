package com.shadow.calorietracker.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.room.withTransaction
import com.shadow.calorietracker.BuildConfig
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class FullBackupPreview(
    val createdAtEpochMillis: Long,
    val appVersion: String,
    val databaseVersion: Int,
    val languageTags: String,
    val diaryEntryCount: Int,
    val personalFoodCount: Int,
    val recipeCount: Int,
    val bodyMeasurementCount: Int,
    val imageCount: Int,
    val feedbackMessageCount: Int,
)

data class FullBackupExportResult(
    val preview: FullBackupPreview,
    val byteCount: Long,
)

data class FullBackupRestoreResult(val preview: FullBackupPreview)

class InvalidFullBackupException(message: String) : IllegalArgumentException(message)

class FullBackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val supportDiagnostics: SupportDiagnosticStore,
) {
    suspend fun exportTo(destination: Uri, languageTags: String): FullBackupExportResult = withContext(Dispatchers.IO) {
        val imageSources = linkedMapOf<String, File>()
        val databaseJson = database.withTransaction {
            JSONObject().put("tables", JSONArray().apply {
                TABLES.forEach { spec -> put(readTable(spec, imageSources)) }
            })
        }
        val feedback = JSONArray().apply {
            supportDiagnostics.feedbackMessages().forEach { message ->
                message.imageLocalPath?.let(::File)?.takeIf(File::isFile)?.let { imageSources[it.absolutePath] = it }
                put(
                    JSONObject()
                        .put("id", message.id)
                        .put("text", message.text)
                        .put("createdAtEpochMillis", message.createdAtEpochMillis)
                        .put("imageLocalPath", message.imageLocalPath ?: JSONObject.NULL),
                )
            }
        }
        val imageEntries = imageSources.entries.mapIndexed { index, (originalPath, source) ->
            ImageEntry(
                originalPath = originalPath,
                archivePath = "backup/images/$index-${source.name.safeFileName()}",
                source = source,
                kind = if (source.parentFile?.name == "feedback-images") "feedback" else "food",
            )
        }
        val preview = FullBackupPreview(
            createdAtEpochMillis = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            databaseVersion = database.openHelper.readableDatabase.version,
            languageTags = languageTags,
            diaryEntryCount = tableRowCount(databaseJson, "diary_entries"),
            personalFoodCount = tableRowCount(databaseJson, "foods"),
            recipeCount = tableRowCount(databaseJson, "recipes"),
            bodyMeasurementCount = tableRowCount(databaseJson, "body_measurements"),
            imageCount = imageEntries.size,
            feedbackMessageCount = feedback.length(),
        )
        val manifest = preview.toManifest(imageEntries)
        var written = 0L
        requireNotNull(context.contentResolver.openOutputStream(destination)).use { output ->
            ZipOutputStream(output).use { zip ->
                written += zip.writeJson(MANIFEST_PATH, manifest)
                written += zip.writeJson(DATABASE_PATH, databaseJson)
                written += zip.writeJson(FEEDBACK_PATH, feedback)
                imageEntries.forEach { image ->
                    zip.putNextEntry(ZipEntry(image.archivePath))
                    image.source.inputStream().use { input -> written += input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        FullBackupExportResult(preview, written)
    }

    suspend fun inspect(source: Uri): FullBackupPreview = withContext(Dispatchers.IO) {
        requireNotNull(context.contentResolver.openInputStream(source)).use(::readAndValidateManifest)
    }

    suspend fun restore(source: Uri): FullBackupRestoreResult = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "full-backup-restore/${UUID.randomUUID()}").apply { mkdirs() }
        val importedImages = mutableListOf<File>()
        try {
            requireNotNull(context.contentResolver.openInputStream(source)).use { input ->
                extractValidatedArchive(input, staging)
            }
            val manifest = JSONObject(File(staging, MANIFEST_PATH).readText())
            val preview = validateManifest(manifest, database.openHelper.readableDatabase.version)
            val databaseJson = JSONObject(File(staging, DATABASE_PATH).readText())
            validateDatabasePayload(databaseJson)
            val imagePathMap = importImages(manifest, staging, importedImages)
            val feedback = readFeedback(File(staging, FEEDBACK_PATH), imagePathMap)
            val previousImagePaths = currentPersonalImagePaths()

            database.withTransaction {
                val sqlDatabase = database.openHelper.writableDatabase
                DELETE_STATEMENTS.forEach(sqlDatabase::execSQL)
                val tables = databaseJson.getJSONArray("tables")
                repeat(tables.length()) { index ->
                    restoreTable(sqlDatabase, tables.getJSONObject(index), imagePathMap)
                }
            }
            supportDiagnostics.replaceFeedbackMessages(feedback)
            previousImagePaths.filter { old -> old !in importedImages.map(File::getAbsolutePath).toSet() }
                .forEach { old -> runCatching { File(old).takeIf(::isManagedFoodImage)?.delete() } }
            FullBackupRestoreResult(preview)
        } catch (error: InvalidFullBackupException) {
            importedImages.forEach(File::delete)
            throw error
        } catch (error: Exception) {
            importedImages.forEach(File::delete)
            throw error
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun readTable(spec: TableSpec, imageSources: MutableMap<String, File>): JSONObject {
        val cursor = database.openHelper.readableDatabase.query(spec.selectSql)
        return cursor.use {
            val columns = JSONArray(cursor.columnNames.toList())
            val rows = JSONArray()
            while (cursor.moveToNext()) {
                val row = JSONArray()
                cursor.columnNames.forEachIndexed { index, column ->
                    row.put(cursor.jsonValue(index, column, imageSources))
                }
                rows.put(row)
            }
            JSONObject().put("name", spec.name).put("columns", columns).put("rows", rows)
        }
    }

    private fun Cursor.jsonValue(
        index: Int,
        column: String,
        imageSources: MutableMap<String, File>,
    ): Any = when (getType(index)) {
        Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
        Cursor.FIELD_TYPE_INTEGER -> getLong(index)
        Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
        Cursor.FIELD_TYPE_STRING -> {
            val value = getString(index)
            if (column == "imageLocalPath") {
                File(value).takeIf(File::isFile)?.also { imageSources[value] = it }?.absolutePath ?: JSONObject.NULL
            } else {
                value
            }
        }
        else -> throw InvalidFullBackupException("Unsupported value in backup table")
    }

    private fun validateDatabasePayload(databaseJson: JSONObject) {
        val tables = databaseJson.optJSONArray("tables")
            ?: throw InvalidFullBackupException("Backup data is missing")
        if (tables.length() != TABLES.size) throw InvalidFullBackupException("Backup table set is incomplete")
        val seen = mutableSetOf<String>()
        var totalRows = 0
        repeat(tables.length()) { index ->
            val table = tables.getJSONObject(index)
            val name = table.optString("name")
            val spec = TABLES_BY_NAME[name] ?: throw InvalidFullBackupException("Unexpected backup table")
            if (!seen.add(name)) throw InvalidFullBackupException("Duplicate backup table")
            val columns = table.optJSONArray("columns") ?: throw InvalidFullBackupException("Missing table columns")
            val rows = table.optJSONArray("rows") ?: throw InvalidFullBackupException("Missing table rows")
            val availableColumns = currentColumns(spec.name)
            val names = buildList { repeat(columns.length()) { add(columns.getString(it)) } }
            if (names.isEmpty() || names.distinct().size != names.size || names.any { it !in availableColumns }) {
                throw InvalidFullBackupException("Backup columns are incompatible")
            }
            repeat(rows.length()) { rowIndex ->
                if (rows.getJSONArray(rowIndex).length() != names.size) {
                    throw InvalidFullBackupException("Backup row has the wrong number of values")
                }
            }
            totalRows += rows.length()
            if (totalRows > MAX_DATABASE_ROWS) throw InvalidFullBackupException("Backup contains too many records")
            if (name == "foods") validatePersonalFoodRows(names, rows)
        }
        if (seen != TABLES_BY_NAME.keys) throw InvalidFullBackupException("Backup table set is incomplete")
    }

    private fun validatePersonalFoodRows(columns: List<String>, rows: JSONArray) {
        val personalIndex = columns.indexOf("isPersonal")
        if (personalIndex < 0) throw InvalidFullBackupException("Personal-food marker is missing")
        repeat(rows.length()) { index ->
            if (rows.getJSONArray(index).optLong(personalIndex, 0L) != 1L) {
                throw InvalidFullBackupException("Backup attempted to replace built-in foods")
            }
        }
    }

    private fun currentColumns(table: String): Set<String> = buildSet {
        database.openHelper.readableDatabase.query("PRAGMA table_info(${table.sqlIdentifier()})").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) add(cursor.getString(nameIndex))
        }
    }

    private fun restoreTable(
        sqlDatabase: androidx.sqlite.db.SupportSQLiteDatabase,
        table: JSONObject,
        imagePathMap: Map<String, String>,
    ) {
        val name = table.getString("name")
        val columnsJson = table.getJSONArray("columns")
        val rows = table.getJSONArray("rows")
        val columns = buildList { repeat(columnsJson.length()) { add(columnsJson.getString(it)) } }
        val sql = "INSERT INTO ${name.sqlIdentifier()} (${columns.joinToString(",") { it.sqlIdentifier() }}) " +
            "VALUES (${columns.joinToString(",") { "?" }})"
        repeat(rows.length()) { rowIndex ->
            val row = rows.getJSONArray(rowIndex)
            val values = Array<Any?>(columns.size) { columnIndex ->
                val value = row.opt(columnIndex).takeUnless { it == JSONObject.NULL }
                if (columns[columnIndex] == "imageLocalPath" && value is String) {
                    imagePathMap[value] ?: throw InvalidFullBackupException("Backup image mapping is missing")
                } else {
                    value
                }
            }
            sqlDatabase.execSQL(sql, values)
        }
    }

    private fun importImages(
        manifest: JSONObject,
        staging: File,
        imported: MutableList<File>,
    ): Map<String, String> {
        val destinationDirectory = File(context.filesDir, "food-images").apply { mkdirs() }
        val result = mutableMapOf<String, String>()
        val images = manifest.getJSONArray("images")
        repeat(images.length()) { index ->
            val image = images.getJSONObject(index)
            val originalPath = image.getString("originalPath")
            val archivePath = image.getString("archivePath")
            val kind = image.getString("kind")
            if (kind !in setOf("food", "feedback")) throw InvalidFullBackupException("Invalid image type")
            if (!archivePath.startsWith("backup/images/")) throw InvalidFullBackupException("Invalid image entry")
            val staged = File(staging, archivePath).takeIf(File::isFile)
                ?: throw InvalidFullBackupException("Backup image is missing")
            val extension = staged.extension.safeExtension()
            val targetDirectory = if (kind == "feedback") {
                File(context.filesDir, "feedback-images").apply { mkdirs() }
            } else {
                destinationDirectory
            }
            val destination = File(targetDirectory, "restored-${UUID.randomUUID()}.$extension")
            staged.copyTo(destination)
            imported += destination
            result[originalPath] = destination.absolutePath
        }
        return result
    }

    private fun currentPersonalImagePaths(): Set<String> = buildSet {
        listOf(
            "SELECT imageLocalPath FROM foods WHERE isPersonal = 1 AND imageLocalPath IS NOT NULL",
            "SELECT imageLocalPath FROM food_preparations WHERE foodId IN " +
                "(SELECT id FROM foods WHERE isPersonal = 1) AND imageLocalPath IS NOT NULL",
        ).forEach { query ->
            database.openHelper.readableDatabase.query(query).use { cursor ->
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
    }

    private fun readFeedback(file: File, imagePathMap: Map<String, String>): List<FeedbackMessage> {
        val array = JSONArray(file.readText())
        if (array.length() > MAX_FEEDBACK_MESSAGES) throw InvalidFullBackupException("Too many feedback messages")
        return buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(
                    FeedbackMessage(
                        id = item.getString("id").take(200),
                        text = item.getString("text").take(2_000),
                        createdAtEpochMillis = item.getLong("createdAtEpochMillis"),
                        imageLocalPath = (item.opt("imageLocalPath") as? String)?.takeIf(String::isNotBlank)
                            ?.let { original ->
                                imagePathMap[original]
                                    ?: throw InvalidFullBackupException("Feedback image mapping is missing")
                            },
                    ),
                )
            }
        }
    }

    private fun extractValidatedArchive(input: InputStream, staging: File) {
        var totalBytes = 0L
        var entryCount = 0
        val seen = mutableSetOf<String>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ARCHIVE_ENTRIES || !seen.add(entry.name)) {
                    throw InvalidFullBackupException("Backup archive contains invalid entries")
                }
                if (entry.name !in REQUIRED_PATHS && !entry.name.startsWith("backup/images/")) {
                    throw InvalidFullBackupException("Backup archive contains an unexpected entry")
                }
                val destination = File(staging, entry.name)
                if (!destination.canonicalPath.startsWith(staging.canonicalPath + File.separator)) {
                    throw InvalidFullBackupException("Backup archive path is unsafe")
                }
                if (!entry.isDirectory) {
                    destination.parentFile?.mkdirs()
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            totalBytes += read
                            if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                                throw InvalidFullBackupException("Backup archive is too large")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        if (!seen.containsAll(REQUIRED_PATHS)) throw InvalidFullBackupException("Backup archive is incomplete")
    }

    private data class TableSpec(val name: String, val selectSql: String)
    private data class ImageEntry(
        val originalPath: String,
        val archivePath: String,
        val source: File,
        val kind: String,
    )

    companion object {
        const val BACKUP_SCHEMA_VERSION = 1
        private const val MANIFEST_PATH = "backup/manifest.json"
        private const val DATABASE_PATH = "backup/database.json"
        private const val FEEDBACK_PATH = "backup/feedback.json"
        private const val MAX_MANIFEST_BYTES = 512 * 1024
        private const val MAX_UNCOMPRESSED_BYTES = 100L * 1024 * 1024
        private const val MAX_ARCHIVE_ENTRIES = 2_000
        private const val MAX_DATABASE_ROWS = 100_000
        private const val MAX_FEEDBACK_MESSAGES = 10_000
        private val REQUIRED_PATHS = setOf(MANIFEST_PATH, DATABASE_PATH, FEEDBACK_PATH)

        private val TABLES = listOf(
            TableSpec("foods", "SELECT * FROM foods WHERE isPersonal = 1"),
            TableSpec("food_aliases", "SELECT * FROM food_aliases WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)"),
            TableSpec("food_preparations", "SELECT * FROM food_preparations WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)"),
            TableSpec("nutrient_values", "SELECT * FROM nutrient_values WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)"),
            TableSpec("allergen_declarations", "SELECT * FROM allergen_declarations WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)"),
            TableSpec("servings", "SELECT * FROM servings WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)"),
            TableSpec("serving_presets", "SELECT * FROM serving_presets WHERE servingId IN (SELECT id FROM servings WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1))"),
            TableSpec("recipes", "SELECT * FROM recipes"),
            TableSpec("recipe_batches", "SELECT * FROM recipe_batches"),
            TableSpec("recipe_ingredients", "SELECT * FROM recipe_ingredients"),
            TableSpec("recipe_ingredient_allergens", "SELECT * FROM recipe_ingredient_allergens"),
            TableSpec("diary_entries", "SELECT * FROM diary_entries"),
            TableSpec("serving_usage", "SELECT * FROM serving_usage"),
            TableSpec("quantity_usage", "SELECT * FROM quantity_usage"),
            TableSpec("preparation_usage", "SELECT * FROM preparation_usage"),
            TableSpec("user_profile", "SELECT * FROM user_profile"),
            TableSpec("body_measurements", "SELECT * FROM body_measurements"),
        )
        private val TABLES_BY_NAME = TABLES.associateBy(TableSpec::name)

        private val DELETE_STATEMENTS = listOf(
            "DELETE FROM serving_presets WHERE servingId IN (SELECT id FROM servings WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1))",
            "DELETE FROM food_aliases WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)",
            "DELETE FROM food_preparations WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)",
            "DELETE FROM nutrient_values WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)",
            "DELETE FROM allergen_declarations WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)",
            "DELETE FROM servings WHERE foodId IN (SELECT id FROM foods WHERE isPersonal = 1)",
            "DELETE FROM foods WHERE isPersonal = 1",
            "DELETE FROM recipe_ingredient_allergens",
            "DELETE FROM recipe_ingredients",
            "DELETE FROM recipe_batches",
            "DELETE FROM recipes",
            "DELETE FROM diary_entries",
            "DELETE FROM serving_usage",
            "DELETE FROM quantity_usage",
            "DELETE FROM preparation_usage",
            "DELETE FROM user_profile",
            "DELETE FROM body_measurements",
        )

        fun readAndValidateManifest(input: InputStream, currentDatabaseVersion: Int = 15): FullBackupPreview {
            ZipInputStream(input).use { zip ->
                val entry = zip.nextEntry
                if (entry?.name == MANIFEST_PATH) {
                    val text = zip.readLimitedText(MAX_MANIFEST_BYTES)
                    return validateManifest(JSONObject(text), currentDatabaseVersion)
                }
            }
            throw InvalidFullBackupException("Backup manifest is missing")
        }

        fun validateManifest(manifest: JSONObject, currentDatabaseVersion: Int): FullBackupPreview {
            if (manifest.optString("schema") != "calorie-quick-full-backup") {
                throw InvalidFullBackupException("This is not a Calorie Quick full backup")
            }
            if (manifest.optInt("schemaVersion", -1) != BACKUP_SCHEMA_VERSION) {
                throw InvalidFullBackupException("Backup format is not supported")
            }
            val databaseVersion = manifest.optInt("databaseVersion", -1)
            if (databaseVersion !in 1..currentDatabaseVersion) {
                throw InvalidFullBackupException("Backup requires a newer app version")
            }
            val counts = manifest.optJSONObject("counts")
                ?: throw InvalidFullBackupException("Backup summary is missing")
            fun count(name: String): Int = counts.optInt(name, -1).also {
                if (it < 0) throw InvalidFullBackupException("Backup summary is invalid")
            }
            val preview = FullBackupPreview(
                createdAtEpochMillis = manifest.optLong("createdAtEpochMillis", -1).also {
                    if (it <= 0) throw InvalidFullBackupException("Backup date is invalid")
                },
                appVersion = manifest.optString("appVersion").takeIf(String::isNotBlank)
                    ?: throw InvalidFullBackupException("Backup app version is missing"),
                databaseVersion = databaseVersion,
                languageTags = manifest.optString("languageTags"),
                diaryEntryCount = count("diaryEntries"),
                personalFoodCount = count("personalFoods"),
                recipeCount = count("recipes"),
                bodyMeasurementCount = count("bodyMeasurements"),
                imageCount = count("images"),
                feedbackMessageCount = count("feedbackMessages"),
            )
            val images = manifest.optJSONArray("images")
                ?: throw InvalidFullBackupException("Backup image summary is missing")
            if (images.length() != preview.imageCount) {
                throw InvalidFullBackupException("Backup image summary is inconsistent")
            }
            val archivePaths = mutableSetOf<String>()
            val originalPaths = mutableSetOf<String>()
            repeat(images.length()) { index ->
                val image = images.optJSONObject(index)
                    ?: throw InvalidFullBackupException("Backup image summary is invalid")
                val originalPath = image.optString("originalPath")
                val archivePath = image.optString("archivePath")
                val kind = image.optString("kind")
                if (originalPath.isBlank() || !originalPaths.add(originalPath) ||
                    !archivePath.startsWith("backup/images/") || !archivePaths.add(archivePath) ||
                    kind !in setOf("food", "feedback")
                ) {
                    throw InvalidFullBackupException("Backup image summary is invalid")
                }
            }
            return preview
        }

        private fun FullBackupPreview.toManifest(images: List<ImageEntry>) = JSONObject()
            .put("schema", "calorie-quick-full-backup")
            .put("schemaVersion", BACKUP_SCHEMA_VERSION)
            .put("createdAtEpochMillis", createdAtEpochMillis)
            .put("appVersion", appVersion)
            .put("appVersionCode", BuildConfig.VERSION_CODE)
            .put("databaseVersion", databaseVersion)
            .put("languageTags", languageTags)
            .put("counts", JSONObject()
                .put("diaryEntries", diaryEntryCount)
                .put("personalFoods", personalFoodCount)
                .put("recipes", recipeCount)
                .put("bodyMeasurements", bodyMeasurementCount)
                .put("images", imageCount)
                .put("feedbackMessages", feedbackMessageCount))
            .put("images", JSONArray().apply {
                images.forEach { image ->
                    put(
                        JSONObject()
                            .put("originalPath", image.originalPath)
                            .put("archivePath", image.archivePath)
                            .put("kind", image.kind),
                    )
                }
            })

        private fun tableRowCount(databaseJson: JSONObject, name: String): Int {
            val tables = databaseJson.getJSONArray("tables")
            repeat(tables.length()) { index ->
                val table = tables.getJSONObject(index)
                if (table.getString("name") == name) return table.getJSONArray("rows").length()
            }
            return 0
        }

        private fun ZipOutputStream.writeJson(path: String, value: Any): Long {
            val bytes = value.toString().toByteArray()
            putNextEntry(ZipEntry(path))
            write(bytes)
            closeEntry()
            return bytes.size.toLong()
        }

        private fun ZipInputStream.readLimitedText(maxBytes: Int): String {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = read(buffer)
                if (read < 0) break
                if (output.size() + read > maxBytes) throw InvalidFullBackupException("Backup manifest is too large")
                output.write(buffer, 0, read)
            }
            return output.toString(Charsets.UTF_8.name())
        }

        private fun String.sqlIdentifier(): String = "\"${replace("\"", "\"\"")}\""
        private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
        private fun String.safeExtension(): String = lowercase().takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
    }

    private fun isManagedFoodImage(file: File): Boolean = runCatching {
        file.canonicalFile.parentFile == File(context.filesDir, "food-images").canonicalFile
    }.getOrDefault(false)
}

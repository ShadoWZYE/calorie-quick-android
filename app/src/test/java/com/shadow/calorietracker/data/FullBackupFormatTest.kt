package com.shadow.calorietracker.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FullBackupFormatTest {
    @Test
    fun manifestRoundTripPreservesRestorePreview() {
        val manifest = validManifest()
            .put("languageTags", "ro")
            .put("images", org.json.JSONArray().apply {
                repeat(4) { index ->
                    put(JSONObject()
                        .put("originalPath", "/private/image-$index.jpg")
                        .put("archivePath", "backup/images/$index-image.jpg")
                        .put("kind", if (index == 3) "feedback" else "food"))
                }
            })
            .put("counts", JSONObject()
                .put("diaryEntries", 42)
                .put("personalFoods", 7)
                .put("recipes", 3)
                .put("bodyMeasurements", 9)
                .put("images", 4)
                .put("feedbackMessages", 2))

        val preview = FullBackupManager.readAndValidateManifest(zipWithManifest(manifest), currentDatabaseVersion = 15)

        assertEquals(42, preview.diaryEntryCount)
        assertEquals(7, preview.personalFoodCount)
        assertEquals(3, preview.recipeCount)
        assertEquals(9, preview.bodyMeasurementCount)
        assertEquals(4, preview.imageCount)
        assertEquals(2, preview.feedbackMessageCount)
        assertEquals("ro", preview.languageTags)
        assertEquals("0.2.0-test", preview.appVersion)
    }

    @Test
    fun rejectsBackupCreatedByNewerDatabaseSchema() {
        val backup = zipWithManifest(validManifest().put("databaseVersion", 16))

        assertThrows(InvalidFullBackupException::class.java) {
            FullBackupManager.readAndValidateManifest(backup, currentDatabaseVersion = 15)
        }
    }

    @Test
    fun rejectsReviewExportOrUnrelatedZip() {
        val backup = zipWithManifest(validManifest().put("schema", "calorie-quick-support-bundle"))

        assertThrows(InvalidFullBackupException::class.java) {
            FullBackupManager.readAndValidateManifest(backup, currentDatabaseVersion = 15)
        }
    }

    @Test
    fun rejectsMissingOrNegativeSummaryCounts() {
        val counts = validManifest().getJSONObject("counts")
        counts.remove("recipes")
        counts.put("images", -1)

        assertThrows(InvalidFullBackupException::class.java) {
            FullBackupManager.validateManifest(validManifest().put("counts", counts), 15)
        }
    }

    private fun validManifest() = JSONObject()
        .put("schema", "calorie-quick-full-backup")
        .put("schemaVersion", FullBackupManager.BACKUP_SCHEMA_VERSION)
        .put("createdAtEpochMillis", 1_786_038_000_000L)
        .put("appVersion", "0.2.0-test")
        .put("databaseVersion", 15)
        .put("languageTags", "en")
        .put("images", org.json.JSONArray())
        .put("counts", JSONObject()
            .put("diaryEntries", 1)
            .put("personalFoods", 1)
            .put("recipes", 1)
            .put("bodyMeasurements", 1)
            .put("images", 0)
            .put("feedbackMessages", 0))

    private fun zipWithManifest(manifest: JSONObject): ByteArrayInputStream {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("backup/manifest.json"))
            zip.write(manifest.toString().toByteArray())
            zip.closeEntry()
        }
        return ByteArrayInputStream(output.toByteArray())
    }
}

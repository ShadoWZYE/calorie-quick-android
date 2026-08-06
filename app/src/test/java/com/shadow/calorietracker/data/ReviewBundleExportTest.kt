package com.shadow.calorietracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReviewBundleExportTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var support: SupportDiagnosticStore
    private lateinit var openFoodFactsCache: OpenFoodFactsResponseCache
    private lateinit var exportFile: File
    private lateinit var sourceImage: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        support = SupportDiagnosticStore(context)
        openFoodFactsCache = OpenFoodFactsResponseCache(context)
        openFoodFactsCache.entries().forEach(File::delete)
        support.replaceFeedbackMessages(emptyList())
        support.clearPerformanceDiagnostics()
        openFoodFactsCache.entries().forEach(File::delete)
        exportFile = File(context.cacheDir, "review-v2.zip")
        sourceImage = File(context.cacheDir, "review-source.jpg")
    }

    @After
    fun tearDown() {
        database.close()
        exportFile.delete()
        sourceImage.delete()
        support.replaceFeedbackMessages(emptyList())
        support.clearPerformanceDiagnostics()
    }

    @Test
    fun exportHasRootManifestAndStripsImageMetadata() = runBlocking {
        Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.eraseColor(Color.RED)
            sourceImage.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            bitmap.recycle()
        }
        ExifInterface(sourceImage).apply {
            setAttribute(ExifInterface.TAG_MAKE, "Private camera")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "45/1,0/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            saveAttributes()
        }
        database.foodDao().upsertFood(
            FoodEntity(
                id = "personal-test",
                nameEn = "Test food",
                nameRo = "Aliment test",
                detailEn = "",
                detailRo = "",
                caloriesPer100g = 100,
                proteinMilligramsPer100g = 4_000,
                carbsMilligramsPer100g = 16_000,
                fatMilligramsPer100g = 2_000,
                isPersonal = true,
                sourceType = "PERSONAL",
                imageLocalPath = sourceImage.absolutePath,
            ),
        )
        support.recordNetworkOperation(
            operation = "open-food-facts-search",
            durationMillis = 2_500,
            outcome = "success",
            resultCount = 3,
            queryLength = 6,
        )
        openFoodFactsCache.write(
            requestUrl = "https://world.openfoodfacts.org/search?query=private-term",
            locale = "en",
            requestKind = "search",
            response = JSONObject("""{"products":[{"code":"12345678","product_name":"Cached drink"}]}"""),
        )

        CatalogueExporter(context, database, support, openFoodFactsCache).exportTo(
            destination = Uri.fromFile(exportFile),
            selectedFoodIds = setOf("personal-test"),
            selectedRecipeIds = emptySet(),
            includeDiagnostics = true,
            includeFreezeReports = false,
            includeCrashReports = false,
            includeFeedback = false,
        )

        ZipFile(exportFile).use { archive ->
            val root = JSONObject(archive.getInputStream(archive.getEntry("bundle.json")).bufferedReader().readText())
            assertEquals("calorie-quick-review-bundle", root.getString("schema"))
            assertEquals(2, root.getInt("schemaVersion"))
            assertNotNull(root.getJSONObject("app").getString("buildId"))
            assertEquals(7, root.getJSONArray("components").getJSONObject(1).getInt("schemaVersion"))
            assertEquals("open-food-facts-cache", root.getJSONArray("components").getJSONObject(2).getString("kind"))
            val media = root.getJSONArray("media").getJSONObject(0)
            val entry = archive.getEntry(media.getString("path"))
            val bytes = archive.getInputStream(entry).readBytes()
            assertEquals(media.getLong("byteLength"), bytes.size.toLong())
            assertEquals(media.getString("sha256"), bytes.sha256())
            val sanitizedFile = File(context.cacheDir, "sanitized-check.jpg").apply { writeBytes(bytes) }
            try {
                val exif = ExifInterface(sanitizedFile)
                assertFalse(exif.hasAttribute(ExifInterface.TAG_MAKE))
                assertFalse(exif.hasAttribute(ExifInterface.TAG_GPS_LATITUDE))
            } finally {
                sanitizedFile.delete()
            }
            val catalogue = JSONObject(
                archive.getInputStream(archive.getEntry("catalogue/catalogue.json")).bufferedReader().readText(),
            )
            assertTrue(catalogue.getJSONArray("foods").getJSONObject(0).getBoolean("selectedForThisExport"))
            val supportManifest = JSONObject(
                archive.getInputStream(archive.getEntry("support/support.json")).bufferedReader().readText(),
            )
            assertEquals(7, supportManifest.getInt("schemaVersion"))
            val performance = supportManifest.getJSONArray("performance").getJSONObject(0)
            assertEquals("open-food-facts-search", performance.getString("operation"))
            assertEquals(2_500, performance.getLong("durationMillis"))
            assertFalse(performance.has("query"))
            val cacheIndex = JSONObject(
                archive.getInputStream(archive.getEntry("open-food-facts/cache.json")).bufferedReader().readText(),
            )
            assertEquals(1, cacheIndex.getInt("responseCount"))
            val cachePath = cacheIndex.getJSONArray("responses").getString(0)
            val cached = archive.getInputStream(archive.getEntry(cachePath)).bufferedReader().readText()
            assertTrue(cached.contains("Cached drink"))
            assertFalse(cached.contains("private-term"))
        }
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }
}

package com.shadow.calorietracker.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FullBackupRoundTripTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var support: SupportDiagnosticStore
    private lateinit var manager: FullBackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        support = SupportDiagnosticStore(context)
        support.replaceFeedbackMessages(emptyList())
        manager = FullBackupManager(context, database, support)
    }

    @After
    fun tearDown() {
        database.close()
        context.cacheDir.resolve("round-trip-backup.zip").delete()
        context.filesDir.resolve("food-images").deleteRecursively()
        context.filesDir.resolve("feedback-images").deleteRecursively()
        support.replaceFeedbackMessages(emptyList())
    }

    @Test
    fun exportThenRestoreReplacesUserDataAndKeepsBuiltInCatalogue() = runBlocking {
        val foodImageBytes = "food-image".toByteArray()
        val foodImage = context.filesDir.resolve("food-images/original.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(foodImageBytes)
        }
        val feedbackImageBytes = "feedback-image".toByteArray()
        val feedbackImage = context.filesDir.resolve("feedback-images/context.png").apply {
            parentFile?.mkdirs()
            writeBytes(feedbackImageBytes)
        }
        database.foodDao().upsertFood(food("built-in", personal = false))
        database.foodDao().upsertFood(food("personal", personal = true, imagePath = foodImage.absolutePath))
        database.foodDao().upsertNutrients(
            listOf(NutrientValueEntity("personal|fiber", "personal", "fiber", 1_000, "test")),
        )
        database.profileDao().upsert(profile("Backup owner"))
        database.diaryDao().insert(
            DiaryEntryEntity(
                id = "entry-1",
                foodId = "personal",
                foodNameEn = "Personal",
                foodNameRo = "Personal",
                grams = 125,
                enteredAmountMilliUnits = 125_000,
                unitKey = "grams",
                unitLabelEn = "g",
                unitLabelRo = "g",
                consumedAtEpochMillis = 1_786_038_000_000L,
                calories = 125,
                proteinMilligrams = 5_000,
                carbsMilligrams = 20_000,
                fatMilligrams = 2_000,
                fiberMilligrams = 1_250,
            ),
        )
        database.recipeDao().upsertRecipe(
            RecipeEntity("personal", "Saved recipe", "batch-1", 1_786_000_000_000L, 1_786_000_000_000L),
        )
        support.replaceFeedbackMessages(
            listOf(FeedbackMessage("feedback-1", "The card clipped", 1_786_038_000_000L, feedbackImage.absolutePath)),
        )
        val backup = context.cacheDir.resolve("round-trip-backup.zip")

        val exported = manager.exportTo(Uri.fromFile(backup), "ro")

        assertEquals(1, exported.preview.diaryEntryCount)
        assertEquals(1, exported.preview.personalFoodCount)
        assertEquals(1, exported.preview.recipeCount)
        assertEquals(2, exported.preview.imageCount)
        database.openHelper.writableDatabase.execSQL("DELETE FROM diary_entries")
        database.openHelper.writableDatabase.execSQL("DELETE FROM recipes")
        database.openHelper.writableDatabase.execSQL("DELETE FROM nutrient_values WHERE foodId = 'personal'")
        database.openHelper.writableDatabase.execSQL("DELETE FROM foods WHERE isPersonal = 1")
        database.profileDao().upsert(profile("Replacement owner"))
        support.replaceFeedbackMessages(emptyList())
        foodImage.delete()

        val restored = manager.restore(Uri.fromFile(backup))

        assertEquals("ro", restored.preview.languageTags)
        assertEquals(1, count("SELECT COUNT(*) FROM diary_entries"))
        assertEquals(1, count("SELECT COUNT(*) FROM recipes"))
        assertEquals(1, count("SELECT COUNT(*) FROM foods WHERE id = 'personal' AND isPersonal = 1"))
        assertEquals(1, count("SELECT COUNT(*) FROM foods WHERE id = 'built-in' AND isPersonal = 0"))
        assertEquals("Backup owner", text("SELECT displayName FROM user_profile WHERE id = 1"))
        val restoredFoodImage = File(text("SELECT imageLocalPath FROM foods WHERE id = 'personal'"))
        assertTrue(restoredFoodImage.isFile)
        assertNotEquals(foodImage.absolutePath, restoredFoodImage.absolutePath)
        assertArrayEquals(foodImageBytes, restoredFoodImage.readBytes())
        val restoredFeedback = support.feedbackMessages().single()
        assertEquals("The card clipped", restoredFeedback.text)
        val restoredFeedbackImage = File(requireNotNull(restoredFeedback.imageLocalPath))
        assertTrue(restoredFeedbackImage.isFile)
        assertArrayEquals(feedbackImageBytes, restoredFeedbackImage.readBytes())
    }

    private fun count(query: String): Int = database.openHelper.readableDatabase.query(query).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    private fun text(query: String): String = database.openHelper.readableDatabase.query(query).use { cursor ->
        cursor.moveToFirst()
        cursor.getString(0)
    }

    private fun food(id: String, personal: Boolean, imagePath: String? = null) = FoodEntity(
        id = id,
        nameEn = id,
        nameRo = id,
        detailEn = "",
        detailRo = "",
        caloriesPer100g = 100,
        proteinMilligramsPer100g = 4_000,
        carbsMilligramsPer100g = 16_000,
        fatMilligramsPer100g = 2_000,
        isPersonal = personal,
        sourceType = if (personal) "PERSONAL" else "BUILT_IN",
        imageLocalPath = imagePath,
    )

    private fun profile(name: String) = UserProfileEntity(
        onboardingComplete = true,
        age = 30,
        heightCm = 175,
        weightGrams = 70_000,
        formulaSex = "MALE",
        activityLevel = "MODERATE",
        goalType = "MAINTAIN",
        targetMode = "CUSTOM",
        calorieGoal = 2_000,
        proteinGoalGrams = 120,
        carbsGoalGrams = 230,
        fatGoalGrams = 65,
        displayName = name,
    )
}

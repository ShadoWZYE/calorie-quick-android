package com.shadow.calorietracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shadow.calorietracker.model.FoodPreparation
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.RecipeDraft
import com.shadow.calorietracker.model.RecipeIngredientDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecipePreparationPersistenceTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: CalorieRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CalorieRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `recipe batch persists preparation snapshot and learns usage`() = runBlocking {
        val fried = FoodPreparation(
            id = "egg-fried",
            names = LocalizedText("Fried", "Prăjit"),
            nutritionPer100g = Nutrition(196, 13.6, 0.8, 14.8, 0.0),
        )
        val ingredient = RecipeIngredientDraft(
            foodId = "egg",
            foodName = LocalizedText("Whole egg", "Ou întreg"),
            nutritionPer100g = fried.nutritionPer100g,
            allergens = emptyMap(),
            grams = 150,
            preparationId = fried.id,
            preparationName = fried.names,
        )

        repository.saveRecipe(
            RecipeDraft(
                name = "Fried eggs",
                ingredients = listOf(ingredient),
                cookedYieldGrams = 150,
                portionCount = 1,
            ),
        )

        val stored = database.recipeDao().listAllIngredients().single()
        assertEquals("egg-fried", stored.preparationId)
        assertEquals("Fried", stored.preparationNameEn)
        assertEquals("Prăjit", stored.preparationNameRo)
        assertEquals(196, stored.caloriesPer100g)

        val usage = database.preparationUsageDao().observeAll().first().single()
        assertEquals("egg", usage.foodId)
        assertEquals("egg-fried", usage.preparationId)
        assertEquals(1, usage.useCount)
    }
}

package com.shadow.calorietracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shadow.calorietracker.model.FoodSourceType
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.RecipeDraft
import com.shadow.calorietracker.model.RecipeIngredientDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: CalorieRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repository = CalorieRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun savingAgainCreatesNewBatchAndUpdatesActiveQuickAddFood() = runBlocking {
        val ingredient = RecipeIngredientDraft(
            foodId = "rice",
            foodName = LocalizedText("Rice", "Orez"),
            nutritionPer100g = Nutrition(130, 2.7, 28.2, 0.3, 0.4),
            allergens = emptyMap(),
            grams = 200,
        )
        val foodId = repository.saveRecipe(
            RecipeDraft(name = "Rice pot", ingredients = listOf(ingredient), cookedYieldGrams = 400, portionCount = 4),
        )
        val firstRecipe = repository.recipes.first().single()
        val firstFood = repository.foods.first().single()

        assertEquals(foodId, firstRecipe.foodId)
        assertEquals(FoodSourceType.RECIPE, firstFood.provenance.type)
        assertEquals(65, firstFood.nutritionPer100g.calories)
        assertEquals(100, firstFood.servings.single().grams)

        repository.saveRecipe(
            RecipeDraft(
                id = foodId,
                name = "Rice pot",
                ingredients = listOf(ingredient.copy(grams = 300)),
                cookedYieldGrams = 450,
                portionCount = 3,
            ),
        )
        val latestRecipe = repository.recipes.first().single()
        val latestFood = repository.foods.first().single()

        assertNotEquals(firstRecipe.activeBatchId, latestRecipe.activeBatchId)
        assertEquals(2, database.recipeDao().countBatches(foodId))
        assertEquals(450, latestRecipe.cookedYieldGrams)
        assertEquals(150, latestFood.servings.single().grams)
        assertEquals(87, latestFood.nutritionPer100g.calories)
    }
}

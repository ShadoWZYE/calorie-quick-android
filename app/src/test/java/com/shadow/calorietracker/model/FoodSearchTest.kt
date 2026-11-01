package com.shadow.calorietracker.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class FoodSearchTest {
    @Test
    fun barcodeIsSearchableAfterAProductIsCachedLocally() {
        val food = Food(
            id = "drink",
            names = LocalizedText("Sparkling drink", "Băutură carbogazoasă"),
            details = LocalizedText("", ""),
            nutritionPer100g = Nutrition.Zero,
            servings = emptyList(),
            barcode = "5941234567890",
        )

        assertTrue(food.matches("5941234567890"))
    }
    private val salmon = Food(
        id = "salmon",
        names = LocalizedText("Salmon", "Somon"),
        details = LocalizedText("Cooked", "Gătit"),
        nutritionPer100g = Nutrition(206, 22.0, 0.0, 12.0),
        servings = emptyList(),
        categoryKey = "fish",
    )

    @Test
    fun `category names and localized synonyms participate in food search`() {
        assertTrue(salmon.matches("fish"))
        assertTrue(salmon.matches("seafood"))
        assertTrue(salmon.matches("pește"))
        assertTrue(salmon.matches("fructe de mare"))
    }

    @Test
    fun `unrelated category term does not match`() {
        assertFalse(salmon.matches("bakery"))
    }

    @Test
    fun `search index preserves localized category and ingredient matching`() {
        val soup = Food(
            id = "soup",
            names = LocalizedText("House soup", "Supă de casă"),
            details = LocalizedText("Cooked", "Gătită"),
            nutritionPer100g = Nutrition.Zero,
            servings = emptyList(),
            categoryKey = "prepared-meals",
        )
        val recipe = RecipeTemplate(
            foodId = soup.id,
            activeBatchId = "",
            name = soup.names.en,
            ingredients = listOf(
                RecipeIngredientDraft(
                    foodId = "celeriac",
                    foodName = LocalizedText("Celeriac", "Țelină"),
                    nutritionPer100g = Nutrition.Zero,
                    allergens = emptyMap(),
                    grams = 100,
                ),
            ),
            cookedYieldGrams = 500,
            portionCount = 4,
            cookedAtEpochMillis = 0,
            remainingGrams = 0,
            batches = emptyList(),
        )
        val index = FoodSearchIndex.build(listOf(salmon, soup), mapOf(soup.id to recipe))

        assertEquals(listOf(salmon), index.matchingFoods(listOf(salmon, soup), "pește"))
        assertEquals(listOf(soup), index.matchingFoods(listOf(salmon, soup), "telina"))
        assertEquals(listOf(soup), index.matchingFoods(listOf(salmon, soup), "prepared meal"))
    }
}

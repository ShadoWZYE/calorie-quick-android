package com.shadow.calorietracker.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}

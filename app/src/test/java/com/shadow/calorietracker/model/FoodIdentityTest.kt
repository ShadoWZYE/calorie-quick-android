package com.shadow.calorietracker.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodIdentityTest {
    private val food = Food(
        id = "personal-1",
        names = LocalizedText("Grandma's soup", "Supa bunicii"),
        details = LocalizedText("Personal food", "Aliment personal"),
        nutritionPer100g = Nutrition(50, 2.0, 8.0, 1.0),
        servings = emptyList(),
        brand = "Acasă",
    )

    @Test
    fun `identity ignores punctuation casing spacing and diacritics`() {
        assertTrue(food.hasSameIdentity("  GRANDMAS---SOUP ", "Acasa"))
        assertTrue(food.hasSameIdentity("SUPA BUNICII", "acasă"))
    }

    @Test
    fun `same name with a different brand remains distinct`() {
        assertFalse(food.hasSameIdentity("Grandma's soup", "Different kitchen"))
    }

    @Test
    fun `catalogue identity prefers barcode and falls back to normalized name and brand`() {
        assertTrue(food.hasSameCatalogueIdentity(food.copy(id = "remote", barcode = "12345678")))
        assertTrue(
            food.copy(barcode = "12345678").hasSameCatalogueIdentity(
                food.copy(id = "remote", names = LocalizedText("Other", "Altul"), barcode = "12345678"),
            ),
        )
        assertFalse(
            food.copy(barcode = "12345678").hasSameCatalogueIdentity(
                food.copy(id = "remote", brand = "Other", barcode = "87654321"),
            ),
        )
    }

    @Test
    fun `recipe names do not hide catalogue foods with the same name`() {
        val recipe = food.copy(provenance = FoodProvenance(FoodSourceType.RECIPE))
        assertFalse(recipe.hasSameCatalogueIdentity(food.copy(id = "catalogue")))
    }
}

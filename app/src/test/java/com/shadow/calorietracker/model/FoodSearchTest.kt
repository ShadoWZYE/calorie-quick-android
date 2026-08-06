package com.shadow.calorietracker.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodSearchTest {
    private val food = Food(
        id = "personal-test",
        names = LocalizedText("Greek yogurt", "Iaurt grecesc"),
        details = LocalizedText("Example brand", "Marcă exemplu"),
        nutritionPer100g = Nutrition(70, 10.0, 4.0, 2.0),
        servings = emptyList(),
        isPersonal = true,
    )

    @Test
    fun exactNamePreventsDuplicatePersonalFoodOfferInEitherLanguage() {
        assertTrue(food.hasExactName(" greek YOGURT "))
        assertTrue(food.hasExactName("IAURT GRECESC"))
        assertFalse(food.hasExactName("Greek yogurt 2%"))
    }

    @Test
    fun searchMatchesNormalizedAliases() {
        val aliased = food.copy(aliases = listOf("aubergine", "vânătă"))

        assertTrue(aliased.matches("auberg"))
        assertTrue(aliased.matches("vânăt"))
        assertTrue(aliased.matches("vanata"))
        assertTrue(food.matches("marca exemplu"))
    }
}

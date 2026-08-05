package com.shadow.calorietracker.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeCalculatorTest {
    @Test
    fun `finished yield determines per 100 gram and portion nutrition`() {
        val result = RecipeCalculator.calculate(
            ingredients = listOf(
                ingredient("rice", 200, Nutrition(130, 2.7, 28.0, 0.3, 0.4)),
                ingredient("chicken", 100, Nutrition(165, 31.0, 0.0, 3.6, 0.0)),
            ),
            cookedYieldGrams = 250,
            portionCount = 2,
        )

        assertEquals(425, result.totalNutrition.calories)
        assertEquals(170, result.nutritionPer100g.calories)
        assertEquals(14.56, result.nutritionPer100g.proteinGrams, 0.001)
        assertEquals(125, result.portionGrams)
        assertEquals(0.32, result.nutritionPer100g.fiberGrams!!, 0.001)
    }

    @Test
    fun `water gain dilutes nutrition without changing batch calories`() {
        val result = RecipeCalculator.calculate(
            ingredients = listOf(ingredient("oats", 100, Nutrition(400, 10.0, 60.0, 8.0, 5.0))),
            cookedYieldGrams = 500,
            portionCount = 5,
        )

        assertEquals(400, result.totalNutrition.calories)
        assertEquals(80, result.nutritionPer100g.calories)
        assertEquals(100, result.portionGrams)
    }

    @Test
    fun `fiber stays unknown when any ingredient is unknown`() {
        val result = RecipeCalculator.calculate(
            ingredients = listOf(
                ingredient("known", 100, Nutrition(100, 1.0, 1.0, 1.0, 3.0)),
                ingredient("unknown", 100, Nutrition(100, 1.0, 1.0, 1.0, null)),
            ),
            cookedYieldGrams = 200,
            portionCount = 2,
        )

        assertNull(result.totalNutrition.fiberGrams)
        assertNull(result.nutritionPer100g.fiberGrams)
    }

    @Test
    fun `contains overrides may contain across ingredients`() {
        val result = RecipeCalculator.calculate(
            ingredients = listOf(
                ingredient(
                    "trace",
                    50,
                    Nutrition(100, 1.0, 1.0, 1.0),
                    mapOf(Allergen.MILK to AllergenDeclaration.MAY_CONTAIN),
                ),
                ingredient(
                    "contains",
                    50,
                    Nutrition(100, 1.0, 1.0, 1.0),
                    mapOf(Allergen.MILK to AllergenDeclaration.CONTAINS),
                ),
            ),
            cookedYieldGrams = 100,
            portionCount = 1,
        )

        assertEquals(AllergenDeclaration.CONTAINS, result.allergens[Allergen.MILK])
    }

    private fun ingredient(
        id: String,
        grams: Int,
        nutrition: Nutrition,
        allergens: Map<Allergen, AllergenDeclaration> = emptyMap(),
    ) = RecipeIngredientDraft(
        foodId = id,
        foodName = LocalizedText(id, id),
        nutritionPer100g = nutrition,
        allergens = allergens,
        grams = grams,
    )
}

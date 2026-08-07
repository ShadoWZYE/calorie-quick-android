package com.shadow.calorietracker.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeCalculatorTest {
    @Test
    fun `fresh batch scales every ingredient and preserves calories per gram`() {
        val original = listOf(
            ingredient("flour", 500, Nutrition(364, 10.0, 76.0, 1.0)),
            ingredient("water", 300, Nutrition.Zero),
            ingredient("oil", 20, Nutrition(884, 0.0, 0.0, 100.0)),
        )
        val scaled = RecipeCalculator.scaleIngredients(original, sourceYieldGrams = 700, targetYieldGrams = 1_050)

        assertEquals(listOf(750, 450, 30), scaled.map(RecipeIngredientDraft::grams))
        val originalResult = RecipeCalculator.calculate(original, cookedYieldGrams = 700, portionCount = 7)
        val scaledResult = RecipeCalculator.calculate(scaled, cookedYieldGrams = 1_050, portionCount = 7)
        assertEquals(originalResult.nutritionPer100g.calories, scaledResult.nutritionPer100g.calories)
        assertEquals(
            originalResult.nutritionPer100g.proteinGrams,
            scaledResult.nutritionPer100g.proteinGrams,
            0.001,
        )
    }

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

    @Test
    fun `selected preparation is snapshotted and used for recipe nutrition`() {
        val fried = FoodPreparation(
            id = "egg-fried",
            names = LocalizedText("Fried", "Prăjit"),
            nutritionPer100g = Nutrition(196, 13.6, 0.8, 14.8, 0.0),
        )
        val egg = Food(
            id = "egg",
            names = LocalizedText("Whole egg", "Ou întreg"),
            details = LocalizedText("Raw", "Crud"),
            nutritionPer100g = Nutrition(143, 12.6, 0.7, 9.5, 0.0),
            servings = emptyList(),
            preparations = listOf(fried),
        )

        val ingredient = egg.toRecipeIngredient(150, fried)
        val result = RecipeCalculator.calculate(
            ingredients = listOf(ingredient),
            cookedYieldGrams = 150,
            portionCount = 1,
        )

        assertEquals("egg-fried", ingredient.preparationId)
        assertEquals("Fried", ingredient.preparationName?.en)
        assertEquals(196, ingredient.nutritionPer100g.calories)
        assertEquals(294, result.totalNutrition.calories)
        assertEquals(196, result.nutritionPer100g.calories)
    }

    @Test
    fun `recipe ingredient search is localized and accent insensitive`() {
        val recipe = RecipeTemplate(
            foodId = "recipe-soup",
            activeBatchId = "template",
            name = "Soup",
            ingredients = listOf(
                ingredient("carrot", 100, Nutrition.Zero).copy(
                    foodName = LocalizedText("Celery", "Țelină coaptă"),
                ),
            ),
            cookedYieldGrams = 500,
            portionCount = 2,
            cookedAtEpochMillis = 0,
            remainingGrams = 0,
            batches = emptyList(),
        )

        assertEquals(true, recipe.hasIngredientMatching("telina"))
        assertEquals(true, recipe.hasIngredientMatching("coapta"))
        assertEquals(false, recipe.hasIngredientMatching("cartof"))
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

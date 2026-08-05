package com.shadow.calorietracker.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroOverageTest {
    private val profile = UserProfile(
        onboardingComplete = true,
        age = 30,
        heightCm = 175,
        weightKg = 75.0,
        formulaSex = FormulaSex.MALE,
        activityLevel = ActivityLevel.LIGHT,
        goalType = GoalType.MAINTAIN,
        calorieGoal = 2_000,
        proteinGoalGrams = 120,
        carbsGoalGrams = 270,
        fatGoalGrams = 60,
    )

    @Test
    fun showsOnlyMacrosCrossedByNeutralDefaultServing() {
        val egg = food(
            Nutrition(155, 12.6, 1.1, 10.6),
            listOf(Serving("large", LocalizedText("1 large egg", "1 ou mare"), 50)),
        )

        val warnings = egg.projectedMacroOverages(Nutrition(1_000, 70.0, 100.0, 56.0), profile)

        assertEquals(listOf(MacroKind.FAT), warnings.map { it.kind })
        assertEquals(50, warnings.single().comparisonGrams)
        assertEquals(5.3, warnings.single().addedGrams, 0.0001)
    }

    @Test
    fun usesOneHundredGramsWhenNoDefaultServingExists() {
        val food = food(Nutrition(200, 10.0, 20.0, 13.0), emptyList())

        val warnings = food.projectedMacroOverages(Nutrition(1_000, 0.0, 0.0, 49.0), profile)

        assertEquals(100, warnings.single().comparisonGrams)
        assertEquals(MacroKind.FAT, warnings.single().kind)
    }

    @Test
    fun hidesMacrosThatRemainWithinTarget() {
        val food = food(Nutrition(100, 10.0, 10.0, 5.0), emptyList())

        assertTrue(food.projectedMacroOverages(Nutrition.Zero, profile).isEmpty())
    }

    private fun food(nutrition: Nutrition, servings: List<Serving>) = Food(
        id = "test",
        names = LocalizedText("Test", "Test"),
        details = LocalizedText("", ""),
        nutritionPer100g = nutrition,
        servings = servings,
    )
}

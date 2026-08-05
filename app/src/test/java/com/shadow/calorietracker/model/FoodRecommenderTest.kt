package com.shadow.calorietracker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodRecommenderTest {
    private val profile = UserProfile(
        onboardingComplete = true,
        age = 30,
        heightCm = 175,
        weightKg = 75.0,
        formulaSex = FormulaSex.MALE,
        activityLevel = ActivityLevel.LIGHT,
        goalType = GoalType.MAINTAIN,
        calorieGoal = 2_000,
        proteinGoalGrams = 100,
        carbsGoalGrams = 200,
        fatGoalGrams = 60,
    )

    @Test
    fun prioritizesTheMacroStillNeeded() {
        val proteinFood = food("protein", Nutrition(150, 30.0, 0.0, 2.0))
        val carbFood = food("carbs", Nutrition(150, 2.0, 30.0, 1.0))

        val ranked = FoodRecommender.rank(
            listOf(carbFood, proteinFood),
            Nutrition(0, 10.0, 200.0, 60.0),
            profile,
            emptyMap(),
        )

        assertEquals("protein", ranked.first().id)
    }

    @Test
    fun frequencyBreaksEquivalentMacroFit() {
        val first = food("first", Nutrition(100, 10.0, 10.0, 5.0))
        val favourite = food("favourite", Nutrition(100, 10.0, 10.0, 5.0))

        val ranked = FoodRecommender.rank(
            listOf(first, favourite),
            Nutrition.Zero,
            profile,
            mapOf("favourite" to 5),
        )

        assertEquals("favourite", ranked.first().id)
    }

    @Test
    fun penalizesFoodsRichInAnAlreadyExceededMacro() {
        val fattyProtein = food("fatty", Nutrition(300, 30.0, 0.0, 30.0))
        val leanProtein = food("lean", Nutrition(150, 25.0, 0.0, 2.0))

        val ranked = FoodRecommender.rank(
            listOf(fattyProtein, leanProtein),
            Nutrition(1_000, 50.0, 200.0, 65.0),
            profile,
            emptyMap(),
        )

        assertEquals("lean", ranked.first().id)
    }

    @Test
    fun overTargetPenaltyOutweighsASecondaryMacroAdvantage() {
        val oats = food("oats", Nutrition(379, 13.2, 67.7, 6.5))
        val rice = food("rice", Nutrition(130, 2.7, 28.2, 0.3))

        val ranked = FoodRecommender.rank(
            listOf(oats, rice),
            Nutrition(1_300, 65.0, 99.0, 65.0),
            profile,
            emptyMap(),
        )

        assertEquals("rice", ranked.first().id)
    }

    private fun food(id: String, nutrition: Nutrition) = Food(
        id = id,
        names = LocalizedText(id, id),
        details = LocalizedText("", ""),
        nutritionPer100g = nutrition,
        servings = emptyList(),
    )
}

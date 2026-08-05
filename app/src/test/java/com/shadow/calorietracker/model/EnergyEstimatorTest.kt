package com.shadow.calorietracker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyEstimatorTest {
    @Test
    fun usesMifflinStJeorForRestingCalories() {
        assertEquals(1_699, EnergyEstimator.restingCalories(30, 175, 75.0, FormulaSex.MALE))
        assertEquals(1_533, EnergyEstimator.restingCalories(30, 175, 75.0, FormulaSex.FEMALE))
    }

    @Test
    fun activityAndGoalAdjustTheDailyEstimate() {
        assertEquals(
            2_336,
            EnergyEstimator.dailyGoal(
                age = 30,
                heightCm = 175,
                weightKg = 75.0,
                sex = FormulaSex.MALE,
                activityLevel = ActivityLevel.LIGHT,
                goalType = GoalType.MAINTAIN,
            ),
        )
        assertEquals(
            2_036,
            EnergyEstimator.dailyGoal(30, 175, 75.0, FormulaSex.MALE, ActivityLevel.LIGHT, GoalType.LOSE),
        )
    }

    @Test
    fun neverSuggestsBelowMinimumFloor() {
        assertEquals(
            1_200,
            EnergyEstimator.dailyGoal(100, 120, 35.0, FormulaSex.FEMALE, ActivityLevel.SEDENTARY, GoalType.LOSE),
        )
    }

    @Test
    fun derivesCoherentMacroTargetsFromEstimatedCalories() {
        assertEquals(
            DailyTargets(calories = 2_336, proteinGrams = 120, carbsGrams = 329, fatGrams = 60),
            EnergyEstimator.dailyTargets(
                age = 30,
                heightCm = 175,
                weightKg = 75.0,
                sex = FormulaSex.MALE,
                activityLevel = ActivityLevel.LIGHT,
                goalType = GoalType.MAINTAIN,
            ),
        )
    }
}

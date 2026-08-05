package com.shadow.calorietracker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionTest {
    @Test
    fun scalesNutritionToServingAndRoundsCalories() {
        val serving = Nutrition(89, 1.1, 22.8, 0.3).forGrams(125)

        assertEquals(111, serving.calories)
        assertEquals(1.375, serving.proteinGrams, 0.001)
        assertEquals(28.5, serving.carbsGrams, 0.001)
    }

    @Test
    fun addsNutritionTotals() {
        val total = Nutrition(100, 10.0, 5.0, 2.0) + Nutrition(50, 3.0, 4.0, 1.0)

        assertEquals(Nutrition(150, 13.0, 9.0, 3.0), total)
    }

    @Test
    fun scalesAndTotalsKnownFiberWithoutInventingUnknownValues() {
        val serving = Nutrition(100, 1.0, 20.0, 2.0, fiberGrams = 8.0).forGrams(50)
        assertEquals(4.0, requireNotNull(serving.fiberGrams), 0.001)

        val partialTotal = Nutrition.Zero + Nutrition(100, 1.0, 20.0, 2.0, fiberGrams = 4.0) +
            Nutrition(100, 1.0, 20.0, 2.0)
        assertEquals(4.0, requireNotNull(partialTotal.fiberGrams), 0.001)
        assertEquals(null, (Nutrition.Zero + Nutrition(100, 1.0, 20.0, 2.0)).fiberGrams)
    }
}

package com.shadow.calorietracker.ui

import com.shadow.calorietracker.data.GRAMS_UNIT_KEY
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.QuantityUsage
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.UnitUsage
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityPresetsTest {
    private val egg = Food(
        id = "eggs",
        names = LocalizedText("Whole egg", "Ou întreg"),
        details = LocalizedText("Boiled", "Fiert"),
        nutritionPer100g = Nutrition(155, 12.6, 1.1, 10.6),
        servings = listOf(Serving("egg-large", LocalizedText("1 large egg", "1 ou mare"), 50)),
    )

    @Test
    fun rememberedServingBecomesPresetWithoutChangingBaseAmount() {
        val unitUsage = listOf(UnitUsage("eggs", "egg-large", 3, 123L, 3.0))
        val quantityUsage = listOf(QuantityUsage("eggs", "egg-large", 3.0, 3, 123L))
        val choices = buildUnitChoices(egg, unitUsage, Locale.ENGLISH, "g")
        val presets = buildQuantityPresets(choices, quantityUsage, Locale.ENGLISH, "g")

        assertEquals(1.0, choices.first().baseAmount, 0.0)
        assertEquals(listOf("3 × large egg · 150 g", "large egg · 50 g", "100 g"), presets.map { it.label })
    }

    @Test
    fun rememberedGramsBecomesPresetWithoutChangingBaseAmount() {
        val unitUsage = listOf(UnitUsage("eggs", GRAMS_UNIT_KEY, 4, 456L, 150.0))
        val quantityUsage = listOf(QuantityUsage("eggs", GRAMS_UNIT_KEY, 150.0, 4, 456L))
        val choices = buildUnitChoices(egg, unitUsage, Locale.ENGLISH, "g")
        val presets = buildQuantityPresets(choices, quantityUsage, Locale.ENGLISH, "g")

        assertEquals(GRAMS_UNIT_KEY, choices.first().key)
        assertEquals(100.0, choices.first().baseAmount, 0.0)
        assertEquals(listOf("150 g", "100 g", "large egg · 50 g"), presets.map { it.label })
    }

    @Test
    fun baseAmountsAreNotDuplicatedAsRememberedPresets() {
        val unitUsage = listOf(UnitUsage("eggs", "egg-large", 1, 123L, 1.0))
        val quantityUsage = listOf(QuantityUsage("eggs", "egg-large", 1.0, 1, 123L))
        val choices = buildUnitChoices(egg, unitUsage, Locale.ENGLISH, "g")
        val presets = buildQuantityPresets(choices, quantityUsage, Locale.ENGLISH, "g")

        assertEquals(listOf("large egg · 50 g", "100 g"), presets.map { it.label })
    }

    @Test
    fun interactionCountRanksReusableQuantitiesBeforeRecency() {
        val choices = buildUnitChoices(egg, emptyList(), Locale.ENGLISH, "g")
        val quantityUsage = listOf(
            QuantityUsage("eggs", "egg-large", 2.0, 2, 999L),
            QuantityUsage("eggs", "egg-large", 3.0, 5, 100L),
        )

        val presets = buildQuantityPresets(choices, quantityUsage, Locale.ENGLISH, "g")

        assertEquals(
            listOf("3 × large egg · 150 g", "2 × large egg · 100 g", "large egg · 50 g", "100 g"),
            presets.map { it.label },
        )
    }
}

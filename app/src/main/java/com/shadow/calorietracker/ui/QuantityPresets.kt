package com.shadow.calorietracker.ui

import com.shadow.calorietracker.data.GRAMS_UNIT_KEY
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.UnitUsage
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class UnitChoice(val key: String, val label: String, val serving: Serving?) {
    val baseAmount: Double get() = if (serving == null) 100.0 else 1.0
}

data class QuantityPreset(
    val unitKey: String,
    val amount: Double,
    val grams: Int,
    val label: String,
)

fun buildUnitChoices(
    food: Food,
    usage: List<UnitUsage>,
    locale: Locale,
    gramsLabel: String,
): List<UnitChoice> {
    val usageByKey = usage.associateBy(UnitUsage::unitKey)
    return (food.servings.map {
        UnitChoice(it.id, it.label.forLocale(locale).removePrefix("1 "), it)
    } + UnitChoice(GRAMS_UNIT_KEY, gramsLabel, null)).sortedWith(
        compareByDescending<UnitChoice> { usageByKey[it.key]?.useCount ?: 0 }
            .thenByDescending { usageByKey[it.key]?.lastUsedAtEpochMillis ?: 0L },
    )
}

fun buildQuantityPresets(
    choices: List<UnitChoice>,
    usage: List<UnitUsage>,
    locale: Locale,
    gramsLabel: String,
): List<QuantityPreset> {
    val usageByKey = usage.associateBy(UnitUsage::unitKey)
    return choices.flatMap { choice ->
        val base = choice.toPreset(choice.baseAmount, locale, gramsLabel)
        val rememberedAmount = usageByKey[choice.key]?.lastAmount
        if (rememberedAmount != null && abs(rememberedAmount - choice.baseAmount) > 0.000_001) {
            listOf(choice.toPreset(rememberedAmount, locale, gramsLabel), base)
        } else {
            listOf(base)
        }
    }
}

private fun UnitChoice.toPreset(amount: Double, locale: Locale, gramsLabel: String): QuantityPreset {
    val grams = (amount * (serving?.grams ?: 1)).roundToInt()
    val formattedAmount = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }.format(amount)
    val label = when {
        serving == null -> "$formattedAmount $gramsLabel"
        amount == 1.0 -> "$label · $grams $gramsLabel"
        else -> "$formattedAmount × $label · $grams $gramsLabel"
    }
    return QuantityPreset(key, amount, grams, label)
}

package com.shadow.calorietracker.model

import java.util.UUID
import java.util.Locale
import kotlin.math.roundToInt

data class Nutrition(
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
) {
    fun forGrams(grams: Int): Nutrition {
        val factor = grams / 100.0
        return Nutrition(
            calories = (calories * factor).roundToInt(),
            proteinGrams = proteinGrams * factor,
            carbsGrams = carbsGrams * factor,
            fatGrams = fatGrams * factor,
        )
    }

    operator fun plus(other: Nutrition) = Nutrition(
        calories + other.calories,
        proteinGrams + other.proteinGrams,
        carbsGrams + other.carbsGrams,
        fatGrams + other.fatGrams,
    )

    companion object {
        val Zero = Nutrition(0, 0.0, 0.0, 0.0)
    }
}

data class Food(
    val id: String,
    val names: LocalizedText,
    val details: LocalizedText,
    val nutritionPer100g: Nutrition,
) {
    fun name(locale: Locale): String = names.forLocale(locale)
    fun detail(locale: Locale): String = details.forLocale(locale)

    fun matches(query: String): Boolean = names.all().any { it.contains(query, ignoreCase = true) } ||
        details.all().any { it.contains(query, ignoreCase = true) }
}

data class LocalizedText(val en: String, val ro: String) {
    fun forLocale(locale: Locale): String = if (locale.language == "ro") ro else en
    fun all(): List<String> = listOf(en, ro)
}

data class FoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val food: Food,
    val grams: Int,
) {
    val nutrition: Nutrition = food.nutritionPer100g.forGrams(grams)
}

val sampleFoods = listOf(
    Food("greek-yogurt", LocalizedText("Greek yogurt", "Iaurt grecesc"), LocalizedText("2% fat", "2% grăsime"), Nutrition(73, 9.9, 3.9, 2.0)),
    Food("banana", LocalizedText("Banana", "Banană"), LocalizedText("Fresh", "Proaspătă"), Nutrition(89, 1.1, 22.8, 0.3)),
    Food("chicken-breast", LocalizedText("Chicken breast", "Piept de pui"), LocalizedText("Cooked, skinless", "Gătit, fără piele"), Nutrition(165, 31.0, 0.0, 3.6)),
    Food("oats", LocalizedText("Rolled oats", "Fulgi de ovăz"), LocalizedText("Dry", "Uscați"), Nutrition(379, 13.2, 67.7, 6.5)),
    Food("eggs", LocalizedText("Whole egg", "Ou întreg"), LocalizedText("Boiled", "Fiert"), Nutrition(155, 12.6, 1.1, 10.6)),
    Food("rice", LocalizedText("White rice", "Orez alb"), LocalizedText("Cooked", "Gătit"), Nutrition(130, 2.7, 28.2, 0.3)),
)

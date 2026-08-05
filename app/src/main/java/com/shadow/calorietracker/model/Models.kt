package com.shadow.calorietracker.model

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

data class LocalizedText(val en: String, val ro: String) {
    fun forLocale(locale: Locale): String = if (locale.language == "ro") ro else en
    fun all(): List<String> = listOf(en, ro)
}

data class Serving(
    val id: String,
    val label: LocalizedText,
    val grams: Int,
)

data class Food(
    val id: String,
    val names: LocalizedText,
    val details: LocalizedText,
    val nutritionPer100g: Nutrition,
    val servings: List<Serving>,
) {
    fun name(locale: Locale): String = names.forLocale(locale)
    fun detail(locale: Locale): String = details.forLocale(locale)

    fun matches(query: String): Boolean = names.all().any { it.contains(query, ignoreCase = true) } ||
        details.all().any { it.contains(query, ignoreCase = true) }
}

data class FoodEntry(
    val id: String,
    val foodId: String,
    val foodName: LocalizedText,
    val grams: Int,
    val consumedAtEpochMillis: Long,
    val nutrition: Nutrition,
)

enum class FormulaSex { FEMALE, MALE }
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
}
enum class GoalType(val calorieAdjustment: Int) {
    LOSE(-300),
    MAINTAIN(0),
    GAIN(300),
}

data class UserProfile(
    val onboardingComplete: Boolean,
    val age: Int,
    val heightCm: Int,
    val weightKg: Double,
    val formulaSex: FormulaSex,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val calorieGoal: Int,
    val proteinGoalGrams: Int,
    val carbsGoalGrams: Int,
    val fatGoalGrams: Int,
)

object EnergyEstimator {
    fun restingCalories(age: Int, heightCm: Int, weightKg: Double, sex: FormulaSex): Int {
        val sexConstant = if (sex == FormulaSex.MALE) 5 else -161
        return (10 * weightKg + 6.25 * heightCm - 5 * age + sexConstant).roundToInt()
    }

    fun dailyGoal(
        age: Int,
        heightCm: Int,
        weightKg: Double,
        sex: FormulaSex,
        activityLevel: ActivityLevel,
        goalType: GoalType,
    ): Int = (restingCalories(age, heightCm, weightKg, sex) * activityLevel.multiplier)
        .roundToInt()
        .plus(goalType.calorieAdjustment)
        .coerceAtLeast(1_200)
}


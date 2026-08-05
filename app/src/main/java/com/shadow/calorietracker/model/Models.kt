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

enum class MacroKind { PROTEIN, CARBS, FAT }

data class MacroOverage(
    val kind: MacroKind,
    val addedGrams: Double,
    val comparisonGrams: Int,
)

fun Food.projectedMacroOverages(totals: Nutrition, profile: UserProfile): List<MacroOverage> {
    val comparisonGrams = servings.firstOrNull()?.grams ?: 100
    val added = nutritionPer100g.forGrams(comparisonGrams)
    return listOf(
        MacroOverage(MacroKind.PROTEIN, added.proteinGrams, comparisonGrams) to
            (totals.proteinGrams + added.proteinGrams > profile.proteinGoalGrams),
        MacroOverage(MacroKind.CARBS, added.carbsGrams, comparisonGrams) to
            (totals.carbsGrams + added.carbsGrams > profile.carbsGoalGrams),
        MacroOverage(MacroKind.FAT, added.fatGrams, comparisonGrams) to
            (totals.fatGrams + added.fatGrams > profile.fatGoalGrams),
    ).filter { (overage, projectedOver) -> projectedOver && overage.addedGrams.roundToInt() > 0 }
        .map { it.first }
}

data class FoodEntry(
    val id: String,
    val foodId: String,
    val foodName: LocalizedText,
    val grams: Int,
    val enteredAmount: Double,
    val unitKey: String,
    val unitLabel: LocalizedText,
    val consumedAtEpochMillis: Long,
    val nutrition: Nutrition,
)

data class UnitUsage(
    val foodId: String,
    val unitKey: String,
    val useCount: Int,
    val lastUsedAtEpochMillis: Long,
    val lastAmount: Double,
)

data class QuantityUsage(
    val foodId: String,
    val unitKey: String,
    val amount: Double,
    val useCount: Int,
    val lastUsedAtEpochMillis: Long,
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
enum class TargetMode { ESTIMATED, CUSTOM }

data class DailyTargets(
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
)

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
    val targetMode: TargetMode = TargetMode.ESTIMATED,
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

    fun dailyTargets(
        age: Int,
        heightCm: Int,
        weightKg: Double,
        sex: FormulaSex,
        activityLevel: ActivityLevel,
        goalType: GoalType,
    ): DailyTargets {
        val calories = dailyGoal(age, heightCm, weightKg, sex, activityLevel, goalType)
        val protein = (weightKg * 1.6).roundToInt()
        val fat = (weightKg * 0.8).roundToInt()
        val carbs = ((calories - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return DailyTargets(calories, protein, carbs, fat)
    }
}

object FoodRecommender {
    fun rank(
        foods: List<Food>,
        totals: Nutrition,
        profile: UserProfile,
        frequencyByFoodId: Map<String, Int>,
    ): List<Food> {
        val proteinWeight = macroWeight(totals.proteinGrams, profile.proteinGoalGrams)
        val carbsWeight = macroWeight(totals.carbsGrams, profile.carbsGoalGrams)
        val fatWeight = macroWeight(totals.fatGrams, profile.fatGoalGrams)

        return foods.sortedWith(
            compareByDescending<Food> { food ->
                food.nutritionPer100g.proteinGrams / profile.proteinGoalGrams.coerceAtLeast(1) * proteinWeight +
                    food.nutritionPer100g.carbsGrams / profile.carbsGoalGrams.coerceAtLeast(1) * carbsWeight +
                    food.nutritionPer100g.fatGrams / profile.fatGoalGrams.coerceAtLeast(1) * fatWeight
            }.thenByDescending { frequencyByFoodId[it.id] ?: 0 }
                .thenBy { it.names.en },
        )
    }

    private fun macroWeight(consumed: Double, goal: Int): Double {
        val safeGoal = goal.coerceAtLeast(1)
        val gapRatio = (goal - consumed) / safeGoal
        return if (gapRatio >= 0.0) {
            gapRatio.coerceAtMost(1.0)
        } else {
            -(2.0 + (-gapRatio).coerceAtMost(1.0))
        }
    }
}

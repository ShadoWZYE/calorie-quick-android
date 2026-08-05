package com.shadow.calorietracker.data

import androidx.room.withTransaction
import com.shadow.calorietracker.model.ActivityLevel
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodEntry
import com.shadow.calorietracker.model.FormulaSex
import com.shadow.calorietracker.model.GoalType
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.UserProfile
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

class CalorieRepository(private val database: AppDatabase) {
    val profile: Flow<UserProfile?> = database.profileDao().observe().map { it?.toModel() }
    val foods: Flow<List<Food>> = database.foodDao().observeFoods().map { rows -> rows.map { it.toModel() } }

    fun todayEntries(zoneId: ZoneId = ZoneId.systemDefault()): Flow<List<FoodEntry>> {
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return database.diaryDao().observeBetween(start, end).map { rows -> rows.map { it.toModel() } }
    }

    suspend fun seedFoods() {
        if (database.foodDao().count() > 0) return
        database.withTransaction {
            database.foodDao().insertFoods(seedFoodEntities)
            database.foodDao().insertServings(seedServingEntities)
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        database.profileDao().upsert(profile.toEntity())
    }

    suspend fun addEntry(food: Food, grams: Int) {
        val nutrition = food.nutritionPer100g.forGrams(grams)
        val now = System.currentTimeMillis()
        database.withTransaction {
            val recent = database.diaryDao().findRecent(food.id, now - ENTRY_MERGE_WINDOW_MILLIS)
            if (recent == null) {
                database.diaryDao().insert(
                    DiaryEntryEntity(
                        id = UUID.randomUUID().toString(),
                        foodId = food.id,
                        foodNameEn = food.names.en,
                        foodNameRo = food.names.ro,
                        grams = grams,
                        consumedAtEpochMillis = now,
                        calories = nutrition.calories,
                        proteinMilligrams = (nutrition.proteinGrams * 1_000).roundToInt(),
                        carbsMilligrams = (nutrition.carbsGrams * 1_000).roundToInt(),
                        fatMilligrams = (nutrition.fatGrams * 1_000).roundToInt(),
                    ),
                )
            } else {
                database.diaryDao().update(
                    recent.copy(
                        grams = recent.grams + grams,
                        calories = recent.calories + nutrition.calories,
                        proteinMilligrams = recent.proteinMilligrams + (nutrition.proteinGrams * 1_000).roundToInt(),
                        carbsMilligrams = recent.carbsMilligrams + (nutrition.carbsGrams * 1_000).roundToInt(),
                        fatMilligrams = recent.fatMilligrams + (nutrition.fatGrams * 1_000).roundToInt(),
                    ),
                )
            }
        }
    }

    suspend fun deleteEntry(entry: FoodEntry) {
        database.diaryDao().delete(entry.toEntity())
    }
}

private const val ENTRY_MERGE_WINDOW_MILLIS = 2 * 60 * 1_000L

private fun FoodWithServings.toModel() = Food(
    id = food.id,
    names = LocalizedText(food.nameEn, food.nameRo),
    details = LocalizedText(food.detailEn, food.detailRo),
    nutritionPer100g = Nutrition(
        calories = food.caloriesPer100g,
        proteinGrams = food.proteinMilligramsPer100g / 1_000.0,
        carbsGrams = food.carbsMilligramsPer100g / 1_000.0,
        fatGrams = food.fatMilligramsPer100g / 1_000.0,
    ),
    servings = servings.sortedByDescending { it.grams }.map {
        Serving(it.id, LocalizedText(it.labelEn, it.labelRo), it.grams)
    },
)

private fun DiaryEntryEntity.toModel() = FoodEntry(
    id = id,
    foodId = foodId,
    foodName = LocalizedText(foodNameEn, foodNameRo),
    grams = grams,
    consumedAtEpochMillis = consumedAtEpochMillis,
    nutrition = Nutrition(
        calories,
        proteinMilligrams / 1_000.0,
        carbsMilligrams / 1_000.0,
        fatMilligrams / 1_000.0,
    ),
)

private fun FoodEntry.toEntity() = DiaryEntryEntity(
    id, foodId, foodName.en, foodName.ro, grams, consumedAtEpochMillis, nutrition.calories,
    (nutrition.proteinGrams * 1_000).roundToInt(),
    (nutrition.carbsGrams * 1_000).roundToInt(),
    (nutrition.fatGrams * 1_000).roundToInt(),
)

private fun UserProfileEntity.toModel() = UserProfile(
    onboardingComplete,
    age,
    heightCm,
    weightGrams / 1_000.0,
    FormulaSex.valueOf(formulaSex),
    ActivityLevel.valueOf(activityLevel),
    GoalType.valueOf(goalType),
    calorieGoal,
    proteinGoalGrams,
    carbsGoalGrams,
    fatGoalGrams,
)

private fun UserProfile.toEntity() = UserProfileEntity(
    onboardingComplete = onboardingComplete,
    age = age,
    heightCm = heightCm,
    weightGrams = (weightKg * 1_000).roundToInt(),
    formulaSex = formulaSex.name,
    activityLevel = activityLevel.name,
    goalType = goalType.name,
    calorieGoal = calorieGoal,
    proteinGoalGrams = proteinGoalGrams,
    carbsGoalGrams = carbsGoalGrams,
    fatGoalGrams = fatGoalGrams,
)

private val seedFoodEntities = listOf(
    FoodEntity("greek-yogurt", "Greek yogurt", "Iaurt grecesc", "2% fat", "2% grăsime", 73, 9_900, 3_900, 2_000),
    FoodEntity("banana", "Banana", "Banană", "Fresh", "Proaspătă", 89, 1_100, 22_800, 300),
    FoodEntity("chicken-breast", "Chicken breast", "Piept de pui", "Cooked, skinless", "Gătit, fără piele", 165, 31_000, 0, 3_600),
    FoodEntity("oats", "Rolled oats", "Fulgi de ovăz", "Dry", "Uscați", 379, 13_200, 67_700, 6_500),
    FoodEntity("eggs", "Whole egg", "Ou întreg", "Boiled", "Fiert", 155, 12_600, 1_100, 10_600),
    FoodEntity("rice", "White rice", "Orez alb", "Cooked", "Gătit", 130, 2_700, 28_200, 300),
)

private val seedServingEntities = listOf(
    ServingEntity("banana-medium", "banana", "1 medium banana", "1 banană medie", 118),
    ServingEntity("banana-small", "banana", "1 small banana", "1 banană mică", 101),
    ServingEntity("egg-large", "eggs", "1 large egg", "1 ou mare", 50),
    ServingEntity("yogurt-cup", "greek-yogurt", "1 cup", "1 cană", 245),
    ServingEntity("oats-half-cup", "oats", "½ cup dry", "½ cană uscată", 40),
    ServingEntity("rice-cup", "rice", "1 cup cooked", "1 cană gătită", 158),
)

package com.shadow.calorietracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameRo: String,
    val detailEn: String,
    val detailRo: String,
    val caloriesPer100g: Int,
    val proteinMilligramsPer100g: Int,
    val carbsMilligramsPer100g: Int,
    val fatMilligramsPer100g: Int,
)

@Entity(
    tableName = "servings",
    indices = [Index("foodId")],
)
data class ServingEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val labelEn: String,
    val labelRo: String,
    val grams: Int,
)

data class FoodWithServings(
    @Embedded val food: FoodEntity,
    @Relation(parentColumn = "id", entityColumn = "foodId")
    val servings: List<ServingEntity>,
)

@Entity(
    tableName = "diary_entries",
    indices = [Index("consumedAtEpochMillis"), Index("foodId")],
)
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val foodNameEn: String,
    val foodNameRo: String,
    val grams: Int,
    val enteredAmountMilliUnits: Long,
    val unitKey: String,
    val unitLabelEn: String,
    val unitLabelRo: String,
    val consumedAtEpochMillis: Long,
    val calories: Int,
    val proteinMilligrams: Int,
    val carbsMilligrams: Int,
    val fatMilligrams: Int,
)

@Entity(tableName = "serving_usage", indices = [Index("foodId")])
data class ServingUsageEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val unitKey: String,
    val useCount: Int,
    val lastUsedAtEpochMillis: Long,
    val lastAmountMilliUnits: Long,
)

@Entity(tableName = "quantity_usage", indices = [Index("foodId")])
data class QuantityUsageEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val unitKey: String,
    val amountMilliUnits: Long,
    val useCount: Int,
    val lastUsedAtEpochMillis: Long,
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingComplete: Boolean,
    val age: Int,
    val heightCm: Int,
    val weightGrams: Int,
    val formulaSex: String,
    val activityLevel: String,
    val goalType: String,
    val targetMode: String,
    val calorieGoal: Int,
    val proteinGoalGrams: Int,
    val carbsGoalGrams: Int,
    val fatGoalGrams: Int,
)

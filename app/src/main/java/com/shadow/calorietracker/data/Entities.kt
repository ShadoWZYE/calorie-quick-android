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
    val brand: String? = null,
    val barcode: String? = null,
    val isPersonal: Boolean = false,
    val archived: Boolean = false,
    val updatedAtEpochMillis: Long = 0,
    val isPackaged: Boolean = false,
    val sourceType: String = "BUILT_IN",
    val sourceId: String? = null,
    val importedAtEpochMillis: Long? = null,
)

@Entity(tableName = "nutrient_values", indices = [Index("foodId")])
data class NutrientValueEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val nutrientKey: String,
    val amountMilliUnitsPer100g: Int,
    val source: String,
)

@Entity(tableName = "allergen_declarations", indices = [Index("foodId")])
data class AllergenDeclarationEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val allergenKey: String,
    val declaration: String,
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
    val isPackage: Boolean = false,
)

@Entity(tableName = "serving_presets", indices = [Index("servingId")])
data class ServingPresetEntity(
    @PrimaryKey val id: String,
    val servingId: String,
    val amountMilliUnits: Long,
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val foodId: String,
    val name: String,
    val activeBatchId: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "recipe_batches", indices = [Index("recipeFoodId")])
data class RecipeBatchEntity(
    @PrimaryKey val id: String,
    val recipeFoodId: String,
    val cookedYieldGrams: Int,
    val remainingGrams: Int,
    val portionCount: Int,
    val cookedAtEpochMillis: Long,
)

@Entity(tableName = "recipe_ingredients", indices = [Index("batchId"), Index("foodId")])
data class RecipeIngredientEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val foodId: String,
    val foodNameEn: String,
    val foodNameRo: String,
    val grams: Int,
    val caloriesPer100g: Int,
    val proteinMilligramsPer100g: Int,
    val carbsMilligramsPer100g: Int,
    val fatMilligramsPer100g: Int,
    val fiberMilligramsPer100g: Int?,
    val sortOrder: Int,
)

@Entity(tableName = "recipe_ingredient_allergens", indices = [Index("recipeIngredientId")])
data class RecipeIngredientAllergenEntity(
    @PrimaryKey val id: String,
    val recipeIngredientId: String,
    val allergenKey: String,
    val declaration: String,
)

data class ServingWithPresets(
    @Embedded val serving: ServingEntity,
    @Relation(parentColumn = "id", entityColumn = "servingId")
    val presets: List<ServingPresetEntity>,
)

data class FoodWithServings(
    @Embedded val food: FoodEntity,
    @Relation(parentColumn = "id", entityColumn = "foodId", entity = ServingEntity::class)
    val servings: List<ServingWithPresets>,
    @Relation(parentColumn = "id", entityColumn = "foodId")
    val nutrientValues: List<NutrientValueEntity>,
    @Relation(parentColumn = "id", entityColumn = "foodId")
    val allergenDeclarations: List<AllergenDeclarationEntity>,
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
    val fiberMilligrams: Int? = null,
    val recipeBatchId: String? = null,
    val recipeBatchGrams: Int = 0,
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
    val fiberGoalGrams: Int = 25,
)

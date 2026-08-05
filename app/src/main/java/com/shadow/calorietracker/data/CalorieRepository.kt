package com.shadow.calorietracker.data

import androidx.room.withTransaction
import com.shadow.calorietracker.model.ActivityLevel
import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodEntry
import com.shadow.calorietracker.model.FoodProvenance
import com.shadow.calorietracker.model.FoodSourceType
import com.shadow.calorietracker.model.FormulaSex
import com.shadow.calorietracker.model.GoalType
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.PersonalFoodDraft
import com.shadow.calorietracker.model.QuantityUsage
import com.shadow.calorietracker.model.RecipeCalculator
import com.shadow.calorietracker.model.RecipeDraft
import com.shadow.calorietracker.model.RecipeIngredientDraft
import com.shadow.calorietracker.model.RecipeTemplate
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.TargetMode
import com.shadow.calorietracker.model.UserProfile
import com.shadow.calorietracker.model.UnitUsage
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

class CalorieRepository(private val database: AppDatabase) {
    val profile: Flow<UserProfile?> = database.profileDao().observe().map { it?.toModel() }
    val foods: Flow<List<Food>> = database.foodDao().observeFoods().map { rows -> rows.map { it.toModel() } }
    val unitUsage: Flow<List<UnitUsage>> = database.servingUsageDao().observeAll().map { rows ->
        rows.map {
            UnitUsage(
                foodId = it.foodId,
                unitKey = it.unitKey,
                useCount = it.useCount,
                lastUsedAtEpochMillis = it.lastUsedAtEpochMillis,
                lastAmount = it.lastAmountMilliUnits / 1_000.0,
            )
        }
    }
    val quantityUsage: Flow<List<QuantityUsage>> = database.quantityUsageDao().observeAll().map { rows ->
        rows.map {
            QuantityUsage(
                foodId = it.foodId,
                unitKey = it.unitKey,
                amount = it.amountMilliUnits / 1_000.0,
                useCount = it.useCount,
                lastUsedAtEpochMillis = it.lastUsedAtEpochMillis,
            )
        }
    }
    val recipes: Flow<List<RecipeTemplate>> = combine(
        database.recipeDao().observeRecipes(),
        database.recipeDao().observeBatches(),
        database.recipeDao().observeIngredients(),
        database.recipeDao().observeIngredientAllergens(),
    ) { recipes, batches, ingredients, ingredientAllergens ->
        val batchesById = batches.associateBy(RecipeBatchEntity::id)
        val ingredientsByBatch = ingredients.groupBy(RecipeIngredientEntity::batchId)
        val allergensByIngredient = ingredientAllergens.groupBy(RecipeIngredientAllergenEntity::recipeIngredientId)
        recipes.mapNotNull { recipe ->
            val batch = batchesById[recipe.activeBatchId] ?: return@mapNotNull null
            RecipeTemplate(
                foodId = recipe.foodId,
                activeBatchId = batch.id,
                name = recipe.name,
                ingredients = ingredientsByBatch[batch.id].orEmpty().sortedBy(RecipeIngredientEntity::sortOrder).map {
                    RecipeIngredientDraft(
                        foodId = it.foodId,
                        foodName = LocalizedText(it.foodNameEn, it.foodNameRo),
                        nutritionPer100g = Nutrition(
                            it.caloriesPer100g,
                            it.proteinMilligramsPer100g / 1_000.0,
                            it.carbsMilligramsPer100g / 1_000.0,
                            it.fatMilligramsPer100g / 1_000.0,
                            it.fiberMilligramsPer100g?.div(1_000.0),
                        ),
                        allergens = allergensByIngredient[it.id].orEmpty().associate { declaration ->
                            Allergen.valueOf(declaration.allergenKey) to
                                AllergenDeclaration.valueOf(declaration.declaration)
                        },
                        grams = it.grams,
                    )
                },
                cookedYieldGrams = batch.cookedYieldGrams,
                portionCount = batch.portionCount,
                cookedAtEpochMillis = batch.cookedAtEpochMillis,
            )
        }
    }

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
            database.foodDao().upsertNutrients(seedNutrientEntities)
            database.foodDao().upsertAllergens(seedAllergenEntities)
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        database.profileDao().upsert(profile.toEntity())
    }

    suspend fun savePersonalFood(draft: PersonalFoodDraft): String {
        val foodId = draft.id ?: "personal-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val name = draft.name.trim()
        val brand = draft.brand?.trim()?.takeIf(String::isNotEmpty)
        val barcode = draft.barcode?.trim()?.takeIf(String::isNotEmpty)
        barcode?.let { normalizedBarcode ->
            database.foodDao().findActiveByBarcode(normalizedBarcode)?.takeIf { it.id != draft.id }?.let {
                return it.id
            }
        }
        val importedAt = when (draft.provenance.type) {
            FoodSourceType.OPEN_FOOD_FACTS, FoodSourceType.OCR -> draft.provenance.importedAtEpochMillis ?: now
            else -> draft.provenance.importedAtEpochMillis
        }
        database.withTransaction {
            database.foodDao().upsertFood(
                FoodEntity(
                    id = foodId,
                    nameEn = name,
                    nameRo = name,
                    detailEn = brand ?: "Personal food",
                    detailRo = brand ?: "Aliment personal",
                    caloriesPer100g = draft.nutritionPer100g.calories,
                    proteinMilligramsPer100g = (draft.nutritionPer100g.proteinGrams * 1_000).roundToInt(),
                    carbsMilligramsPer100g = (draft.nutritionPer100g.carbsGrams * 1_000).roundToInt(),
                    fatMilligramsPer100g = (draft.nutritionPer100g.fatGrams * 1_000).roundToInt(),
                    brand = brand,
                    barcode = barcode,
                    isPersonal = true,
                    archived = false,
                    updatedAtEpochMillis = now,
                    isPackaged = draft.isPackaged,
                    sourceType = draft.provenance.type.name,
                    sourceId = draft.provenance.sourceId,
                    importedAtEpochMillis = importedAt,
                ),
            )
            database.foodDao().deleteNutrients(foodId)
            database.foodDao().upsertNutrients(
                draft.nutritionPer100g.toNutrientEntities(foodId, draft.provenance.type.name.lowercase()),
            )
            database.foodDao().deleteAllergens(foodId)
            database.foodDao().upsertAllergens(
                draft.allergens.map { (allergen, declaration) ->
                    AllergenDeclarationEntity("$foodId|${allergen.name}", foodId, allergen.name, declaration.name)
                },
            )
            database.foodDao().deleteServingPresets(foodId)
            database.foodDao().deleteServings(foodId)
            val servingEntities = draft.measures.map { measure ->
                    val labelEn = measure.label.en.trim().removePrefix("1 ").trim()
                    val labelRo = measure.label.ro.trim().removePrefix("1 ").trim()
                    ServingEntity(
                        id = measure.id ?: "personal-measure-${UUID.randomUUID()}",
                        foodId = foodId,
                        labelEn = "1 $labelEn",
                        labelRo = "1 $labelRo",
                        grams = measure.grams,
                        isPackage = measure.isPackage,
                    )
                }
            database.foodDao().upsertServings(servingEntities)
            database.foodDao().upsertServingPresets(
                draft.measures.zip(servingEntities).flatMap { (measure, serving) ->
                    measure.suggestedAmounts.map { amount ->
                        val milliUnits = (amount * 1_000).roundToInt().toLong()
                        ServingPresetEntity("${serving.id}|$milliUnits", serving.id, milliUnits)
                    }
                },
            )
        }
        return foodId
    }

    suspend fun saveRecipe(draft: RecipeDraft): String {
        val foodId = draft.id ?: "recipe-${UUID.randomUUID()}"
        val batchId = "recipe-batch-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val calculation = RecipeCalculator.calculate(draft.ingredients, draft.cookedYieldGrams, draft.portionCount)
        val existingRecipe = database.recipeDao().findRecipe(foodId)
        val nutrition = calculation.nutritionPer100g
        val name = draft.name.trim()
        database.withTransaction {
            database.foodDao().upsertFood(
                FoodEntity(
                    id = foodId,
                    nameEn = name,
                    nameRo = name,
                    detailEn = "${draft.ingredients.size} ingredients · ${draft.cookedYieldGrams} g batch",
                    detailRo = "${draft.ingredients.size} ingrediente · preparat de ${draft.cookedYieldGrams} g",
                    caloriesPer100g = nutrition.calories,
                    proteinMilligramsPer100g = (nutrition.proteinGrams * 1_000).roundToInt(),
                    carbsMilligramsPer100g = (nutrition.carbsGrams * 1_000).roundToInt(),
                    fatMilligramsPer100g = (nutrition.fatGrams * 1_000).roundToInt(),
                    isPersonal = true,
                    updatedAtEpochMillis = now,
                    sourceType = FoodSourceType.RECIPE.name,
                    sourceId = foodId,
                ),
            )
            database.foodDao().deleteNutrients(foodId)
            database.foodDao().upsertNutrients(nutrition.toNutrientEntities(foodId, "recipe"))
            database.foodDao().deleteAllergens(foodId)
            database.foodDao().upsertAllergens(
                calculation.allergens.map { (allergen, declaration) ->
                    AllergenDeclarationEntity("$foodId|${allergen.name}", foodId, allergen.name, declaration.name)
                },
            )
            database.foodDao().deleteServingPresets(foodId)
            database.foodDao().deleteServings(foodId)
            database.foodDao().upsertServings(
                listOf(
                    ServingEntity(
                        id = "$foodId-portion",
                        foodId = foodId,
                        labelEn = "1 portion",
                        labelRo = "1 porție",
                        grams = calculation.portionGrams,
                    ),
                ),
            )
            database.recipeDao().insertBatch(
                RecipeBatchEntity(batchId, foodId, draft.cookedYieldGrams, draft.portionCount, now),
            )
            val ingredientEntities = draft.ingredients.mapIndexed { index, ingredient ->
                RecipeIngredientEntity(
                    id = "$batchId|$index",
                    batchId = batchId,
                    foodId = ingredient.foodId,
                    foodNameEn = ingredient.foodName.en,
                    foodNameRo = ingredient.foodName.ro,
                    grams = ingredient.grams,
                    caloriesPer100g = ingredient.nutritionPer100g.calories,
                    proteinMilligramsPer100g = (ingredient.nutritionPer100g.proteinGrams * 1_000).roundToInt(),
                    carbsMilligramsPer100g = (ingredient.nutritionPer100g.carbsGrams * 1_000).roundToInt(),
                    fatMilligramsPer100g = (ingredient.nutritionPer100g.fatGrams * 1_000).roundToInt(),
                    fiberMilligramsPer100g = ingredient.nutritionPer100g.fiberGrams?.let { (it * 1_000).roundToInt() },
                    sortOrder = index,
                )
            }
            database.recipeDao().insertIngredients(ingredientEntities)
            database.recipeDao().insertIngredientAllergens(
                draft.ingredients.zip(ingredientEntities).flatMap { (ingredient, entity) ->
                    ingredient.allergens.map { (allergen, declaration) ->
                        RecipeIngredientAllergenEntity(
                            id = "${entity.id}|${allergen.name}",
                            recipeIngredientId = entity.id,
                            allergenKey = allergen.name,
                            declaration = declaration.name,
                        )
                    }
                },
            )
            database.recipeDao().upsertRecipe(
                RecipeEntity(
                    foodId = foodId,
                    name = name,
                    activeBatchId = batchId,
                    createdAtEpochMillis = existingRecipe?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                ),
            )
        }
        return foodId
    }

    suspend fun archivePersonalFood(foodId: String) {
        database.foodDao().archivePersonalFood(foodId, System.currentTimeMillis())
    }

    suspend fun addEntry(food: Food, amount: Double, serving: Serving?) {
        val grams = (amount * (serving?.grams ?: 1)).roundToInt().coerceIn(1, 5_000)
        val nutrition = food.nutritionPer100g.forGrams(grams)
        val now = System.currentTimeMillis()
        val amountMilliUnits = (amount * 1_000).roundToInt().toLong()
        val unitKey = serving?.id ?: GRAMS_UNIT_KEY
        val unitLabel = serving?.label?.withoutLeadingOne() ?: LocalizedText("g", "g")
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
                        enteredAmountMilliUnits = amountMilliUnits,
                        unitKey = unitKey,
                        unitLabelEn = unitLabel.en,
                        unitLabelRo = unitLabel.ro,
                        consumedAtEpochMillis = now,
                        calories = nutrition.calories,
                        proteinMilligrams = (nutrition.proteinGrams * 1_000).roundToInt(),
                        carbsMilligrams = (nutrition.carbsGrams * 1_000).roundToInt(),
                        fatMilligrams = (nutrition.fatGrams * 1_000).roundToInt(),
                        fiberMilligrams = nutrition.fiberGrams?.let { (it * 1_000).roundToInt() },
                    ),
                )
            } else {
                val sameUnit = recent.unitKey == unitKey
                database.diaryDao().update(
                    recent.copy(
                        grams = recent.grams + grams,
                        enteredAmountMilliUnits = if (sameUnit) {
                            recent.enteredAmountMilliUnits + amountMilliUnits
                        } else {
                            (recent.grams + grams) * 1_000L
                        },
                        unitKey = if (sameUnit) unitKey else GRAMS_UNIT_KEY,
                        unitLabelEn = if (sameUnit) unitLabel.en else "g",
                        unitLabelRo = if (sameUnit) unitLabel.ro else "g",
                        calories = recent.calories + nutrition.calories,
                        proteinMilligrams = recent.proteinMilligrams + (nutrition.proteinGrams * 1_000).roundToInt(),
                        carbsMilligrams = recent.carbsMilligrams + (nutrition.carbsGrams * 1_000).roundToInt(),
                        fatMilligrams = recent.fatMilligrams + (nutrition.fatGrams * 1_000).roundToInt(),
                        fiberMilligrams = combineMilligrams(
                            recent.fiberMilligrams,
                            nutrition.fiberGrams?.let { (it * 1_000).roundToInt() },
                        ),
                    ),
                )
            }
            val usageId = "${food.id}|$unitKey"
            if (database.servingUsageDao().increment(usageId, now, amountMilliUnits) == 0) {
                database.servingUsageDao().insert(
                    ServingUsageEntity(usageId, food.id, unitKey, 1, now, amountMilliUnits),
                )
            }
            val quantityUsageId = "$usageId|$amountMilliUnits"
            if (database.quantityUsageDao().increment(quantityUsageId, now) == 0) {
                database.quantityUsageDao().insert(
                    QuantityUsageEntity(
                        quantityUsageId,
                        food.id,
                        unitKey,
                        amountMilliUnits,
                        1,
                        now,
                    ),
                )
            }
        }
    }

    suspend fun deleteEntry(entry: FoodEntry) {
        database.withTransaction {
            database.diaryDao().delete(entry.toEntity())
            val usageId = "${entry.foodId}|${entry.unitKey}"
            database.servingUsageDao().decrement(usageId)
            database.servingUsageDao().deleteIfUnused(usageId)
        }
    }
}

private const val ENTRY_MERGE_WINDOW_MILLIS = 2 * 60 * 1_000L
const val GRAMS_UNIT_KEY = "grams"

private fun FoodWithServings.toModel() = Food(
    id = food.id,
    names = LocalizedText(food.nameEn, food.nameRo),
    details = LocalizedText(food.detailEn, food.detailRo),
    nutritionPer100g = nutrientValues.associateBy(NutrientValueEntity::nutrientKey).let { nutrients ->
        Nutrition(
            calories = nutrients["calories"]?.amountMilliUnitsPer100g ?: food.caloriesPer100g,
            proteinGrams = (nutrients["protein"]?.amountMilliUnitsPer100g ?: food.proteinMilligramsPer100g) / 1_000.0,
            carbsGrams = (nutrients["carbs"]?.amountMilliUnitsPer100g ?: food.carbsMilligramsPer100g) / 1_000.0,
            fatGrams = (nutrients["fat"]?.amountMilliUnitsPer100g ?: food.fatMilligramsPer100g) / 1_000.0,
            fiberGrams = nutrients["fiber"]?.amountMilliUnitsPer100g?.div(1_000.0),
        )
    },
    servings = servings.sortedByDescending { it.serving.grams }.map {
        Serving(
            it.serving.id,
            LocalizedText(it.serving.labelEn, it.serving.labelRo),
            it.serving.grams,
            it.presets.map { preset -> preset.amountMilliUnits / 1_000.0 },
            it.serving.isPackage,
        )
    },
    brand = food.brand,
    barcode = food.barcode,
    isPersonal = food.isPersonal,
    isPackaged = food.isPackaged,
    allergens = allergenDeclarations.associate {
        Allergen.valueOf(it.allergenKey) to AllergenDeclaration.valueOf(it.declaration)
    },
    provenance = FoodProvenance(
        type = runCatching { FoodSourceType.valueOf(food.sourceType) }.getOrDefault(FoodSourceType.PERSONAL),
        sourceId = food.sourceId,
        importedAtEpochMillis = food.importedAtEpochMillis,
        locallyModified = food.importedAtEpochMillis?.let { food.updatedAtEpochMillis > it } == true,
    ),
)

private fun DiaryEntryEntity.toModel() = FoodEntry(
    id = id,
    foodId = foodId,
    foodName = LocalizedText(foodNameEn, foodNameRo),
    grams = grams,
    enteredAmount = enteredAmountMilliUnits / 1_000.0,
    unitKey = unitKey,
    unitLabel = LocalizedText(unitLabelEn, unitLabelRo),
    consumedAtEpochMillis = consumedAtEpochMillis,
    nutrition = Nutrition(
        calories,
        proteinMilligrams / 1_000.0,
        carbsMilligrams / 1_000.0,
        fatMilligrams / 1_000.0,
        fiberMilligrams?.div(1_000.0),
    ),
)

private fun FoodEntry.toEntity() = DiaryEntryEntity(
    id = id,
    foodId = foodId,
    foodNameEn = foodName.en,
    foodNameRo = foodName.ro,
    grams = grams,
    enteredAmountMilliUnits = (enteredAmount * 1_000).roundToInt().toLong(),
    unitKey = unitKey,
    unitLabelEn = unitLabel.en,
    unitLabelRo = unitLabel.ro,
    consumedAtEpochMillis = consumedAtEpochMillis,
    calories = nutrition.calories,
    proteinMilligrams = (nutrition.proteinGrams * 1_000).roundToInt(),
    carbsMilligrams = (nutrition.carbsGrams * 1_000).roundToInt(),
    fatMilligrams = (nutrition.fatGrams * 1_000).roundToInt(),
    fiberMilligrams = nutrition.fiberGrams?.let { (it * 1_000).roundToInt() },
)

private fun LocalizedText.withoutLeadingOne() = LocalizedText(en.removePrefix("1 "), ro.removePrefix("1 "))

private fun UserProfileEntity.toModel() = UserProfile(
    onboardingComplete = onboardingComplete,
    age = age,
    heightCm = heightCm,
    weightKg = weightGrams / 1_000.0,
    formulaSex = FormulaSex.valueOf(formulaSex),
    activityLevel = ActivityLevel.valueOf(activityLevel),
    goalType = GoalType.valueOf(goalType),
    calorieGoal = calorieGoal,
    proteinGoalGrams = proteinGoalGrams,
    carbsGoalGrams = carbsGoalGrams,
    fatGoalGrams = fatGoalGrams,
    fiberGoalGrams = fiberGoalGrams,
    targetMode = TargetMode.valueOf(targetMode),
)

private fun UserProfile.toEntity() = UserProfileEntity(
    onboardingComplete = onboardingComplete,
    age = age,
    heightCm = heightCm,
    weightGrams = (weightKg * 1_000).roundToInt(),
    formulaSex = formulaSex.name,
    activityLevel = activityLevel.name,
    goalType = goalType.name,
    targetMode = targetMode.name,
    calorieGoal = calorieGoal,
    proteinGoalGrams = proteinGoalGrams,
    carbsGoalGrams = carbsGoalGrams,
    fatGoalGrams = fatGoalGrams,
    fiberGoalGrams = fiberGoalGrams,
)

private fun combineMilligrams(first: Int?, second: Int?): Int? = when {
    first == null && second == null -> null
    else -> (first ?: 0) + (second ?: 0)
}

private fun Nutrition.toNutrientEntities(foodId: String, source: String): List<NutrientValueEntity> = buildList {
    add(NutrientValueEntity("$foodId|calories", foodId, "calories", calories, source))
    add(NutrientValueEntity("$foodId|protein", foodId, "protein", (proteinGrams * 1_000).roundToInt(), source))
    add(NutrientValueEntity("$foodId|carbs", foodId, "carbs", (carbsGrams * 1_000).roundToInt(), source))
    add(NutrientValueEntity("$foodId|fat", foodId, "fat", (fatGrams * 1_000).roundToInt(), source))
    fiberGrams?.let { add(NutrientValueEntity("$foodId|fiber", foodId, "fiber", (it * 1_000).roundToInt(), source)) }
}

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

private val seedFiberMilligrams = mapOf(
    "greek-yogurt" to 0,
    "banana" to 2_600,
    "chicken-breast" to 0,
    "oats" to 10_100,
    "eggs" to 0,
    "rice" to 400,
)

private val seedNutrientEntities = seedFoodEntities.flatMap { food ->
    Nutrition(
        calories = food.caloriesPer100g,
        proteinGrams = food.proteinMilligramsPer100g / 1_000.0,
        carbsGrams = food.carbsMilligramsPer100g / 1_000.0,
        fatGrams = food.fatMilligramsPer100g / 1_000.0,
        fiberGrams = seedFiberMilligrams[food.id]?.div(1_000.0),
    ).toNutrientEntities(food.id, "seed")
}

private val seedAllergenEntities = listOf(
    AllergenDeclarationEntity("eggs|EGGS", "eggs", Allergen.EGGS.name, AllergenDeclaration.CONTAINS.name),
    AllergenDeclarationEntity(
        "greek-yogurt|MILK",
        "greek-yogurt",
        Allergen.MILK.name,
        AllergenDeclaration.CONTAINS.name,
    ),
)

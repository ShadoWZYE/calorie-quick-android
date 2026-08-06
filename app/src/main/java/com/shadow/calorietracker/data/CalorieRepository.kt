package com.shadow.calorietracker.data

import androidx.room.withTransaction
import com.shadow.calorietracker.model.ActivityLevel
import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import com.shadow.calorietracker.model.BodyMeasurement
import com.shadow.calorietracker.model.BodyMeasurementSource
import com.shadow.calorietracker.model.BodyLengthUnit
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodImage
import com.shadow.calorietracker.model.FoodImageSource
import com.shadow.calorietracker.model.FoodPreparation
import com.shadow.calorietracker.model.FoodEntry
import com.shadow.calorietracker.model.FoodProvenance
import com.shadow.calorietracker.model.FoodSourceType
import com.shadow.calorietracker.model.FormulaSex
import com.shadow.calorietracker.model.GoalType
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.PersonalFoodDraft
import com.shadow.calorietracker.model.PreparationUsage
import com.shadow.calorietracker.model.QuantityUsage
import com.shadow.calorietracker.model.RecipeCalculator
import com.shadow.calorietracker.model.RecipeDraft
import com.shadow.calorietracker.model.RecipeIngredientDraft
import com.shadow.calorietracker.model.RecipeBatchSummary
import com.shadow.calorietracker.model.RecipeTemplate
import com.shadow.calorietracker.model.ReviewStatus
import com.shadow.calorietracker.model.Serving
import com.shadow.calorietracker.model.TargetMode
import com.shadow.calorietracker.model.UserProfile
import com.shadow.calorietracker.model.UnitUsage
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

class CalorieRepository(
    private val database: AppDatabase,
    private val builtInCatalogueImporter: BuiltInCatalogueImporter? = null,
) {
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
    val allEntries: Flow<List<FoodEntry>> = database.diaryDao().observeAll().map { rows -> rows.map { it.toModel() } }
    val ingredientUsage: Flow<Map<String, Int>> = database.recipeDao().observeIngredients().map { rows ->
        rows.groupBy(RecipeIngredientEntity::foodId).mapValues { (_, uses) ->
            uses.distinctBy(RecipeIngredientEntity::batchId).size
        }
    }
    val bodyMeasurements: Flow<List<BodyMeasurement>> = database.bodyMeasurementDao().observeAll().map { rows ->
        rows.map(BodyMeasurementEntity::toModel)
    }
    val preparationUsage: Flow<List<PreparationUsage>> = database.preparationUsageDao().observeAll().map { rows ->
        rows.map { PreparationUsage(it.foodId, it.preparationId, it.useCount, it.lastUsedAtEpochMillis) }
    }
    val recipes: Flow<List<RecipeTemplate>> = combine(
        database.recipeDao().observeRecipes(),
        database.recipeDao().observeBatches(),
        database.recipeDao().observeIngredients(),
        database.recipeDao().observeIngredientAllergens(),
    ) { recipes, batches, ingredients, ingredientAllergens ->
        val batchesById = batches.associateBy(RecipeBatchEntity::id)
        val batchesByRecipe = batches.groupBy(RecipeBatchEntity::recipeFoodId)
        val ingredientsByBatch = ingredients.groupBy(RecipeIngredientEntity::batchId)
        val allergensByIngredient = ingredientAllergens.groupBy(RecipeIngredientAllergenEntity::recipeIngredientId)
        fun ingredientsFor(batchId: String) = ingredientsByBatch[batchId].orEmpty()
            .sortedBy(RecipeIngredientEntity::sortOrder)
            .map {
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
            }
        recipes.mapNotNull { recipe ->
            val batch = batchesById[recipe.activeBatchId] ?: return@mapNotNull null
            val activeIngredients = ingredientsFor(batch.id)
            val batchHistory = batchesByRecipe[recipe.foodId].orEmpty().mapNotNull { recipeBatch ->
                val batchIngredients = ingredientsFor(recipeBatch.id)
                if (batchIngredients.isEmpty()) return@mapNotNull null
                RecipeBatchSummary(
                    id = recipeBatch.id,
                    cookedYieldGrams = recipeBatch.cookedYieldGrams,
                    remainingGrams = recipeBatch.remainingGrams,
                    portionCount = recipeBatch.portionCount,
                    cookedAtEpochMillis = recipeBatch.cookedAtEpochMillis,
                    nutritionPer100g = RecipeCalculator.calculate(
                        batchIngredients,
                        recipeBatch.cookedYieldGrams,
                        recipeBatch.portionCount,
                    ).nutritionPer100g,
                )
            }.sortedByDescending(RecipeBatchSummary::cookedAtEpochMillis)
            RecipeTemplate(
                foodId = recipe.foodId,
                activeBatchId = batch.id,
                name = recipe.name,
                ingredients = activeIngredients,
                cookedYieldGrams = batch.cookedYieldGrams,
                portionCount = batch.portionCount,
                cookedAtEpochMillis = batch.cookedAtEpochMillis,
                remainingGrams = batch.remainingGrams,
                batches = batchHistory,
                reviewStatus = runCatching { ReviewStatus.valueOf(recipe.reviewStatus) }
                    .getOrDefault(ReviewStatus.PRIVATE),
            )
        }
    }

    suspend fun seedFoods() {
        builtInCatalogueImporter?.let {
            it.import()
            return
        }
        if (database.foodDao().count() > 0) return
        database.withTransaction {
            database.foodDao().insertFoods(seedFoodEntities)
            database.foodDao().insertServings(seedServingEntities)
            database.foodDao().upsertNutrients(seedNutrientEntities)
            database.foodDao().upsertAllergens(seedAllergenEntities)
            database.foodDao().upsertPreparations(seedPreparationEntities)
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        database.profileDao().upsert(profile.toEntity())
    }

    suspend fun saveBodyMeasurement(measurement: BodyMeasurement) {
        database.bodyMeasurementDao().upsert(measurement.toEntity())
    }

    suspend fun deleteBodyMeasurement(measurement: BodyMeasurement) {
        measurement.id?.let { database.bodyMeasurementDao().delete(measurement.toEntity()) }
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
                    imageLocalPath = draft.image?.localPath,
                    imageRemoteUrl = draft.image?.remoteUrl,
                    imageSource = draft.image?.source?.name,
                    imageAttribution = draft.image?.attribution,
                    imageLicense = draft.image?.license,
                    reviewStatus = draft.reviewStatus.name,
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
                    reviewStatus = draft.reviewStatus.name,
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
                RecipeBatchEntity(
                    id = batchId,
                    recipeFoodId = foodId,
                    cookedYieldGrams = draft.cookedYieldGrams,
                    remainingGrams = draft.cookedYieldGrams,
                    portionCount = draft.portionCount,
                    cookedAtEpochMillis = now,
                ),
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
                    reviewStatus = draft.reviewStatus.name,
                ),
            )
        }
        return foodId
    }

    suspend fun archivePersonalFood(foodId: String) {
        database.foodDao().archivePersonalFood(foodId, System.currentTimeMillis())
    }

    suspend fun addEntry(
        food: Food,
        amount: Double,
        serving: Serving?,
        recipeBatchId: String? = null,
        consumedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val grams = (amount * (serving?.grams ?: 1)).roundToInt().coerceIn(1, 5_000)
        val interactionAt = System.currentTimeMillis()
        val amountMilliUnits = (amount * 1_000).roundToInt().toLong()
        val unitKey = serving?.id ?: GRAMS_UNIT_KEY
        val unitLabel = serving?.label?.withoutLeadingOne() ?: LocalizedText("g", "g")
        database.withTransaction {
            val recipeBatch = if (food.provenance.type == FoodSourceType.RECIPE && recipeBatchId != null) {
                val batch = database.recipeDao().findBatch(recipeBatchId) ?: return@withTransaction
                if (batch.recipeFoodId != food.id) return@withTransaction
                batch
            } else {
                null
            }
            if (recipeBatch != null && database.recipeDao().consumeBatch(recipeBatch.id, grams) == 0) {
                return@withTransaction
            }
            val nutrition = food.nutritionPer100g.forGrams(grams)
            val recent = database.diaryDao().findNear(
                food.id,
                consumedAtEpochMillis - ENTRY_MERGE_WINDOW_MILLIS,
                consumedAtEpochMillis + ENTRY_MERGE_WINDOW_MILLIS,
            )
            if (recent == null || recent.recipeBatchId != recipeBatch?.id || recent.preparationId != food.activePreparationId) {
                val preparation = food.preparations.firstOrNull { it.id == food.activePreparationId }
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
                        consumedAtEpochMillis = consumedAtEpochMillis,
                        calories = nutrition.calories,
                        proteinMilligrams = (nutrition.proteinGrams * 1_000).roundToInt(),
                        carbsMilligrams = (nutrition.carbsGrams * 1_000).roundToInt(),
                        fatMilligrams = (nutrition.fatGrams * 1_000).roundToInt(),
                        fiberMilligrams = nutrition.fiberGrams?.let { (it * 1_000).roundToInt() },
                        recipeBatchId = recipeBatch?.id,
                        recipeBatchGrams = if (recipeBatch == null) 0 else grams,
                        preparationId = preparation?.id,
                        preparationNameEn = preparation?.names?.en,
                        preparationNameRo = preparation?.names?.ro,
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
                        recipeBatchGrams = recent.recipeBatchGrams + if (recipeBatch == null) 0 else grams,
                    ),
                )
            }
            val usageId = "${food.id}|$unitKey"
            if (database.servingUsageDao().increment(usageId, interactionAt, amountMilliUnits) == 0) {
                database.servingUsageDao().insert(
                    ServingUsageEntity(usageId, food.id, unitKey, 1, interactionAt, amountMilliUnits),
                )
            }
            val quantityUsageId = "$usageId|$amountMilliUnits"
            if (database.quantityUsageDao().increment(quantityUsageId, interactionAt) == 0) {
                database.quantityUsageDao().insert(
                    QuantityUsageEntity(
                        quantityUsageId,
                        food.id,
                        unitKey,
                        amountMilliUnits,
                        1,
                        interactionAt,
                    ),
                )
            }
            food.activePreparationId?.let { preparationId ->
                val preparationUsageId = "${food.id}|$preparationId"
                if (database.preparationUsageDao().increment(preparationUsageId, interactionAt) == 0) {
                    database.preparationUsageDao().insert(
                        PreparationUsageEntity(preparationUsageId, food.id, preparationId, 1, interactionAt),
                    )
                }
            }
        }
    }

    suspend fun updateEntry(entry: FoodEntry, enteredAmount: Double, consumedAtEpochMillis: Long): Boolean {
        if (enteredAmount <= 0.0) return false
        val gramsPerUnit = entry.grams / entry.enteredAmount.coerceAtLeast(0.001)
        val newGrams = (enteredAmount * gramsPerUnit).roundToInt().coerceIn(1, 5_000)
        val scale = newGrams / entry.grams.toDouble().coerceAtLeast(1.0)
        return database.withTransaction {
            entry.recipeBatchId?.let { batchId ->
                val difference = newGrams - entry.recipeBatchGrams
                when {
                    difference > 0 && database.recipeDao().consumeBatch(batchId, difference) == 0 -> return@withTransaction false
                    difference < 0 -> database.recipeDao().restoreBatch(batchId, -difference)
                }
            }
            database.diaryDao().update(
                entry.copy(
                    grams = newGrams,
                    enteredAmount = enteredAmount,
                    consumedAtEpochMillis = consumedAtEpochMillis,
                    nutrition = Nutrition(
                        calories = (entry.nutrition.calories * scale).roundToInt(),
                        proteinGrams = entry.nutrition.proteinGrams * scale,
                        carbsGrams = entry.nutrition.carbsGrams * scale,
                        fatGrams = entry.nutrition.fatGrams * scale,
                        fiberGrams = entry.nutrition.fiberGrams?.times(scale),
                    ),
                    recipeBatchGrams = if (entry.recipeBatchId == null) 0 else newGrams,
                ).toEntity(),
            )
            true
        }
    }

    suspend fun deleteEntry(entry: FoodEntry) {
        database.withTransaction {
            database.diaryDao().delete(entry.toEntity())
            entry.recipeBatchId?.let { batchId ->
                if (entry.recipeBatchGrams > 0) database.recipeDao().restoreBatch(batchId, entry.recipeBatchGrams)
            }
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
    image = food.toImage(),
    reviewStatus = runCatching { ReviewStatus.valueOf(food.reviewStatus) }.getOrDefault(ReviewStatus.PRIVATE),
    preparations = preparations.sortedBy(FoodPreparationEntity::sortOrder).map { preparation ->
        FoodPreparation(
            id = preparation.id,
            names = LocalizedText(preparation.nameEn, preparation.nameRo),
            nutritionPer100g = Nutrition(
                preparation.caloriesPer100g,
                preparation.proteinMilligramsPer100g / 1_000.0,
                preparation.carbsMilligramsPer100g / 1_000.0,
                preparation.fatMilligramsPer100g / 1_000.0,
                preparation.fiberMilligramsPer100g?.div(1_000.0),
            ),
            image = preparation.toImage(),
            sortOrder = preparation.sortOrder,
        )
    },
    defaultPreparationId = food.defaultPreparationId,
    categoryKey = food.categoryKey,
    aliases = aliases.map(FoodAliasEntity::value),
)

private fun FoodEntity.toImage(): FoodImage? = imageSource?.let { source ->
    FoodImage(
        source = runCatching { FoodImageSource.valueOf(source) }.getOrDefault(FoodImageSource.LOCAL),
        localPath = imageLocalPath,
        remoteUrl = imageRemoteUrl,
        attribution = imageAttribution,
        license = imageLicense,
    )
}

private fun FoodPreparationEntity.toImage(): FoodImage? = imageSource?.let { source ->
    FoodImage(
        source = runCatching { FoodImageSource.valueOf(source) }.getOrDefault(FoodImageSource.LOCAL),
        localPath = imageLocalPath,
        remoteUrl = imageRemoteUrl,
        attribution = imageAttribution,
        license = imageLicense,
    )
}

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
    recipeBatchId = recipeBatchId,
    recipeBatchGrams = recipeBatchGrams,
    preparationId = preparationId,
    preparationName = preparationNameEn?.let { LocalizedText(it, preparationNameRo ?: it) },
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
    recipeBatchId = recipeBatchId,
    recipeBatchGrams = recipeBatchGrams,
    preparationId = preparationId,
    preparationNameEn = preparationName?.en,
    preparationNameRo = preparationName?.ro,
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
    displayName = displayName,
    lastGoalReviewAtEpochMillis = lastGoalReviewAtEpochMillis,
    bodyLengthUnit = runCatching { BodyLengthUnit.valueOf(bodyLengthUnit) }.getOrDefault(BodyLengthUnit.CENTIMETERS),
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
    displayName = displayName.trim(),
    lastGoalReviewAtEpochMillis = lastGoalReviewAtEpochMillis,
    bodyLengthUnit = bodyLengthUnit.name,
)

private fun BodyMeasurementEntity.toModel() = BodyMeasurement(
    id = id,
    measuredAtEpochMillis = measuredAtEpochMillis,
    weightKg = weightGrams / 1_000.0,
    bmi = bmiMilliUnits?.div(1_000.0),
    bodyFatPercent = bodyFatMilliPercent?.div(1_000.0),
    fatMassKg = fatMassGrams?.div(1_000.0),
    fatFreeMassKg = fatFreeMassGrams?.div(1_000.0),
    muscleMassKg = muscleMassGrams?.div(1_000.0),
    musclePercent = muscleMilliPercent?.div(1_000.0),
    skeletalMusclePercent = skeletalMuscleMilliPercent?.div(1_000.0),
    boneMassKg = boneMassGrams?.div(1_000.0),
    proteinMassKg = proteinMassGrams?.div(1_000.0),
    proteinPercent = proteinMilliPercent?.div(1_000.0),
    waterMassKg = waterMassGrams?.div(1_000.0),
    bodyWaterPercent = bodyWaterMilliPercent?.div(1_000.0),
    subcutaneousFatPercent = subcutaneousFatMilliPercent?.div(1_000.0),
    visceralFat = visceralFatMilliUnits?.div(1_000.0),
    bmrCalories = bmrCalories,
    bodyAge = bodyAge,
    neckCm = neckMillimeters?.div(10.0),
    chestCm = chestMillimeters?.div(10.0),
    waistCm = waistMillimeters?.div(10.0),
    hipsCm = hipsMillimeters?.div(10.0),
    upperArmCm = upperArmMillimeters?.div(10.0),
    thighCm = thighMillimeters?.div(10.0),
    calfCm = calfMillimeters?.div(10.0),
    source = runCatching { BodyMeasurementSource.valueOf(source) }.getOrDefault(BodyMeasurementSource.MANUAL),
)

private fun BodyMeasurement.toEntity() = BodyMeasurementEntity(
    id = id ?: "body-${UUID.randomUUID()}",
    measuredAtEpochMillis = measuredAtEpochMillis,
    weightGrams = (weightKg * 1_000).roundToInt(),
    bmiMilliUnits = bmi?.let { (it * 1_000).roundToInt() },
    bodyFatMilliPercent = bodyFatPercent?.let { (it * 1_000).roundToInt() },
    fatMassGrams = fatMassKg?.let { (it * 1_000).roundToInt() },
    fatFreeMassGrams = fatFreeMassKg?.let { (it * 1_000).roundToInt() },
    muscleMassGrams = muscleMassKg?.let { (it * 1_000).roundToInt() },
    muscleMilliPercent = musclePercent?.let { (it * 1_000).roundToInt() },
    skeletalMuscleMilliPercent = skeletalMusclePercent?.let { (it * 1_000).roundToInt() },
    boneMassGrams = boneMassKg?.let { (it * 1_000).roundToInt() },
    proteinMassGrams = proteinMassKg?.let { (it * 1_000).roundToInt() },
    proteinMilliPercent = proteinPercent?.let { (it * 1_000).roundToInt() },
    waterMassGrams = waterMassKg?.let { (it * 1_000).roundToInt() },
    bodyWaterMilliPercent = bodyWaterPercent?.let { (it * 1_000).roundToInt() },
    subcutaneousFatMilliPercent = subcutaneousFatPercent?.let { (it * 1_000).roundToInt() },
    visceralFatMilliUnits = visceralFat?.let { (it * 1_000).roundToInt() },
    bmrCalories = bmrCalories,
    bodyAge = bodyAge,
    neckMillimeters = neckCm?.let { (it * 10).roundToInt() },
    chestMillimeters = chestCm?.let { (it * 10).roundToInt() },
    waistMillimeters = waistCm?.let { (it * 10).roundToInt() },
    hipsMillimeters = hipsCm?.let { (it * 10).roundToInt() },
    upperArmMillimeters = upperArmCm?.let { (it * 10).roundToInt() },
    thighMillimeters = thighCm?.let { (it * 10).roundToInt() },
    calfMillimeters = calfCm?.let { (it * 10).roundToInt() },
    source = source.name,
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
    FoodEntity(
        "eggs", "Whole egg", "Ou întreg", "Boiled", "Fiert", 155, 12_600, 1_100, 10_600,
        defaultPreparationId = "eggs|boiled",
    ),
    FoodEntity("rice", "White rice", "Orez alb", "Cooked", "Gătit", 130, 2_700, 28_200, 300),
)

private val seedPreparationEntities = listOf(
    FoodPreparationEntity(
        id = "eggs|boiled",
        foodId = "eggs",
        nameEn = "Boiled",
        nameRo = "Fiert",
        caloriesPer100g = 155,
        proteinMilligramsPer100g = 12_600,
        carbsMilligramsPer100g = 1_100,
        fatMilligramsPer100g = 10_600,
        fiberMilligramsPer100g = 0,
        sortOrder = 0,
    ),
    FoodPreparationEntity(
        id = "eggs|fried",
        foodId = "eggs",
        nameEn = "Fried",
        nameRo = "Prăjit",
        caloriesPer100g = 196,
        proteinMilligramsPer100g = 13_600,
        carbsMilligramsPer100g = 800,
        fatMilligramsPer100g = 14_800,
        fiberMilligramsPer100g = 0,
        sortOrder = 1,
    ),
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

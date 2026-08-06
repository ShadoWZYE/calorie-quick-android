package com.shadow.calorietracker.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

data class CatalogueExportResult(
    val itemCount: Int,
    val imageCount: Int,
    val diagnosticCount: Int = 0,
    val freezeCount: Int = 0,
    val crashCount: Int = 0,
    val hasFeedback: Boolean = false,
)

class CatalogueExporter(
    private val context: Context,
    private val database: AppDatabase,
    private val supportDiagnostics: SupportDiagnosticStore,
) {
    suspend fun exportTo(
        destination: Uri,
        selectedFoodIds: Set<String>? = null,
        selectedRecipeIds: Set<String>? = null,
        includeDiagnostics: Boolean = true,
        includeFreezeReports: Boolean = true,
        includeCrashReports: Boolean = true,
        includeFeedback: Boolean = true,
        feedback: String = "",
    ): CatalogueExportResult {
        val foods = selectedFoodIds?.let { ids ->
            if (ids.isEmpty()) emptyList() else database.foodDao().listPersonalFoodsByIds(ids.toList())
        } ?: database.foodDao().listReviewableFoods()
        val recipes = selectedRecipeIds?.let { ids ->
            if (ids.isEmpty()) emptyList() else database.recipeDao().listRecipesByIds(ids.toList())
        } ?: database.recipeDao().listReviewableRecipes()
        val batches = database.recipeDao().listAllBatches().groupBy(RecipeBatchEntity::recipeFoodId)
        val ingredients = database.recipeDao().listAllIngredients().groupBy(RecipeIngredientEntity::batchId)
        val ingredientAllergens = database.recipeDao().listAllIngredientAllergens()
            .groupBy(RecipeIngredientAllergenEntity::recipeIngredientId)
        val imageEntries = linkedMapOf<String, File>()

        val foodJson = JSONArray().apply {
            foods.forEach { row ->
                val food = row.food
                val imageEntry = food.imageLocalPath?.let { path ->
                    File(path).takeIf(File::isFile)?.let { file ->
                        "images/${food.id.safeFileName()}.${file.extension.ifBlank { "jpg" }}".also { imageEntries[it] = file }
                    }
                }
                put(
                    JSONObject()
                        .put("id", food.id)
                        .put("name", JSONObject().put("en", food.nameEn).put("ro", food.nameRo))
                        .put("brand", food.brand ?: JSONObject.NULL)
                        .put("barcode", food.barcode ?: JSONObject.NULL)
                        .put("sourceType", food.sourceType)
                        .put("sourceId", food.sourceId ?: JSONObject.NULL)
                        .put("isPackaged", food.isPackaged)
                        .put("category", food.categoryKey ?: JSONObject.NULL)
                        .put("aliases", JSONArray(row.aliases.map(FoodAliasEntity::value)))
                        .put("reviewStatus", food.reviewStatus)
                        .put("image", imageJson(food, imageEntry))
                        .put("nutritionPer100g", nutritionJson(row))
                        .put("allergens", JSONArray().apply {
                            row.allergenDeclarations.forEach { declaration ->
                                put(JSONObject().put("key", declaration.allergenKey).put("declaration", declaration.declaration))
                            }
                        })
                        .put("measures", JSONArray().apply {
                            row.servings.forEach { serving ->
                                put(
                                    JSONObject()
                                        .put("id", serving.serving.id)
                                        .put("label", JSONObject().put("en", serving.serving.labelEn).put("ro", serving.serving.labelRo))
                                        .put("grams", serving.serving.grams)
                                        .put("isPackage", serving.serving.isPackage)
                                        .put("suggestedAmounts", JSONArray(serving.presets.map { it.amountMilliUnits / 1_000.0 })),
                                )
                            }
                        })
                        .put("preparations", JSONArray().apply {
                            row.preparations.sortedBy(FoodPreparationEntity::sortOrder).forEach { preparation ->
                                put(
                                    JSONObject()
                                        .put("id", preparation.id)
                                        .put("name", JSONObject().put("en", preparation.nameEn).put("ro", preparation.nameRo))
                                        .put("sortOrder", preparation.sortOrder)
                                        .put("nutritionPer100g", JSONObject()
                                            .put("calories", preparation.caloriesPer100g)
                                            .put("proteinGrams", preparation.proteinMilligramsPer100g / 1_000.0)
                                            .put("carbsGrams", preparation.carbsMilligramsPer100g / 1_000.0)
                                            .put("fatGrams", preparation.fatMilligramsPer100g / 1_000.0)
                                            .put("fiberGrams", preparation.fiberMilligramsPer100g?.div(1_000.0) ?: JSONObject.NULL)),
                                )
                            }
                        }),
                )
            }
        }

        val recipeJson = JSONArray().apply {
            recipes.forEach { recipe ->
                put(
                    JSONObject()
                        .put("foodId", recipe.foodId)
                        .put("name", recipe.name)
                        .put("activeBatchId", recipe.activeBatchId)
                        .put("reviewStatus", recipe.reviewStatus)
                        .put("batches", JSONArray().apply {
                            batches[recipe.foodId].orEmpty().forEach { batch ->
                                put(
                                    JSONObject()
                                        .put("id", batch.id)
                                        .put("cookedYieldGrams", batch.cookedYieldGrams)
                                        .put("portionCount", batch.portionCount)
                                        .put("cookedAtEpochMillis", batch.cookedAtEpochMillis)
                                        .put("ingredients", JSONArray().apply {
                                            ingredients[batch.id].orEmpty().sortedBy(RecipeIngredientEntity::sortOrder).forEach { ingredient ->
                                                put(
                                                    JSONObject()
                                                        .put("foodId", ingredient.foodId)
                                                        .put("name", JSONObject().put("en", ingredient.foodNameEn).put("ro", ingredient.foodNameRo))
                                                        .put("grams", ingredient.grams)
                                                        .put("nutritionPer100g", JSONObject()
                                                            .put("calories", ingredient.caloriesPer100g)
                                                            .put("proteinGrams", ingredient.proteinMilligramsPer100g / 1_000.0)
                                                            .put("carbsGrams", ingredient.carbsMilligramsPer100g / 1_000.0)
                                                            .put("fatGrams", ingredient.fatMilligramsPer100g / 1_000.0)
                                                            .put("fiberGrams", ingredient.fiberMilligramsPer100g?.div(1_000.0) ?: JSONObject.NULL))
                                                        .put("allergens", JSONArray().apply {
                                                            ingredientAllergens[ingredient.id].orEmpty().forEach { allergen ->
                                                                put(JSONObject().put("key", allergen.allergenKey).put("declaration", allergen.declaration))
                                                            }
                                                        }),
                                                )
                                            }
                                        }),
                                )
                            }
                        }),
                )
            }
        }

        val manifest = JSONObject()
            .put("schema", "calorie-quick-personal-catalogue")
            .put("schemaVersion", 1)
            .put("generatedAtEpochMillis", System.currentTimeMillis())
            .put("foods", foodJson)
            .put("recipes", recipeJson)

        var supportResult = SupportExportResult(0, 0, 0, false)
        requireNotNull(context.contentResolver.openOutputStream(destination)).use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("catalogue/catalogue.json"))
                zip.write(manifest.toString(2).toByteArray())
                zip.closeEntry()
                imageEntries.forEach { (entryName, file) ->
                    zip.putNextEntry(ZipEntry("catalogue/$entryName"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                supportResult = supportDiagnostics.appendTo(
                    zip,
                    feedback = feedback,
                    includeDiagnostics = includeDiagnostics,
                    includeFreezeReports = includeFreezeReports,
                    includeCrashReports = includeCrashReports,
                    includeFeedback = includeFeedback,
                )
            }
        }
        val foodIds = foods.map { it.food.id }
        if (foodIds.isNotEmpty()) database.foodDao().markExported(foodIds)
        val recipeIds = recipes.map(RecipeEntity::foodId)
        if (recipeIds.isNotEmpty()) database.recipeDao().markExported(recipeIds)
        return CatalogueExportResult(
            itemCount = foodJson.length() + recipeJson.length(),
            imageCount = imageEntries.size,
            diagnosticCount = supportResult.diagnosticCount,
            freezeCount = supportResult.freezeCount,
            crashCount = supportResult.crashCount,
            hasFeedback = supportResult.hasFeedback,
        )
    }

    private fun nutritionJson(row: FoodWithServings): JSONObject {
        val nutrients = row.nutrientValues.associateBy(NutrientValueEntity::nutrientKey)
        return JSONObject()
            .put("calories", nutrients["calories"]?.amountMilliUnitsPer100g ?: row.food.caloriesPer100g)
            .put("proteinGrams", (nutrients["protein"]?.amountMilliUnitsPer100g ?: row.food.proteinMilligramsPer100g) / 1_000.0)
            .put("carbsGrams", (nutrients["carbs"]?.amountMilliUnitsPer100g ?: row.food.carbsMilligramsPer100g) / 1_000.0)
            .put("fatGrams", (nutrients["fat"]?.amountMilliUnitsPer100g ?: row.food.fatMilligramsPer100g) / 1_000.0)
            .put("fiberGrams", nutrients["fiber"]?.amountMilliUnitsPer100g?.div(1_000.0) ?: JSONObject.NULL)
    }

    private fun imageJson(food: FoodEntity, bundlePath: String?): Any = if (
        bundlePath == null && food.imageRemoteUrl == null
    ) {
        JSONObject.NULL
    } else {
        JSONObject()
            .put("bundlePath", bundlePath ?: JSONObject.NULL)
            .put("remoteUrl", food.imageRemoteUrl ?: JSONObject.NULL)
            .put("source", food.imageSource ?: JSONObject.NULL)
            .put("attribution", food.imageAttribution ?: JSONObject.NULL)
            .put("license", food.imageLicense ?: JSONObject.NULL)
    }
}

private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

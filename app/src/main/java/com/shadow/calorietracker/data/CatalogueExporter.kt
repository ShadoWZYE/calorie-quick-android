package com.shadow.calorietracker.data

import android.content.Context
import android.net.Uri
import com.shadow.calorietracker.BuildConfig
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
    val performanceCount: Int = 0,
    val openFoodFactsResponseCount: Int = 0,
    val productIssueCount: Int = 0,
    val hasFeedback: Boolean = false,
)

class CatalogueExporter(
    private val context: Context,
    private val database: AppDatabase,
    private val supportDiagnostics: SupportDiagnosticStore,
    private val openFoodFactsCache: OpenFoodFactsResponseCache,
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
        return ReviewBundleMediaSanitizer(context).use { mediaSanitizer ->
        val imageEntries = linkedMapOf<String, ReviewBundleMedia>()

        val foodJson = JSONArray().apply {
            foods.forEach { row ->
                val food = row.food
                val imageEntry = food.imageLocalPath?.let { path ->
                    File(path).takeIf(File::isFile)?.let { file ->
                        mediaSanitizer.sanitize(
                            source = file,
                            bundlePathWithoutExtension = "catalogue/images/${food.id.safeFileName()}",
                            role = "catalogue-food",
                            ownerId = food.id,
                        )?.also { imageEntries[food.id] = it }?.bundlePath
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
                        .put("reviewStatusAtExport", food.reviewStatus)
                        .put("selectedForThisExport", true)
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
                        .put("reviewStatusAtExport", recipe.reviewStatus)
                        .put("selectedForThisExport", true)
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

        val generatedAt = System.currentTimeMillis()
        val cachedOpenFoodFactsResponses = openFoodFactsCache.entries()
        val manifest = JSONObject()
            .put("schema", "calorie-quick-personal-catalogue")
            .put("schemaVersion", 2)
            .put("generatedAtEpochMillis", generatedAt)
            .put("foods", foodJson)
            .put("recipes", recipeJson)

        var supportResult = SupportExportResult(0, 0, 0, false)
        requireNotNull(context.contentResolver.openOutputStream(destination)).use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("catalogue/catalogue.json"))
                zip.write(manifest.toString(2).toByteArray())
                zip.closeEntry()
                imageEntries.values.forEach { media ->
                    zip.putNextEntry(ZipEntry(media.bundlePath))
                    media.file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                val openFoodFactsIndex = JSONObject()
                    .put("schema", "calorie-quick-open-food-facts-cache")
                    .put("schemaVersion", 1)
                    .put("generatedAtEpochMillis", generatedAt)
                    .put("responseCount", cachedOpenFoodFactsResponses.size)
                    .put("responses", JSONArray(cachedOpenFoodFactsResponses.map { "open-food-facts/responses/${it.name}" }))
                zip.putNextEntry(ZipEntry("open-food-facts/cache.json"))
                zip.write(openFoodFactsIndex.toString(2).toByteArray())
                zip.closeEntry()
                cachedOpenFoodFactsResponses.forEach { file ->
                    zip.putNextEntry(ZipEntry("open-food-facts/responses/${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                supportResult = supportDiagnostics.appendTo(
                    zip,
                    mediaSanitizer = mediaSanitizer,
                    feedback = feedback,
                    includeDiagnostics = includeDiagnostics,
                    includeFreezeReports = includeFreezeReports,
                    includeCrashReports = includeCrashReports,
                    includeFeedback = includeFeedback,
                )
                val allMedia = imageEntries.values + supportResult.media
                val bundleManifest = JSONObject()
                    .put("schema", "calorie-quick-review-bundle")
                    .put("schemaVersion", 2)
                    .put("generatedAtEpochMillis", generatedAt)
                    .put("app", JSONObject()
                        .put("versionName", BuildConfig.VERSION_NAME)
                        .put("versionCode", BuildConfig.VERSION_CODE)
                        .put("buildId", BuildConfig.BUILD_ID)
                        .put("sourceCommit", BuildConfig.SOURCE_COMMIT)
                        .put("sourceDirty", BuildConfig.SOURCE_DIRTY))
                    .put("selection", JSONObject()
                        .put("foodCount", foodJson.length())
                        .put("recipeCount", recipeJson.length())
                        .put("openFoodFactsResponseCount", cachedOpenFoodFactsResponses.size)
                        .put("includeDiagnostics", includeDiagnostics)
                        .put("includeFreezeReports", includeFreezeReports)
                        .put("includeCrashReports", includeCrashReports)
                        .put("includeFeedback", includeFeedback))
                    .put("components", JSONArray()
                        .put(JSONObject()
                            .put("kind", "catalogue")
                            .put("path", "catalogue/catalogue.json")
                            .put("schemaVersion", 2)
                            .put("recordCount", foodJson.length() + recipeJson.length()))
                        .put(JSONObject()
                            .put("kind", "support")
                            .put("path", "support/support.json")
                            .put("schemaVersion", 8)
                            .put("recordCount", supportResult.diagnosticCount + supportResult.freezeCount + supportResult.crashCount + supportResult.feedbackCount + supportResult.performanceCount + supportResult.productIssueCount))
                        .put(JSONObject()
                            .put("kind", "open-food-facts-cache")
                            .put("path", "open-food-facts/cache.json")
                            .put("schemaVersion", 1)
                            .put("recordCount", cachedOpenFoodFactsResponses.size)))
                    .put("media", JSONArray(allMedia.map(ReviewBundleMedia::toJson)))
                zip.putNextEntry(ZipEntry("bundle.json"))
                zip.write(bundleManifest.toString(2).toByteArray())
                zip.closeEntry()
            }
        }
        val foodIds = foods.map { it.food.id }
        if (foodIds.isNotEmpty()) database.foodDao().markExported(foodIds)
        val recipeIds = recipes.map(RecipeEntity::foodId)
        if (recipeIds.isNotEmpty()) database.recipeDao().markExported(recipeIds)
        CatalogueExportResult(
            itemCount = foodJson.length() + recipeJson.length(),
            imageCount = imageEntries.size,
            diagnosticCount = supportResult.diagnosticCount,
            freezeCount = supportResult.freezeCount,
            crashCount = supportResult.crashCount,
            performanceCount = supportResult.performanceCount,
            openFoodFactsResponseCount = cachedOpenFoodFactsResponses.size,
            productIssueCount = supportResult.productIssueCount,
            hasFeedback = supportResult.hasFeedback,
        )
        }
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

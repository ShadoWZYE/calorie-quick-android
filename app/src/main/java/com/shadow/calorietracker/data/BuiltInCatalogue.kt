package com.shadow.calorietracker.data

import android.content.Context
import androidx.room.withTransaction
import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import org.json.JSONObject
import kotlin.math.roundToInt

private val CATALOGUE_ASSETS = listOf(
    "catalogue/built_in_catalogue_v1.json",
    "catalogue/promoted_catalogue_v1.json",
)
private const val CATALOGUE_SCHEMA = "calorie-quick-built-in-catalogue"

data class BuiltInCatalogue(
    val version: Int,
    val sourceName: String,
    val sourceUrl: String,
    val license: String,
    val foods: List<BuiltInFoodRecord>,
    val recipes: List<BuiltInRecipeRecord>,
)

data class BuiltInRecipeRecord(
    val foodId: String,
    val name: String,
    val cookedYieldGrams: Int,
    val portionCount: Int,
    val ingredients: List<BuiltInRecipeIngredientRecord>,
)

data class BuiltInRecipeIngredientRecord(
    val id: String,
    val foodId: String,
    val names: LocalizedText,
    val preparationId: String?,
    val preparationNames: LocalizedText?,
    val grams: Int,
    val nutrition: Nutrition,
    val allergens: Map<Allergen, AllergenDeclaration>,
)

data class BuiltInFoodRecord(
    val id: String,
    val names: LocalizedText,
    val details: LocalizedText,
    val categoryKey: String,
    val sourceId: String,
    val brand: String?,
    val barcode: String?,
    val isPackaged: Boolean,
    val image: BuiltInImageRecord?,
    val aliases: List<String>,
    val nutrition: Nutrition,
    val servings: List<BuiltInServingRecord>,
    val allergens: Map<Allergen, AllergenDeclaration>,
    val preparations: List<BuiltInPreparationRecord>,
    val defaultPreparationId: String?,
)

data class BuiltInServingRecord(
    val id: String,
    val labels: LocalizedText,
    val grams: Double,
    val suggestedAmounts: List<Double>,
    val isPackage: Boolean,
)

data class BuiltInImageRecord(
    val remoteUrl: String?,
    val attribution: String?,
    val license: String?,
)

data class BuiltInPreparationRecord(
    val id: String,
    val names: LocalizedText,
    val nutrition: Nutrition,
)

object BuiltInCatalogueParser {
    fun parse(json: String): BuiltInCatalogue {
        val root = JSONObject(json)
        require(root.getString("schema") == CATALOGUE_SCHEMA) { "Unsupported catalogue schema" }
        val source = root.getJSONObject("source")
        val catalogue = BuiltInCatalogue(
            version = root.getInt("version"),
            sourceName = source.getString("name"),
            sourceUrl = source.getString("url"),
            license = source.getString("license"),
            foods = root.getJSONArray("foods").objects().map(::food),
            recipes = root.optJSONArray("recipes")?.objects()?.map(::recipe).orEmpty(),
        )
        validate(catalogue)
        return catalogue
    }

    private fun food(json: JSONObject) = BuiltInFoodRecord(
        id = json.getString("id"),
        names = json.getJSONObject("name").localizedText(),
        details = json.getJSONObject("detail").localizedText(),
        categoryKey = json.getString("category"),
        sourceId = json.getString("sourceId"),
        brand = json.optString("brand").takeIf(String::isNotBlank),
        barcode = json.optString("barcode").takeIf(String::isNotBlank),
        isPackaged = json.optBoolean("isPackaged"),
        image = json.optJSONObject("image")?.let { image ->
            BuiltInImageRecord(
                remoteUrl = image.optString("remoteUrl").takeIf(String::isNotBlank),
                attribution = image.optString("attribution").takeIf(String::isNotBlank),
                license = image.optString("license").takeIf(String::isNotBlank),
            )
        },
        aliases = json.optJSONArray("aliases")?.strings().orEmpty(),
        nutrition = json.getJSONObject("nutritionPer100g").nutrition(),
        servings = json.optJSONArray("servings")?.objects()?.map { serving ->
            BuiltInServingRecord(
                id = serving.getString("id"),
                labels = serving.getJSONObject("label").localizedText(),
                grams = serving.getDouble("grams"),
                suggestedAmounts = serving.optJSONArray("suggestedAmounts")?.doubles().orEmpty(),
                isPackage = serving.optBoolean("isPackage"),
            )
        }.orEmpty() + json.getString("id").millilitreServing(),
        allergens = json.optJSONObject("allergens")?.let { declarations ->
            declarations.keys().asSequence().associate { key ->
                Allergen.valueOf(key) to AllergenDeclaration.valueOf(declarations.getString(key))
            }
        }.orEmpty(),
        preparations = json.optJSONArray("preparations")?.objects()?.map { preparation ->
            BuiltInPreparationRecord(
                id = preparation.getString("id"),
                names = preparation.getJSONObject("name").localizedText(),
                nutrition = preparation.getJSONObject("nutritionPer100g").nutrition(),
            )
        }.orEmpty(),
        defaultPreparationId = json.optString("defaultPreparationId").takeIf(String::isNotBlank),
    )

    private fun recipe(json: JSONObject) = BuiltInRecipeRecord(
        foodId = json.getString("foodId"),
        name = json.getString("name"),
        cookedYieldGrams = json.getInt("cookedYieldGrams"),
        portionCount = json.getInt("portionCount"),
        ingredients = json.getJSONArray("ingredients").objects().map { ingredient ->
            BuiltInRecipeIngredientRecord(
                id = ingredient.getString("id"),
                foodId = ingredient.getString("foodId"),
                names = ingredient.getJSONObject("name").localizedText(),
                preparationId = ingredient.optString("preparationId").takeIf(String::isNotBlank),
                preparationNames = ingredient.optJSONObject("preparationName")?.localizedText(),
                grams = ingredient.getInt("grams"),
                nutrition = ingredient.getJSONObject("nutritionPer100g").nutrition(),
                allergens = ingredient.optJSONObject("allergens")?.let { declarations ->
                    declarations.keys().asSequence().associate { key ->
                        Allergen.valueOf(key) to AllergenDeclaration.valueOf(declarations.getString(key))
                    }
                }.orEmpty(),
            )
        },
    )

    private fun validate(catalogue: BuiltInCatalogue) {
        require(catalogue.version > 0)
        require(catalogue.license in setOf("CC0-1.0", "MIXED-PER-RECORD"))
        require(catalogue.sourceName.isNotBlank() && catalogue.sourceUrl.startsWith("https://"))
        require(catalogue.foods.isNotEmpty())
        require(catalogue.foods.map(BuiltInFoodRecord::id).distinct().size == catalogue.foods.size)
        catalogue.foods.forEach { food ->
            require(food.id.matches(Regex("[a-z0-9-]+"))) { "Invalid food id: ${food.id}" }
            require(food.names.en.isNotBlank() && food.names.ro.isNotBlank())
            require(food.details.en.isNotBlank() && food.details.ro.isNotBlank())
            require(food.categoryKey.matches(Regex("[a-z-]+")))
            require(food.sourceId.isNotBlank())
            validateNutrition(food.id, food.nutrition)
            require(food.aliases.all(String::isNotBlank))
            require(food.servings.map(BuiltInServingRecord::id).distinct().size == food.servings.size)
            food.servings.forEach { serving ->
                require(serving.id.startsWith("${food.id}-"))
                require(serving.grams in 0.01..5_000.0)
                require(serving.labels.en.isNotBlank() && serving.labels.ro.isNotBlank())
                require(serving.suggestedAmounts.all { it > 0.0 && it <= 5_000.0 })
            }
            require(food.preparations.map(BuiltInPreparationRecord::id).distinct().size == food.preparations.size)
            food.preparations.forEach { preparation ->
                require(preparation.id.startsWith("${food.id}|"))
                validateNutrition(preparation.id, preparation.nutrition)
            }
            require(food.defaultPreparationId == null || food.preparations.any { it.id == food.defaultPreparationId })
        }
        require(catalogue.recipes.map(BuiltInRecipeRecord::foodId).distinct().size == catalogue.recipes.size)
        catalogue.recipes.forEach { recipe ->
            require(catalogue.foods.any { it.id == recipe.foodId })
            require(recipe.name.isNotBlank())
            require(recipe.cookedYieldGrams in 1..100_000)
            require(recipe.portionCount in 1..1_000)
            require(recipe.ingredients.isNotEmpty())
            recipe.ingredients.forEach { ingredient ->
                require(ingredient.id.startsWith("${recipe.foodId}-ingredient-"))
                require(ingredient.foodId.isNotBlank())
                require(ingredient.names.en.isNotBlank() && ingredient.names.ro.isNotBlank())
                require(ingredient.grams in 1..50_000)
                validateNutrition(ingredient.id, ingredient.nutrition)
            }
        }
    }

    private fun validateNutrition(id: String, nutrition: Nutrition) {
        require(nutrition.calories in 0..1_000) { "Invalid calories for $id" }
        listOf(nutrition.proteinGrams, nutrition.carbsGrams, nutrition.fatGrams).forEach {
            require(it in 0.0..100.0) { "Invalid macro for $id" }
        }
        require(nutrition.fiberGrams == null || nutrition.fiberGrams in 0.0..100.0)
    }
}

private fun String.millilitreServing(): List<BuiltInServingRecord> {
    val densityGramsPerMl = LIQUID_DENSITIES_GRAMS_PER_ML[this] ?: return emptyList()
    return listOf(
        BuiltInServingRecord(
            id = "$this-millilitre",
            labels = LocalizedText("ml", "ml"),
            grams = densityGramsPerMl,
            suggestedAmounts = listOf(100.0, 200.0, 250.0),
            isPackage = false,
        ),
    )
}

private val LIQUID_DENSITIES_GRAMS_PER_ML = mapOf(
    "water" to 1.0,
    "whole-milk" to 1.03,
    "semi-skimmed-milk" to 1.03,
    "skim-milk" to 1.03,
    "black-coffee" to 1.0,
    "tea" to 1.0,
    "orange-juice" to 1.04,
    "apple-juice" to 1.04,
    "cola" to 1.04,
    "beer" to 1.01,
    "red-wine" to 0.99,
    "olive-oil" to 0.92,
    "sunflower-oil" to 0.92,
    "coconut-milk" to 1.01,
)

class BuiltInCatalogueImporter(
    private val context: Context,
    private val database: AppDatabase,
) {
    suspend fun import(): Int {
        val availableAssets = context.assets.list("catalogue").orEmpty().toSet()
        val catalogues = CATALOGUE_ASSETS
            .filter { it.substringAfterLast('/') in availableAssets }
            .map { asset ->
                context.assets.open(asset).bufferedReader().use { reader ->
                    BuiltInCatalogueParser.parse(reader.readText())
                }
            }
        require(catalogues.isNotEmpty()) { "No packaged catalogue assets found" }
        require(catalogues.flatMap(BuiltInCatalogue::foods).map(BuiltInFoodRecord::id).let { it.distinct().size == it.size }) {
            "Duplicate food id across packaged catalogues"
        }
        val recipeIds = catalogues.flatMap(BuiltInCatalogue::recipes).map(BuiltInRecipeRecord::foodId).toSet()
        database.withTransaction {
            catalogues.forEach { catalogue -> catalogue.foods.forEach { record ->
                if (database.foodDao().findFood(record.id)?.isPersonal == true) return@forEach
                database.foodDao().upsertFood(record.toFoodEntity(catalogue.version, record.id in recipeIds))
                database.foodDao().deleteNutrients(record.id)
                database.foodDao().upsertNutrients(record.nutrition.toNutrients(record.id))
                database.foodDao().deleteAllergens(record.id)
                database.foodDao().upsertAllergens(record.toAllergens())
                database.foodDao().deleteServingPresets(record.id)
                database.foodDao().deleteServings(record.id)
                database.foodDao().upsertServings(record.toServings())
                database.foodDao().upsertServingPresets(record.toServingPresets())
                database.foodDao().deletePreparations(record.id)
                database.foodDao().upsertPreparations(record.toPreparations())
                database.foodDao().deleteAliases(record.id)
                database.foodDao().upsertAliases(record.toAliases())
            } }
            catalogues.flatMap(BuiltInCatalogue::recipes).forEach { recipe ->
                if (database.recipeDao().findRecipe(recipe.foodId) != null) return@forEach
                val batchId = "recipe-template|${recipe.foodId}"
                database.recipeDao().upsertBatch(
                    RecipeBatchEntity(
                        id = batchId,
                        recipeFoodId = recipe.foodId,
                        cookedYieldGrams = recipe.cookedYieldGrams,
                        remainingGrams = 0,
                        portionCount = recipe.portionCount,
                        cookedAtEpochMillis = 0,
                    ),
                )
                val ingredients = recipe.ingredients.mapIndexed { index, ingredient ->
                    RecipeIngredientEntity(
                        id = "$batchId|$index",
                        batchId = batchId,
                        foodId = ingredient.foodId,
                        foodNameEn = ingredient.names.en,
                        foodNameRo = ingredient.names.ro,
                        grams = ingredient.grams,
                        caloriesPer100g = ingredient.nutrition.calories,
                        proteinMilligramsPer100g = (ingredient.nutrition.proteinGrams * 1_000).roundToInt(),
                        carbsMilligramsPer100g = (ingredient.nutrition.carbsGrams * 1_000).roundToInt(),
                        fatMilligramsPer100g = (ingredient.nutrition.fatGrams * 1_000).roundToInt(),
                        fiberMilligramsPer100g = ingredient.nutrition.fiberGrams?.let { (it * 1_000).roundToInt() },
                        sortOrder = index,
                        preparationId = ingredient.preparationId,
                        preparationNameEn = ingredient.preparationNames?.en,
                        preparationNameRo = ingredient.preparationNames?.ro,
                    )
                }
                database.recipeDao().insertIngredients(ingredients)
                database.recipeDao().insertIngredientAllergens(
                    recipe.ingredients.zip(ingredients).flatMap { (record, entity) ->
                        record.allergens.map { (allergen, declaration) ->
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
                        foodId = recipe.foodId,
                        name = recipe.name,
                        activeBatchId = batchId,
                        createdAtEpochMillis = 0,
                        updatedAtEpochMillis = 0,
                        reviewStatus = "PRIVATE",
                    ),
                )
            }
        }
        return catalogues.sumOf { it.foods.size }
    }
}

private fun BuiltInFoodRecord.toFoodEntity(version: Int, isRecipe: Boolean) = FoodEntity(
    id = id,
    nameEn = names.en,
    nameRo = names.ro,
    detailEn = details.en,
    detailRo = details.ro,
    caloriesPer100g = nutrition.calories,
    proteinMilligramsPer100g = (nutrition.proteinGrams * 1_000).roundToInt(),
    carbsMilligramsPer100g = (nutrition.carbsGrams * 1_000).roundToInt(),
    fatMilligramsPer100g = (nutrition.fatGrams * 1_000).roundToInt(),
    updatedAtEpochMillis = version.toLong(),
    sourceType = if (isRecipe) "RECIPE" else "BUILT_IN",
    sourceId = sourceId,
    brand = brand,
    barcode = barcode,
    isPackaged = isPackaged,
    imageRemoteUrl = image?.remoteUrl,
    imageSource = image?.remoteUrl?.let { "REMOTE" },
    imageAttribution = image?.attribution,
    imageLicense = image?.license,
    defaultPreparationId = defaultPreparationId,
    categoryKey = categoryKey,
)

private fun Nutrition.toNutrients(foodId: String) = buildList {
    add(NutrientValueEntity("$foodId|calories", foodId, "calories", calories, "usda_fdc_cc0"))
    add(NutrientValueEntity("$foodId|protein", foodId, "protein", (proteinGrams * 1_000).roundToInt(), "usda_fdc_cc0"))
    add(NutrientValueEntity("$foodId|carbs", foodId, "carbs", (carbsGrams * 1_000).roundToInt(), "usda_fdc_cc0"))
    add(NutrientValueEntity("$foodId|fat", foodId, "fat", (fatGrams * 1_000).roundToInt(), "usda_fdc_cc0"))
    fiberGrams?.let {
        add(NutrientValueEntity("$foodId|fiber", foodId, "fiber", (it * 1_000).roundToInt(), "usda_fdc_cc0"))
    }
}

private fun BuiltInFoodRecord.toServings() = servings.map {
    ServingEntity(it.id, id, it.labels.en, it.labels.ro, it.grams, it.isPackage)
}

private fun BuiltInFoodRecord.toServingPresets() = servings.flatMap { serving ->
    serving.suggestedAmounts.map { amount ->
        val milliUnits = (amount * 1_000).roundToInt().toLong()
        ServingPresetEntity("${serving.id}|$milliUnits", serving.id, milliUnits)
    }
}

private fun BuiltInFoodRecord.toAllergens() = allergens.map { (allergen, declaration) ->
    AllergenDeclarationEntity("$id|${allergen.name}", id, allergen.name, declaration.name)
}

private fun BuiltInFoodRecord.toPreparations() = preparations.mapIndexed { index, preparation ->
    FoodPreparationEntity(
        id = preparation.id,
        foodId = id,
        nameEn = preparation.names.en,
        nameRo = preparation.names.ro,
        caloriesPer100g = preparation.nutrition.calories,
        proteinMilligramsPer100g = (preparation.nutrition.proteinGrams * 1_000).roundToInt(),
        carbsMilligramsPer100g = (preparation.nutrition.carbsGrams * 1_000).roundToInt(),
        fatMilligramsPer100g = (preparation.nutrition.fatGrams * 1_000).roundToInt(),
        fiberMilligramsPer100g = preparation.nutrition.fiberGrams?.let { (it * 1_000).roundToInt() },
        sortOrder = index,
    )
}

private fun BuiltInFoodRecord.toAliases() = aliases.distinct().mapIndexed { index, alias ->
    FoodAliasEntity("$id|$index", id, alias)
}

private fun JSONObject.localizedText() = LocalizedText(getString("en"), getString("ro"))

private fun JSONObject.nutrition() = Nutrition(
    calories = getInt("calories"),
    proteinGrams = getDouble("proteinGrams"),
    carbsGrams = getDouble("carbsGrams"),
    fatGrams = getDouble("fatGrams"),
    fiberGrams = if (isNull("fiberGrams")) null else optDouble("fiberGrams"),
)

private fun org.json.JSONArray.objects() = (0 until length()).map(::getJSONObject)
private fun org.json.JSONArray.strings() = (0 until length()).map(::getString)
private fun org.json.JSONArray.doubles() = (0 until length()).map(::getDouble)

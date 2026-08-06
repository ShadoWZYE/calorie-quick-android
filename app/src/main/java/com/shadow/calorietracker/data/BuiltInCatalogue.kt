package com.shadow.calorietracker.data

import android.content.Context
import androidx.room.withTransaction
import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import org.json.JSONObject
import kotlin.math.roundToInt

private const val CATALOGUE_ASSET = "catalogue/built_in_catalogue_v1.json"
private const val CATALOGUE_SCHEMA = "calorie-quick-built-in-catalogue"

data class BuiltInCatalogue(
    val version: Int,
    val sourceName: String,
    val sourceUrl: String,
    val license: String,
    val foods: List<BuiltInFoodRecord>,
)

data class BuiltInFoodRecord(
    val id: String,
    val names: LocalizedText,
    val details: LocalizedText,
    val categoryKey: String,
    val sourceId: String,
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
    val grams: Int,
    val suggestedAmounts: List<Double>,
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
        aliases = json.optJSONArray("aliases")?.strings().orEmpty(),
        nutrition = json.getJSONObject("nutritionPer100g").nutrition(),
        servings = json.optJSONArray("servings")?.objects()?.map { serving ->
            BuiltInServingRecord(
                id = serving.getString("id"),
                labels = serving.getJSONObject("label").localizedText(),
                grams = serving.getInt("grams"),
                suggestedAmounts = serving.optJSONArray("suggestedAmounts")?.doubles().orEmpty(),
            )
        }.orEmpty(),
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

    private fun validate(catalogue: BuiltInCatalogue) {
        require(catalogue.version > 0)
        require(catalogue.license == "CC0-1.0")
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
                require(serving.grams in 1..5_000)
                require(serving.labels.en.isNotBlank() && serving.labels.ro.isNotBlank())
                require(serving.suggestedAmounts.all { it > 0.0 && it <= 100.0 })
            }
            require(food.preparations.map(BuiltInPreparationRecord::id).distinct().size == food.preparations.size)
            food.preparations.forEach { preparation ->
                require(preparation.id.startsWith("${food.id}|"))
                validateNutrition(preparation.id, preparation.nutrition)
            }
            require(food.defaultPreparationId == null || food.preparations.any { it.id == food.defaultPreparationId })
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

class BuiltInCatalogueImporter(
    private val context: Context,
    private val database: AppDatabase,
) {
    suspend fun import(): Int {
        val catalogue = context.assets.open(CATALOGUE_ASSET).bufferedReader().use { reader ->
            BuiltInCatalogueParser.parse(reader.readText())
        }
        database.withTransaction {
            catalogue.foods.forEach { record ->
                if (database.foodDao().findFood(record.id)?.isPersonal == true) return@forEach
                database.foodDao().upsertFood(record.toFoodEntity(catalogue.version))
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
            }
        }
        return catalogue.foods.size
    }
}

private fun BuiltInFoodRecord.toFoodEntity(version: Int) = FoodEntity(
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
    sourceType = "BUILT_IN",
    sourceId = sourceId,
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
    ServingEntity(it.id, id, it.labels.en, it.labels.ro, it.grams)
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

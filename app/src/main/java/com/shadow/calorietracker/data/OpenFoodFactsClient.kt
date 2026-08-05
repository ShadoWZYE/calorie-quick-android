package com.shadow.calorietracker.data

import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodProvenance
import com.shadow.calorietracker.model.FoodSourceType
import com.shadow.calorietracker.model.LocalizedText
import com.shadow.calorietracker.model.Nutrition
import com.shadow.calorietracker.model.Serving
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class OpenFoodFactsClient(
    private val connectionFactory: (String) -> HttpURLConnection = { url ->
        URI(url).toURL().openConnection() as HttpURLConnection
    },
) {
    suspend fun search(query: String, locale: String): List<Food> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val url = "$BASE_URL/cgi/search.pl?action=process&search_terms=$encodedQuery&json=1&page_size=20" +
            "&fields=$PRODUCT_FIELDS"
        val root = requireNotNull(requestJson(url, locale))
        root.optJSONArray("products").orEmptyObjects().mapNotNull(::parseProduct)
            .distinctBy { it.barcode ?: it.id }
    }

    suspend fun productByBarcode(barcode: String, locale: String): Food? = withContext(Dispatchers.IO) {
        val normalized = barcode.filter(Char::isDigit)
        require(normalized.length in 8..14) { "Invalid barcode" }
        val url = "$BASE_URL/api/v3/product/$normalized?fields=$PRODUCT_FIELDS&lc=$locale"
        val root = requestJson(url, locale, allowNotFound = true) ?: return@withContext null
        if (root.optString("status") == "failure") return@withContext null
        root.optJSONObject("product")?.let(::parseProduct)
    }

    private fun requestJson(url: String, locale: String, allowNotFound: Boolean = false): JSONObject? {
        val connection = connectionFactory(url)
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Language", locale)
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val status = connection.responseCode
            if (allowNotFound && status == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (status == 429) throw OpenFoodFactsException.RateLimited
            if (status !in 200..299) throw IOException("Open Food Facts returned HTTP $status")
            connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org"
        private const val USER_AGENT =
            "CalorieQuick/0.1.0 (https://github.com/ShadoWZYE/calorie-quick-android)"
        private const val PRODUCT_FIELDS =
            "code,product_name,product_name_en,product_name_ro,brands,nutriments," +
                "allergens_tags,traces_tags,quantity,product_quantity,product_quantity_unit," +
                "serving_size,serving_quantity,serving_quantity_unit,nutrition_data_per"

        fun parseProduct(product: JSONObject): Food? {
            val code = product.optString("code").trim().takeIf(String::isNotEmpty) ?: return null
            val fallbackName = product.optString("product_name").trim()
            val nameEn = product.optString("product_name_en").trim().ifEmpty { fallbackName }
            val nameRo = product.optString("product_name_ro").trim().ifEmpty { fallbackName.ifEmpty { nameEn } }
            if (nameEn.isEmpty() && nameRo.isEmpty()) return null

            val nutriments = product.optJSONObject("nutriments") ?: return null
            val calories = nutriments.finiteDouble("energy-kcal_100g")
                ?: nutriments.finiteDouble("energy-kj_100g")?.div(4.184)
                ?: return null
            val protein = nutriments.finiteDouble("proteins_100g") ?: return null
            val carbs = nutriments.finiteDouble("carbohydrates_100g") ?: return null
            val fat = nutriments.finiteDouble("fat_100g") ?: return null
            if (listOf(calories, protein, carbs, fat).any { it < 0.0 }) return null

            val brand = product.optString("brands").trim().takeIf(String::isNotEmpty)
            val packageGrams = packageGrams(product)
            val servingGrams = gramsFromQuantity(
                product.optDoubleOrNull("serving_quantity"),
                product.optString("serving_quantity_unit"),
            ) ?: parseGrams(product.optString("serving_size"))
            val servings = buildList {
                packageGrams?.let {
                    add(
                        Serving(
                            id = "off-$code-package",
                            label = LocalizedText("1 package", "1 ambalaj"),
                            grams = it,
                            suggestedAmounts = listOf(0.5, 0.25),
                            isPackage = true,
                        ),
                    )
                }
                servingGrams?.takeIf { it != packageGrams }?.let {
                    add(Serving("off-$code-serving", LocalizedText("1 serving", "1 porție"), it))
                }
            }

            return Food(
                id = "off-$code",
                names = LocalizedText(nameEn.ifEmpty { nameRo }, nameRo.ifEmpty { nameEn }),
                details = LocalizedText(brand ?: "Open Food Facts", brand ?: "Open Food Facts"),
                nutritionPer100g = Nutrition(
                    calories = calories.roundToInt().coerceAtLeast(0),
                    proteinGrams = protein,
                    carbsGrams = carbs,
                    fatGrams = fat,
                    fiberGrams = nutriments.finiteDouble("fiber_100g")?.takeIf { it >= 0.0 },
                ),
                servings = servings,
                brand = brand,
                barcode = code,
                isPackaged = packageGrams != null,
                allergens = allergenDeclarations(product),
                provenance = FoodProvenance(FoodSourceType.OPEN_FOOD_FACTS, sourceId = code),
            )
        }

        private fun packageGrams(product: JSONObject): Int? = gramsFromQuantity(
            product.optDoubleOrNull("product_quantity"),
            product.optString("product_quantity_unit"),
        ) ?: parseGrams(product.optString("quantity"))

        private fun gramsFromQuantity(value: Double?, unit: String): Int? {
            if (value == null || value <= 0.0) return null
            val grams = when (unit.trim().lowercase()) {
                "g", "gram", "grams" -> value
                "kg", "kilogram", "kilograms" -> value * 1_000.0
                else -> return null
            }
            return grams.roundToInt().takeIf { it in 1..50_000 }
        }

        private fun parseGrams(text: String): Int? {
            val match = GRAM_QUANTITY.find(text.lowercase()) ?: return null
            val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            return gramsFromQuantity(value, match.groupValues[2])
        }

        private fun allergenDeclarations(product: JSONObject): Map<Allergen, AllergenDeclaration> {
            val declarations = mutableMapOf<Allergen, AllergenDeclaration>()
            product.optJSONArray("traces_tags").tagAllergens().forEach {
                declarations[it] = AllergenDeclaration.MAY_CONTAIN
            }
            product.optJSONArray("allergens_tags").tagAllergens().forEach {
                declarations[it] = AllergenDeclaration.CONTAINS
            }
            return declarations
        }

        private val GRAM_QUANTITY = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(kg|g)\b""")
    }
}

sealed class OpenFoodFactsException(message: String) : IOException(message) {
    data object RateLimited : OpenFoodFactsException("Open Food Facts rate limit reached")
}

private fun JSONObject.finiteDouble(key: String): Double? = optDoubleOrNull(key)?.takeIf(Double::isFinite)

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key).takeUnless(Double::isNaN)

private fun JSONArray?.orEmptyObjects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull(::optJSONObject)

private fun JSONArray?.tagAllergens(): Set<Allergen> = orEmptyObjectsStrings().mapNotNull { tag ->
    when (tag.substringAfter(':').lowercase()) {
        "gluten", "cereals-containing-gluten", "wheat", "rye", "barley", "oats", "spelt" -> Allergen.GLUTEN
        "crustaceans" -> Allergen.CRUSTACEANS
        "eggs" -> Allergen.EGGS
        "fish" -> Allergen.FISH
        "peanuts" -> Allergen.PEANUTS
        "soybeans", "soy" -> Allergen.SOY
        "milk" -> Allergen.MILK
        "nuts", "tree-nuts" -> Allergen.NUTS
        "celery" -> Allergen.CELERY
        "mustard" -> Allergen.MUSTARD
        "sesame-seeds", "sesame" -> Allergen.SESAME
        "sulphur-dioxide-and-sulphites", "sulfites", "sulphites" -> Allergen.SULPHITES
        "lupin" -> Allergen.LUPIN
        "molluscs" -> Allergen.MOLLUSCS
        else -> null
    }
}.toSet()

private fun JSONArray?.orEmptyObjectsStrings(): List<String> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { index -> optString(index).takeIf(String::isNotEmpty) }

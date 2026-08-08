package com.shadow.calorietracker.data

import com.shadow.calorietracker.BuildConfig
import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import com.shadow.calorietracker.model.CatalogueOrigin
import com.shadow.calorietracker.model.Food
import com.shadow.calorietracker.model.FoodImage
import com.shadow.calorietracker.model.FoodImageSource
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class OpenFoodFactsClient(
    private val cache: OpenFoodFactsResponseCache? = null,
    private val connectionFactory: (String) -> HttpURLConnection = { url ->
        URI(url).toURL().openConnection() as HttpURLConnection
    },
) {
    suspend fun search(query: String, locale: String, page: Int = 1): List<Food> =
        searchPage(query, locale, page).foods

    suspend fun searchPage(
        query: String,
        locale: String,
        page: Int = 1,
    ): OpenFoodFactsSearchPage = withContext(Dispatchers.IO) {
        val normalizedPage = page.coerceAtLeast(1)
        val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val encodedLocale = URLEncoder.encode(locale, StandardCharsets.UTF_8.toString())
        val url = "$BASE_URL/cgi/search.pl?action=process&search_terms=$encodedQuery&json=1" +
            "&page_size=$SEARCH_PAGE_SIZE&page=$normalizedPage&lc=$encodedLocale"
        val root = requireNotNull(requestJsonWithRetry(url, locale, requestKind = "search"))
        val rawProducts = root.optJSONArray("products").orEmptyObjects()
        val foods = rawProducts.mapNotNull(::parseProduct).distinctBy { it.barcode ?: it.id }
        val totalCount = root.optInt("count", -1)
        OpenFoodFactsSearchPage(
            foods = foods,
            hasMore = hasMoreSearchResults(normalizedPage, totalCount, rawProducts.size),
        )
    }

    suspend fun productByBarcode(barcode: String, locale: String): Food? = withContext(Dispatchers.IO) {
        val normalized = barcode.filter(Char::isDigit)
        require(normalized.length in 8..14) { "Invalid barcode" }
        val encodedLocale = URLEncoder.encode(locale, StandardCharsets.UTF_8.toString())
        val url = "$BASE_URL/api/v3/product/$normalized?lc=$encodedLocale"
        val root = requestJsonWithRetry(url, locale, allowNotFound = true, requestKind = "barcode")
            ?: return@withContext null
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
            if (status in listOf(502, 503, 504)) throw OpenFoodFactsException.ServiceUnavailable
            if (status !in 200..299) throw IOException("Open Food Facts returned HTTP $status")
            connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun requestJsonWithRetry(
        url: String,
        locale: String,
        allowNotFound: Boolean = false,
        requestKind: String,
    ): JSONObject? {
        cache?.readFresh(url, locale)?.let { return it }
        repeat(2) { attempt ->
            try {
                return requestJson(url, locale, allowNotFound)?.also {
                    cache?.write(url, locale, requestKind, it)
                }
            } catch (error: OpenFoodFactsException.ServiceUnavailable) {
                if (attempt == 1) return cache?.readAny(url, locale) ?: throw error
                delay(750)
            } catch (error: IOException) {
                return cache?.readAny(url, locale) ?: throw error
            }
        }
        error("unreachable")
    }

    companion object {
        const val SEARCH_PAGE_SIZE = 10
        fun hasMoreSearchResults(page: Int, totalCount: Int, rawProductCount: Int): Boolean =
            rawProductCount == SEARCH_PAGE_SIZE &&
                (totalCount < 0 || page.coerceAtLeast(1) * SEARCH_PAGE_SIZE < totalCount)

        fun hasMoreAfterMerge(serverHasMore: Boolean, page: Int, addedUniqueProductCount: Int): Boolean =
            serverHasMore && (page <= 1 || addedUniqueProductCount > 0)

        private const val BASE_URL = "https://world.openfoodfacts.org"
        private val USER_AGENT =
            "CalorieQuick/${BuildConfig.VERSION_NAME} (https://github.com/ShadoWZYE/calorie-quick-android)"
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
            val declaredQuantity = product.optString("quantity").trim().takeIf(String::isNotEmpty)
            val productDetail = listOfNotNull(brand, declaredQuantity)
                .distinctBy { it.lowercase() }
                .joinToString(" · ")
                .ifEmpty { "Open Food Facts" }
            val imageUrl = product.optString("image_front_small_url").trim().ifEmpty {
                product.optString("image_front_url").trim()
            }.takeIf { it.startsWith("https://") }
            val packageGrams = packageGrams(product)
            val liquidPackage = hasLiquidQuantity(product)
            val servingGrams = gramsFromQuantity(
                product.optDoubleOrNull("serving_quantity"),
                product.optString("serving_quantity_unit"),
            ) ?: parseGrams(product.optString("serving_size"))
            val servings = buildList {
                packageGrams?.let {
                    add(
                        Serving(
                            id = "off-$code-package",
                            label = if (liquidPackage) {
                                LocalizedText("1 bottle", "1 sticlă")
                            } else {
                                LocalizedText("1 package", "1 ambalaj")
                            },
                            grams = it,
                            suggestedAmounts = listOf(0.5, 0.25),
                            isPackage = true,
                        ),
                    )
                }
                servingGrams?.takeIf { it != packageGrams }?.let {
                    add(Serving("off-$code-serving", LocalizedText("1 serving", "1 porție"), it))
                }
                if (liquidPackage) {
                    add(
                        Serving(
                            id = "off-$code-millilitre",
                            label = LocalizedText("1 ml", "1 ml"),
                            grams = 1,
                            suggestedAmounts = listOf(100.0, 250.0),
                        ),
                    )
                }
            }

            return Food(
                id = "off-$code",
                names = LocalizedText(nameEn.ifEmpty { nameRo }, nameRo.ifEmpty { nameEn }),
                details = LocalizedText(productDetail, productDetail),
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
                provenance = FoodProvenance(
                    FoodSourceType.OPEN_FOOD_FACTS,
                    sourceId = code,
                    catalogueOrigin = CatalogueOrigin.OPEN_FOOD_FACTS,
                ),
                image = imageUrl?.let {
                    FoodImage(
                        source = FoodImageSource.REMOTE,
                        remoteUrl = it,
                        attribution = "Open Food Facts contributors",
                    )
                },
                categoryKey = categoryKey(product),
            )
        }

        private fun categoryKey(product: JSONObject): String? {
            val tags = product.optJSONArray("categories_tags").orEmptyObjectsAndStrings()
                .joinToString(" ") { it.lowercase() }
            val searchableText = listOf(
                tags,
                product.optString("product_name"),
                product.optString("product_name_en"),
                product.optString("generic_name"),
            ).joinToString(" ").lowercase()
            return when {
                listOf(
                    "beverages", "waters", "juices", "sodas", "coffees", "teas",
                    "cola", "pepsi", "schweppes", "soft drink", "energy drink",
                ).any(searchableText::contains) -> "beverages"
                listOf("dairies", "milks", "cheeses", "yogurts").any(tags::contains) -> "dairy"
                listOf("fruits", "fruit-based").any(tags::contains) -> "fruit"
                listOf("vegetables", "vegetable-based").any(tags::contains) -> "vegetables"
                listOf("meats", "poultries").any(tags::contains) -> "meat"
                listOf("seafood", "fishes").any(tags::contains) -> "fish"
                "eggs" in tags -> "eggs"
                listOf("breads", "pastries", "bakery").any(tags::contains) -> "bakery"
                listOf("cereals", "grains", "pastas", "rices").any(tags::contains) -> "grains"
                listOf("legumes", "beans", "lentils").any(tags::contains) -> "legumes"
                listOf("nuts", "nut-", "seeds").any(tags::contains) -> "nuts-seeds"
                listOf("oils", "fats", "butters").any(tags::contains) -> "fats"
                listOf("meals", "prepared-foods", "sandwiches", "pizzas").any(tags::contains) -> "prepared-meals"
                else -> null
            }
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
                "ml", "millilitre", "millilitres", "milliliter", "milliliters" -> value
                "cl", "centilitre", "centilitres", "centiliter", "centiliters" -> value * 10.0
                "l", "litre", "litres", "liter", "liters" -> value * 1_000.0
                else -> return null
            }
            return grams.roundToInt().takeIf { it in 1..50_000 }
        }

        private fun parseGrams(text: String): Int? {
            val match = QUANTITY.find(text.lowercase()) ?: return null
            val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            return gramsFromQuantity(value, match.groupValues[2])
        }

        private fun hasLiquidQuantity(product: JSONObject): Boolean {
            val unit = product.optString("product_quantity_unit").trim().lowercase()
            if (unit in LIQUID_UNITS) return true
            return QUANTITY.find(product.optString("quantity").lowercase())
                ?.groupValues?.getOrNull(2)?.lowercase() in LIQUID_UNITS
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

        private val QUANTITY = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(kg|g|ml|cl|l)\b""")
        private val LIQUID_UNITS = setOf(
            "ml", "cl", "l", "millilitre", "millilitres", "milliliter", "milliliters",
            "centilitre", "centilitres", "centiliter", "centiliters", "litre", "litres", "liter", "liters",
        )
    }
}

data class OpenFoodFactsSearchPage(
    val foods: List<Food>,
    val hasMore: Boolean,
)

private fun JSONArray?.orEmptyObjectsAndStrings(): List<String> = buildList {
    if (this@orEmptyObjectsAndStrings == null) return@buildList
    for (index in 0 until length()) optString(index).takeIf(String::isNotBlank)?.let(::add)
}

sealed class OpenFoodFactsException(message: String) : IOException(message) {
    data object RateLimited : OpenFoodFactsException("Open Food Facts rate limit reached")
    data object ServiceUnavailable : OpenFoodFactsException("Open Food Facts is temporarily unavailable")
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

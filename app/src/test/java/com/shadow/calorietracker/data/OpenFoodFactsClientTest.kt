package com.shadow.calorietracker.data

import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import com.shadow.calorietracker.model.FoodSourceType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsClientTest {
    @Test
    fun `pagination stops on short pages even when API total is stale`() {
        assertEquals(false, OpenFoodFactsClient.hasMoreSearchResults(page = 1, totalCount = 87, rawProductCount = 4))
        assertTrue(OpenFoodFactsClient.hasMoreSearchResults(page = 1, totalCount = 87, rawProductCount = 10))
        assertEquals(false, OpenFoodFactsClient.hasMoreSearchResults(page = 9, totalCount = 87, rawProductCount = 7))
        assertTrue(OpenFoodFactsClient.hasMoreSearchResults(page = 1, totalCount = -1, rawProductCount = 10))
    }

    @Test
    fun `pagination stops when another page contributes no new identities`() {
        assertEquals(false, OpenFoodFactsClient.hasMoreAfterMerge(true, page = 2, addedUniqueProductCount = 0))
        assertTrue(OpenFoodFactsClient.hasMoreAfterMerge(true, page = 2, addedUniqueProductCount = 1))
        assertTrue(OpenFoodFactsClient.hasMoreAfterMerge(true, page = 1, addedUniqueProductCount = 0))
    }

    @Test
    fun `complete product is normalized for editor review`() {
        val food = requireNotNull(
            OpenFoodFactsClient.parseProduct(
                JSONObject(
                    """
                    {
                      "code": "3017620422003",
                      "product_name": "Hazelnut spread",
                      "product_name_ro": "Cremă de alune",
                      "brands": "Example Brand",
                      "quantity": "400 g",
                      "serving_size": "15 g",
                      "nutriments": {
                        "energy-kcal_100g": 539,
                        "proteins_100g": 6.3,
                        "carbohydrates_100g": 57.5,
                        "fat_100g": 30.9,
                        "fiber_100g": 3.4
                      },
                      "allergens_tags": ["en:milk", "en:nuts"],
                      "traces_tags": ["en:soybeans", "en:milk"],
                      "categories_tags": ["en:spreads", "en:nut-butters"],
                      "image_front_small_url": "https://images.openfoodfacts.org/product.jpg"
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals("off-3017620422003", food.id)
        assertEquals("Cremă de alune", food.names.ro)
        assertEquals("Example Brand · 400 g", food.details.en)
        assertEquals(539, food.nutritionPer100g.calories)
        assertEquals(3.4, food.nutritionPer100g.fiberGrams!!, 0.001)
        assertTrue(food.isPackaged)
        assertEquals(400.0, food.servings.first { it.isPackage }.grams, 0.001)
        assertEquals(15.0, food.servings.first { !it.isPackage }.grams, 0.001)
        assertEquals(AllergenDeclaration.CONTAINS, food.allergens[Allergen.MILK])
        assertEquals(AllergenDeclaration.CONTAINS, food.allergens[Allergen.NUTS])
        assertEquals(AllergenDeclaration.MAY_CONTAIN, food.allergens[Allergen.SOY])
        assertEquals(FoodSourceType.OPEN_FOOD_FACTS, food.provenance.type)
        assertEquals("3017620422003", food.provenance.sourceId)
        assertEquals("https://images.openfoodfacts.org/product.jpg", food.image?.remoteUrl)
        assertEquals("nuts-seeds", food.categoryKey)
    }

    @Test
    fun `millilitre package gets practical bottle and volume measures`() {
        val food = requireNotNull(
            OpenFoodFactsClient.parseProduct(
                JSONObject(
                    """
                    {
                      "code": "12345678",
                      "product_name": "Schweppes 0.5",
                      "brands": "Schweppes",
                      "quantity": "330 ml",
                      "product_quantity": 330,
                      "product_quantity_unit": "ml",
                      "serving_quantity": 100,
                      "serving_quantity_unit": "ml",
                      "nutriments": {
                        "energy-kcal_100g": 42,
                        "proteins_100g": 0,
                        "carbohydrates_100g": 10,
                        "fat_100g": 0
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertTrue(food.isPackaged)
        assertEquals(330.0, food.servings.first { it.isPackage }.grams, 0.001)
        assertEquals("1 bottle", food.servings.first { it.isPackage }.label.en)
        assertEquals("Schweppes · 330 ml", food.details.en)
        assertEquals(1.0, food.servings.first { it.label.en == "1 ml" }.grams, 0.001)
        assertEquals(listOf(100.0, 250.0), food.servings.first { it.label.en == "1 ml" }.suggestedAmounts)
        assertEquals("beverages", food.categoryKey)
    }

    @Test
    fun `incomplete nutrition is excluded instead of inventing zeroes`() {
        val product = JSONObject(
            """
            {
              "code": "12345678",
              "product_name": "Incomplete product",
              "nutriments": {
                "energy-kcal_100g": 100,
                "proteins_100g": 2,
                "fat_100g": 3
              }
            }
            """.trimIndent(),
        )

        assertNull(OpenFoodFactsClient.parseProduct(product))
    }

    @Test
    fun `kilojoules are converted when kilocalories are absent`() {
        val product = JSONObject(
            """
            {
              "code": "12345678",
              "product_name": "Energy fallback",
              "nutriments": {
                "energy-kj_100g": 418.4,
                "proteins_100g": 2,
                "carbohydrates_100g": 10,
                "fat_100g": 3
              }
            }
            """.trimIndent(),
        )

        assertEquals(100, requireNotNull(OpenFoodFactsClient.parseProduct(product)).nutritionPer100g.calories)
    }
}

package com.shadow.calorietracker.data

import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsClientTest {
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
                      "traces_tags": ["en:soybeans", "en:milk"]
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals("off-3017620422003", food.id)
        assertEquals("Cremă de alune", food.names.ro)
        assertEquals(539, food.nutritionPer100g.calories)
        assertEquals(3.4, food.nutritionPer100g.fiberGrams!!, 0.001)
        assertTrue(food.isPackaged)
        assertEquals(400, food.servings.first { it.isPackage }.grams)
        assertEquals(15, food.servings.first { !it.isPackage }.grams)
        assertEquals(AllergenDeclaration.CONTAINS, food.allergens[Allergen.MILK])
        assertEquals(AllergenDeclaration.CONTAINS, food.allergens[Allergen.NUTS])
        assertEquals(AllergenDeclaration.MAY_CONTAIN, food.allergens[Allergen.SOY])
    }

    @Test
    fun `millilitre package is not silently converted to grams`() {
        val food = requireNotNull(
            OpenFoodFactsClient.parseProduct(
                JSONObject(
                    """
                    {
                      "code": "12345678",
                      "product_name": "Drink",
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

        assertFalse(food.isPackaged)
        assertTrue(food.servings.isEmpty())
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

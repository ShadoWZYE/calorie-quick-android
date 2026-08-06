package com.shadow.calorietracker.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInCatalogueParserTest {
    private val assetFile = File("src/main/assets/catalogue/built_in_catalogue_v1.json")

    @Test
    fun `bundled catalogue passes validation and maintains broad coverage`() {
        val catalogue = BuiltInCatalogueParser.parse(assetFile.readText())

        assertEquals(1, catalogue.version)
        assertEquals("CC0-1.0", catalogue.license)
        assertTrue(catalogue.foods.size >= 60)
        assertTrue(catalogue.foods.map { it.categoryKey }.distinct().size >= 10)
        assertTrue(catalogue.foods.any { it.id == "honey" && it.servings.any { serving -> serving.id == "honey-teaspoon" } })
        assertTrue(catalogue.foods.any { it.id == "eggplant" && "aubergine" in it.aliases })
        assertTrue(catalogue.foods.any { it.id == "eggs" && it.preparations.size >= 2 })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `catalogue rejects a missing default preparation`() {
        BuiltInCatalogueParser.parse(minimalCatalogue(defaultPreparationId = "test|missing"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `catalogue rejects nutrition outside physical bounds`() {
        BuiltInCatalogueParser.parse(minimalCatalogue(calories = 1_001))
    }

    private fun minimalCatalogue(calories: Int = 100, defaultPreparationId: String? = null): String {
        val defaultField = defaultPreparationId?.let { ", \"defaultPreparationId\": \"$it\"" }.orEmpty()
        return """
            {
              "schema": "calorie-quick-built-in-catalogue",
              "version": 1,
              "source": {"name": "Test", "url": "https://example.com", "license": "CC0-1.0"},
              "foods": [{
                "id": "test",
                "name": {"en": "Test", "ro": "Test"},
                "detail": {"en": "Generic", "ro": "Generic"},
                "category": "test-foods",
                "sourceId": "test:1",
                "aliases": [],
                "nutritionPer100g": {
                  "calories": $calories,
                  "proteinGrams": 1.0,
                  "carbsGrams": 2.0,
                  "fatGrams": 3.0,
                  "fiberGrams": 1.0
                },
                "servings": [],
                "allergens": {},
                "preparations": []
                $defaultField
              }]
            }
        """.trimIndent()
    }
}

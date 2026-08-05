package com.shadow.calorietracker.data

import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionLabelParserTest {
    @Test
    fun `english per 100 gram label prefills nutrition package barcode and allergens`() {
        val result = NutritionLabelParser.parse(
            """
            Nutrition information per 100 g
            Energy 2252 kJ 539 kcal
            Fat 30.9 g
            Carbohydrate 57.5 g
            Fiber 3.4 g
            Protein 6.3 g
            Net weight 400 g
            Contains milk and tree nuts
            May contain soy
            3017620422003
            """.trimIndent(),
            suggestedName = "Hazelnut spread",
        )

        assertEquals("Hazelnut spread", result.suggestedName)
        assertEquals("3017620422003", result.barcode)
        assertEquals(539, result.caloriesPer100g)
        assertEquals(30.9, result.fatPer100g!!, 0.001)
        assertEquals(57.5, result.carbsPer100g!!, 0.001)
        assertEquals(6.3, result.proteinPer100g!!, 0.001)
        assertEquals(3.4, result.fiberPer100g!!, 0.001)
        assertEquals(400, result.packageGrams)
        assertEquals(AllergenDeclaration.CONTAINS, result.allergens[Allergen.MILK])
        assertEquals(AllergenDeclaration.CONTAINS, result.allergens[Allergen.NUTS])
        assertEquals(AllergenDeclaration.MAY_CONTAIN, result.allergens[Allergen.SOY])
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `dual column table selects per 100 gram column`() {
        val result = NutritionLabelParser.parse(
            """
            Per 100 g   Per serving
            Energy 200 kcal 50 kcal
            Fat 10 g 2.5 g
            Carbohydrate 20 g 5 g
            Protein 8 g 2 g
            Fiber 4 g 1 g
            """.trimIndent(),
        )

        assertEquals(200, result.caloriesPer100g)
        assertEquals(10.0, result.fatPer100g!!, 0.001)
        assertEquals(20.0, result.carbsPer100g!!, 0.001)
        assertEquals(8.0, result.proteinPer100g!!, 0.001)
        assertEquals(4.0, result.fiberPer100g!!, 0.001)
    }

    @Test
    fun `serving values are normalized only when serving weight is present`() {
        val result = NutritionLabelParser.parse(
            """
            Nutrition per serving
            Serving size 25 g
            Energy 100 kcal
            Fat 2 g
            Carbohydrate 15 g
            Protein 5 g
            """.trimIndent(),
        )

        assertEquals(400, result.caloriesPer100g)
        assertEquals(8.0, result.fatPer100g!!, 0.001)
        assertEquals(60.0, result.carbsPer100g!!, 0.001)
        assertEquals(20.0, result.proteinPer100g!!, 0.001)
        assertTrue(NutritionLabelWarning.NORMALIZED_FROM_SERVING in result.warnings)
    }

    @Test
    fun `unknown basis leaves nutrition blank instead of guessing`() {
        val result = NutritionLabelParser.parse(
            """
            Energy 100 kcal
            Fat 2 g
            Carbohydrate 15 g
            Protein 5 g
            """.trimIndent(),
        )

        assertNull(result.caloriesPer100g)
        assertNull(result.fatPer100g)
        assertTrue(NutritionLabelWarning.BASIS_UNKNOWN in result.warnings)
        assertTrue(NutritionLabelWarning.CORE_VALUES_MISSING in result.warnings)
    }

    @Test
    fun `romanian terms and diacritics are recognized`() {
        val result = NutritionLabelParser.parse(
            """
            Valori nutriționale per 100 g
            Valoare energetică 150 kcal
            Grăsimi 4,5 g
            Carbohidrați 20 g
            Proteine 7 g
            Fibre 2,5 g
            Cantitate netă 250 g
            Alergeni: conține lapte și ouă
            Poate conține urme de arahide
            """.trimIndent(),
        )

        assertEquals(150, result.caloriesPer100g)
        assertEquals(4.5, result.fatPer100g!!, 0.001)
        assertEquals(20.0, result.carbsPer100g!!, 0.001)
        assertEquals(7.0, result.proteinPer100g!!, 0.001)
        assertEquals(2.5, result.fiberPer100g!!, 0.001)
        assertEquals(250, result.packageGrams)
        assertEquals(AllergenDeclaration.CONTAINS, result.allergens[Allergen.MILK])
        assertEquals(AllergenDeclaration.CONTAINS, result.allergens[Allergen.EGGS])
        assertEquals(AllergenDeclaration.MAY_CONTAIN, result.allergens[Allergen.PEANUTS])
    }
}

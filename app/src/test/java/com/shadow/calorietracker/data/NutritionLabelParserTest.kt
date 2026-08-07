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

    @Test
    fun `romanian liquid table selects per 100 ml column and package volume`() {
        val result = NutritionLabelParser.parse(
            """
            DECLARAȚIE NUTRIȚIONALĂ
            PENTRU: 100 ml 250 ml
            Valoare energetică: 179 kJ / 42 kcal 448 kJ / 105 kcal
            Grăsimi: 0 g 0 g
            din care acizi grași saturați: 0 g 0 g
            Glucide: 10,1 g 25 g
            din care zaharuri: 10,1 g 25 g
            Proteine: 0 g 0 g
            Sare: 0 g 0 g
            500 ml = 2 x 250 ml
            """.trimIndent(),
            suggestedName = "Schweppes Mandarin",
        )

        assertEquals(42, result.caloriesPer100g)
        assertEquals(0.0, result.fatPer100g!!, 0.001)
        assertEquals(10.1, result.carbsPer100g!!, 0.001)
        assertEquals(0.0, result.proteinPer100g!!, 0.001)
        assertNull(result.fiberPer100g)
        assertEquals(500, result.packageGrams)
        assertEquals(NutritionBasisUnit.MILLILITERS, result.basisUnit)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `split OCR row lines stay within their nutrient boundaries`() {
        val result = NutritionLabelParser.parse(
            """
            Pentru 100 ml 250 ml
            Valoare energetică
            42 kcal
            105 kcal
            Grăsimi
            0 g
            0 g
            din care acizi grași saturați
            3 g
            8 g
            Glucide
            10,1 g
            25 g
            din care zaharuri
            10,1 g
            25 g
            Proteine
            0 g
            0 g
            """.trimIndent(),
        )

        assertEquals(42, result.caloriesPer100g)
        assertEquals(0.0, result.fatPer100g!!, 0.001)
        assertEquals(10.1, result.carbsPer100g!!, 0.001)
        assertEquals(0.0, result.proteinPer100g!!, 0.001)
    }

    @Test
    fun `curved label zeroes misread as letters are normalized`() {
        val result = NutritionLabelParser.parse(
            """
            Pentru 100 ml 250 ml
            Valoare energetica 42 kcal 105 kcal
            Grasimi 0g Dg
            Glucide 10,1 g 25 g
            Proteine Og Dg
            """.trimIndent(),
        )

        assertEquals(0.0, result.fatPer100g!!, 0.001)
        assertEquals(0.0, result.proteinPer100g!!, 0.001)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `spatial fallback reconstructs per 100 ml values from curved two column label`() {
        val spatial = buildSpatialNutritionRows(
            listOf(
                PositionedOcrLine("100 ml", 566, 649, 646, 681),
                PositionedOcrLine("250 ml %", 704, 641, 856, 681),
                PositionedOcrLine("toticenergetica:", 254, 743, 478, 778),
                PositionedOcrLine("42 kcal105 kcal (56)", 571, 724, 866, 770),
                PositionedOcrLine("siGrasimi:", 308, 779, 448, 813),
                PositionedOcrLine("0g", 616, 778, 650, 815),
                PositionedOcrLine("nete si Glucide:", 291, 885, 451, 936),
                PositionedOcrLine("10,19", 580, 897, 654, 936),
                PositionedOcrLine("icaProteine:", 320, 960, 463, 1017),
                PositionedOcrLine("0g", 622, 981, 655, 1016),
                PositionedOcrLine("25g [10%", 739, 883, 866, 932),
            ),
        )
        val result = NutritionLabelParser.parse(spatial.orEmpty())

        assertEquals(42, result.caloriesPer100g)
        assertEquals(0.0, result.fatPer100g!!, 0.001)
        assertEquals(10.1, result.carbsPer100g!!, 0.001)
        assertEquals(0.0, result.proteinPer100g!!, 0.001)
        assertEquals(NutritionBasisUnit.MILLILITERS, result.basisUnit)
        assertTrue(result.warnings.isEmpty())
    }
}

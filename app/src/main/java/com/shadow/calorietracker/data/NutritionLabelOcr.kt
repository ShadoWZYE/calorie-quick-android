package com.shadow.calorietracker.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.shadow.calorietracker.model.Allergen
import com.shadow.calorietracker.model.AllergenDeclaration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine

data class NutritionLabelPrefill(
    val suggestedName: String,
    val barcode: String? = null,
    val caloriesPer100g: Int? = null,
    val proteinPer100g: Double? = null,
    val carbsPer100g: Double? = null,
    val fatPer100g: Double? = null,
    val fiberPer100g: Double? = null,
    val packageGrams: Int? = null,
    val basisUnit: NutritionBasisUnit = NutritionBasisUnit.GRAMS,
    val allergens: Map<Allergen, AllergenDeclaration> = emptyMap(),
    val warnings: Set<NutritionLabelWarning> = emptySet(),
)

enum class NutritionBasisUnit { GRAMS, MILLILITERS }

enum class NutritionLabelWarning {
    BASIS_UNKNOWN,
    MULTIPLE_COLUMNS_UNCLEAR,
    NORMALIZED_FROM_SERVING,
    CORE_VALUES_MISSING,
}

class NutritionLabelOcr(context: Context) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun scan(uri: Uri, suggestedName: String): NutritionLabelPrefill {
        val image = InputImage.fromFilePath(applicationContext, uri)
        val recognized = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    if (continuation.isActive) continuation.resume(text.text)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
        return NutritionLabelParser.parse(recognized, suggestedName)
    }

    override fun close() = recognizer.close()
}

object NutritionLabelParser {
    fun parse(rawText: String, suggestedName: String = ""): NutritionLabelPrefill {
        val lines = rawText.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        val normalizedLines = lines.map { it.lowercase().withoutDiacritics() }
        val normalizedText = normalizedLines.joinToString("\n")
        val firstNutritionRow = normalizedLines.indexOfFirst(::isNutritionRow).takeIf { it >= 0 } ?: lines.size
        val headerText = normalizedLines.take(firstNutritionRow).joinToString(" ")
        val basisColumns = BASIS_VALUE.findAll(headerText).map { match ->
            BasisColumn(
                amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0,
                unit = match.groupValues[2].lowercase(),
            )
        }.filter { it.amount > 0.0 }.toList()
        val per100ColumnIndex = basisColumns.indexOfFirst { it.amount == 100.0 }.takeIf { it >= 0 }
        val per100Column = per100ColumnIndex?.let(basisColumns::get)
        val hasPer100 = per100Column != null || PER_100.containsMatchIn(normalizedText)
        val hasPerServing = PER_SERVING.containsMatchIn(normalizedText)
        val hasMultipleColumns = basisColumns.size > 1 || hasPerServing
        val servingAmount = findMeasure(lines, SERVING_KEYWORDS)?.normalizedAmount
        val packageMeasure = findMeasure(lines, PACKAGE_KEYWORDS) ?: findDeclaredPackageMeasure(normalizedText)
        val warnings = mutableSetOf<NutritionLabelWarning>()

        val extraction = when {
            hasPer100 && hasMultipleColumns && per100ColumnIndex == null -> {
                warnings += NutritionLabelWarning.MULTIPLE_COLUMNS_UNCLEAR
                Extraction()
            }
            hasPer100 -> extractNutrition(lines, per100ColumnIndex ?: 0, requireMultiple = hasMultipleColumns)
            hasPerServing && servingAmount != null -> {
                warnings += NutritionLabelWarning.NORMALIZED_FROM_SERVING
                extractNutrition(lines, 0, requireMultiple = false).scaled(100.0 / servingAmount)
            }
            else -> {
                warnings += NutritionLabelWarning.BASIS_UNKNOWN
                Extraction()
            }
        }

        if (listOf(extraction.calories, extraction.protein, extraction.carbs, extraction.fat).any { it == null }) {
            warnings += NutritionLabelWarning.CORE_VALUES_MISSING
        }

        return NutritionLabelPrefill(
            suggestedName = suggestedName.trim(),
            barcode = findBarcode(lines),
            caloriesPer100g = extraction.calories?.roundToInt()?.takeIf { it in 0..5_000 },
            proteinPer100g = extraction.protein.validNutrient(),
            carbsPer100g = extraction.carbs.validNutrient(),
            fatPer100g = extraction.fat.validNutrient(),
            fiberPer100g = extraction.fiber.validNutrient(),
            packageGrams = packageMeasure?.normalizedAmount?.roundToInt()?.takeIf { it in 1..50_000 },
            basisUnit = if (per100Column?.unit == "ml") NutritionBasisUnit.MILLILITERS else NutritionBasisUnit.GRAMS,
            allergens = extractAllergens(lines),
            warnings = warnings,
        )
    }

    private fun extractNutrition(lines: List<String>, columnIndex: Int, requireMultiple: Boolean): Extraction = Extraction(
        calories = rowValues(lines, ENERGY_KEYWORDS, KCAL_VALUE, columnIndex, requireMultiple),
        protein = rowValues(lines, PROTEIN_KEYWORDS, GRAM_VALUE, columnIndex, requireMultiple),
        carbs = rowValues(lines, CARB_KEYWORDS, GRAM_VALUE, columnIndex, requireMultiple),
        fat = rowValues(lines, FAT_KEYWORDS, GRAM_VALUE, columnIndex, requireMultiple, EXCLUDED_FAT_KEYWORDS),
        fiber = rowValues(lines, FIBER_KEYWORDS, GRAM_VALUE, columnIndex, requireMultiple),
    )

    private fun rowValues(
        lines: List<String>,
        keywords: List<String>,
        valuePattern: Regex,
        columnIndex: Int,
        requireMultiple: Boolean,
        excludedKeywords: List<String> = emptyList(),
    ): Double? {
        val rowIndex = lines.indexOfFirst { rawLine ->
            val normalized = rawLine.lowercase().withoutDiacritics()
            keywords.any(normalized::contains) && excludedKeywords.none(normalized::contains)
        }
        if (rowIndex < 0) return null
        val block = buildList {
            add(lines[rowIndex])
            for (index in rowIndex + 1 until minOf(lines.size, rowIndex + 7)) {
                val normalized = lines[index].lowercase().withoutDiacritics()
                if (isNutritionRow(normalized)) break
                add(lines[index])
            }
        }.joinToString(" ")
        val values = valuePattern.findAll(block.lowercase()).mapNotNull { match ->
            match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }.toList()
        if (requireMultiple && values.size < 2) return null
        return values.getOrNull(columnIndex)
    }

    private fun findMeasure(lines: List<String>, keywords: List<String>): Measure? {
        val line = lines.firstOrNull { rawLine ->
            val normalized = rawLine.lowercase().withoutDiacritics()
            keywords.any(normalized::contains)
        } ?: return null
        val match = MEASURE_VALUE.find(line.lowercase()) ?: return null
        val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        return Measure(amount, match.groupValues[2].lowercase())
    }

    private fun findDeclaredPackageMeasure(normalizedText: String): Measure? {
        val match = DECLARED_PACKAGE.find(normalizedText) ?: return null
        val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        return Measure(amount, match.groupValues[2].lowercase())
    }

    private fun isNutritionRow(line: String): Boolean = ROW_BOUNDARY_KEYWORDS.any(line::contains)

    private fun findBarcode(lines: List<String>): String? = lines.asSequence()
        .flatMap { DIGIT_SEQUENCE.findAll(it.replace(" ", "")).map(MatchResult::value) }
        .filter { it.length in setOf(8, 12, 13, 14) && hasValidGtinCheckDigit(it) }
        .firstOrNull()

    private fun hasValidGtinCheckDigit(value: String): Boolean {
        val digits = value.map(Char::digitToInt)
        val expected = digits.last()
        val sum = digits.dropLast(1).asReversed().mapIndexed { index, digit ->
            digit * if (index % 2 == 0) 3 else 1
        }.sum()
        return (10 - sum % 10) % 10 == expected
    }

    private fun extractAllergens(lines: List<String>): Map<Allergen, AllergenDeclaration> {
        val declarations = mutableMapOf<Allergen, AllergenDeclaration>()
        lines.forEach { rawLine ->
            val line = rawLine.lowercase().withoutDiacritics()
            val declaration = when {
                MAY_CONTAIN_KEYWORDS.any(line::contains) -> AllergenDeclaration.MAY_CONTAIN
                CONTAINS_KEYWORDS.any(line::contains) -> AllergenDeclaration.CONTAINS
                else -> null
            } ?: return@forEach
            ALLERGEN_TERMS.forEach { (allergen, terms) ->
                if (terms.any(line::contains)) {
                    val existing = declarations[allergen]
                    if (existing != AllergenDeclaration.CONTAINS || declaration == AllergenDeclaration.CONTAINS) {
                        declarations[allergen] = declaration
                    }
                }
            }
        }
        return declarations
    }

    private data class Extraction(
        val calories: Double? = null,
        val protein: Double? = null,
        val carbs: Double? = null,
        val fat: Double? = null,
        val fiber: Double? = null,
    ) {
        fun scaled(factor: Double) = Extraction(
            calories?.times(factor),
            protein?.times(factor),
            carbs?.times(factor),
            fat?.times(factor),
            fiber?.times(factor),
        )
    }

    private data class BasisColumn(val amount: Double, val unit: String)

    private data class Measure(val amount: Double, val unit: String) {
        val normalizedAmount: Double = when (unit) {
            "kg", "l" -> amount * 1_000.0
            else -> amount
        }
    }

    private val PER_100 = Regex("""(?:(?:per|/|la)\s*)?100\s*(?:g|ml)\b""")
    private val PER_SERVING = Regex("""(?:per|/|pe)\s*(?:serving|portion|portie)\b""")
    private val BASIS_VALUE = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(g|ml)\b""")
    private val KCAL_VALUE = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*kcal\b""")
    private val GRAM_VALUE = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*g\b""")
    private val MEASURE_VALUE = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(kg|g|ml|l)\b""")
    private val DECLARED_PACKAGE = Regex("""\b([0-9]+(?:[.,][0-9]+)?)\s*(ml|l|g|kg)\s*(?:=|x\s*[0-9]+)""")
    private val DIGIT_SEQUENCE = Regex("""\d{8,14}""")
    private val ENERGY_KEYWORDS = listOf("energy", "energie", "valoare energetica")
    private val PROTEIN_KEYWORDS = listOf("protein", "proteine")
    private val CARB_KEYWORDS = listOf("carbohydrate", "carbohidrati", "glucide")
    private val FAT_KEYWORDS = listOf("fat", "grasimi", "lipide")
    private val EXCLUDED_FAT_KEYWORDS = listOf("saturat", "trans")
    private val FIBER_KEYWORDS = listOf("fiber", "fibre")
    private val SERVING_KEYWORDS = listOf("serving size", "portion size", "marimea portiei", "portie")
    private val PACKAGE_KEYWORDS = listOf("net weight", "net wt", "greutate neta", "cantitate neta")
    private val ROW_BOUNDARY_KEYWORDS = listOf(
        "energy", "energie", "valoare energetica", "protein", "proteine", "carbohydrate", "carbohidrati",
        "glucide", "fat", "grasimi", "lipide", "fiber", "fibre", "saturat", "trans", "zahar", "sugar",
        "salt", "sare",
    )
    private val CONTAINS_KEYWORDS = listOf("contains", "allergens", "contine", "alergeni")
    private val MAY_CONTAIN_KEYWORDS = listOf("may contain", "traces of", "poate contine", "urme de")
    private val ALLERGEN_TERMS = mapOf(
        Allergen.GLUTEN to listOf("gluten", "wheat", "grau", "orz", "secara"),
        Allergen.CRUSTACEANS to listOf("crustacean", "crustacee"),
        Allergen.EGGS to listOf("egg", "oua"),
        Allergen.FISH to listOf("fish", "peste"),
        Allergen.PEANUTS to listOf("peanut", "arahide"),
        Allergen.SOY to listOf("soy", "soia"),
        Allergen.MILK to listOf("milk", "lapte", "lactose", "lactoza"),
        Allergen.NUTS to listOf("tree nut", "nuts", "alune", "migdale", "nuci"),
        Allergen.CELERY to listOf("celery", "telina"),
        Allergen.MUSTARD to listOf("mustard", "mustar"),
        Allergen.SESAME to listOf("sesame", "susan"),
        Allergen.SULPHITES to listOf("sulphite", "sulfite", "sulfit"),
        Allergen.LUPIN to listOf("lupin"),
        Allergen.MOLLUSCS to listOf("mollusc", "moluste"),
    )
}

private fun String.withoutDiacritics(): String = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")

private fun Double?.validNutrient(): Double? = this?.takeIf { it.isFinite() && it in 0.0..100.0 }

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
    val allergens: Map<Allergen, AllergenDeclaration> = emptyMap(),
    val warnings: Set<NutritionLabelWarning> = emptySet(),
)

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
        val normalizedText = lines.joinToString("\n").lowercase().withoutDiacritics()
        val hasPer100g = PER_100G.containsMatchIn(normalizedText)
        val hasPerServing = PER_SERVING.containsMatchIn(normalizedText)
        val header = lines.firstOrNull { line ->
            val normalized = line.lowercase().withoutDiacritics()
            PER_100G.containsMatchIn(normalized) && PER_SERVING.containsMatchIn(normalized)
        }?.lowercase()?.withoutDiacritics()
        val per100ColumnIndex = header?.let {
            if (PER_100G.find(it)!!.range.first < PER_SERVING.find(it)!!.range.first) 0 else 1
        }
        val servingGrams = findWeight(lines, SERVING_KEYWORDS)
        val packageGrams = findWeight(lines, PACKAGE_KEYWORDS)
        val warnings = mutableSetOf<NutritionLabelWarning>()

        val extraction = when {
            hasPer100g && hasPerServing && per100ColumnIndex == null -> {
                warnings += NutritionLabelWarning.MULTIPLE_COLUMNS_UNCLEAR
                Extraction()
            }
            hasPer100g -> extractNutrition(lines, per100ColumnIndex ?: 0, requireMultiple = hasPerServing)
            hasPerServing && servingGrams != null -> {
                warnings += NutritionLabelWarning.NORMALIZED_FROM_SERVING
                extractNutrition(lines, 0, requireMultiple = false).scaled(100.0 / servingGrams)
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
            packageGrams = packageGrams,
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
        val line = lines.firstOrNull { rawLine ->
            val normalized = rawLine.lowercase().withoutDiacritics()
            keywords.any(normalized::contains) && excludedKeywords.none(normalized::contains)
        } ?: return null
        val values = valuePattern.findAll(line.lowercase()).mapNotNull { match ->
            match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }.toList()
        if (requireMultiple && values.size < 2) return null
        return values.getOrNull(columnIndex)
    }

    private fun findWeight(lines: List<String>, keywords: List<String>): Int? {
        val line = lines.firstOrNull { rawLine ->
            val normalized = rawLine.lowercase().withoutDiacritics()
            keywords.any(normalized::contains)
        } ?: return null
        val match = WEIGHT_VALUE.find(line.lowercase()) ?: return null
        val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val grams = if (match.groupValues[2].lowercase() == "kg") amount * 1_000.0 else amount
        return grams.roundToInt().takeIf { it in 1..50_000 }
    }

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

    private val PER_100G = Regex("""(?:per|/|la)\s*100\s*g\b""")
    private val PER_SERVING = Regex("""(?:per|/|pe)\s*(?:serving|portion|portie)\b""")
    private val KCAL_VALUE = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*kcal\b""")
    private val GRAM_VALUE = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*g\b""")
    private val WEIGHT_VALUE = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(kg|g)\b""")
    private val DIGIT_SEQUENCE = Regex("""\d{8,14}""")
    private val ENERGY_KEYWORDS = listOf("energy", "energie", "valoare energetica")
    private val PROTEIN_KEYWORDS = listOf("protein", "proteine")
    private val CARB_KEYWORDS = listOf("carbohydrate", "carbohidrati", "glucide")
    private val FAT_KEYWORDS = listOf("fat", "grasimi", "lipide")
    private val EXCLUDED_FAT_KEYWORDS = listOf("saturat", "trans")
    private val FIBER_KEYWORDS = listOf("fiber", "fibre")
    private val SERVING_KEYWORDS = listOf("serving size", "portion size", "marimea portiei", "portie")
    private val PACKAGE_KEYWORDS = listOf("net weight", "net wt", "greutate neta", "cantitate neta")
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

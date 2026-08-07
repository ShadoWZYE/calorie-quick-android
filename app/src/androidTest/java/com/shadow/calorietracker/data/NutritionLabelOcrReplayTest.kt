package com.shadow.calorietracker.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Replays an external field-test image through the complete on-device OCR pipeline.
 *
 * Push a sanitized fixture into the target app's external files directory, then pass its path and
 * expected values as instrumentation arguments. With no image argument this test is skipped, so
 * normal connected-test runs do not depend on private field evidence.
 */
@RunWith(AndroidJUnit4::class)
class NutritionLabelOcrReplayTest {
    @Test
    fun replaySanitizedFieldLabel() {
        runBlocking {
            val arguments = InstrumentationRegistry.getArguments()
            val imagePath = arguments.getString("ocrImage").orEmpty()
            assumeTrue("No external OCR fixture supplied", imagePath.isNotBlank())
            val image = File(imagePath)
            assumeTrue("OCR fixture is missing: $imagePath", image.isFile)

            val result = NutritionLabelOcr(InstrumentationRegistry.getInstrumentation().targetContext).use { ocr ->
                ocr.scan(Uri.fromFile(image), suggestedName = "OCR replay fixture")
            }

            val expectedCalories = arguments.getString("expectedCalories")?.toIntOrNull()
            val expectedProtein = arguments.getString("expectedProtein")?.toDoubleOrNull()
            val expectedCarbs = arguments.getString("expectedCarbs")?.toDoubleOrNull()
            val expectedFat = arguments.getString("expectedFat")?.toDoubleOrNull()
            val expectedPackage = arguments.getString("expectedPackage")?.toIntOrNull()
            val expectedBasis = arguments.getString("expectedBasis")?.let(NutritionBasisUnit::valueOf)

            assertNotNull("OCR did not recover calories; warnings=${result.warnings}", result.caloriesPer100g)
            expectedCalories?.let { assertEquals(it, result.caloriesPer100g) }
            expectedProtein?.let { assertEquals(it, result.proteinPer100g ?: Double.NaN, 0.11) }
            expectedCarbs?.let { assertEquals(it, result.carbsPer100g ?: Double.NaN, 0.11) }
            expectedFat?.let { assertEquals(it, result.fatPer100g ?: Double.NaN, 0.11) }
            expectedPackage?.let { assertEquals(it, result.packageGrams) }
            expectedBasis?.let { assertEquals(it, result.basisUnit) }
        }
    }
}

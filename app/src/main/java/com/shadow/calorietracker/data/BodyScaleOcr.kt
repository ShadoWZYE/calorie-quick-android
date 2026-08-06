package com.shadow.calorietracker.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.Text
import com.shadow.calorietracker.model.BodyMeasurement
import com.shadow.calorietracker.model.BodyMeasurementSource
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class BodyScalePrefill(
    val measurement: BodyMeasurement?,
    val warnings: Set<BodyScaleWarning>,
)

enum class BodyScaleWarning { WEIGHT_MISSING, DATE_MISSING, LIMITED_METRICS }

class BodyScaleOcr(context: Context) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun scan(uri: Uri, zoneId: ZoneId = ZoneId.systemDefault()): BodyScalePrefill {
        val image = InputImage.fromFilePath(applicationContext, uri)
        val recognized = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text -> if (continuation.isActive) continuation.resume(text) }
                .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
        }
        val reconstructed = recognized.reconstructedRows()
        val structuredResult = BodyScaleParser.parse(reconstructed, zoneId)
        return if (structuredResult.measurement != null) {
            structuredResult
        } else {
            BodyScaleParser.parse(recognized.text, zoneId)
        }
    }

    override fun close() = recognizer.close()
}

/**
 * ML Kit commonly returns tall comparison reports block-by-block (all labels, then
 * each numeric column). Rebuild visual rows from bounding boxes before parsing so
 * values remain attached to their metric label.
 */
private fun Text.reconstructedRows(): String {
    data class PositionedLine(val text: String, val left: Int, val centerY: Double, val height: Int)
    data class VisualRow(
        val lines: MutableList<PositionedLine>,
        var centerY: Double,
        var averageHeight: Double,
    )

    val positioned = textBlocks.flatMap { block -> block.lines }.mapNotNull { line ->
        val box = line.boundingBox ?: return@mapNotNull null
        PositionedLine(line.text.trim(), box.left, box.exactCenterY().toDouble(), box.height())
            .takeIf { it.text.isNotEmpty() }
    }.sortedBy(PositionedLine::centerY)
    if (positioned.isEmpty()) return this.text

    val rows = mutableListOf<VisualRow>()
    positioned.forEach { line ->
        val row = rows.lastOrNull()?.takeIf { candidate ->
            val tolerance = maxOf(6.0, minOf(candidate.averageHeight, line.height.toDouble()) * 0.7)
            kotlin.math.abs(candidate.centerY - line.centerY) <= tolerance
        }
        if (row == null) {
            rows += VisualRow(mutableListOf(line), line.centerY, line.height.toDouble())
        } else {
            row.lines += line
            row.centerY = row.lines.map(PositionedLine::centerY).average()
            row.averageHeight = row.lines.map { it.height.toDouble() }.average()
        }
    }
    return rows.joinToString("\n") { row ->
        row.lines.sortedBy(PositionedLine::left).joinToString(" ", transform = PositionedLine::text)
    }
}

object BodyScaleParser {
    fun parse(rawText: String, zoneId: ZoneId = ZoneId.systemDefault()): BodyScalePrefill {
        val lines = rawText.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        val weight = currentValue(lines, "weight")?.takeIf { it in 20.0..400.0 }
        val measuredAt = findNewestDateTime(rawText)?.atZone(zoneId)?.toInstant()?.toEpochMilli()
        val warnings = buildSet {
            if (weight == null) add(BodyScaleWarning.WEIGHT_MISSING)
            if (measuredAt == null) add(BodyScaleWarning.DATE_MISSING)
        }.toMutableSet()
        if (weight == null) return BodyScalePrefill(null, warnings)

        val measurement = BodyMeasurement(
            measuredAtEpochMillis = measuredAt ?: System.currentTimeMillis(),
            weightKg = weight,
            bmi = currentValue(lines, "bmi").valid(5.0, 100.0),
            bodyFatPercent = currentValue(lines, "body fat").validPercent(),
            fatMassKg = currentValue(lines, "fat mass").validMass(),
            fatFreeMassKg = currentValue(lines, "fat-free body weight", "fat free body weight").validMass(),
            muscleMassKg = currentValue(lines, "muscle mass").validMass(),
            musclePercent = currentValue(lines, "muscle rate").validPercent(),
            skeletalMusclePercent = currentValue(lines, "skeletal muscle").validPercent(),
            boneMassKg = currentValue(lines, "bone mass").validMass(),
            proteinMassKg = currentValue(lines, "protein mass").validMass(),
            proteinPercent = currentValue(lines, "protein").validPercent(),
            waterMassKg = currentValue(lines, "water weight").validMass(),
            bodyWaterPercent = currentValue(lines, "body water").validPercent(),
            subcutaneousFatPercent = currentValue(lines, "subcutaneous fat").validPercent(),
            visceralFat = currentValue(lines, "visceral fat").valid(0.0, 100.0),
            bmrCalories = currentValue(lines, "bmr")?.toInt()?.takeIf { it in 500..5_000 },
            bodyAge = currentValue(lines, "body age")?.toInt()?.takeIf { it in 1..120 },
            source = BodyMeasurementSource.OCR,
        )
        val optionalCount = listOf(
            measurement.bmi,
            measurement.bodyFatPercent,
            measurement.fatMassKg,
            measurement.fatFreeMassKg,
            measurement.muscleMassKg,
            measurement.musclePercent,
            measurement.skeletalMusclePercent,
            measurement.boneMassKg,
            measurement.proteinMassKg,
            measurement.proteinPercent,
            measurement.waterMassKg,
            measurement.bodyWaterPercent,
            measurement.subcutaneousFatPercent,
            measurement.visceralFat,
            measurement.bmrCalories,
            measurement.bodyAge,
        ).count { it != null }
        if (optionalCount < 3) warnings += BodyScaleWarning.LIMITED_METRICS
        return BodyScalePrefill(measurement, warnings)
    }

    private fun currentValue(lines: List<String>, vararg labels: String): Double? {
        val normalizedLabels = labels.map(String::normalizeScaleText)
        val index = lines.indexOfFirst { line ->
            val normalized = line.normalizeScaleText()
            normalizedLabels.any { normalized.matchesScaleLabel(it) }
        }
        if (index < 0) return null
        val label = normalizedLabels.first { candidate ->
            val normalized = lines[index].normalizeScaleText()
            normalized.matchesScaleLabel(candidate)
        }
        val initialValues = lines[index].normalizeScaleText().removePrefix(label)
        val row = buildString {
            append(initialValues)
            if (!NUMBER.containsMatchIn(initialValues)) {
                for (next in index + 1..minOf(index + 3, lines.lastIndex)) {
                    val continuation = lines[next].normalizeScaleText()
                    if (continuation.firstOrNull()?.let { it.isDigit() || it == '-' } != true) break
                    append(' ')
                    append(continuation)
                }
            }
        }
        val values = NUMBER.findAll(row).mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }.toList()
        return when {
            values.size >= 2 -> values[1]
            else -> values.firstOrNull()
        }
    }

    private fun findNewestDateTime(rawText: String): LocalDateTime? = DATE_TIME.findAll(rawText)
        .mapNotNull { match ->
            val month = MONTHS[match.groupValues[3].lowercase()] ?: return@mapNotNull null
            runCatching {
                LocalDateTime.of(
                    match.groupValues[5].toInt(),
                    month,
                    match.groupValues[4].toInt(),
                    match.groupValues[1].toIntOrNull() ?: 12,
                    match.groupValues[2].toIntOrNull() ?: 0,
                )
            }.getOrNull()
        }
        .maxOrNull()

    private fun Double?.valid(minimum: Double, maximum: Double) = this?.takeIf { it.isFinite() && it in minimum..maximum }
    private fun Double?.validPercent() = valid(0.0, 100.0)
    private fun Double?.validMass() = valid(0.0, 400.0)

    private val NUMBER = Regex("""-?\d+(?:[.,]\d+)?""")
    private val DATE_TIME = Regex(
        """(?i)(?:(\d{1,2}):(\d{2})\s*)?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?\s*(\d{1,2}),?\s*(\d{4})""",
    )
    private val MONTHS = mapOf(
        "jan" to Month.JANUARY, "feb" to Month.FEBRUARY, "mar" to Month.MARCH,
        "apr" to Month.APRIL, "may" to Month.MAY, "jun" to Month.JUNE,
        "jul" to Month.JULY, "aug" to Month.AUGUST, "sep" to Month.SEPTEMBER,
        "oct" to Month.OCTOBER, "nov" to Month.NOVEMBER, "dec" to Month.DECEMBER,
    )
}

private fun String.normalizeScaleText(): String = lowercase()
    .replace('–', '-')
    .replace(Regex("""\s+"""), " ")
    .trim()

private fun String.matchesScaleLabel(label: String): Boolean {
    if (this == label) return true
    val suffix = removePrefix("$label ")
    return suffix != this && suffix.firstOrNull()?.let { it.isDigit() || it == '-' } == true
}

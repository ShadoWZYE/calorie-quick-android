package com.shadow.calorietracker.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class DailyNutritionSummary(
    val date: LocalDate,
    val nutrition: Nutrition,
    val entryCount: Int,
    val fiberIncomplete: Boolean,
)

data class PeriodNutritionSummary(
    val startDate: LocalDate,
    val endDateInclusive: LocalDate,
    val loggedDays: Int,
    val totalDays: Int,
    val averageNutrition: Nutrition,
    val calorieGoalDays: Int,
)

object HistoryReportCalculator {
    fun daily(entries: List<FoodEntry>, zoneId: ZoneId): List<DailyNutritionSummary> = entries
        .groupBy { Instant.ofEpochMilli(it.consumedAtEpochMillis).atZone(zoneId).toLocalDate() }
        .map { (date, dayEntries) ->
            DailyNutritionSummary(
                date = date,
                nutrition = dayEntries.fold(Nutrition.Zero) { total, entry -> total + entry.nutrition },
                entryCount = dayEntries.size,
                fiberIncomplete = dayEntries.any { it.nutrition.fiberGrams == null },
            )
        }
        .sortedByDescending(DailyNutritionSummary::date)

    fun period(
        days: List<DailyNutritionSummary>,
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        calorieGoal: Int,
    ): PeriodNutritionSummary {
        require(!endDateInclusive.isBefore(startDate))
        val selected = days.filter { it.date in startDate..endDateInclusive }
        val totalDays = (endDateInclusive.toEpochDay() - startDate.toEpochDay() + 1).toInt()
        val totals = selected.fold(Nutrition.Zero) { total, day -> total + day.nutrition }
        val divisor = selected.size.coerceAtLeast(1).toDouble()
        return PeriodNutritionSummary(
            startDate = startDate,
            endDateInclusive = endDateInclusive,
            loggedDays = selected.size,
            totalDays = totalDays,
            averageNutrition = Nutrition(
                calories = (totals.calories / divisor).roundToInt(),
                proteinGrams = totals.proteinGrams / divisor,
                carbsGrams = totals.carbsGrams / divisor,
                fatGrams = totals.fatGrams / divisor,
                fiberGrams = if (selected.any(DailyNutritionSummary::fiberIncomplete)) null else totals.fiberGrams?.div(divisor),
            ),
            calorieGoalDays = selected.count { it.nutrition.calories <= calorieGoal },
        )
    }
}

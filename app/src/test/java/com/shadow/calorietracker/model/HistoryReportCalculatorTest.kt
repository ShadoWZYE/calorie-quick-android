package com.shadow.calorietracker.model

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryReportCalculatorTest {
    private val zone = ZoneOffset.UTC

    @Test
    fun `daily report groups entries by local calendar day`() {
        val firstDay = LocalDate.of(2026, 8, 1)
        val entries = listOf(
            entry("breakfast", firstDay, 500, 25.0),
            entry("dinner", firstDay, 700, 35.0),
            entry("next", firstDay.plusDays(1), 900, 40.0),
        )

        val days = HistoryReportCalculator.daily(entries, zone)

        assertEquals(2, days.size)
        assertEquals(firstDay.plusDays(1), days[0].date)
        assertEquals(1_200, days[1].nutrition.calories)
        assertEquals(60.0, days[1].nutrition.proteinGrams, 0.001)
        assertEquals(2, days[1].entryCount)
    }

    @Test
    fun `period averages use logged days and report coverage separately`() {
        val start = LocalDate.of(2026, 8, 1)
        val days = HistoryReportCalculator.daily(
            listOf(entry("one", start, 2_100, 100.0), entry("two", start.plusDays(1), 1_400, 80.0)),
            zone,
        )

        val report = HistoryReportCalculator.period(days, start, start.plusDays(6), calorieGoal = 2_000)

        assertEquals(7, report.totalDays)
        assertEquals(2, report.loggedDays)
        assertEquals(1_750, report.averageNutrition.calories)
        assertEquals(90.0, report.averageNutrition.proteinGrams, 0.01)
    }

    @Test
    fun `unknown fiber is retained as an incomplete daily value`() {
        val date = LocalDate.of(2026, 8, 1)
        val unknown = entry("unknown", date, 100, 2.0).copy(
            nutrition = Nutrition(100, 2.0, 3.0, 1.0, null),
        )

        val day = HistoryReportCalculator.daily(listOf(unknown), zone).single()

        assertTrue(day.fiberIncomplete)
    }

    private fun entry(id: String, date: LocalDate, calories: Int, protein: Double) = FoodEntry(
        id = id,
        foodId = id,
        foodName = LocalizedText(id, id),
        grams = 100,
        enteredAmount = 100.0,
        unitKey = "grams",
        unitLabel = LocalizedText("g", "g"),
        consumedAtEpochMillis = date.atStartOfDay(zone).plusHours(12).toInstant().toEpochMilli(),
        nutrition = Nutrition(calories, protein, 20.0, 10.0, 5.0),
    )
}

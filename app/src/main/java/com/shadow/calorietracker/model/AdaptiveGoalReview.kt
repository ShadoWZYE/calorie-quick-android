package com.shadow.calorietracker.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

sealed interface AdaptiveGoalReview {
    data class NeedMoreCheckIns(val current: Int, val required: Int = 4) : AdaptiveGoalReview
    data class NeedMoreTime(val spanDays: Int, val requiredDays: Int = 14) : AdaptiveGoalReview
    data class NeedMoreDiaryDays(val current: Int, val required: Int = 10) : AdaptiveGoalReview
    data class OnTrack(val weeklyChangeKg: Double, val checkInCount: Int, val spanDays: Int) : AdaptiveGoalReview
    data class Suggestion(
        val weeklyChangeKg: Double,
        val targetWeeklyChangeKg: Double,
        val calorieAdjustment: Int,
        val proposedCalories: Int,
        val checkInCount: Int,
        val spanDays: Int,
    ) : AdaptiveGoalReview
}

object AdaptiveGoalReviewer {
    private const val MILLIS_PER_DAY = 86_400_000.0

    fun review(
        profile: UserProfile,
        measurements: List<BodyMeasurement>,
        loggedDates: Set<LocalDate>,
        zoneId: ZoneId,
    ): AdaptiveGoalReview {
        val sorted = measurements.sortedBy { it.measuredAtEpochMillis }
        if (sorted.size < 4) return AdaptiveGoalReview.NeedMoreCheckIns(sorted.size)

        val newest = sorted.last()
        val cutoff = newest.measuredAtEpochMillis - 28L * MILLIS_PER_DAY.toLong()
        val recent = sorted.filter { it.measuredAtEpochMillis >= cutoff }
        if (recent.size < 4) return AdaptiveGoalReview.NeedMoreCheckIns(recent.size)
        val spanDays = ((recent.last().measuredAtEpochMillis - recent.first().measuredAtEpochMillis) / MILLIS_PER_DAY)
            .roundToInt()
        if (spanDays < 14) return AdaptiveGoalReview.NeedMoreTime(spanDays)

        val newestDate = Instant.ofEpochMilli(newest.measuredAtEpochMillis).atZone(zoneId).toLocalDate()
        val diaryStart = newestDate.minusDays(13)
        val loggedDayCount = loggedDates.count { !it.isBefore(diaryStart) && !it.isAfter(newestDate) }
        if (loggedDayCount < 10) return AdaptiveGoalReview.NeedMoreDiaryDays(loggedDayCount)

        val weeklyChange = regressionKgPerDay(recent) * 7.0
        val targetWeeklyChange = when (profile.goalType) {
            GoalType.LOSE -> -newest.weightKg * 0.005
            GoalType.MAINTAIN -> 0.0
            GoalType.GAIN -> newest.weightKg * 0.0025
        }
        val difference = targetWeeklyChange - weeklyChange
        if (abs(difference) < 0.15) {
            return AdaptiveGoalReview.OnTrack(weeklyChange, recent.size, spanDays)
        }

        val rawAdjustment = difference * 7_700.0 / 7.0
        val adjustment = ((rawAdjustment / 25.0).roundToInt() * 25).coerceIn(-200, 200)
        val proposed = (profile.calorieGoal + adjustment).coerceIn(1_200, 5_000)
        val actualAdjustment = proposed - profile.calorieGoal
        if (abs(actualAdjustment) < 50) {
            return AdaptiveGoalReview.OnTrack(weeklyChange, recent.size, spanDays)
        }
        return AdaptiveGoalReview.Suggestion(
            weeklyChangeKg = weeklyChange,
            targetWeeklyChangeKg = targetWeeklyChange,
            calorieAdjustment = actualAdjustment,
            proposedCalories = proposed,
            checkInCount = recent.size,
            spanDays = spanDays,
        )
    }

    private fun regressionKgPerDay(measurements: List<BodyMeasurement>): Double {
        val origin = measurements.first().measuredAtEpochMillis
        val points = measurements.map {
            (it.measuredAtEpochMillis - origin) / MILLIS_PER_DAY to it.weightKg
        }
        val meanX = points.map(Pair<Double, Double>::first).average()
        val meanY = points.map(Pair<Double, Double>::second).average()
        val denominator = points.sumOf { (x, _) -> (x - meanX) * (x - meanX) }
        if (denominator == 0.0) return 0.0
        return points.sumOf { (x, y) -> (x - meanX) * (y - meanY) } / denominator
    }
}

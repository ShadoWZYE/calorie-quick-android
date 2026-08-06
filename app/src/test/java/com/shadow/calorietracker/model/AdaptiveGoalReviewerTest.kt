package com.shadow.calorietracker.model

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveGoalReviewerTest {
    private val profile = UserProfile(
        onboardingComplete = true,
        age = 30,
        heightCm = 175,
        weightKg = 75.0,
        formulaSex = FormulaSex.FEMALE,
        activityLevel = ActivityLevel.LIGHT,
        goalType = GoalType.LOSE,
        calorieGoal = 2_000,
        proteinGoalGrams = 120,
        carbsGoalGrams = 220,
        fatGoalGrams = 70,
    )
    private val start = LocalDate.of(2026, 7, 1)
    private val loggedDates = (8L..21L).map { start.plusDays(it) }.toSet()

    @Test
    fun waitsForEnoughCheckInsAndDiaryEvidence() {
        val measurements = listOf(0L, 7L, 14L).map { measurement(it, 75.0) }
        assertTrue(review(measurements, loggedDates) is AdaptiveGoalReview.NeedMoreCheckIns)

        val four = measurements + measurement(21, 75.0)
        assertTrue(review(four, emptySet()) is AdaptiveGoalReview.NeedMoreDiaryDays)
    }

    @Test
    fun proposesSmallCappedChangeAndNeverAppliesItDirectly() {
        val measurements = listOf(0L, 7L, 14L, 21L).map { measurement(it, 75.0) }
        val result = review(measurements, loggedDates) as AdaptiveGoalReview.Suggestion

        assertEquals(-200, result.calorieAdjustment)
        assertEquals(1_800, result.proposedCalories)
        assertEquals(2_000, profile.calorieGoal)
    }

    @Test
    fun recognizesTrendNearGoalWithoutSuggestingChange() {
        val measurements = listOf(
            measurement(0, 75.0),
            measurement(7, 74.625),
            measurement(14, 74.25),
            measurement(21, 73.875),
        )

        assertTrue(review(measurements, loggedDates) is AdaptiveGoalReview.OnTrack)
    }

    private fun review(measurements: List<BodyMeasurement>, diary: Set<LocalDate>) =
        AdaptiveGoalReviewer.review(profile, measurements, diary, ZoneOffset.UTC)

    private fun measurement(day: Long, kg: Double) = BodyMeasurement(
        measuredAtEpochMillis = start.plusDays(day).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        weightKg = kg,
    )
}

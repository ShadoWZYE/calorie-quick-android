package com.shadow.calorietracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shadow.calorietracker.model.ActivityLevel
import com.shadow.calorietracker.model.BodyMeasurement
import com.shadow.calorietracker.model.BodyMeasurementSource
import com.shadow.calorietracker.model.FormulaSex
import com.shadow.calorietracker.model.GoalType
import com.shadow.calorietracker.model.TargetMode
import com.shadow.calorietracker.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BodyMeasurementRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: CalorieRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repository = CalorieRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun bodyCompositionRoundTripsAndCanBeEditedAndDeleted() = runBlocking {
        repository.saveBodyMeasurement(
            BodyMeasurement(
                measuredAtEpochMillis = 1_786_000_000_000L,
                weightKg = 77.0,
                bmi = 24.3,
                bodyFatPercent = 17.0,
                muscleMassKg = 60.8,
                visceralFat = 7.3,
                bmrCalories = 1_751,
                bodyAge = 26,
                source = BodyMeasurementSource.OCR,
            ),
        )
        val saved = repository.bodyMeasurements.first().single()

        assertEquals(77.0, saved.weightKg, 0.001)
        assertEquals(17.0, saved.bodyFatPercent!!, 0.001)
        assertEquals(60.8, saved.muscleMassKg!!, 0.001)
        assertEquals(BodyMeasurementSource.OCR, saved.source)

        repository.saveBodyMeasurement(saved.copy(weightKg = 76.4))
        assertEquals(76.4, repository.bodyMeasurements.first().single().weightKg, 0.001)

        repository.deleteBodyMeasurement(repository.bodyMeasurements.first().single())
        assertTrue(repository.bodyMeasurements.first().isEmpty())
    }

    @Test
    fun displayNameRoundTripsWithoutAffectingTargetData() = runBlocking {
        repository.saveProfile(
            UserProfile(
                onboardingComplete = true,
                age = 30,
                heightCm = 175,
                weightKg = 75.0,
                formulaSex = FormulaSex.FEMALE,
                activityLevel = ActivityLevel.LIGHT,
                goalType = GoalType.MAINTAIN,
                calorieGoal = 2_000,
                proteinGoalGrams = 120,
                carbsGoalGrams = 220,
                fatGoalGrams = 70,
                targetMode = TargetMode.CUSTOM,
                displayName = "Local profile",
            ),
        )

        val profile = repository.profile.first()!!
        assertEquals("Local profile", profile.displayName)
        assertEquals(2_000, profile.calorieGoal)
    }
}

package com.shadow.calorietracker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavigationTest {
    @Test
    fun todayLeavesBackToAndroid() {
        assertNull(appBackDestination(AppScreen.TODAY))
    }

    @Test
    fun whatsNewReturnsToSettings() {
        assertEquals(AppScreen.SETTINGS, appBackDestination(AppScreen.WHATS_NEW))
    }

    @Test
    fun secondaryWorkflowsReturnToToday() {
        listOf(
            AppScreen.HISTORY,
            AppScreen.PROGRESS,
            AppScreen.SETTINGS,
            AppScreen.FEEDBACK,
            AppScreen.FOOD_EDITOR,
            AppScreen.RECIPE_EDITOR,
        ).forEach { screen ->
            assertEquals(AppScreen.TODAY, appBackDestination(screen))
        }
    }
}

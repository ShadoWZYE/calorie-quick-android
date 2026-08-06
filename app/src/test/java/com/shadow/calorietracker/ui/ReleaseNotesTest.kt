package com.shadow.calorietracker.ui

import com.shadow.calorietracker.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {
    @Test
    fun `current release notes match the packaged version`() {
        assertEquals(BuildConfig.VERSION_NAME, ReleaseNotes.current.versionName)
    }

    @Test
    fun `existing user sees notes after version increases`() {
        assertTrue(
            ReleaseNotes.shouldPrompt(
                onboardingComplete = true,
                lastSeenVersionCode = 5,
                currentVersionCode = 6,
            ),
        )
    }

    @Test
    fun `current version is not shown again`() {
        assertFalse(
            ReleaseNotes.shouldPrompt(
                onboardingComplete = true,
                lastSeenVersionCode = 6,
                currentVersionCode = 6,
            ),
        )
    }

    @Test
    fun `onboarding never receives an upgrade prompt`() {
        assertFalse(
            ReleaseNotes.shouldPrompt(
                onboardingComplete = false,
                lastSeenVersionCode = 5,
                currentVersionCode = 6,
            ),
        )
    }
}

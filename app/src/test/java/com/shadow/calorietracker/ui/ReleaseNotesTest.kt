package com.shadow.calorietracker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {
    @Test
    fun `in-app release history stops at the last consolidated stable build`() {
        assertEquals("0.2.2", ReleaseNotes.current.versionName)
        assertEquals(listOf("0.2.2", "0.2.1", "0.2.0"), ReleaseNotes.all.map { it.versionName })
        assertFalse(ReleaseNotes.all.any { '-' in it.versionName })
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

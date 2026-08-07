package com.shadow.calorietracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UiJankMonitorTest {
    @Test
    fun sessionAggregatesFramesByScreenWithoutRetainingFrameDetails() {
        val session = JankSessionAccumulator(startedAtEpochMillis = 1_000)

        session.record("TODAY", durationMillis = 10, isJank = false)
        session.record("TODAY", durationMillis = 40, isJank = true)
        session.record("TODAY", durationMillis = 120, isJank = true)
        session.record("SETTINGS", durationMillis = 12, isJank = false)
        session.record("SETTINGS", durationMillis = 50, isJank = true)

        val result = session.snapshot(endedAtEpochMillis = 2_000)

        assertEquals(5, result.totalFrames)
        assertEquals(3, result.jankyFrames)
        assertEquals(1, result.severeFrames)
        assertEquals(120, result.maxFrameDurationMillis)
        assertEquals(2, result.screens.size)
        assertEquals("TODAY", result.screens.first().screen)
        assertEquals(3, result.screens.first().totalFrames)
        assertEquals(2, result.screens.first().jankyFrames)
        assertEquals(160, result.screens.first().totalJankDurationMillis)
    }
}

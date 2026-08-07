package com.shadow.calorietracker.data

import android.view.Window
import androidx.metrics.performance.JankStats
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

object UiPerformanceContext {
    @Volatile var currentScreen: String = "UNKNOWN"
}

data class UiJankScreenSummary(
    val screen: String,
    val totalFrames: Long,
    val jankyFrames: Long,
    val severeFrames: Long,
    val maxFrameDurationMillis: Long,
    val totalJankDurationMillis: Long,
)

data class UiJankSessionSummary(
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val totalFrames: Long,
    val jankyFrames: Long,
    val severeFrames: Long,
    val maxFrameDurationMillis: Long,
    val screens: List<UiJankScreenSummary>,
)

class JankSessionAccumulator(
    val startedAtEpochMillis: Long = System.currentTimeMillis(),
) {
    private class Stats {
        val totalFrames = AtomicLong()
        val jankyFrames = AtomicLong()
        val severeFrames = AtomicLong()
        val maxFrameDurationMillis = AtomicLong()
        val totalJankDurationMillis = AtomicLong()
    }

    private val totalFrames = AtomicLong()
    private val jankyFrames = AtomicLong()
    private val severeFrames = AtomicLong()
    private val maxFrameDurationMillis = AtomicLong()
    private val screens = ConcurrentHashMap<String, Stats>()

    fun record(screen: String, durationMillis: Long, isJank: Boolean) {
        val normalizedScreen = screen.take(80).ifBlank { "UNKNOWN" }
        val stats = screens.computeIfAbsent(normalizedScreen) { Stats() }
        totalFrames.incrementAndGet()
        stats.totalFrames.incrementAndGet()
        if (!isJank) return
        jankyFrames.incrementAndGet()
        stats.jankyFrames.incrementAndGet()
        stats.totalJankDurationMillis.addAndGet(durationMillis)
        maxFrameDurationMillis.accumulateAndGet(durationMillis, ::maxOf)
        stats.maxFrameDurationMillis.accumulateAndGet(durationMillis, ::maxOf)
        if (durationMillis >= SEVERE_FRAME_MILLIS) {
            severeFrames.incrementAndGet()
            stats.severeFrames.incrementAndGet()
        }
    }

    fun snapshot(endedAtEpochMillis: Long = System.currentTimeMillis()) = UiJankSessionSummary(
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        totalFrames = totalFrames.get(),
        jankyFrames = jankyFrames.get(),
        severeFrames = severeFrames.get(),
        maxFrameDurationMillis = maxFrameDurationMillis.get(),
        screens = screens.entries.map { (screen, stats) ->
            UiJankScreenSummary(
                screen = screen,
                totalFrames = stats.totalFrames.get(),
                jankyFrames = stats.jankyFrames.get(),
                severeFrames = stats.severeFrames.get(),
                maxFrameDurationMillis = stats.maxFrameDurationMillis.get(),
                totalJankDurationMillis = stats.totalJankDurationMillis.get(),
            )
        }.sortedByDescending(UiJankScreenSummary::jankyFrames),
    )

    private companion object {
        const val SEVERE_FRAME_MILLIS = 100L
    }
}

class UiJankMonitor(
    window: Window,
    private val diagnostics: SupportDiagnosticStore,
) : Closeable {
    private val activeSession = AtomicReference<JankSessionAccumulator?>(null)
    private val jankStats = JankStats.createAndTrack(window) { frame ->
        activeSession.get()?.record(
            screen = UiPerformanceContext.currentScreen,
            durationMillis = frame.frameDurationUiNanos / 1_000_000L,
            isJank = frame.isJank,
        )
    }.apply {
        jankHeuristicMultiplier = 2f
        isTrackingEnabled = false
    }

    fun setForeground(value: Boolean) {
        if (value) {
            activeSession.compareAndSet(null, JankSessionAccumulator())
            jankStats.isTrackingEnabled = true
        } else {
            jankStats.isTrackingEnabled = false
            activeSession.getAndSet(null)?.snapshot()?.let(diagnostics::recordUiJankSession)
        }
    }

    override fun close() {
        setForeground(false)
    }
}

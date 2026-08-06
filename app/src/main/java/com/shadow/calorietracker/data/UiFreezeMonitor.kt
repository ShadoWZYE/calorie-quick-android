package com.shadow.calorietracker.data

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UiFreezeMonitor(
    private val diagnostics: SupportDiagnosticStore,
    private val freezeThresholdMillis: Long = 6_000L,
) : Closeable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scheduler = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "calorie-quick-freeze-monitor").apply { isDaemon = true }
    }
    @Volatile private var foreground = false
    @Volatile private var lastHeartbeat = SystemClock.elapsedRealtime()
    @Volatile private var reportedCurrentStall = false

    private val heartbeat = object : Runnable {
        override fun run() {
            lastHeartbeat = SystemClock.elapsedRealtime()
            reportedCurrentStall = false
            mainHandler.postDelayed(this, HEARTBEAT_INTERVAL_MILLIS)
        }
    }

    init {
        mainHandler.post(heartbeat)
        scheduler.scheduleAtFixedRate(::checkHeartbeat, 2, 2, TimeUnit.SECONDS)
    }

    fun setForeground(value: Boolean) {
        foreground = value
        lastHeartbeat = SystemClock.elapsedRealtime()
        reportedCurrentStall = false
    }

    private fun checkHeartbeat() {
        if (!foreground || reportedCurrentStall) return
        val stalledFor = SystemClock.elapsedRealtime() - lastHeartbeat
        if (stalledFor < freezeThresholdMillis) return
        reportedCurrentStall = true
        val mainThread = Looper.getMainLooper().thread
        diagnostics.recordUiFreeze(
            stalledForMillis = stalledFor,
            threadState = mainThread.state.name,
            stackTrace = mainThread.stackTrace.joinToString("\n"),
        )
    }

    override fun close() {
        foreground = false
        mainHandler.removeCallbacks(heartbeat)
        scheduler.shutdownNow()
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MILLIS = 1_000L
    }
}

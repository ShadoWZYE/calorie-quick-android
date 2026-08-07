package com.shadow.calorietracker

import android.app.Application
import android.os.Process
import com.shadow.calorietracker.data.SupportDiagnosticStore
import com.shadow.calorietracker.data.UiFreezeMonitor

class CalorieQuickApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashReporter(SupportDiagnosticStore(this))
    }

    private fun installCrashReporter(diagnostics: SupportDiagnosticStore) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            UiFreezeMonitor.suppressForCrash()
            runCatching { diagnostics.recordCrash(thread.name, throwable) }
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
            }
        }
    }
}

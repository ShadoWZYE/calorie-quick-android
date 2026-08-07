package com.shadow.calorietracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.shadow.calorietracker.ui.CalorieQuickApp
import com.shadow.calorietracker.ui.theme.CalorieQuickTheme
import com.shadow.calorietracker.data.SupportDiagnosticStore
import com.shadow.calorietracker.data.UiFreezeMonitor
import com.shadow.calorietracker.data.UiJankMonitor

class MainActivity : AppCompatActivity() {
    private lateinit var freezeMonitor: UiFreezeMonitor
    private lateinit var jankMonitor: UiJankMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        freezeMonitor = UiFreezeMonitor(SupportDiagnosticStore(applicationContext))
        enableEdgeToEdge()
        setContent {
            CalorieQuickTheme {
                CalorieQuickApp()
            }
        }
        jankMonitor = UiJankMonitor(window, SupportDiagnosticStore(applicationContext))
    }

    override fun onStart() {
        super.onStart()
        freezeMonitor.setForeground(true)
        jankMonitor.setForeground(true)
    }

    override fun onStop() {
        freezeMonitor.setForeground(false)
        jankMonitor.setForeground(false)
        super.onStop()
    }

    override fun onDestroy() {
        freezeMonitor.close()
        jankMonitor.close()
        super.onDestroy()
    }
}

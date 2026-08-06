package com.shadow.calorietracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.shadow.calorietracker.ui.CalorieQuickApp
import com.shadow.calorietracker.ui.theme.CalorieQuickTheme
import com.shadow.calorietracker.data.SupportDiagnosticStore
import com.shadow.calorietracker.data.UiFreezeMonitor

class MainActivity : AppCompatActivity() {
    private lateinit var freezeMonitor: UiFreezeMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        freezeMonitor = UiFreezeMonitor(SupportDiagnosticStore(applicationContext))
        enableEdgeToEdge()
        setContent {
            CalorieQuickTheme {
                CalorieQuickApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        freezeMonitor.setForeground(true)
    }

    override fun onStop() {
        freezeMonitor.setForeground(false)
        super.onStop()
    }

    override fun onDestroy() {
        freezeMonitor.close()
        super.onDestroy()
    }
}

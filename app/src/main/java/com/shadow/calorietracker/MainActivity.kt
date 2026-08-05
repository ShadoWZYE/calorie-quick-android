package com.shadow.calorietracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.shadow.calorietracker.ui.CalorieQuickApp
import com.shadow.calorietracker.ui.theme.CalorieQuickTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalorieQuickTheme {
                CalorieQuickApp()
            }
        }
    }
}

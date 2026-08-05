package com.shadow.calorietracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF166534),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = Color(0xFF2563EB),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF1F5F1),
    outlineVariant = Color(0xFFDDE5DD),
    error = Color(0xFFB42318),
)

@Composable
fun CalorieQuickTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}


package com.lgu.drive.importation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ModernGovernmentColorScheme = lightColorScheme(
    primary = Color(0xFF0F4C81),      // Official Deep Classic Blue
    secondary = Color(0xFF16A085),    // Contemporary Secure Emerald Accents
    background = Color(0xFFFAFAFA),   // Soft Off-White Background
    surface = Color(0xFFFFFFFF),      // Crisp Pure White Card Surfaces
    onPrimary = Color.White,
    onBackground = Color(0xFF2C3E50), // Deep Charcoal Primary Text
    onSurface = Color(0xFF2C3E50)
)

@Composable
fun LguDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ModernGovernmentColorScheme,
        content = content
    )
}
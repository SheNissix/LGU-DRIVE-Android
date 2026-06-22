package com.lgu.drive.importation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. Colors (Exact Professional Navy & Grey Palette)
private val ProfessionalBlueColorScheme = lightColorScheme(
    primary = Color(0xFF20368F),          // Royal Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF000B4F), // Dark Navy
    secondary = Color(0xFF829CD0),        // Light Blue
    background = Color(0xFFEBEBEB),       // Light Grey App Background
    surface = Color(0xFFFFFFFF),          // Pure White Cards
    onBackground = Color(0xFF323232),     // Dark Grey Main Text
    onSurface = Color(0xFF323232),
    onSurfaceVariant = Color(0xFF6D6D6D), // Medium Grey Subtext
    error = Color(0xFFE11D48)
)

// 2. Typography (Scaled down for a cleaner, less clunky look)
private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 20.sp, // Scaled down
        letterSpacing = 0.5.sp,
        color = Color(0xFF000B4F) // Dark Navy headers
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp, // Scaled down
        letterSpacing = 0.15.sp,
        color = Color(0xFF323232)
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp, // Scaled down
        letterSpacing = 0.25.sp,
        color = Color(0xFF6D6D6D)
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp, // Scaled down
        letterSpacing = 0.5.sp,
        color = Color(0xFF6D6D6D)
    )
)

// 3. Shapes (Slightly sharper corners for a professional feel)
private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), // Text fields
    large = RoundedCornerShape(16.dp)   // Cards
)

@Composable
fun LguDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ProfessionalBlueColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
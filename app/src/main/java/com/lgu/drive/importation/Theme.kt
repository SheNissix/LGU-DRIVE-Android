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

// 1. Colors (Your existing palette)
private val ModernGovernmentColorScheme = lightColorScheme(
    primary = Color(0xFF0F4C81),      // Official Deep Classic Blue
    secondary = Color(0xFF16A085),    // Contemporary Secure Emerald Accents
    background = Color(0xFFFAFAFA),   // Soft Off-White Background
    surface = Color(0xFFFFFFFF),      // Crisp Pure White Card Surfaces
    onPrimary = Color.White,
    onBackground = Color(0xFF2C3E50), // Deep Charcoal Primary Text
    onSurface = Color(0xFF2C3E50)
)

// 2. Typography (For crisp, modern text scaling)
private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 0.15.sp,
        color = Color(0xFF0F4C81) // Automatically colors section titles primary blue
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        color = Color(0xFF2C3E50)
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = Color.Gray
    )
)

// 3. Shapes (To unify the rounded corners across the app)
private val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),   // For small UI elements like checkboxes or badges
    medium = RoundedCornerShape(8.dp),  // Perfect for Buttons and TextFields
    large = RoundedCornerShape(12.dp)   // Perfect for the ElevatedCards we added
)

@Composable
fun LguDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ModernGovernmentColorScheme,
        typography = AppTypography,     // Injects your font styles
        shapes = AppShapes,             // Injects your corner radii
        content = content
    )
}
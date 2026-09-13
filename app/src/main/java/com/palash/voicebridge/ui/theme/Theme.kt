package com.palash.voicebridge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// PALASH VoiceBridge Brand Colors
// Inspired by Jharkhand's forest greens and tribal art
val PalashGreen = Color(0xFF1B5E20)       // Deep forest green
val PalashGreenLight = Color(0xFF4CAF50)  // Medium green
val PalashGreenSurface = Color(0xFFE8F5E9) // Light green surface
val PalashAmber = Color(0xFFFF8F00)       // Warm amber for accents
val PalashAmberLight = Color(0xFFFFB300)  // Light amber
val PalashEarth = Color(0xFF5D4037)       // Earth brown for tribal feel
val PalashSand = Color(0xFFFFF8E1)        // Warm sand background

// Status colors
val OfflineReadyGreen = Color(0xFF2E7D32)
val DemoModeAmber = Color(0xFFF57F17)
val ErrorRed = Color(0xFFB71C1C)
val VerifiedBlue = Color(0xFF1565C0)
val UnverifiedGray = Color(0xFF757575)

private val LightColorScheme = lightColorScheme(
    primary = PalashGreen,
    onPrimary = Color.White,
    primaryContainer = PalashGreenSurface,
    onPrimaryContainer = PalashGreen,
    secondary = PalashAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFF4A3500),
    tertiary = PalashEarth,
    onTertiary = Color.White,
    background = Color(0xFFF9FBF9),
    onBackground = Color(0xFF1A1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFDEE5D8),
    onSurfaceVariant = Color(0xFF424940),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFF72796F)
)

private val DarkColorScheme = darkColorScheme(
    primary = PalashGreenLight,
    onPrimary = Color(0xFF003909),
    primaryContainer = Color(0xFF005313),
    onPrimaryContainer = Color(0xFF95F77B),
    secondary = PalashAmberLight,
    onSecondary = Color(0xFF3E2E00),
    background = Color(0xFF1A1C1A),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C1A),
    onSurface = Color(0xFFE2E3DD)
)

@Composable
fun PalashVoiceBridgeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PalashTypography,
        content = content
    )
}

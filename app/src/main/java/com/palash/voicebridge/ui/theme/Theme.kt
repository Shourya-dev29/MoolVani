package com.palash.voicebridge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================================
// PALASH VoiceBridge — Neutral Educational Design System
// Calm • Professional • Educational • Trustworthy • Accessible
// ============================================================================

// Primary Brand Accent: Deep Scholar Forest Green
val PalashGreen = Color(0xFF1B5E20)           // Primary accent: calm, authoritative
val PalashGreenLight = Color(0xFF2E7D32)      // Slightly lighter forest green
val PalashGreenContainer = Color(0xFFE8F5E9)  // Soft green container
val PalashGreenOnContainer = Color(0xFF0F3E14)// Dark contrast green on container

// Neutral Surfaces & Backgrounds
val SurfaceLight = Color(0xFFFFFFFF)          // Pure white cards
val BackgroundLight = Color(0xFFF8F9FA)       // Neutral soft grey-white background
val SurfaceVariantLight = Color(0xFFF1F3F5)   // Subtle surface background for secondary sections
val OutlineBorderLight = Color(0xFFE2E8F0)    // Crisp, subtle border for clean cards

// Typography Contrast Colors
val TextPrimaryLight = Color(0xFF1A1C1A)      // Deep charcoal (high contrast, >= 7:1)
val TextSecondaryLight = Color(0xFF4A5568)    // Muted slate (contrast >= 4.5:1)
val TextMutedLight = Color(0xFF64748B)        // Subtle metadata text

// Status & Indicator Colors (Muted & Restrained, never neon)
val OfflineReadyGreen = Color(0xFF2E7D32)     // Positive status / fully offline ready
val DemoModeAmber = Color(0xFFB45309)         // Warm amber for review/prototype notes
val ErrorRed = Color(0xFFB91C1C)              // Clear error red
val VerifiedBlue = Color(0xFF1565C0)          // SCERT curriculum verified tag
val MutedSlate = Color(0xFF475569)            // Neutral badge slate

// Secondary Earth Accent (Used sparingly for visual distinction)
val PalashEarth = Color(0xFF5D4037)           // Warm earth brown
val PalashEarthContainer = Color(0xFFEFEBE9)  // Soft earth container

// Backward-compatibility aliases for existing references
val PalashGreenSurface = PalashGreenContainer
val PalashAmber = DemoModeAmber
val PalashAmberLight = Color(0xFFD97706)
val PalashSand = Color(0xFFFFFBEB)
val UnverifiedGray = MutedSlate

private val LightColorScheme = lightColorScheme(
    primary = PalashGreen,
    onPrimary = Color.White,
    primaryContainer = PalashGreenContainer,
    onPrimaryContainer = PalashGreenOnContainer,
    secondary = PalashEarth,
    onSecondary = Color.White,
    secondaryContainer = PalashEarthContainer,
    onSecondaryContainer = PalashEarth,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineBorderLight,
    outlineVariant = Color(0xFFCBD5E1),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFD7CCC8),
    onSecondary = Color(0xFF3E2723),
    background = Color(0xFF121412),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1A1C1A),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF242824),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = Color(0xFFEF5350),
    onError = Color.White
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

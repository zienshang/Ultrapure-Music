package com.ultrapuremusic.core.ui.theme

import androidx.compose.ui.graphics.Color

// Dark palette — primary dark-first theme
val Background = Color(0xFF0A0A0A)
val Surface = Color(0xFF141414)
val CardBackground = Color(0xFF1E1E1E)
val SurfaceVariant = Color(0xFF242424)

// Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9E9E9E)
val TextDisabled = Color(0xFF4A4A4A)

// Accent (default purple — dynamic from album art at runtime)
val AccentPrimary = Color(0xFF7B61FF)
val AccentSecondary = Color(0xFF9D8BFF)
val AccentSubtle = Color(0x337B61FF)

// Glass layer
val GlassBorder = Color(0x1AFFFFFF)   // white 10%
val GlassBackground = Color(0xA6141414) // 65% opacity surface
val GlassHighlight = Color(0x14FFFFFF) // inner glow 8%

// Status
val ErrorColor = Color(0xFFCF6679)
val SuccessColor = Color(0xFF4CAF50)
val WarningColor = Color(0xFFFF9800)

// Ripple
val RippleColor = Color(0x1AFFFFFF)

// Player gradient colors (default; overridden by album art palette)
val PlayerGradientStart = Color(0xFF1A0A2E)
val PlayerGradientEnd = Color(0xFF0A0A0A)

// Material 3 role mappings for dark theme
val md_theme_dark_primary = AccentPrimary
val md_theme_dark_onPrimary = Color(0xFF000000)
val md_theme_dark_primaryContainer = Color(0xFF3A2E6E)
val md_theme_dark_onPrimaryContainer = Color(0xFFE2DAFF)
val md_theme_dark_secondary = AccentSecondary
val md_theme_dark_onSecondary = Color(0xFF000000)
val md_theme_dark_secondaryContainer = Color(0xFF2D2550)
val md_theme_dark_onSecondaryContainer = Color(0xFFD9D0FF)
val md_theme_dark_background = Background
val md_theme_dark_onBackground = TextPrimary
val md_theme_dark_surface = Surface
val md_theme_dark_onSurface = TextPrimary
val md_theme_dark_surfaceVariant = SurfaceVariant
val md_theme_dark_onSurfaceVariant = TextSecondary
val md_theme_dark_error = ErrorColor
val md_theme_dark_onError = Color(0xFF000000)

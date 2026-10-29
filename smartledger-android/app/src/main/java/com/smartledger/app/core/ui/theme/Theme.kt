package com.smartledger.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ============================================================
// Dark Color Scheme — Primary theme for SmartLedger
// A business owner often views this app in bright indoor light;
// dark mode with gold accents reduces eye strain and looks premium.
// ============================================================
private val SmartLedgerDarkColorScheme = darkColorScheme(
    primary = SilkGold80,
    onPrimary = DarkBackground,
    primaryContainer = SilkGoldContainer,
    onPrimaryContainer = OnSilkGoldContainer,

    secondary = WarmMaroon80,
    onSecondary = DarkBackground,
    secondaryContainer = WarmMaroonContainer,
    onSecondaryContainer = OnWarmMaroonContainer,

    tertiary = SilkTeal80,
    onTertiary = DarkBackground,
    tertiaryContainer = SilkTealContainer,
    onTertiaryContainer = OnSilkTealContainer,

    background = DarkBackground,
    onBackground = LightBackground,

    surface = DarkSurface,
    onSurface = LightBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightOutline,

    outline = DarkOutline,
    error = ErrorRed
)

// ============================================================
// Light Color Scheme — Warm ivory / gold for daytime use
// ============================================================
private val SmartLedgerLightColorScheme = lightColorScheme(
    primary = SilkGold40,
    onPrimary = LightBackground,
    primaryContainer = OnSilkGoldContainer,
    onPrimaryContainer = SilkGoldContainer,

    secondary = WarmMaroon40,
    onSecondary = LightBackground,
    secondaryContainer = OnWarmMaroonContainer,
    onSecondaryContainer = WarmMaroonContainer,

    tertiary = SilkTeal40,
    onTertiary = LightBackground,
    tertiaryContainer = OnSilkTealContainer,
    onTertiaryContainer = SilkTealContainer,

    background = LightBackground,
    onBackground = DarkBackground,

    surface = LightSurface,
    onSurface = DarkBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = DarkSurfaceVariant,

    outline = LightOutline,
    error = ErrorRed
)

/**
 * SmartLedger Material3 theme composable.
 *
 * Design decisions:
 * - Dynamic color is DISABLED — we use our custom silk-gold palette consistently
 * - Dark theme is ON by default to match the premium, focused feel of a business tool
 */
@Composable
fun SmartLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        SmartLedgerDarkColorScheme
    } else {
        SmartLedgerLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartLedgerTypography,
        shapes = SmartLedgerShapes,
        content = content
    )
}

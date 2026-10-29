package com.smartledger.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// SmartLedger Silk Warp Color Palette
// Inspired by silk thread: gold, warm ivory, deep maroon
// ============================================================

// Gold / Primary
val SilkGold80 = Color(0xFFE8C878)       // Light variant for dark theme
val SilkGold40 = Color(0xFFC9A84C)       // Primary for light theme
val SilkGoldContainer = Color(0xFF3D2E00)
val OnSilkGoldContainer = Color(0xFFFFDFA1)

// Maroon / Secondary
val WarmMaroon80 = Color(0xFFFFB3B6)
val WarmMaroon40 = Color(0xFF8B2635)
val WarmMaroonContainer = Color(0xFF5C0A17)
val OnWarmMaroonContainer = Color(0xFFFFDADC)

// Tertiary — Deep silk teal
val SilkTeal80 = Color(0xFF9FDBCF)
val SilkTeal40 = Color(0xFF1C8578)
val SilkTealContainer = Color(0xFF003732)
val OnSilkTealContainer = Color(0xFFBBEDE5)

// Dark theme backgrounds
val DarkBackground = Color(0xFF100E00)
val DarkSurface = Color(0xFF1A1600)
val DarkSurfaceVariant = Color(0xFF2E2800)
val DarkOutline = Color(0xFF6B5F00)

// Light theme backgrounds
val LightBackground = Color(0xFFFFFBF0)
val LightSurface = Color(0xFFFFF9E8)
val LightSurfaceVariant = Color(0xFFEFE3C8)
val LightOutline = Color(0xFF7D6B3C)

// Semantic
val SuccessGreen = Color(0xFF2E7D32)
val WarningOrange = Color(0xFFE65100)
val ErrorRed = Color(0xFFB00020)
val InfoBlue = Color(0xFF01579B)

// Status badge colors (match plan specifications)
val StatusPendingOrange = Color(0xFFE65100)   // PENDING_RECEIPT, IN_PROGRESS
val StatusPartialBlue = Color(0xFF0277BD)      // PARTIALLY_RECEIVED, MATERIAL_ISSUED
val StatusCompletedGreen = Color(0xFF2E7D32)  // FULLY_RECEIVED, COMPLETED
val StatusCancelledRed = Color(0xFFB00020)    // CANCELLED
val StatusCreatedGrey = Color(0xFF757575)     // CREATED
val StatusActiveGreen = Color(0xFF2E7D32)     // ACTIVE
val StatusInactiveGrey = Color(0xFF9E9E9E)    // INACTIVE

// Convenient semantic aliases
val GoldPrimary = SilkGold40
val DebitRed = ErrorRed
val CreditGreen = SuccessGreen
val SilkDarkBackground = Color(0xFF0F172A)
val SilkDarkSurface = Color(0xFF1E293B)
val SilkDarkCard = Color(0xFF1E293B)

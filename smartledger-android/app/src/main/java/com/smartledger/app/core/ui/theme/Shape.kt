package com.smartledger.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * SmartLedger shape scale.
 * Uses rounded corners — professional yet approachable for a business app.
 */
val SmartLedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Chips, tooltips
    small = RoundedCornerShape(8.dp),         // Buttons, text fields
    medium = RoundedCornerShape(12.dp),       // Cards, dialogs
    large = RoundedCornerShape(16.dp),        // Bottom sheets, nav drawer
    extraLarge = RoundedCornerShape(28.dp)    // FABs, large cards
)

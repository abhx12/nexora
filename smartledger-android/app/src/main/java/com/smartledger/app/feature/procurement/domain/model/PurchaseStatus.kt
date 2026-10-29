package com.smartledger.app.feature.procurement.domain.model

import androidx.compose.ui.graphics.Color
import com.smartledger.app.core.ui.theme.StatusCancelledRed
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.ui.theme.StatusPendingOrange

/**
 * Purchase order lifecycle status in SmartLedger.
 *
 * Status colors as defined in DEV_B_IMPLEMENTATION_PLAN.md:
 * - PENDING_RECEIPT -> Orange
 * - PARTIALLY_RECEIVED -> Blue
 * - FULLY_RECEIVED -> Green
 * - CANCELLED -> Red
 */
enum class PurchaseStatus(val displayName: String, val color: Color) {
    PENDING_RECEIPT("Pending Receipt", StatusPendingOrange),
    PARTIALLY_RECEIVED("Partially Received", StatusPartialBlue),
    FULLY_RECEIVED("Fully Received", StatusCompletedGreen),
    CANCELLED("Cancelled", StatusCancelledRed);

    companion object {
        fun fromString(status: String): PurchaseStatus {
            return entries.firstOrNull { it.name.equals(status, ignoreCase = true) } ?: PENDING_RECEIPT
        }
    }
}

package com.smartledger.app.feature.orders.domain.model

import androidx.compose.ui.graphics.Color
import com.smartledger.app.core.ui.theme.StatusCancelledRed
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.ui.theme.StatusCreatedGrey
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.ui.theme.StatusPendingOrange

/**
 * Lifecycle status of customer warp orders.
 *
 * Status colors as defined in DEV_B_IMPLEMENTATION_PLAN.md:
 * - CREATED -> Grey (StatusCreatedGrey)
 * - CONFIRMED -> Blue (StatusPartialBlue)
 * - IN_PROGRESS -> Orange (StatusPendingOrange)
 * - COMPLETED -> Green (StatusCompletedGreen)
 * - CANCELLED -> Red (StatusCancelledRed)
 */
enum class OrderStatus(val displayName: String, val color: Color) {
    CREATED("Created", StatusCreatedGrey),
    CONFIRMED("Confirmed", StatusPartialBlue),
    IN_PROGRESS("In Progress", StatusPendingOrange),
    COMPLETED("Completed", StatusCompletedGreen),
    CANCELLED("Cancelled", StatusCancelledRed);

    companion object {
        fun fromString(status: String?): OrderStatus {
            return entries.firstOrNull { it.name.equals(status, ignoreCase = true) } ?: CREATED
        }
    }
}

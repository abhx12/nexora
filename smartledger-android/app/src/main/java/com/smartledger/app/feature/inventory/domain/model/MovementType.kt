package com.smartledger.app.feature.inventory.domain.model

import androidx.compose.ui.graphics.Color
import com.smartledger.app.core.ui.theme.CreditGreen
import com.smartledger.app.core.ui.theme.DebitRed
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.ui.theme.StatusPendingOrange

/**
 * Material Movement Types tracking raw silk, warp yarns and WIP transitions.
 */
enum class MovementType(
    val displayName: String,
    val isIncoming: Boolean,
    val color: Color
) {
    RECEIPT("Material Receipt", true, CreditGreen),
    CONSUMPTION("Production Consumption", false, DebitRed),
    PRODUCTION_OUTPUT("Production Output", true, CreditGreen),
    OUTSOURCE_ISSUE("Outsource Issue", false, StatusPendingOrange),
    OUTSOURCE_RECEIPT("Outsource Return", true, StatusPartialBlue),
    DISPATCH("Order Dispatch", false, DebitRed);

    companion object {
        fun fromString(value: String): MovementType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: RECEIPT
        }
    }
}

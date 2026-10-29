package com.smartledger.app.feature.outsourcing.domain.model

import androidx.compose.ui.graphics.Color
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.ui.theme.StatusCreatedGrey
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.ui.theme.StatusPendingOrange

/**
 * Status lifecycle of an Outsourced Job Work:
 * CREATED -> MATERIAL_ISSUED -> PARTIALLY_RECEIVED -> COMPLETED
 */
enum class OutsourcingStatus(val label: String, val color: Color) {
    CREATED("Created", StatusCreatedGrey),
    MATERIAL_ISSUED("Material Issued", StatusPartialBlue),
    PARTIALLY_RECEIVED("Partially Received", StatusPendingOrange),
    COMPLETED("Completed", StatusCompletedGreen);

    companion object {
        fun fromString(status: String?): OutsourcingStatus {
            return entries.firstOrNull { it.name.equals(status, ignoreCase = true) } ?: CREATED
        }
    }
}

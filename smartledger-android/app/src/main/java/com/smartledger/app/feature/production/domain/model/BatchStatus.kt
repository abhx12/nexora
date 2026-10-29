package com.smartledger.app.feature.production.domain.model

import androidx.compose.ui.graphics.Color
import com.smartledger.app.core.ui.theme.StatusCancelledRed
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.ui.theme.StatusCreatedGrey
import com.smartledger.app.core.ui.theme.StatusPendingOrange

enum class BatchStatus(
    val displayName: String,
    val color: Color
) {
    PLANNED("Planned", StatusCreatedGrey),
    IN_PROGRESS("In Progress", StatusPendingOrange),
    COMPLETED("Completed", StatusCompletedGreen),
    CANCELLED("Cancelled", StatusCancelledRed);

    companion object {
        fun fromString(value: String): BatchStatus {
            return when (value.uppercase()) {
                "PLANNED", "CREATED" -> PLANNED
                "IN_PROGRESS", "RUNNING" -> IN_PROGRESS
                "COMPLETED" -> COMPLETED
                "CANCELLED" -> CANCELLED
                else -> PLANNED
            }
        }
    }
}

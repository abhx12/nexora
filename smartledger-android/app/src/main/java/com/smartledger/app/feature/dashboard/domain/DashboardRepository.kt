package com.smartledger.app.feature.dashboard.domain

import com.smartledger.app.feature.dashboard.data.dto.DashboardSummaryResponse

/**
 * Domain repository contract for Dashboard data operations.
 * Dashboard is strictly READ-ONLY.
 */
interface DashboardRepository {

    suspend fun getDashboardSummary(): Result<DashboardSummaryResponse>
}

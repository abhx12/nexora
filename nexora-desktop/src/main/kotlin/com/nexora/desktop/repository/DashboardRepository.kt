package com.nexora.desktop.repository

import com.nexora.desktop.api.ApiClient
import com.nexora.desktop.api.dto.DashboardResponse

/** Owner dashboard (05 E-RPT-01) and Post-MVP AI Financial Advisory. */
class DashboardRepository(private val api: ApiClient) {
    suspend fun summary(): ApiResult<DashboardResponse> = api.get("/dashboard/summary")
    suspend fun aiAdvisory(): ApiResult<com.nexora.desktop.api.dto.AiAdvisoryResponse> = api.get("/dashboard/ai-advisory")
}

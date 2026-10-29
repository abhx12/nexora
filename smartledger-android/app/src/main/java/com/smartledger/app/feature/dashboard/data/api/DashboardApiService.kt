package com.smartledger.app.feature.dashboard.data.api

import com.smartledger.app.feature.dashboard.data.dto.DashboardSummaryResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * Retrofit API interface for Dashboard endpoints.
 */
interface DashboardApiService {

    @GET("api/v1/dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>
}

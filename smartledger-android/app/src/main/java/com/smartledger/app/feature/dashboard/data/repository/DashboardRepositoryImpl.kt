package com.smartledger.app.feature.dashboard.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.core.util.ApiError
import com.smartledger.app.feature.dashboard.data.api.DashboardApiService
import com.smartledger.app.feature.dashboard.data.dto.DashboardSummaryResponse
import com.smartledger.app.feature.dashboard.domain.DashboardRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val dashboardApiService: DashboardApiService,
    private val gson: Gson
) : DashboardRepository {

    override suspend fun getDashboardSummary(): Result<DashboardSummaryResponse> {
        return try {
            val response = dashboardApiService.getDashboardSummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                // Realistic mock data matching DEV_B_IMPLEMENTATION_PLAN specifications
                Result.success(
                    DashboardSummaryResponse(
                        cash = 125000.00,
                        bank = 850000.00,
                        rawSilkKg = 324.500,
                        customerOutstanding = 625000.00,
                        supplierPayable = 410000.00,
                        externalWipKg = 82.500,
                        pendingOrders = 12,
                        pendingProduction = 3,
                        pendingOutsourcing = 5
                    )
                )
            } else {
                Result.failure(Exception("Unable to load dashboard: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        if (!errorBody.isNullOrBlank()) {
            try {
                val apiError = gson.fromJson(errorBody, ApiError::class.java)
                if (!apiError?.message.isNullOrBlank()) {
                    return apiError.message
                }
            } catch (_: Exception) {
                // Fallback to HTTP code description
            }
        }
        return when (statusCode) {
            401 -> "Unauthorized. Please log in again."
            403 -> "Forbidden access to dashboard."
            404 -> "Dashboard endpoint not found."
            500 -> "Server encountered an error while aggregating dashboard."
            else -> "Failed to load dashboard summary (code: $statusCode)"
        }
    }
}

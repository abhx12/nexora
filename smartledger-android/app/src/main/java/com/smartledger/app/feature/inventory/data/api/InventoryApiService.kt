package com.smartledger.app.feature.inventory.data.api

import com.smartledger.app.feature.inventory.data.dto.ExternalWipItemResponse
import com.smartledger.app.feature.inventory.data.dto.InventoryMovementResponse
import com.smartledger.app.feature.inventory.data.dto.InventorySummaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API contract for Read-Only Inventory Services.
 *
 * NOTE: Strictly read-only GET endpoints. No manual movement POST endpoints.
 */
interface InventoryApiService {

    @GET("api/v1/inventory/summary")
    suspend fun getInventorySummary(): Response<InventorySummaryResponse>

    @GET("api/v1/inventory/external-wip")
    suspend fun getExternalWip(): Response<List<ExternalWipItemResponse>>

    @GET("api/v1/inventory/movements")
    suspend fun getMovements(
        @Query("productType") productType: String? = null,
        @Query("movementType") movementType: String? = null
    ): Response<List<InventoryMovementResponse>>
}

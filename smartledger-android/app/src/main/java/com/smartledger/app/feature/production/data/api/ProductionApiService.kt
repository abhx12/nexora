package com.smartledger.app.feature.production.data.api

import com.smartledger.app.feature.production.data.dto.CompleteProductionRequest
import com.smartledger.app.feature.production.data.dto.CreateProductionBatchRequest
import com.smartledger.app.feature.production.data.dto.ProductionBatchResponse
import com.smartledger.app.feature.production.data.dto.StartProductionRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductionApiService {

    @GET("/api/v1/production-batches")
    suspend fun getProductionBatches(
        @Query("status") status: String? = null
    ): List<ProductionBatchResponse>

    @GET("/api/v1/production-batches/{id}")
    suspend fun getProductionBatchById(
        @Path("id") id: String
    ): ProductionBatchResponse

    @POST("/api/v1/production-batches")
    suspend fun createProductionBatch(
        @Body request: CreateProductionBatchRequest
    ): ProductionBatchResponse

    @POST("/api/v1/production-batches/{id}/start")
    suspend fun startProduction(
        @Path("id") id: String,
        @Body request: StartProductionRequest = StartProductionRequest()
    ): ProductionBatchResponse

    @POST("/api/v1/production-batches/{id}/complete")
    suspend fun completeProduction(
        @Path("id") id: String,
        @Body request: CompleteProductionRequest
    ): ProductionBatchResponse
}

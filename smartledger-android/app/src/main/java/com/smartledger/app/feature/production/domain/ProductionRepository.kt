package com.smartledger.app.feature.production.domain

import com.smartledger.app.feature.production.data.dto.CompleteProductionRequest
import com.smartledger.app.feature.production.data.dto.CreateProductionBatchRequest
import com.smartledger.app.feature.production.data.dto.ProductionBatchResponse

interface ProductionRepository {
    suspend fun getProductionBatches(status: String? = null): Result<List<ProductionBatchResponse>>
    suspend fun getProductionBatchById(id: String): Result<ProductionBatchResponse>
    suspend fun createProductionBatch(request: CreateProductionBatchRequest): Result<ProductionBatchResponse>
    suspend fun startProduction(id: String, startDate: String? = null, notes: String? = null): Result<ProductionBatchResponse>
    suspend fun completeProduction(id: String, request: CompleteProductionRequest): Result<ProductionBatchResponse>
    suspend fun getAvailableRawSilkKg(): Double
}

package com.smartledger.app.feature.production.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.core.util.ApiError
import com.smartledger.app.core.util.ApiErrorCode
import com.smartledger.app.feature.production.data.api.ProductionApiService
import com.smartledger.app.feature.production.data.dto.CompleteProductionRequest
import com.smartledger.app.feature.production.data.dto.CreateProductionBatchRequest
import com.smartledger.app.feature.production.data.dto.ProductionBatchResponse
import com.smartledger.app.feature.production.data.dto.StartProductionRequest
import com.smartledger.app.feature.production.domain.ProductionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class ProductionRepositoryImpl @Inject constructor(
    private val apiService: ProductionApiService,
    private val gson: Gson
) : ProductionRepository {

    // Available raw silk in kg (matches dashboard KPI of 324.500 kg)
    private var availableRawSilkKg: Double = 324.500

    private val mockBatches = mutableListOf(
        ProductionBatchResponse(
            id = "batch-1",
            batchNumber = "BATCH-2026-0001",
            orderId = "order-1",
            orderNumber = "ORD-2026-0001",
            customerName = "Sri Lakshmi Silks",
            productType = "SILK_WARP",
            status = "COMPLETED",
            inputWeightKg = 100.0,
            outputWeightKg = 96.5,
            wastageWeightKg = 3.5,
            startDate = "2026-10-10",
            completionDate = "2026-10-12",
            notes = "Standard 8-ply warp batch. Excellent yield.",
            createdAt = "2026-10-10T09:00:00Z"
        ),
        ProductionBatchResponse(
            id = "batch-2",
            batchNumber = "BATCH-2026-0002",
            orderId = "order-2",
            orderNumber = "ORD-2026-0002",
            customerName = "Venkateshwara Tex",
            productType = "KORA_WARP",
            status = "IN_PROGRESS",
            inputWeightKg = 85.0,
            outputWeightKg = null,
            wastageWeightKg = null,
            startDate = "2026-10-18",
            completionDate = null,
            notes = "Winding stage completed. Warping underway.",
            createdAt = "2026-10-18T10:30:00Z"
        ),
        ProductionBatchResponse(
            id = "batch-3",
            batchNumber = "BATCH-2026-0003",
            orderId = null,
            orderNumber = null,
            customerName = null,
            productType = "VUDA_WARP",
            status = "PLANNED",
            inputWeightKg = 50.0,
            outputWeightKg = null,
            wastageWeightKg = null,
            startDate = "2026-10-22",
            completionDate = null,
            notes = "Reserved batch for seasonal inventory.",
            createdAt = "2026-10-19T14:15:00Z"
        )
    )

    override suspend fun getAvailableRawSilkKg(): Double {
        return availableRawSilkKg
    }

    override suspend fun getProductionBatches(status: String?): Result<List<ProductionBatchResponse>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val filtered = if (status.isNullOrBlank() || status.equals("ALL", ignoreCase = true)) {
                    mockBatches.toList()
                } else {
                    mockBatches.filter { it.status.equals(status, ignoreCase = true) }
                }
                return@withContext Result.success(filtered)
            }

            try {
                val response = apiService.getProductionBatches(status)
                Result.success(response)
            } catch (e: Exception) {
                handleException(e)
            }
        }

    override suspend fun getProductionBatchById(id: String): Result<ProductionBatchResponse> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.USE_MOCK) {
                val batch = mockBatches.find { it.id == id }
                return@withContext if (batch != null) {
                    Result.success(batch)
                } else {
                    Result.failure(Exception("Production batch not found with id: $id"))
                }
            }

            try {
                val response = apiService.getProductionBatchById(id)
                Result.success(response)
            } catch (e: Exception) {
                handleException(e)
            }
        }

    override suspend fun createProductionBatch(request: CreateProductionBatchRequest): Result<ProductionBatchResponse> =
        withContext(Dispatchers.IO) {
            if (request.inputWeightKg <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Input weight must be greater than 0 kg"))
            }

            if (BuildConfig.USE_MOCK) {
                // Stock check
                if (request.inputWeightKg > availableRawSilkKg) {
                    val errMsg = "Not enough raw silk (%.3f kg needed, %.3f kg available)".format(
                        request.inputWeightKg,
                        availableRawSilkKg
                    )
                    return@withContext Result.failure(Exception(errMsg))
                }

                val nextNumber = mockBatches.size + 1
                val batchNumber = "BATCH-2026-%04d".format(nextNumber)
                val newBatch = ProductionBatchResponse(
                    id = UUID.randomUUID().toString(),
                    batchNumber = batchNumber,
                    orderId = request.orderId,
                    orderNumber = if (!request.orderId.isNullOrBlank()) "ORD-2026-%04d".format(nextNumber) else null,
                    customerName = if (!request.orderId.isNullOrBlank()) "Linked Customer" else null,
                    productType = request.productType,
                    status = "PLANNED",
                    inputWeightKg = request.inputWeightKg,
                    outputWeightKg = null,
                    wastageWeightKg = null,
                    startDate = request.startDate,
                    completionDate = null,
                    notes = request.notes,
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                )

                mockBatches.add(0, newBatch)
                return@withContext Result.success(newBatch)
            }

            try {
                val response = apiService.createProductionBatch(request)
                Result.success(response)
            } catch (e: Exception) {
                handleException(e)
            }
        }

    override suspend fun startProduction(
        id: String,
        startDate: String?,
        notes: String?
    ): Result<ProductionBatchResponse> = withContext(Dispatchers.IO) {
        if (BuildConfig.USE_MOCK) {
            val index = mockBatches.indexOfFirst { it.id == id }
            if (index == -1) {
                return@withContext Result.failure(Exception("Production batch not found"))
            }

            val current = mockBatches[index]
            if (current.status != "PLANNED") {
                return@withContext Result.failure(Exception("Cannot start a batch with status: ${current.status}"))
            }

            // Verify raw silk inventory availability before moving into production
            if (current.inputWeightKg > availableRawSilkKg) {
                val errMsg = "Not enough raw silk (%.3f kg needed, %.3f kg available)".format(
                    current.inputWeightKg,
                    availableRawSilkKg
                )
                return@withContext Result.failure(Exception(errMsg))
            }

            // Deduct raw silk from available inventory
            availableRawSilkKg -= current.inputWeightKg

            val effectiveDate = startDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val updated = current.copy(
                status = "IN_PROGRESS",
                startDate = effectiveDate,
                notes = if (!notes.isNullOrBlank()) notes else current.notes
            )
            mockBatches[index] = updated
            return@withContext Result.success(updated)
        }

        try {
            val response = apiService.startProduction(id, StartProductionRequest(startDate, notes))
            Result.success(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override suspend fun completeProduction(
        id: String,
        request: CompleteProductionRequest
    ): Result<ProductionBatchResponse> = withContext(Dispatchers.IO) {
        if (request.outputWeightKg < 0 || request.wastageWeightKg < 0) {
            return@withContext Result.failure(IllegalArgumentException("Output and wastage weights must be non-negative"))
        }

        if (BuildConfig.USE_MOCK) {
            val index = mockBatches.indexOfFirst { it.id == id }
            if (index == -1) {
                return@withContext Result.failure(Exception("Production batch not found"))
            }

            val current = mockBatches[index]
            if (current.status != "IN_PROGRESS") {
                return@withContext Result.failure(Exception("Only IN_PROGRESS batches can be completed"))
            }

            // Validate equation: Input = Output + Wastage
            val diff = abs(current.inputWeightKg - (request.outputWeightKg + request.wastageWeightKg))
            if (diff > 0.005) {
                val errMsg = "Input weight (%.3f kg) must equal Output (%.3f kg) + Wastage (%.3f kg)".format(
                    current.inputWeightKg,
                    request.outputWeightKg,
                    request.wastageWeightKg
                )
                return@withContext Result.failure(IllegalArgumentException(errMsg))
            }

            val updated = current.copy(
                status = "COMPLETED",
                outputWeightKg = request.outputWeightKg,
                wastageWeightKg = request.wastageWeightKg,
                completionDate = request.completionDate,
                notes = if (!request.notes.isNullOrBlank()) request.notes else current.notes
            )
            mockBatches[index] = updated
            return@withContext Result.success(updated)
        }

        try {
            val response = apiService.completeProduction(id, request)
            Result.success(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun <T> handleException(e: Exception): Result<T> {
        return when (e) {
            is HttpException -> {
                val errorBody = e.response()?.errorBody()?.string()
                val message = parseErrorMessage(errorBody, e.code())
                Result.failure(Exception(message, e))
            }
            is IOException -> {
                Result.failure(Exception("Network error. Please check your internet connection.", e))
            }
            else -> {
                Result.failure(e)
            }
        }
    }

    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        if (!errorBody.isNullOrBlank()) {
            try {
                val apiError = gson.fromJson(errorBody, ApiError::class.java)
                if (apiError?.code == ApiErrorCode.INSUFFICIENT_INVENTORY) {
                    return apiError.message.ifBlank { "Not enough raw silk available in inventory." }
                }
                if (!apiError?.message.isNullOrBlank()) {
                    return apiError.message
                }
            } catch (_: Exception) {
            }
        }
        return when (statusCode) {
            400 -> "Invalid production batch data"
            404 -> "Production batch not found"
            409 -> "Not enough raw silk or invalid production batch state"
            else -> "Production request failed (code: $statusCode)"
        }
    }
}

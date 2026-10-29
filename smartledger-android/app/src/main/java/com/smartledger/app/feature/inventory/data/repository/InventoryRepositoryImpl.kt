package com.smartledger.app.feature.inventory.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.core.util.ApiError
import com.smartledger.app.feature.inventory.data.api.InventoryApiService
import com.smartledger.app.feature.inventory.data.dto.ExternalWipItemResponse
import com.smartledger.app.feature.inventory.data.dto.InventoryMovementResponse
import com.smartledger.app.feature.inventory.data.dto.InventorySummaryResponse
import com.smartledger.app.feature.inventory.domain.InventoryRepository
import com.smartledger.app.feature.inventory.domain.model.MovementType
import com.smartledger.app.feature.inventory.domain.model.ProductType
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val inventoryApiService: InventoryApiService,
    private val gson: Gson
) : InventoryRepository {

    // In-memory mock summary
    private var mockSummary = InventorySummaryResponse(
        rawSilkKg = 324.500,
        koraWarpKg = 85.200,
        silkWarpKg = 42.750,
        vudaWarpKg = 31.000,
        externalWipKg = 82.500
    )

    // In-memory mock external WIP
    private val mockExternalWip = CopyOnWriteArrayList<ExternalWipItemResponse>().apply {
        addAll(
            listOf(
                ExternalWipItemResponse(
                    id = "wip-1",
                    manufacturerId = "mfg-1",
                    manufacturerName = "Sri Balaji Twisters & Warpers",
                    productType = "KORA_WARP",
                    quantityKg = 52.500,
                    jobId = "JOB-2026-0012",
                    jobTitle = "Twisting 28/30 denier silk",
                    issueDate = "2026-10-14"
                ),
                ExternalWipItemResponse(
                    id = "wip-2",
                    manufacturerId = "mfg-2",
                    manufacturerName = "Chamundeshwari Silk Sizers",
                    productType = "SILK_WARP",
                    quantityKg = 30.000,
                    jobId = "JOB-2026-0015",
                    jobTitle = "Sizing and beam preparation",
                    issueDate = "2026-10-17"
                )
            )
        )
    }

    // In-memory mock material movements
    private val mockMovements = CopyOnWriteArrayList<InventoryMovementResponse>().apply {
        addAll(
            listOf(
                InventoryMovementResponse(
                    id = "mov-1",
                    movementNumber = "MOV-2026-0101",
                    productType = "RAW_SILK",
                    movementType = "RECEIPT",
                    quantityKg = 100.000,
                    fromLocation = "Karnataka Silk Filatures Ltd",
                    toLocation = "Main Godown (Bin A1)",
                    movementDate = "2026-10-12",
                    referenceType = "PURCHASE",
                    referenceId = "PUR-2026-0001",
                    notes = "Accepted delivery batch REC-001"
                ),
                InventoryMovementResponse(
                    id = "mov-2",
                    movementNumber = "MOV-2026-0102",
                    productType = "RAW_SILK",
                    movementType = "OUTSOURCE_ISSUE",
                    quantityKg = 52.500,
                    fromLocation = "Main Godown (Bin A1)",
                    toLocation = "Sri Balaji Twisters & Warpers",
                    movementDate = "2026-10-14",
                    referenceType = "OUTSOURCING",
                    referenceId = "JOB-2026-0012",
                    notes = "Dispatched for 2-ply twisting"
                ),
                InventoryMovementResponse(
                    id = "mov-3",
                    movementNumber = "MOV-2026-0103",
                    productType = "RAW_SILK",
                    movementType = "CONSUMPTION",
                    quantityKg = 40.000,
                    fromLocation = "Main Godown (Bin A1)",
                    toLocation = "In-House Warping Unit",
                    movementDate = "2026-10-15",
                    referenceType = "PRODUCTION",
                    referenceId = "BATCH-2026-0044",
                    notes = "Warp beam setup for order ORD-1002"
                ),
                InventoryMovementResponse(
                    id = "mov-4",
                    movementNumber = "MOV-2026-0104",
                    productType = "SILK_WARP",
                    movementType = "PRODUCTION_OUTPUT",
                    quantityKg = 38.500,
                    fromLocation = "In-House Warping Unit",
                    toLocation = "Finished Warp Rack (Rack B)",
                    movementDate = "2026-10-16",
                    referenceType = "PRODUCTION",
                    referenceId = "BATCH-2026-0044",
                    notes = "Completed warp beam 60s x 40s (1.5 kg standard wastage)"
                ),
                InventoryMovementResponse(
                    id = "mov-5",
                    movementNumber = "MOV-2026-0105",
                    productType = "SILK_WARP",
                    movementType = "DISPATCH",
                    quantityKg = 38.500,
                    fromLocation = "Finished Warp Rack (Rack B)",
                    toLocation = "Ramesh Silks (Customer)",
                    movementDate = "2026-10-18",
                    referenceType = "ORDER",
                    referenceId = "ORD-2026-1002",
                    notes = "Dispatched against invoice INV-2026-088"
                )
            )
        )
    }

    override suspend fun getInventorySummary(): Result<InventorySummaryResponse> {
        return try {
            val response = inventoryApiService.getInventorySummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                Result.success(mockSummary)
            } else {
                Result.failure(Exception("Unable to connect to server: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExternalWip(): Result<List<ExternalWipItemResponse>> {
        return try {
            val response = inventoryApiService.getExternalWip()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                Result.success(mockExternalWip.toList())
            } else {
                Result.failure(Exception("Unable to connect to server: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMovements(
        productType: ProductType?,
        movementType: MovementType?
    ): Result<List<InventoryMovementResponse>> {
        return try {
            val response = inventoryApiService.getMovements(
                productType = productType?.name,
                movementType = movementType?.name
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                var list = mockMovements.toList()
                if (productType != null) {
                    list = list.filter { it.productType.equals(productType.name, ignoreCase = true) }
                }
                if (movementType != null) {
                    list = list.filter { it.movementType.equals(movementType.name, ignoreCase = true) }
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Unable to connect to server: ${e.localizedMessage ?: "Network error"}"))
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
            }
        }
        return when (statusCode) {
            404 -> "Inventory data not found"
            409 -> "Inventory state conflict"
            else -> "Failed to load inventory data (code: $statusCode)"
        }
    }
}

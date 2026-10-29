package com.smartledger.app.feature.procurement.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.core.util.ApiError
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.procurement.data.api.PurchaseApiService
import com.smartledger.app.feature.procurement.data.dto.CreatePurchaseRequest
import com.smartledger.app.feature.procurement.data.dto.MaterialReceiptResponse
import com.smartledger.app.feature.procurement.data.dto.PurchaseResponse
import com.smartledger.app.feature.procurement.data.dto.RecordReceiptRequest
import com.smartledger.app.feature.procurement.domain.PurchaseRepository
import com.smartledger.app.feature.procurement.domain.model.PurchaseStatus
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRepositoryImpl @Inject constructor(
    private val purchaseApiService: PurchaseApiService,
    private val partiesRepository: PartiesRepository,
    private val gson: Gson
) : PurchaseRepository {

    // Thread-safe in-memory cache / mock data for standalone development
    private val mockPurchases = CopyOnWriteArrayList<PurchaseResponse>().apply {
        addAll(
            listOf(
                PurchaseResponse(
                    id = "pur-1",
                    purchaseNumber = "PUR-2026-0001",
                    supplierId = "supp-1",
                    supplierName = "Karnataka Silk Filatures Ltd",
                    purchaseDate = "2026-10-10",
                    orderedWeightKg = 250.000,
                    ratePerKg = 4250.00,
                    paymentType = "CREDIT",
                    creditDays = 30,
                    status = "PARTIALLY_RECEIVED",
                    receivedWeightKg = 100.000,
                    notes = "Grade A CSR raw silk cocoons",
                    receipts = listOf(
                        MaterialReceiptResponse(
                            id = "rec-1",
                            purchaseId = "pur-1",
                            receiptNumber = "REC-001",
                            receiptDate = "2026-10-12",
                            receivedWeightKg = 101.500,
                            acceptedWeightKg = 100.000,
                            rejectedWeightKg = 1.500,
                            notes = "Moisture dampness in rejected fraction"
                        )
                    )
                ),
                PurchaseResponse(
                    id = "pur-2",
                    purchaseNumber = "PUR-2026-0002",
                    supplierId = "supp-2",
                    supplierName = "Venkateshwara Silk Reelers",
                    purchaseDate = "2026-10-15",
                    orderedWeightKg = 150.000,
                    ratePerKg = 4300.00,
                    paymentType = "CASH",
                    creditDays = null,
                    status = "PENDING_RECEIPT",
                    receivedWeightKg = 0.0,
                    notes = "Express consignment for festival warp orders",
                    receipts = emptyList()
                ),
                PurchaseResponse(
                    id = "pur-3",
                    purchaseNumber = "PUR-2026-0003",
                    supplierId = "supp-1",
                    supplierName = "Karnataka Silk Filatures Ltd",
                    purchaseDate = "2026-09-28",
                    orderedWeightKg = 200.000,
                    ratePerKg = 4200.00,
                    paymentType = "CREDIT",
                    creditDays = 15,
                    status = "FULLY_RECEIVED",
                    receivedWeightKg = 200.000,
                    notes = "Completed consignment",
                    receipts = listOf(
                        MaterialReceiptResponse(
                            id = "rec-2",
                            purchaseId = "pur-3",
                            receiptNumber = "REC-002",
                            receiptDate = "2026-10-02",
                            receivedWeightKg = 200.000,
                            acceptedWeightKg = 200.000,
                            rejectedWeightKg = 0.0,
                            notes = "Full batch accepted"
                        )
                    )
                )
            )
        )
    }

    override suspend fun getPurchases(
        supplierId: String?,
        status: PurchaseStatus?
    ): Result<List<PurchaseResponse>> {
        return try {
            val response = purchaseApiService.getPurchases(
                supplierId = supplierId,
                status = status?.name
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                var list = mockPurchases.toList()
                if (!supplierId.isNullOrBlank()) {
                    list = list.filter { it.supplierId == supplierId }
                }
                if (status != null) {
                    list = list.filter { it.status.equals(status.name, ignoreCase = true) }
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Unable to connect to server: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPurchaseById(id: String): Result<PurchaseResponse> {
        return try {
            val response = purchaseApiService.getPurchaseById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val found = mockPurchases.firstOrNull { it.id == id || it.purchaseNumber.equals(id, ignoreCase = true) }
                if (found != null) {
                    Result.success(found)
                } else {
                    Result.failure(Exception("Purchase not found"))
                }
            } else {
                Result.failure(Exception("Unable to fetch purchase details: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPurchase(request: CreatePurchaseRequest): Result<PurchaseResponse> {
        if (request.orderedWeightKg <= 0) {
            return Result.failure(IllegalArgumentException("Ordered weight must be greater than 0"))
        }
        if (request.ratePerKg <= 0) {
            return Result.failure(IllegalArgumentException("Rate per kg must be greater than 0"))
        }
        if (request.paymentType.equals("CREDIT", ignoreCase = true) && (request.creditDays == null || request.creditDays < 0)) {
            return Result.failure(IllegalArgumentException("Credit days must be >= 0 for credit purchases"))
        }

        return try {
            val response = purchaseApiService.createPurchase(request)
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!
                mockPurchases.add(0, created)
                Result.success(created)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val supplierName = partiesRepository.getPartyById(request.supplierId)
                    .getOrNull()?.name ?: "Silk Supplier"
                val purchaseNum = "PUR-2026-%04d".format(mockPurchases.size + 1)

                val created = PurchaseResponse(
                    id = UUID.randomUUID().toString(),
                    purchaseNumber = purchaseNum,
                    supplierId = request.supplierId,
                    supplierName = supplierName,
                    purchaseDate = request.purchaseDate,
                    orderedWeightKg = request.orderedWeightKg,
                    ratePerKg = request.ratePerKg,
                    paymentType = request.paymentType,
                    creditDays = request.creditDays,
                    status = "PENDING_RECEIPT",
                    receivedWeightKg = 0.0,
                    notes = request.notes,
                    receipts = emptyList()
                )
                mockPurchases.add(0, created)
                Result.success(created)
            } else {
                Result.failure(Exception("Unable to create purchase: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordReceipt(
        purchaseId: String,
        request: RecordReceiptRequest
    ): Result<MaterialReceiptResponse> {
        if (request.receivedWeightKg <= 0) {
            return Result.failure(IllegalArgumentException("Received weight must be greater than 0"))
        }
        if (request.acceptedWeightKg < 0 || request.rejectedWeightKg < 0) {
            return Result.failure(IllegalArgumentException("Weights cannot be negative"))
        }
        // UI RULE: accepted + rejected must be <= received
        if (request.acceptedWeightKg + request.rejectedWeightKg > request.receivedWeightKg + 0.001) {
            return Result.failure(IllegalArgumentException("Accepted weight + rejected weight cannot exceed received weight"))
        }

        return try {
            val response = purchaseApiService.recordReceipt(purchaseId, request)
            if (response.isSuccessful && response.body() != null) {
                val receipt = response.body()!!
                updateMockPurchaseReceipt(purchaseId, receipt)
                Result.success(receipt)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val receipt = MaterialReceiptResponse(
                    id = UUID.randomUUID().toString(),
                    purchaseId = purchaseId,
                    receiptNumber = "REC-%03d".format((1..999).random()),
                    receiptDate = request.receiptDate,
                    receivedWeightKg = request.receivedWeightKg,
                    acceptedWeightKg = request.acceptedWeightKg,
                    rejectedWeightKg = request.rejectedWeightKg,
                    notes = request.notes
                )
                updateMockPurchaseReceipt(purchaseId, receipt)
                Result.success(receipt)
            } else {
                Result.failure(Exception("Unable to record receipt: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun updateMockPurchaseReceipt(purchaseId: String, receipt: MaterialReceiptResponse) {
        val idx = mockPurchases.indexOfFirst { it.id == purchaseId || it.purchaseNumber.equals(purchaseId, ignoreCase = true) }
        if (idx != -1) {
            val current = mockPurchases[idx]
            // Note: Only accepted weight enters inventory and counts towards fulfilled order quantity
            val newReceived = current.receivedWeightKg + receipt.acceptedWeightKg
            val newStatus = if (newReceived >= current.orderedWeightKg - 0.001) {
                "FULLY_RECEIVED"
            } else {
                "PARTIALLY_RECEIVED"
            }
            mockPurchases[idx] = current.copy(
                receivedWeightKg = newReceived,
                status = newStatus,
                receipts = current.receipts + receipt
            )
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
            400 -> "Invalid purchase or receipt data submitted"
            404 -> "Purchase record not found"
            409 -> "Receipt quantity exceeds purchase order balance"
            else -> "Purchase operation failed (code: $statusCode)"
        }
    }
}

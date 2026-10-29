package com.smartledger.app.feature.procurement.domain

import com.smartledger.app.feature.procurement.data.dto.CreatePurchaseRequest
import com.smartledger.app.feature.procurement.data.dto.MaterialReceiptResponse
import com.smartledger.app.feature.procurement.data.dto.PurchaseResponse
import com.smartledger.app.feature.procurement.data.dto.RecordReceiptRequest
import com.smartledger.app.feature.procurement.domain.model.PurchaseStatus

/**
 * Domain repository contract for Silk Raw Material Procurement.
 */
interface PurchaseRepository {

    suspend fun getPurchases(
        supplierId: String? = null,
        status: PurchaseStatus? = null
    ): Result<List<PurchaseResponse>>

    suspend fun getPurchaseById(id: String): Result<PurchaseResponse>

    suspend fun createPurchase(request: CreatePurchaseRequest): Result<PurchaseResponse>

    suspend fun recordReceipt(
        purchaseId: String,
        request: RecordReceiptRequest
    ): Result<MaterialReceiptResponse>
}

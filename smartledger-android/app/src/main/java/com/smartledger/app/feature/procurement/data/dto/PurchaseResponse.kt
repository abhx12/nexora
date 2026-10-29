package com.smartledger.app.feature.procurement.data.dto

import com.google.gson.annotations.SerializedName
import com.smartledger.app.feature.procurement.domain.model.PurchaseStatus

/**
 * Purchase response DTO.
 *
 * All weight values are in kg (3 decimal places).
 * Rate is in INR per kg.
 */
data class PurchaseResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("purchaseNumber")
    val purchaseNumber: String,

    @SerializedName("supplierId")
    val supplierId: String,

    @SerializedName("supplierName")
    val supplierName: String,

    @SerializedName("purchaseDate")
    val purchaseDate: String,

    @SerializedName("orderedWeightKg")
    val orderedWeightKg: Double,

    @SerializedName("ratePerKg")
    val ratePerKg: Double,

    @SerializedName("totalAmount")
    val totalAmount: Double = orderedWeightKg * ratePerKg,

    @SerializedName("paymentType")
    val paymentType: String = "CASH",

    @SerializedName("creditDays")
    val creditDays: Int? = null,

    @SerializedName("status")
    val status: String = "PENDING_RECEIPT",

    @SerializedName("receivedWeightKg")
    val receivedWeightKg: Double = 0.0,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("receipts")
    val receipts: List<MaterialReceiptResponse> = emptyList()
) {
    val remainingWeightKg: Double
        get() = (orderedWeightKg - receivedWeightKg).coerceAtLeast(0.0)

    val purchaseStatus: PurchaseStatus
        get() = PurchaseStatus.fromString(status)

    val isFullyReceived: Boolean
        get() = status.equals("FULLY_RECEIVED", ignoreCase = true) || remainingWeightKg <= 0.0001

    val isCancelled: Boolean
        get() = status.equals("CANCELLED", ignoreCase = true)
}

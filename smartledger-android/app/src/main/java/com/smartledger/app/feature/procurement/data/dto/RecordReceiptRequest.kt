package com.smartledger.app.feature.procurement.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload for POST /api/v1/purchases/{purchaseId}/receipts.
 *
 * Rules:
 * - receivedWeightKg > 0
 * - acceptedWeightKg >= 0
 * - rejectedWeightKg >= 0
 * - UI RULE: acceptedWeightKg + rejectedWeightKg <= receivedWeightKg
 * - Only acceptedWeightKg enters inventory.
 */
data class RecordReceiptRequest(
    @SerializedName("receiptDate")
    val receiptDate: String,

    @SerializedName("receivedWeightKg")
    val receivedWeightKg: Double,

    @SerializedName("acceptedWeightKg")
    val acceptedWeightKg: Double,

    @SerializedName("rejectedWeightKg")
    val rejectedWeightKg: Double,

    @SerializedName("notes")
    val notes: String? = null
)

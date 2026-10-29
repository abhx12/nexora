package com.smartledger.app.feature.procurement.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload for POST /api/v1/purchases.
 *
 * Rules:
 * - orderedWeightKg > 0
 * - ratePerKg > 0
 * - paymentType: CASH or CREDIT
 * - creditDays: required and >= 0 if paymentType == CREDIT
 */
data class CreatePurchaseRequest(
    @SerializedName("supplierId")
    val supplierId: String,

    @SerializedName("purchaseDate")
    val purchaseDate: String,

    @SerializedName("orderedWeightKg")
    val orderedWeightKg: Double,

    @SerializedName("ratePerKg")
    val ratePerKg: Double,

    @SerializedName("paymentType")
    val paymentType: String = "CASH",

    @SerializedName("creditDays")
    val creditDays: Int? = null,

    @SerializedName("notes")
    val notes: String? = null
)

package com.smartledger.app.feature.procurement.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Representation of a single recorded material delivery receipt against a purchase.
 */
data class MaterialReceiptResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("purchaseId")
    val purchaseId: String,

    @SerializedName("receiptNumber")
    val receiptNumber: String,

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

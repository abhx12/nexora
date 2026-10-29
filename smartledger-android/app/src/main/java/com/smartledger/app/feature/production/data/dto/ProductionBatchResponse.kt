package com.smartledger.app.feature.production.data.dto

import com.google.gson.annotations.SerializedName

data class ProductionBatchResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("batchNumber")
    val batchNumber: String,

    @SerializedName("orderId")
    val orderId: String? = null,

    @SerializedName("orderNumber")
    val orderNumber: String? = null,

    @SerializedName("customerName")
    val customerName: String? = null,

    @SerializedName("productType")
    val productType: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("inputWeightKg")
    val inputWeightKg: Double,

    @SerializedName("outputWeightKg")
    val outputWeightKg: Double? = null,

    @SerializedName("wastageWeightKg")
    val wastageWeightKg: Double? = null,

    @SerializedName("startDate")
    val startDate: String? = null,

    @SerializedName("completionDate")
    val completionDate: String? = null,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null
) {
    /**
     * Yield percentage = (outputWeight / inputWeight) * 100
     */
    val yieldPercentage: Double?
        get() = if (outputWeightKg != null && inputWeightKg > 0) {
            (outputWeightKg / inputWeightKg) * 100.0
        } else null

    /**
     * Wastage percentage = (wastageWeight / inputWeight) * 100
     */
    val wastagePercentage: Double?
        get() = if (wastageWeightKg != null && inputWeightKg > 0) {
            (wastageWeightKg / inputWeightKg) * 100.0
        } else null
}

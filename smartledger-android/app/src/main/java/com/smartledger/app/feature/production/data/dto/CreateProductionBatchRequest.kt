package com.smartledger.app.feature.production.data.dto

import com.google.gson.annotations.SerializedName

data class CreateProductionBatchRequest(
    @SerializedName("orderId")
    val orderId: String? = null,

    @SerializedName("productType")
    val productType: String,

    @SerializedName("inputWeightKg")
    val inputWeightKg: Double,

    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("notes")
    val notes: String? = null
)

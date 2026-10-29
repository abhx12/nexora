package com.smartledger.app.feature.orders.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Line item payload within a Create Order request.
 */
data class CreateOrderItemRequest(
    @SerializedName("productType")
    val productType: String,

    @SerializedName("requiredWeightKg")
    val requiredWeightKg: Double,

    @SerializedName("ratePerKg")
    val ratePerKg: Double,

    @SerializedName("warpSpecification")
    val warpSpecification: String? = null
)

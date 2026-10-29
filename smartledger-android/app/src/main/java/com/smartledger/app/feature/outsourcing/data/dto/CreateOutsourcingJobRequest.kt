package com.smartledger.app.feature.outsourcing.data.dto

import com.google.gson.annotations.SerializedName

data class CreateOutsourcingJobRequest(
    @SerializedName("manufacturerId")
    val manufacturerId: String,

    @SerializedName("orderId")
    val orderId: String? = null,

    @SerializedName("productType")
    val productType: String, // SILK_WARP, KORA_WARP, VUDA_WARP

    @SerializedName("plannedWeightKg")
    val plannedWeightKg: Double,

    @SerializedName("expectedReturnDate")
    val expectedReturnDate: String? = null,

    @SerializedName("ratePerKg")
    val ratePerKg: Double? = null,

    @SerializedName("notes")
    val notes: String? = null
)

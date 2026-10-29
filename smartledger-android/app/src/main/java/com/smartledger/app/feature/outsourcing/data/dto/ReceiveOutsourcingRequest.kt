package com.smartledger.app.feature.outsourcing.data.dto

import com.google.gson.annotations.SerializedName

data class ReceiveOutsourcingRequest(
    @SerializedName("receiptDate")
    val receiptDate: String,

    @SerializedName("receivedWeightKg")
    val receivedWeightKg: Double,

    @SerializedName("wastageWeightKg")
    val wastageWeightKg: Double = 0.0,

    @SerializedName("notes")
    val notes: String? = null
)

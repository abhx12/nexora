package com.smartledger.app.feature.production.data.dto

import com.google.gson.annotations.SerializedName

data class CompleteProductionRequest(
    @SerializedName("outputWeightKg")
    val outputWeightKg: Double,

    @SerializedName("wastageWeightKg")
    val wastageWeightKg: Double,

    @SerializedName("completionDate")
    val completionDate: String,

    @SerializedName("notes")
    val notes: String? = null
)

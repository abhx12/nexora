package com.smartledger.app.feature.outsourcing.data.dto

import com.google.gson.annotations.SerializedName

data class IssueMaterialRequest(
    @SerializedName("issueDate")
    val issueDate: String,

    @SerializedName("issuedWeightKg")
    val issuedWeightKg: Double,

    @SerializedName("notes")
    val notes: String? = null
)

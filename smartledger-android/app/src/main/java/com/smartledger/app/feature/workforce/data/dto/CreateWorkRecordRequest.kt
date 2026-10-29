package com.smartledger.app.feature.workforce.data.dto

import com.google.gson.annotations.SerializedName

data class CreateWorkRecordRequest(
    @SerializedName("workerId")
    val workerId: String,

    @SerializedName("workDate")
    val workDate: String,

    @SerializedName("workType")
    val workType: String, // "WARPING" or "ROLLING"

    @SerializedName("quantityKg")
    val quantityKg: Double? = null,

    @SerializedName("hours")
    val hours: Double? = null,

    @SerializedName("rate")
    val rate: Double,

    @SerializedName("notes")
    val notes: String? = null
)

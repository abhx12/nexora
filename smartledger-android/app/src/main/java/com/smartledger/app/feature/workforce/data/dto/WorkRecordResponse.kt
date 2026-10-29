package com.smartledger.app.feature.workforce.data.dto

import com.google.gson.annotations.SerializedName

data class WorkRecordResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("recordNumber")
    val recordNumber: String, // e.g. "WRK-2026-0001"

    @SerializedName("workerId")
    val workerId: String,

    @SerializedName("workerName")
    val workerName: String,

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

    @SerializedName("totalAmount")
    val totalAmount: Double,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null
)

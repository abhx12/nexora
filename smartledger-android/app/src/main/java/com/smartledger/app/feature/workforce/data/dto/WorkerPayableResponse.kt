package com.smartledger.app.feature.workforce.data.dto

import com.google.gson.annotations.SerializedName

data class WorkerPayableResponse(
    @SerializedName("workerId")
    val workerId: String,

    @SerializedName("workerName")
    val workerName: String,

    @SerializedName("totalEarned")
    val totalEarned: Double,

    @SerializedName("totalPaid")
    val totalPaid: Double,

    @SerializedName("outstandingPayable")
    val outstandingPayable: Double,

    @SerializedName("recordsCount")
    val recordsCount: Int
)

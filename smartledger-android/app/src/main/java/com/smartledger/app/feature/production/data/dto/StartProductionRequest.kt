package com.smartledger.app.feature.production.data.dto

import com.google.gson.annotations.SerializedName

data class StartProductionRequest(
    @SerializedName("startDate")
    val startDate: String? = null,

    @SerializedName("notes")
    val notes: String? = null
)

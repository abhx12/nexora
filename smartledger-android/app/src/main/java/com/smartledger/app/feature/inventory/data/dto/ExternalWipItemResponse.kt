package com.smartledger.app.feature.inventory.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Representation of silk material currently with an external manufacturer for outsourced job work.
 */
data class ExternalWipItemResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("manufacturerId")
    val manufacturerId: String,

    @SerializedName("manufacturerName")
    val manufacturerName: String,

    @SerializedName("productType")
    val productType: String,

    @SerializedName("quantityKg")
    val quantityKg: Double,

    @SerializedName("jobId")
    val jobId: String,

    @SerializedName("jobTitle")
    val jobTitle: String? = null,

    @SerializedName("issueDate")
    val issueDate: String
)

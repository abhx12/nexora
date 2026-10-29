package com.smartledger.app.feature.outsourcing.data.dto

import com.google.gson.annotations.SerializedName

data class OutsourcingJobResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("jobNumber")
    val jobNumber: String, // e.g. "OUT-2026-0001"

    @SerializedName("manufacturerId")
    val manufacturerId: String,

    @SerializedName("manufacturerName")
    val manufacturerName: String? = null,

    @SerializedName("orderId")
    val orderId: String? = null,

    @SerializedName("orderNumber")
    val orderNumber: String? = null,

    @SerializedName("productType")
    val productType: String,

    @SerializedName("status")
    val status: String, // CREATED, MATERIAL_ISSUED, PARTIALLY_RECEIVED, COMPLETED

    @SerializedName("plannedWeightKg")
    val plannedWeightKg: Double,

    @SerializedName("issuedWeightKg")
    val issuedWeightKg: Double? = null,

    @SerializedName("receivedWeightKg")
    val receivedWeightKg: Double? = null,

    @SerializedName("wastageWeightKg")
    val wastageWeightKg: Double? = null,

    @SerializedName("issueDate")
    val issueDate: String? = null,

    @SerializedName("expectedReturnDate")
    val expectedReturnDate: String? = null,

    @SerializedName("completionDate")
    val completionDate: String? = null,

    @SerializedName("ratePerKg")
    val ratePerKg: Double? = null,

    @SerializedName("totalCost")
    val totalCost: Double? = null,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null
)

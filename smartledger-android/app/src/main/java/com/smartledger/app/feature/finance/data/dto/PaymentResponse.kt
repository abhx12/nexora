package com.smartledger.app.feature.finance.data.dto

import com.google.gson.annotations.SerializedName

data class PaymentAllocationResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("targetType")
    val targetType: String, // "ORDER" or "PURCHASE"

    @SerializedName("targetNumber")
    val targetNumber: String,

    @SerializedName("amount")
    val amount: Double
)

data class PaymentResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("paymentNumber")
    val paymentNumber: String, // e.g. "PAY-2026-0001"

    @SerializedName("partyId")
    val partyId: String,

    @SerializedName("partyName")
    val partyName: String,

    @SerializedName("direction")
    val direction: String, // "IN" or "OUT"

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("paymentMethod")
    val paymentMethod: String,

    @SerializedName("financialAccountId")
    val financialAccountId: String,

    @SerializedName("accountName")
    val accountName: String? = null,

    @SerializedName("paymentDate")
    val paymentDate: String,

    @SerializedName("referenceNumber")
    val referenceNumber: String? = null,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("allocations")
    val allocations: List<PaymentAllocationResponse> = emptyList(),

    @SerializedName("createdAt")
    val createdAt: String? = null
)

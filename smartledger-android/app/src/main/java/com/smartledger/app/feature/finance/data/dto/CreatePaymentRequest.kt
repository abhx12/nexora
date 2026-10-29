package com.smartledger.app.feature.finance.data.dto

import com.google.gson.annotations.SerializedName

data class PaymentAllocationRequest(
    @SerializedName("orderItemId")
    val orderItemId: String? = null,

    @SerializedName("purchaseId")
    val purchaseId: String? = null,

    @SerializedName("amount")
    val amount: Double
)

data class CreatePaymentRequest(
    @SerializedName("partyId")
    val partyId: String,

    @SerializedName("direction")
    val direction: String, // "IN" (from Customer) or "OUT" (to Supplier / Worker)

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("paymentMethod")
    val paymentMethod: String, // "CASH", "UPI", "BANK_TRANSFER", "CHEQUE"

    @SerializedName("financialAccountId")
    val financialAccountId: String,

    @SerializedName("paymentDate")
    val paymentDate: String,

    @SerializedName("referenceNumber")
    val referenceNumber: String? = null,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("allocations")
    val allocations: List<PaymentAllocationRequest> = emptyList()
)

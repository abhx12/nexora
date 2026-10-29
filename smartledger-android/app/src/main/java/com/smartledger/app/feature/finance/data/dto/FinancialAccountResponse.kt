package com.smartledger.app.feature.finance.data.dto

import com.google.gson.annotations.SerializedName

data class FinancialAccountResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("accountName")
    val accountName: String,

    @SerializedName("accountType")
    val accountType: String, // "CASH" or "BANK"

    @SerializedName("balance")
    val balance: Double,

    @SerializedName("accountNumber")
    val accountNumber: String? = null,

    @SerializedName("bankName")
    val bankName: String? = null
)

data class CreateAccountRequest(
    @SerializedName("accountName")
    val accountName: String,

    @SerializedName("accountType")
    val accountType: String,

    @SerializedName("initialBalance")
    val initialBalance: Double,

    @SerializedName("accountNumber")
    val accountNumber: String? = null,

    @SerializedName("bankName")
    val bankName: String? = null
)

data class AccountTransferRequest(
    @SerializedName("fromAccountId")
    val fromAccountId: String,

    @SerializedName("toAccountId")
    val toAccountId: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("transferDate")
    val transferDate: String,

    @SerializedName("notes")
    val notes: String? = null
)

data class AccountTransferResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("transferNumber")
    val transferNumber: String, // e.g. "TRF-2026-0001"

    @SerializedName("fromAccountId")
    val fromAccountId: String,

    @SerializedName("fromAccountName")
    val fromAccountName: String,

    @SerializedName("toAccountId")
    val toAccountId: String,

    @SerializedName("toAccountName")
    val toAccountName: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("transferDate")
    val transferDate: String,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null
)

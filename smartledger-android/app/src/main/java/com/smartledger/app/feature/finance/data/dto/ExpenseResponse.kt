package com.smartledger.app.feature.finance.data.dto

import com.google.gson.annotations.SerializedName

data class CreateExpenseRequest(
    @SerializedName("category")
    val category: String, // "RAW_MATERIAL", "UTILITIES", "RENT", "SALARIES", "MAINTENANCE", "TRANSPORT", "OTHER"

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("financialAccountId")
    val financialAccountId: String,

    @SerializedName("expenseDate")
    val expenseDate: String,

    @SerializedName("description")
    val description: String
)

data class ExpenseResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("expenseNumber")
    val expenseNumber: String, // e.g. "EXP-2026-0001"

    @SerializedName("category")
    val category: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("financialAccountId")
    val financialAccountId: String,

    @SerializedName("accountName")
    val accountName: String? = null,

    @SerializedName("expenseDate")
    val expenseDate: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("createdAt")
    val createdAt: String? = null
)

package com.smartledger.app.feature.reports.data.dto

import com.google.gson.annotations.SerializedName

data class CustomerOutstandingItem(
    @SerializedName("customerId")
    val customerId: String,

    @SerializedName("customerName")
    val customerName: String,

    @SerializedName("customerCode")
    val customerCode: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("totalOrderValue")
    val totalOrderValue: Double,

    @SerializedName("totalPaid")
    val totalPaid: Double,

    @SerializedName("outstandingAmount")
    val outstandingAmount: Double,

    @SerializedName("pendingOrdersCount")
    val pendingOrdersCount: Int,

    @SerializedName("oldestPendingOrderDate")
    val oldestPendingOrderDate: String? = null
)

data class CustomerOutstandingReportResponse(
    @SerializedName("items")
    val items: List<CustomerOutstandingItem>,

    @SerializedName("totalOutstanding")
    val totalOutstanding: Double,

    @SerializedName("generatedAt")
    val generatedAt: String? = null
)

data class SupplierPayableItem(
    @SerializedName("supplierId")
    val supplierId: String,

    @SerializedName("supplierName")
    val supplierName: String,

    @SerializedName("supplierCode")
    val supplierCode: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("totalPurchasedValue")
    val totalPurchasedValue: Double,

    @SerializedName("totalPaid")
    val totalPaid: Double,

    @SerializedName("outstandingPayable")
    val outstandingPayable: Double,

    @SerializedName("isOverdue")
    val isOverdue: Boolean = false,

    @SerializedName("dueDate")
    val dueDate: String? = null
)

data class SupplierPayablesReportResponse(
    @SerializedName("items")
    val items: List<SupplierPayableItem>,

    @SerializedName("totalPayable")
    val totalPayable: Double,

    @SerializedName("overduePayable")
    val overduePayable: Double = 0.0,

    @SerializedName("generatedAt")
    val generatedAt: String? = null
)

data class ExpenseSummaryCategoryItem(
    @SerializedName("category")
    val category: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("percentage")
    val percentage: Double
)

data class ExpenseSummaryReportResponse(
    @SerializedName("categories")
    val categories: List<ExpenseSummaryCategoryItem>,

    @SerializedName("totalExpenses")
    val totalExpenses: Double,

    @SerializedName("startDate")
    val startDate: String? = null,

    @SerializedName("endDate")
    val endDate: String? = null
)

package com.smartledger.app.feature.dashboard.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Response payload from GET /api/v1/dashboard/summary.
 * Contains core financial, stock, and operational pipeline KPIs.
 */
data class DashboardSummaryResponse(
    @SerializedName("cash")
    val cash: Double = 0.0,

    @SerializedName("bank")
    val bank: Double = 0.0,

    @SerializedName("rawSilkKg")
    val rawSilkKg: Double = 0.0,

    @SerializedName("customerOutstanding")
    val customerOutstanding: Double = 0.0,

    @SerializedName("supplierPayable")
    val supplierPayable: Double = 0.0,

    @SerializedName("externalWipKg")
    val externalWipKg: Double = 0.0,

    @SerializedName("pendingOrders")
    val pendingOrders: Int = 0,

    @SerializedName("pendingProduction")
    val pendingProduction: Int = 0,

    @SerializedName("pendingOutsourcing")
    val pendingOutsourcing: Int = 0
)

package com.smartledger.app.feature.inventory.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Real-time inventory stock summary.
 * All quantities are in kg (3 decimal places).
 */
data class InventorySummaryResponse(
    @SerializedName("rawSilkKg")
    val rawSilkKg: Double = 0.0,

    @SerializedName("koraWarpKg")
    val koraWarpKg: Double = 0.0,

    @SerializedName("silkWarpKg")
    val silkWarpKg: Double = 0.0,

    @SerializedName("vudaWarpKg")
    val vudaWarpKg: Double = 0.0,

    @SerializedName("externalWipKg")
    val externalWipKg: Double = 0.0
) {
    val totalInHouseStockKg: Double
        get() = rawSilkKg + koraWarpKg + silkWarpKg + vudaWarpKg

    val totalStockKg: Double
        get() = totalInHouseStockKg + externalWipKg
}

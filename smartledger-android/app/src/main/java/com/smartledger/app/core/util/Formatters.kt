package com.smartledger.app.core.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats a Double as Indian Rupee string.
 * Example: 125000.0.toIndianRupee() → "₹1,25,000.00"
 *
 * UI Rule: Money always uses ₹ symbol and Indian comma format.
 */
fun Double.toIndianRupee(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(this)
}

/**
 * Formats a Double as a weight string with 3 decimal places.
 * Example: 324.5.toWeightString() → "324.500 kg"
 *
 * UI Rule: Weights always show 3 decimal places.
 */
fun Double.toWeightString(): String = "%.3f kg".format(this)

/**
 * Formats a date string from ISO (YYYY-MM-DD) to Indian display format (dd/MM/yyyy).
 * Returns the original string if parsing fails.
 *
 * UI Rule: Display dates as dd/MM/yyyy. Send dates to API as YYYY-MM-DD.
 */
fun String.toDisplayDate(): String {
    return try {
        val parts = this.split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else this
    } catch (e: Exception) {
        this
    }
}

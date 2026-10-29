package com.smartledger.app.core.util

import com.google.gson.Gson
import okhttp3.ResponseBody

/**
 * Represents a structured error response from the SmartLedger backend API.
 *
 * Matches Dev A's error format:
 * ```json
 * {
 *   "timestamp": "2026-10-19T17:00:00+05:30",
 *   "status": 409,
 *   "code": "INSUFFICIENT_INVENTORY",
 *   "message": "Only 42.500 kg of raw silk is available.",
 *   "path": "/api/v1/production-batches"
 * }
 * ```
 */
data class ApiError(
    val timestamp: String? = null,
    val status: Int = 0,
    val code: String = "",
    val message: String = "",
    val path: String? = null
)

/**
 * Error code constants matching Dev A's backend.
 * Used to show user-friendly messages in the UI.
 */
object ApiErrorCode {
    const val RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND"
    const val INSUFFICIENT_INVENTORY = "INSUFFICIENT_INVENTORY"
    const val ORDER_LOCKED = "ORDER_LOCKED"
    const val INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION"
    const val PAYMENT_ALLOCATION_EXCEEDED = "PAYMENT_ALLOCATION_EXCEEDED"
    const val PURCHASE_RECEIPT_EXCEEDED = "PURCHASE_RECEIPT_EXCEEDED"
}

/**
 * Parses a Retrofit error body into an [ApiError].
 * Returns null if parsing fails (e.g. unexpected format or empty body).
 */
fun parseApiError(errorBody: ResponseBody?): ApiError? = try {
    Gson().fromJson(errorBody?.string(), ApiError::class.java)
} catch (e: Exception) {
    null
}

/**
 * Maps an API error code to a user-friendly UI message.
 */
fun ApiError?.toUserMessage(): String {
    if (this == null) return "An unexpected error occurred"
    return when (code) {
        ApiErrorCode.RESOURCE_NOT_FOUND -> "Item not found"
        ApiErrorCode.INSUFFICIENT_INVENTORY -> "Not enough stock: ${message}"
        ApiErrorCode.ORDER_LOCKED -> "Order is already in production"
        ApiErrorCode.INVALID_STATE_TRANSITION -> "This action is not allowed now"
        ApiErrorCode.PAYMENT_ALLOCATION_EXCEEDED -> "Allocation exceeds outstanding balance"
        ApiErrorCode.PURCHASE_RECEIPT_EXCEEDED -> "Cannot receive more than ordered"
        else -> message.ifBlank { "Request failed" }
    }
}

package com.smartledger.app.feature.orders.data.dto

import com.google.gson.annotations.SerializedName
import com.smartledger.app.feature.orders.domain.model.OrderStatus

/**
 * Customer warp order response.
 */
data class OrderResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("orderNumber")
    val orderNumber: String,

    @SerializedName("customerId")
    val customerId: String,

    @SerializedName("customerName")
    val customerName: String,

    @SerializedName("orderDate")
    val orderDate: String,

    @SerializedName("requiredDate")
    val requiredDate: String,

    @SerializedName("status")
    val status: String = "CREATED",

    @SerializedName("isLocked")
    val isLocked: Boolean = false,

    @SerializedName("items")
    val items: List<OrderItemResponse> = emptyList(),

    @SerializedName("totalWeightKg")
    val totalWeightKg: Double = items.sumOf { it.requiredWeightKg },

    @SerializedName("totalAmount")
    val totalAmount: Double = items.sumOf { it.amount },

    @SerializedName("notes")
    val notes: String? = null
) {
    val orderStatus: OrderStatus
        get() = OrderStatus.fromString(status)

    val isOrderLocked: Boolean
        get() = isLocked || status.equals("IN_PROGRESS", ignoreCase = true) ||
                status.equals("COMPLETED", ignoreCase = true) ||
                status.equals("CANCELLED", ignoreCase = true)

    val canBeCancelled: Boolean
        get() = !status.equals("COMPLETED", ignoreCase = true) &&
                !status.equals("CANCELLED", ignoreCase = true) &&
                !status.equals("IN_PROGRESS", ignoreCase = true)
}

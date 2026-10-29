package com.smartledger.app.feature.orders.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload for PATCH /api/v1/orders/{id}.
 */
data class UpdateOrderRequest(
    @SerializedName("requiredDate")
    val requiredDate: String? = null,

    @SerializedName("items")
    val items: List<CreateOrderItemRequest>? = null,

    @SerializedName("notes")
    val notes: String? = null
)

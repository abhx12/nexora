package com.smartledger.app.feature.orders.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload for POST /api/v1/orders.
 */
data class CreateOrderRequest(
    @SerializedName("customerId")
    val customerId: String,

    @SerializedName("orderDate")
    val orderDate: String,

    @SerializedName("requiredDate")
    val requiredDate: String,

    @SerializedName("items")
    val items: List<CreateOrderItemRequest>,

    @SerializedName("notes")
    val notes: String? = null
)

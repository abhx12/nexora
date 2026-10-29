package com.smartledger.app.feature.orders.data.dto

import com.google.gson.annotations.SerializedName
import com.smartledger.app.feature.orders.domain.model.OrderProductType

/**
 * Line item in a customer warp order.
 */
data class OrderItemResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("orderId")
    val orderId: String? = null,

    @SerializedName("productType")
    val productType: String,

    @SerializedName("requiredWeightKg")
    val requiredWeightKg: Double,

    @SerializedName("ratePerKg")
    val ratePerKg: Double,

    @SerializedName("warpSpecification")
    val warpSpecification: String? = null,

    @SerializedName("amount")
    val amount: Double = requiredWeightKg * ratePerKg
) {
    val typedProduct: OrderProductType
        get() = OrderProductType.fromString(productType)
}

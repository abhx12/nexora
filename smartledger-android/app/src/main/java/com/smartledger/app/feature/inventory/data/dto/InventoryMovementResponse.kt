package com.smartledger.app.feature.inventory.data.dto

import com.google.gson.annotations.SerializedName
import com.smartledger.app.feature.inventory.domain.model.MovementType
import com.smartledger.app.feature.inventory.domain.model.ProductType

/**
 * Representation of a material movement ledger record.
 */
data class InventoryMovementResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("movementNumber")
    val movementNumber: String,

    @SerializedName("productType")
    val productType: String,

    @SerializedName("movementType")
    val movementType: String,

    @SerializedName("quantityKg")
    val quantityKg: Double,

    @SerializedName("fromLocation")
    val fromLocation: String,

    @SerializedName("toLocation")
    val toLocation: String,

    @SerializedName("movementDate")
    val movementDate: String,

    @SerializedName("referenceType")
    val referenceType: String? = null,

    @SerializedName("referenceId")
    val referenceId: String? = null,

    @SerializedName("notes")
    val notes: String? = null
) {
    val typedMovement: MovementType
        get() = MovementType.fromString(movementType)

    val typedProduct: ProductType?
        get() = ProductType.fromString(productType)
}

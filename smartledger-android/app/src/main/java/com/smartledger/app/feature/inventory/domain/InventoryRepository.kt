package com.smartledger.app.feature.inventory.domain

import com.smartledger.app.feature.inventory.data.dto.ExternalWipItemResponse
import com.smartledger.app.feature.inventory.data.dto.InventoryMovementResponse
import com.smartledger.app.feature.inventory.data.dto.InventorySummaryResponse
import com.smartledger.app.feature.inventory.domain.model.MovementType
import com.smartledger.app.feature.inventory.domain.model.ProductType

/**
 * Domain repository contract for Silk Inventory & Material Stock (Read-Only).
 */
interface InventoryRepository {

    suspend fun getInventorySummary(): Result<InventorySummaryResponse>

    suspend fun getExternalWip(): Result<List<ExternalWipItemResponse>>

    suspend fun getMovements(
        productType: ProductType? = null,
        movementType: MovementType? = null
    ): Result<List<InventoryMovementResponse>>
}

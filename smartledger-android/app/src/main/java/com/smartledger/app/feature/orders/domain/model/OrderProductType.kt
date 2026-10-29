package com.smartledger.app.feature.orders.domain.model

/**
 * Product types available for Customer Warp Orders:
 * SILK_WARP, KORA_WARP, VUDA_WARP
 */
enum class OrderProductType(val displayName: String, val code: String) {
    SILK_WARP("Silk Warp", "SILK_WARP"),
    KORA_WARP("Kora Warp", "KORA_WARP"),
    VUDA_WARP("Vuda Warp", "VUDA_WARP");

    companion object {
        fun fromString(value: String?): OrderProductType {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.code.equals(value, ignoreCase = true)
            } ?: SILK_WARP
        }
    }
}

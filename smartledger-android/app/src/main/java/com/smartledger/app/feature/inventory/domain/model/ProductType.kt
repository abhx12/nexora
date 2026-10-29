package com.smartledger.app.feature.inventory.domain.model

/**
 * Product types in the SmartLedger Silk Manufacturing Ecosystem.
 */
enum class ProductType(val displayName: String, val code: String) {
    RAW_SILK("Raw Silk", "RAW_SILK"),
    KORA_WARP("Kora Warp", "KORA_WARP"),
    SILK_WARP("Silk Warp", "SILK_WARP"),
    VUDA_WARP("Vuda Warp", "VUDA_WARP");

    companion object {
        fun fromString(value: String?): ProductType? {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.code.equals(value, ignoreCase = true)
            }
        }
    }
}

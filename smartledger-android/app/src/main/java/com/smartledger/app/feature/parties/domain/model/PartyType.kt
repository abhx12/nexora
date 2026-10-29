package com.smartledger.app.feature.parties.domain.model

/**
 * The four supported party types in SmartLedger.
 */
enum class PartyType(val displayName: String, val prefix: String) {
    CUSTOMER("Customers", "CUST"),
    SUPPLIER("Suppliers", "SUPP"),
    MANUFACTURER("Manufacturers", "MFG"),
    WORKER("Workers", "WRK");

    companion object {
        fun fromString(value: String): PartyType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CUSTOMER
        }
    }
}

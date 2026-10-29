package com.smartledger.app.core.navigation

/**
 * All navigation routes for the SmartLedger app.
 * Using string constants so Navigation Compose can parse arguments consistently.
 *
 * Naming convention: lowercase, slash-separated (matches typical NavGraph convention)
 */
object AppRoute {
    // Auth flow
    const val SPLASH = "splash"
    const val LOGIN = "login"

    // Main tabs (bottom nav)
    const val DASHBOARD = "dashboard"
    const val PARTIES = "parties"
    const val ORDERS = "orders"
    const val FINANCE = "finance"
    const val MORE = "more"

    // Procurement
    const val PURCHASE_LIST = "purchases"
    const val PURCHASE_DETAIL = "purchases/{purchaseId}"
    const val CREATE_PURCHASE = "purchases/create"
    const val RECORD_RECEIPT = "purchases/{purchaseId}/receipt"

    // Inventory
    const val INVENTORY_SUMMARY = "inventory"
    const val MOVEMENT_HISTORY = "inventory/movements"

    // Orders detail
    const val ORDER_DETAIL = "orders/{orderId}"
    const val CREATE_ORDER = "orders/create"

    // Production
    const val PRODUCTION_LIST = "production"
    const val PRODUCTION_DETAIL = "production/{batchId}"
    const val CREATE_PRODUCTION = "production/create"
    const val COMPLETE_PRODUCTION = "production/{batchId}/complete"

    // Outsourcing
    const val OUTSOURCING_LIST = "outsourcing"
    const val OUTSOURCING_DETAIL = "outsourcing/{jobId}"
    const val CREATE_OUTSOURCING = "outsourcing/create"
    const val ISSUE_OUTSOURCING = "outsourcing/{jobId}/issue"
    const val RECEIVE_OUTSOURCING = "outsourcing/{jobId}/receive"

    // Workforce
    const val WORK_RECORD_LIST = "workforce"
    const val CREATE_WORK_RECORD = "workforce/create"
    const val WORKER_PAYABLES = "workforce/payables"

    // Finance detail
    const val RECORD_PAYMENT = "finance/payment"
    const val RECORD_EXPENSE = "finance/expense"
    const val ACCOUNT_LIST = "finance/accounts"
    const val ACCOUNT_TRANSFER = "finance/transfer"

    // Reports
    const val REPORTS = "reports"
    const val CUSTOMER_OUTSTANDING_REPORT = "reports/customer-outstanding"
    const val SUPPLIER_PAYABLES_REPORT = "reports/supplier-payables"
    const val EXPENSE_REPORT = "reports/expenses"

    // Parties detail
    const val PARTY_DETAIL = "parties/{partyId}"
    const val CREATE_PARTY = "parties/create"

    // Helper to build routes with arguments
    fun purchaseDetail(purchaseId: String) = "purchases/$purchaseId"
    fun recordReceipt(purchaseId: String) = "purchases/$purchaseId/receipt"
    fun orderDetail(orderId: String) = "orders/$orderId"
    fun productionDetail(batchId: String) = "production/$batchId"
    fun completeProduction(batchId: String) = "production/$batchId/complete"
    fun outsourcingDetail(jobId: String) = "outsourcing/$jobId"
    fun issueOutsourcing(jobId: String) = "outsourcing/$jobId/issue"
    fun receiveOutsourcing(jobId: String) = "outsourcing/$jobId/receive"
    fun partyDetail(partyId: String) = "parties/$partyId"
}

package com.smartledger.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartledger.app.core.ui.components.BottomNavBar
import com.smartledger.app.core.ui.components.BottomNavItem
import com.smartledger.app.feature.auth.presentation.ui.LoginScreen
import com.smartledger.app.feature.auth.presentation.ui.SplashScreen
import com.smartledger.app.feature.dashboard.presentation.ui.DashboardScreen
import com.smartledger.app.feature.finance.presentation.ui.AccountListScreen
import com.smartledger.app.feature.finance.presentation.ui.AccountTransferScreen
import com.smartledger.app.feature.finance.presentation.ui.FinanceScreen
import com.smartledger.app.feature.finance.presentation.ui.RecordExpenseScreen
import com.smartledger.app.feature.finance.presentation.ui.RecordPaymentScreen
import com.smartledger.app.feature.inventory.presentation.ui.InventorySummaryScreen
import com.smartledger.app.feature.inventory.presentation.ui.MovementHistoryScreen
import com.smartledger.app.feature.more.presentation.ui.MoreScreen
import com.smartledger.app.feature.orders.presentation.ui.CreateOrderScreen
import com.smartledger.app.feature.orders.presentation.ui.OrderDetailScreen
import com.smartledger.app.feature.orders.presentation.ui.OrderListScreen
import com.smartledger.app.feature.outsourcing.presentation.ui.CreateOutsourcingJobScreen
import com.smartledger.app.feature.outsourcing.presentation.ui.IssueOutsourcingScreen
import com.smartledger.app.feature.outsourcing.presentation.ui.OutsourcingDetailScreen
import com.smartledger.app.feature.outsourcing.presentation.ui.OutsourcingListScreen
import com.smartledger.app.feature.outsourcing.presentation.ui.ReceiveOutsourcingScreen
import com.smartledger.app.feature.parties.presentation.ui.CreatePartyScreen
import com.smartledger.app.feature.parties.presentation.ui.PartyDetailScreen
import com.smartledger.app.feature.parties.presentation.ui.PartyListScreen
import com.smartledger.app.feature.procurement.presentation.ui.CreatePurchaseScreen
import com.smartledger.app.feature.procurement.presentation.ui.PurchaseDetailScreen
import com.smartledger.app.feature.procurement.presentation.ui.PurchaseListScreen
import com.smartledger.app.feature.procurement.presentation.ui.RecordReceiptScreen
import com.smartledger.app.feature.production.presentation.ui.CompleteProductionScreen
import com.smartledger.app.feature.production.presentation.ui.CreateProductionBatchScreen
import com.smartledger.app.feature.production.presentation.ui.ProductionDetailScreen
import com.smartledger.app.feature.production.presentation.ui.ProductionListScreen
import com.smartledger.app.feature.reports.presentation.ui.CustomerOutstandingReportScreen
import com.smartledger.app.feature.reports.presentation.ui.ExpenseSummaryReportScreen
import com.smartledger.app.feature.reports.presentation.ui.ReportsScreen
import com.smartledger.app.feature.reports.presentation.ui.SupplierPayablesReportScreen
import com.smartledger.app.feature.workforce.presentation.ui.CreateWorkRecordScreen
import com.smartledger.app.feature.workforce.presentation.ui.WorkRecordListScreen

/**
 * Root NavHost for the entire SmartLedger app.
 *
 * Two logical zones:
 * 1. Auth zone (no bottom bar): Splash, Login
 * 2. Main zone (with bottom bar): Dashboard, Parties, Orders, Finance, More + sub-screens
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes that should show the bottom navigation bar
    val bottomNavRoutes = BottomNavItem.entries.map { it.route }
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute ?: AppRoute.DASHBOARD,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Auth zone ──────────────────────────────────────────────────
            composable(AppRoute.SPLASH) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(AppRoute.LOGIN) {
                            popUpTo(AppRoute.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(AppRoute.DASHBOARD) {
                            popUpTo(AppRoute.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppRoute.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(AppRoute.DASHBOARD) {
                            popUpTo(AppRoute.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // ── Bottom Nav: Dashboard ──────────────────────────────────────
            composable(AppRoute.DASHBOARD) {
                DashboardScreen()
            }

            // ── Bottom Nav: Parties ────────────────────────────────────────
            composable(AppRoute.PARTIES) {
                PartyListScreen(
                    onNavigateToCreate = { navController.navigate(AppRoute.CREATE_PARTY) },
                    onNavigateToDetail = { partyId -> navController.navigate(AppRoute.partyDetail(partyId)) }
                )
            }
            composable(AppRoute.CREATE_PARTY) {
                CreatePartyScreen(
                    onPartyCreated = { navController.popBackStack() }
                )
            }
            composable(AppRoute.PARTY_DETAIL) { backStackEntry ->
                val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
                PartyDetailScreen(
                    partyId = partyId,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Bottom Nav: Orders ─────────────────────────────────────────
            composable(AppRoute.ORDERS) {
                OrderListScreen(
                    onNavigateToCreate = { navController.navigate(AppRoute.CREATE_ORDER) },
                    onNavigateToDetail = { orderId -> navController.navigate(AppRoute.orderDetail(orderId)) }
                )
            }
            composable(AppRoute.CREATE_ORDER) {
                CreateOrderScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOrderCreated = { navController.popBackStack() }
                )
            }
            composable(AppRoute.ORDER_DETAIL) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailScreen(
                    orderId = orderId,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Bottom Nav: Finance ────────────────────────────────────────
            composable(AppRoute.FINANCE) {
                FinanceScreen(
                    onNavigateToPayment = { navController.navigate(AppRoute.RECORD_PAYMENT) },
                    onNavigateToExpense = { navController.navigate(AppRoute.RECORD_EXPENSE) },
                    onNavigateToAccounts = { navController.navigate(AppRoute.ACCOUNT_LIST) },
                    onNavigateToTransfer = { navController.navigate(AppRoute.ACCOUNT_TRANSFER) }
                )
            }
            composable(AppRoute.RECORD_PAYMENT) {
                RecordPaymentScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPaymentRecorded = { navController.popBackStack() }
                )
            }
            composable(AppRoute.RECORD_EXPENSE) {
                RecordExpenseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onExpenseRecorded = { navController.popBackStack() }
                )
            }
            composable(AppRoute.ACCOUNT_LIST) {
                AccountListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTransfer = { navController.navigate(AppRoute.ACCOUNT_TRANSFER) }
                )
            }
            composable(AppRoute.ACCOUNT_TRANSFER) {
                AccountTransferScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onTransferSuccess = { navController.popBackStack() }
                )
            }

            // ── Bottom Nav: More ───────────────────────────────────────────
            composable(AppRoute.MORE) {
                MoreScreen(
                    onNavigateToProcurement = { navController.navigate(AppRoute.PURCHASE_LIST) },
                    onNavigateToInventory = { navController.navigate(AppRoute.INVENTORY_SUMMARY) },
                    onNavigateToProduction = { navController.navigate(AppRoute.PRODUCTION_LIST) },
                    onNavigateToOutsourcing = { navController.navigate(AppRoute.OUTSOURCING_LIST) },
                    onNavigateToWorkforce = { navController.navigate(AppRoute.WORK_RECORD_LIST) },
                    onNavigateToReports = { navController.navigate(AppRoute.REPORTS) }
                )
            }

            // ── Phase 5: Silk Procurement ──────────────────────────────────
            composable(AppRoute.PURCHASE_LIST) {
                PurchaseListScreen(
                    onNavigateToCreate = { navController.navigate(AppRoute.CREATE_PURCHASE) },
                    onNavigateToDetail = { purchaseId -> navController.navigate(AppRoute.purchaseDetail(purchaseId)) }
                )
            }
            composable(AppRoute.CREATE_PURCHASE) {
                CreatePurchaseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPurchaseCreated = { navController.popBackStack() }
                )
            }
            composable(AppRoute.PURCHASE_DETAIL) { backStackEntry ->
                val purchaseId = backStackEntry.arguments?.getString("purchaseId") ?: ""
                PurchaseDetailScreen(
                    purchaseId = purchaseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRecordReceipt = { id -> navController.navigate(AppRoute.recordReceipt(id)) }
                )
            }
            composable(AppRoute.RECORD_RECEIPT) { backStackEntry ->
                val purchaseId = backStackEntry.arguments?.getString("purchaseId") ?: ""
                RecordReceiptScreen(
                    purchaseId = purchaseId,
                    onNavigateBack = { navController.popBackStack() },
                    onReceiptRecorded = { navController.popBackStack() }
                )
            }

            // ── Phase 6: Inventory & Material Stock (Read-Only) ────────────
            composable(AppRoute.INVENTORY_SUMMARY) {
                InventorySummaryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMovements = { navController.navigate(AppRoute.MOVEMENT_HISTORY) }
                )
            }
            composable(AppRoute.MOVEMENT_HISTORY) {
                MovementHistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Phase 8: Production ────────────────────────────────────────
            composable(AppRoute.PRODUCTION_LIST) {
                ProductionListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateBatch = { navController.navigate(AppRoute.CREATE_PRODUCTION) },
                    onNavigateToBatchDetail = { batchId -> navController.navigate(AppRoute.productionDetail(batchId)) }
                )
            }
            composable(AppRoute.CREATE_PRODUCTION) {
                CreateProductionBatchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onBatchCreated = { batchId ->
                        navController.popBackStack()
                        navController.navigate(AppRoute.productionDetail(batchId))
                    }
                )
            }
            composable(AppRoute.PRODUCTION_DETAIL) {
                ProductionDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToComplete = { batchId ->
                        navController.navigate(AppRoute.completeProduction(batchId))
                    }
                )
            }
            composable(AppRoute.COMPLETE_PRODUCTION) {
                CompleteProductionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCompletedSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            // ── Phase 9: Outsourcing ───────────────────────────────────────
            composable(AppRoute.OUTSOURCING_LIST) {
                OutsourcingListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate(AppRoute.CREATE_OUTSOURCING) },
                    onNavigateToDetail = { jobId -> navController.navigate(AppRoute.outsourcingDetail(jobId)) }
                )
            }
            composable(AppRoute.CREATE_OUTSOURCING) {
                CreateOutsourcingJobScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onJobCreated = { jobId ->
                        navController.popBackStack()
                        navController.navigate(AppRoute.outsourcingDetail(jobId))
                    }
                )
            }
            composable(AppRoute.OUTSOURCING_DETAIL) {
                OutsourcingDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToIssue = { jobId -> navController.navigate(AppRoute.issueOutsourcing(jobId)) },
                    onNavigateToReceive = { jobId -> navController.navigate(AppRoute.receiveOutsourcing(jobId)) }
                )
            }
            composable(AppRoute.ISSUE_OUTSOURCING) {
                IssueOutsourcingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onIssueSuccess = { navController.popBackStack() }
                )
            }
            composable(AppRoute.RECEIVE_OUTSOURCING) {
                ReceiveOutsourcingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onReceiveSuccess = { navController.popBackStack() }
                )
            }

            // ── Phase 10: Workforce ────────────────────────────────────────
            composable(AppRoute.WORK_RECORD_LIST) {
                WorkRecordListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate(AppRoute.CREATE_WORK_RECORD) },
                    onNavigateToPayment = { navController.navigate(AppRoute.RECORD_PAYMENT) }
                )
            }
            composable(AppRoute.CREATE_WORK_RECORD) {
                CreateWorkRecordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRecordCreated = { navController.popBackStack() }
                )
            }

            // ── Phase 12: Reports ──────────────────────────────────────────
            composable(AppRoute.REPORTS) {
                ReportsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCustomerOutstanding = { navController.navigate(AppRoute.CUSTOMER_OUTSTANDING_REPORT) },
                    onNavigateToSupplierPayables = { navController.navigate(AppRoute.SUPPLIER_PAYABLES_REPORT) },
                    onNavigateToExpenses = { navController.navigate(AppRoute.EXPENSE_REPORT) },
                    onNavigateToInventoryMovements = { navController.navigate(AppRoute.MOVEMENT_HISTORY) }
                )
            }
            composable(AppRoute.CUSTOMER_OUTSTANDING_REPORT) {
                CustomerOutstandingReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.SUPPLIER_PAYABLES_REPORT) {
                SupplierPayablesReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.EXPENSE_REPORT) {
                ExpenseSummaryReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

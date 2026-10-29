package com.nexora.desktop.di

import com.nexora.desktop.api.ApiClient
import com.nexora.desktop.api.AuthSession
import com.nexora.desktop.repository.AuthRepository
import com.nexora.desktop.repository.PartyRepository
import com.nexora.desktop.util.CredentialStore
import com.nexora.desktop.util.OsKeyring
import com.nexora.desktop.util.SettingsStore
import com.nexora.desktop.repository.SystemSettingRepository
import com.nexora.desktop.viewmodel.auth.LoginViewModel
import com.nexora.desktop.viewmodel.auth.OnboardingViewModel
import com.nexora.desktop.viewmodel.auth.SessionViewModel
import com.nexora.desktop.viewmodel.shell.NavigationViewModel
import com.nexora.desktop.viewmodel.auth.ServerSetupViewModel
import io.ktor.client.engine.cio.CIO
import com.nexora.desktop.repository.DashboardRepository
import com.nexora.desktop.viewmodel.dashboard.DashboardViewModel
import com.nexora.desktop.repository.LoanRepository
import com.nexora.desktop.viewmodel.parties.PartyListViewModel
import com.nexora.desktop.viewmodel.parties.PartyFormViewModel
import com.nexora.desktop.viewmodel.parties.PartyDetailViewModel
import com.nexora.desktop.repository.PurchaseRepository
import com.nexora.desktop.viewmodel.purchases.PurchaseListViewModel
import com.nexora.desktop.viewmodel.purchases.PurchaseFormViewModel
import com.nexora.desktop.viewmodel.purchases.PurchaseDetailViewModel
import com.nexora.desktop.viewmodel.purchases.ReceiveSilkViewModel
import com.nexora.desktop.viewmodel.purchases.ReturnDialogViewModel
import com.nexora.desktop.ui.purchases.SupplierPayablesViewModel
import com.nexora.desktop.repository.InventoryRepository
import com.nexora.desktop.viewmodel.inventory.InventoryViewModel
import com.nexora.desktop.viewmodel.inventory.StockSummaryViewModel
import com.nexora.desktop.viewmodel.inventory.MovementHistoryViewModel
import com.nexora.desktop.viewmodel.inventory.ExternalWipViewModel
import com.nexora.desktop.viewmodel.inventory.StockAdjustmentViewModel
import com.nexora.desktop.repository.OrderRepository
import com.nexora.desktop.viewmodel.orders.OrderListViewModel
import com.nexora.desktop.viewmodel.orders.OrderFormViewModel
import com.nexora.desktop.viewmodel.orders.OrderDetailViewModel
import com.nexora.desktop.viewmodel.orders.RecordCustomerReturnViewModel
import com.nexora.desktop.repository.PaymentRepository
import com.nexora.desktop.repository.FinanceRepository
import com.nexora.desktop.viewmodel.payments.PaymentListViewModel
import com.nexora.desktop.viewmodel.payments.PaymentDetailViewModel
import com.nexora.desktop.viewmodel.payments.RecordPaymentViewModel
import com.nexora.desktop.viewmodel.money.AccountsViewModel
import com.nexora.desktop.viewmodel.money.NewAccountViewModel
import com.nexora.desktop.viewmodel.money.TransferViewModel
import com.nexora.desktop.viewmodel.money.BalanceAdjustmentViewModel
import com.nexora.desktop.viewmodel.money.TransactionsViewModel
import com.nexora.desktop.repository.ExpenseRepository
import com.nexora.desktop.repository.ChitRepository
import com.nexora.desktop.viewmodel.expenses.ExpensesViewModel
import com.nexora.desktop.viewmodel.expenses.RecordExpenseViewModel
import com.nexora.desktop.viewmodel.loans.LoanListViewModel
import com.nexora.desktop.viewmodel.loans.NewLoanViewModel
import com.nexora.desktop.viewmodel.loans.LoanDetailViewModel
import com.nexora.desktop.viewmodel.chits.ChitListViewModel
import com.nexora.desktop.viewmodel.chits.NewChitViewModel
import com.nexora.desktop.viewmodel.chits.ChitDetailViewModel
import com.nexora.desktop.repository.ProductionRepository
import com.nexora.desktop.repository.OutsourcingRepository
import com.nexora.desktop.repository.WorkRepository
import com.nexora.desktop.viewmodel.production.BatchListViewModel
import com.nexora.desktop.viewmodel.production.BatchFormViewModel
import com.nexora.desktop.viewmodel.production.BatchDetailViewModel
import com.nexora.desktop.viewmodel.production.CompleteBatchViewModel
import com.nexora.desktop.viewmodel.outsourcing.JobListViewModel
import com.nexora.desktop.viewmodel.outsourcing.NewJobViewModel
import com.nexora.desktop.viewmodel.outsourcing.JobDetailViewModel
import com.nexora.desktop.viewmodel.outsourcing.ReceiveWarpViewModel
import com.nexora.desktop.viewmodel.work.RecordWorkViewModel
import com.nexora.desktop.viewmodel.work.WorkRecordsViewModel
import com.nexora.desktop.ui.components.PickOption
import java.math.BigDecimal
import com.nexora.desktop.repository.ReportRepository
import com.nexora.desktop.repository.NotificationRepository
import com.nexora.desktop.repository.DocumentRepository
import com.nexora.desktop.viewmodel.reports.ProfitAndLossViewModel
import com.nexora.desktop.viewmodel.reports.ReportViewerViewModel
import com.nexora.desktop.viewmodel.reports.REPORTS
import com.nexora.desktop.viewmodel.notifications.NotificationBellViewModel
import com.nexora.desktop.viewmodel.notifications.NotificationListViewModel
import com.nexora.desktop.viewmodel.notifications.ReminderSettingsViewModel
import com.nexora.desktop.viewmodel.documents.AttachDocumentViewModel
import com.nexora.desktop.ui.documents.DocumentListViewModel
import com.nexora.desktop.viewmodel.settings.SettingsViewModel
import com.nexora.desktop.viewmodel.shell.OfflineStateViewModel
import org.koin.dsl.module
import java.time.Clock
import java.time.ZoneId

/** Business dates are Asia/Kolkata dates (the shop's calendar), whatever the computer's own time zone is. */
val BUSINESS_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

/**
 * Koin definitions, one module per layer (06 section 5.2). Definitions are written by hand (no classpath
 * scanning), so starting the app does no reflection and a missing dependency fails the AppModuleTest.
 */
val coreModule = module {
    // One clock for the app; tests replace it with a fixed clock to make "today" predictable.
    single<Clock> { Clock.system(BUSINESS_ZONE) }
    single { SettingsStore() }
    // null keyring = no OS credential store; the user then logs in at every start.
    single { CredentialStore(OsKeyring.open()) }
}

val apiModule = module {
    single { AuthSession(get(), get()) }
    // CIO is Ktor's pure-Kotlin engine; the server address is read from the settings on every request.
    single { ApiClient(CIO.create(), { get<SettingsStore>().current.serverUrl.orEmpty() }, get()) }
}

val repositoryModule = module {
    single { DocumentRepository(get()) }
    single { NotificationRepository(get()) }
    single { ReportRepository(get()) }
    single { WorkRepository(get()) }
    single { OutsourcingRepository(get()) }
    single { ProductionRepository(get()) }
    single { ChitRepository(get()) }
    single { ExpenseRepository(get()) }
    single { FinanceRepository(get()) }
    single { PaymentRepository(get()) }
    single { OrderRepository(get()) }
    single { InventoryRepository(get()) }
    single { PurchaseRepository(get()) }
    single { LoanRepository(get()) }
    single { DashboardRepository(get()) }
    single { AuthRepository(get(), get(), get()) }
    single { PartyRepository(get()) }
    single { SystemSettingRepository(get()) }
    single { com.nexora.desktop.repository.UserRepository(get()) }
}

val viewModelModule = module {
    // factory: every screen visit gets a fresh view-model; the navigation entry keeps it while it is open.
    factory { ServerSetupViewModel(get(), get()) }
    factory { OnboardingViewModel(get(), get()) }
    factory { LoginViewModel(get(), get(), get(), get()) }
    factory { SessionViewModel(get(), get(), get(), get()) }
    factory { NavigationViewModel(get()) }
    factory { OfflineStateViewModel(get()) }
    factory { SettingsViewModel(get(), get(), get(), get(), get()) }
    factory { p -> DocumentListViewModel(get(), get(), p.getOrNull<Map<String, String>>() ?: emptyMap()) }
    factory { p -> AttachDocumentViewModel(get(), p.values.getOrNull(0) as String?, p.values.getOrNull(1) as String?) }
    factory { ReminderSettingsViewModel(get()) }
    factory { NotificationListViewModel(get(), get()) }
    factory { NotificationBellViewModel(get()) }
    factory { p -> ReportViewerViewModel(get(), get(), p.getOrNull<String>()?.takeIf { key -> REPORTS.any { it.key == key } } ?: REPORTS.first().key) }
    factory { ProfitAndLossViewModel(get(), get()) }
    factory { p -> WorkRecordsViewModel(get(), get(), p.getOrNull<Map<String, String>>() ?: emptyMap()) }
    factory { p -> RecordWorkViewModel(get(), get(), get(), get(), p.getOrNull<PickOption>()) }
    factory { p -> ReceiveWarpViewModel(get(), get(), p.values[0] as String, p.values.getOrNull(1) as? BigDecimal ?: BigDecimal.ZERO) }
    factory { p -> JobDetailViewModel(get(), get(), p.get()) }
    factory { NewJobViewModel(get()) }
    factory { p -> JobListViewModel(get(), get(), p.getOrNull<Map<String, String>>() ?: emptyMap()) }
    factory { p -> CompleteBatchViewModel(get(), get(), p.values[0] as String, p.values.getOrNull(1) as? BigDecimal ?: BigDecimal.ZERO) }
    factory { p -> BatchDetailViewModel(get(), get(), p.get()) }
    factory { p -> BatchFormViewModel(get(), get(), get(), p.getOrNull<String>()) }
    factory { p -> BatchListViewModel(get(), get(), p.getOrNull<Map<String, String>>() ?: emptyMap()) }
    factory { p -> ChitDetailViewModel(get(), get(), get(), p.get()) }
    factory { NewChitViewModel(get(), get()) }
    factory { ChitListViewModel(get(), get()) }
    factory { p -> LoanDetailViewModel(get(), get(), get(), p.get()) }
    factory { NewLoanViewModel(get(), get(), get()) }
    factory { LoanListViewModel(get(), get()) }
    factory { RecordExpenseViewModel(get(), get(), get(), get()) }
    factory { ExpensesViewModel(get(), get()) }
    @Suppress("UNCHECKED_CAST") factory { p -> TransactionsViewModel(get(), get(), p.values.getOrNull(0) as? String, (p.values.getOrNull(1) as? Map<String, String>) ?: emptyMap()) }
    factory { p -> BalanceAdjustmentViewModel(get(), get(), p.get()) }
    factory { TransferViewModel(get(), get()) }
    factory { NewAccountViewModel(get(), get()) }
    factory { AccountsViewModel(get(), get()) }
    factory { p -> RecordPaymentViewModel(get(), get(), get(), get(), get(), get(), p.values.getOrNull(0) as String?, p.values.getOrNull(1) as String?) }
    factory { p -> PaymentDetailViewModel(get(), get(), p.get()) }
    factory { p -> PaymentListViewModel(get(), get(), p.getOrNull<Map<String, String>>() ?: emptyMap()) }
    factory { p -> OrderDetailViewModel(get(), get(), p.get()) }
    factory { p -> RecordCustomerReturnViewModel(get(), get(), p.get()) }
    factory { p -> OrderFormViewModel(get(), get(), p.getOrNull<String>()) }
    factory { p -> OrderListViewModel(get(), get(), p.getOrNull<Map<String, String>>() ?: emptyMap()) }
    factory { p -> StockAdjustmentViewModel(get(), get(), p.getOrNull<Boolean>() ?: false) }
    factory { ExternalWipViewModel(get(), get()) }
    factory { p -> MovementHistoryViewModel(get(), get(), p.getOrNull<Map<String, String>>() ?: emptyMap()) }
    factory { StockSummaryViewModel(get(), get()) }
    factory { SupplierPayablesViewModel(get(), get()) }
    factory { p -> ReturnDialogViewModel(get(), get(), p.get()) }
    factory { p -> ReceiveSilkViewModel(get(), get(), p.get()) }
    factory { p -> PurchaseDetailViewModel(get(), get(), p.get()) }
    factory { PurchaseFormViewModel(get(), get(), get(), get(), get()) }
    factory { InventoryViewModel(get(), get()) }
    factory { p -> PurchaseListViewModel(get(), get(), p.getOrNull<Map<String, String>>() ?: emptyMap()) }
    factory { p -> PartyDetailViewModel(get(), get(), get(), p.get()) }
    factory { p -> PartyFormViewModel(get(), p.getOrNull<String>()) }
    factory { p -> PartyListViewModel(get(), get(), p.getOrNull<Map<String, String>>() ?: emptyMap()) }
    factory { DashboardViewModel(get(), get()) }
}

val appModules = listOf(coreModule, apiModule, repositoryModule, viewModelModule)

package com.nexora.desktop.ui.parties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.desktop.api.dto.PartyResponse
import com.nexora.desktop.repository.ApiResult
import com.nexora.desktop.ui.components.ConfirmDialog
import com.nexora.desktop.ui.components.DateField
import com.nexora.desktop.ui.components.ErrorBanner
import com.nexora.desktop.ui.components.FigureTile
import com.nexora.desktop.ui.components.LabeledValue
import com.nexora.desktop.ui.components.LoadBanner
import com.nexora.desktop.ui.components.ReloadOn
import com.nexora.desktop.ui.components.ScreenHeader
import com.nexora.desktop.ui.components.StaticTable
import com.nexora.desktop.ui.components.StatusChip
import com.nexora.desktop.ui.components.enumLabel
import com.nexora.desktop.ui.components.screenViewModel
import com.nexora.desktop.util.Dates
import com.nexora.desktop.util.FileDialogs
import com.nexora.desktop.util.IndianFormat.money
import com.nexora.desktop.util.IndianFormat.weight
import com.nexora.desktop.util.s
import com.nexora.desktop.viewmodel.common.Loaded
import com.nexora.desktop.viewmodel.parties.PartyDetailViewModel
import com.nexora.desktop.viewmodel.parties.PartyTab
import com.nexora.desktop.viewmodel.shell.NavigationViewModel
import com.nexora.desktop.viewmodel.shell.Overlay
import com.nexora.desktop.viewmodel.shell.Screen
import com.nexora.desktop.viewmodel.shell.Section
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.Clock

/** S-PTY-03 Party detail: header, activate/deactivate and the role tabs. */
@Composable
fun PartyDetailScreen(nav: NavigationViewModel, partyId: String, reloadTick: Int) {
    val vm: PartyDetailViewModel = screenViewModel(partyId)
    val data by vm.data.collectAsState()
    val action by vm.action.collectAsState()
    val tabs by vm.tabs.collectAsState()
    ReloadOn(reloadTick, vm::reload)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        val party = data.value
        ScreenHeader(party?.name ?: s("common.loading"), onBack = nav::back) {
            if (party != null && nav.can("PARTY_MANAGE")) {
                OutlinedButton(onClick = { nav.show(Overlay.PartyForm(party.id)) }) { Text(s("common.edit")) }
                if (party.status == "ACTIVE") {
                    OutlinedButton(onClick = { vm.ask("deactivate") }) { Text(s("party.deactivate")) }
                } else {
                    OutlinedButton(onClick = { vm.ask("activate") }) { Text(s("party.activate")) }
                }
            }
        }
        if (data.error?.code == "RESOURCE_NOT_FOUND") {
            Text(s("party.notFound"))
            return@Column
        }
        LoadBanner(data.error, data.lastUpdated, vm::reload)
        action.error?.let { ErrorBanner(it, onDismiss = vm::dismiss) }
        if (party == null) return@Column
        PartyHeader(party)
        val visible = vm.visibleTabs(party)
        LaunchedEffect(party.id) { if (tabs.selected == null) vm.select(visible.first()) }
        val selected = tabs.selected ?: visible.first()
        SecondaryTabRow(selectedTabIndex = visible.indexOf(selected).coerceAtLeast(0)) {
            visible.forEach { tab ->
                Tab(selected = tab == selected, onClick = { vm.select(tab) }, text = { Text(s("party.tab.${tab.name}")) })
            }
        }
        Column(Modifier.padding(top = 12.dp)) {
            when (selected) {
                PartyTab.OUTSTANDING -> OutstandingTab(nav, vm, party)
                PartyTab.STATEMENT -> StatementTab(vm)
                PartyTab.PAYABLES -> SupplierTab(nav, vm, party)
                PartyTab.MANUFACTURER_PAYABLE -> ManufacturerTab(nav, vm, party)
                PartyTab.WORKER_PAYABLE -> WorkerTab(nav, vm, party)
                PartyTab.LOANS -> LoansTab(nav, vm)
                PartyTab.ACTIVITY -> ActivityTab(nav, party)
            }
        }
    }
    when (action.confirming) {
        "deactivate" -> ConfirmDialog(
            s("party.deactivate"), data.value?.name.orEmpty(), s("party.deactivateEffect"), s("party.deactivate"),
            onConfirm = { vm.deactivate() }, onCancel = vm::dismiss, busy = action.busy,
        )
        "activate" -> ConfirmDialog(
            s("party.activate"), data.value?.name.orEmpty(), s("party.activateEffect"), s("party.activate"),
            onConfirm = { vm.activate() }, onCancel = vm::dismiss, busy = action.busy,
        )
    }
}

@Composable
private fun PartyHeader(party: PartyResponse) {
    FlowRow {
        LabeledValue(s("party.code"), party.partyCode)
        LabeledValue(s("party.roles"), party.roles.joinToString { enumLabel("PartyRole", it) })
        Column(Modifier.padding(end = 24.dp)) { Text(s("common.status")); StatusChip("PartyStatus", party.status) }
        LabeledValue(s("party.phones"), party.phones.joinToString { "${it.phoneNumber} (${it.label})" }.ifEmpty { "—" })
        LabeledValue(
            s("party.address"),
            listOfNotNull(party.addressLine, party.area, party.city, party.district, party.state, party.pincode).joinToString(", ").ifEmpty { "—" },
        )
        party.defaultCreditDays?.let { LabeledValue(s("party.creditDays"), it.toString()) }
        party.customerCreditDays?.let { LabeledValue(s("party.customerCreditDays"), it.toString()) }
        party.defaultRollingRatePerHour?.let { LabeledValue(s("party.rollingRate"), money(it)) }
        party.defaultWarpingRatePerKg?.let { LabeledValue(s("party.warpingRate"), money(it)) }
    }
}

@Composable
private fun <T> TabLoad(loaded: Loaded<T>, content: @Composable (T) -> Unit) {
    loaded.error?.let { ErrorBanner(it) }
    if (loaded.loading) Text(s("common.loading"))
    loaded.value?.let { content(it) }
}

@Composable
private fun PayButton(nav: NavigationViewModel, party: PartyResponse, purpose: String) {
    if (nav.can("PAYMENT_CREATE") && party.status == "ACTIVE") {
        Button(onClick = { nav.show(Overlay.RecordPayment(party.id, purpose)) }) { Text(s("payment.record")) }
    }
}

@Composable
private fun OutstandingTab(nav: NavigationViewModel, vm: PartyDetailViewModel, party: PartyResponse) {
    val tabs by vm.tabs.collectAsState()
    TabLoad(tabs.outstanding) { o ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FigureTile(s("party.totalOutstanding"), money(o.totalOutstanding))
            FigureTile(s("party.advance"), money(o.advance))
        }
        FlowRow {
            LabeledValue(s("party.age0to30"), money(o.ageing.days0to30))
            LabeledValue(s("party.age31to60"), money(o.ageing.days31to60))
            LabeledValue(s("party.age61to90"), money(o.ageing.days61to90))
            LabeledValue(s("party.ageOver90"), money(o.ageing.over90))
            o.creditDays?.let { LabeledValue(s("party.customerCreditDays"), it.toString()) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PayButton(nav, party, "CUSTOMER")
            OutlinedButton(onClick = {
                val phone = party.phones.firstOrNull()?.phoneNumber
                val msg = com.nexora.desktop.util.WhatsAppHelper.formatCustomerStatementMessage(
                    customerName = party.name,
                    outstanding = money(o.totalOutstanding).removePrefix("₹"),
                    advance = money(o.advance),
                )
                com.nexora.desktop.util.WhatsAppHelper.openChat(phone, msg)
            }) {
                Text("💬 " + s("whatsapp.shareStatement"))
            }
        }
        StaticTable(
            listOf(s("order.number"), s("common.date"), s("common.status"), s("common.amount"), s("common.allocated"), s("common.outstanding"), s("party.ageDays"), s("order.dueDate"), s("order.overdue")),
            o.orders.map {
                listOf(
                    it.orderNumber,
                    Dates.formatIso(it.orderDate),
                    enumLabel("OrderStatus", it.status),
                    money(it.amount),
                    money(it.allocated),
                    money(it.outstanding),
                    it.ageDays.toString(),
                    it.dueDate?.let(Dates::formatIso).orEmpty(),
                    if (it.overdue == true) s("order.overdue") else "",
                )
            } + o.openingObligations.map {
                listOf(s("party.openingBalance"), Dates.formatIso(it.obligationDate), it.referenceNote.orEmpty(), money(it.originalAmount), money(it.allocated), money(it.outstanding), it.ageDays?.toString().orEmpty(), "", "")
            },
            numeric = setOf(3, 4, 5, 6),
            onRowClick = { i -> o.orders.getOrNull(i)?.let { nav.push(Screen.OrderDetail(it.orderId)) } },
        )
    }
}

@Composable
private fun StatementTab(vm: PartyDetailViewModel) {
    val tabs by vm.tabs.collectAsState()
    val clock: Clock = koinInject()
    val scope = rememberCoroutineScope()
    var from by remember { mutableStateOf(Dates.formatIso(tabs.statementFrom)) }
    var to by remember { mutableStateOf(Dates.formatIso(tabs.statementTo)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateField(s("filter.from"), from, { from = it }, Modifier.width(180.dp))
        DateField(s("filter.to"), to, { to = it }, Modifier.width(180.dp))
        OutlinedButton(onClick = {
            val f = Dates.toIso(from, clock)
            val t = Dates.toIso(to, clock)
            if (f != null && t != null) vm.setStatementRange(f, t)
        }) { Text(s("common.show")) }
        OutlinedButton(onClick = {
            scope.launch {
                val csv = vm.statementCsv()
                if (csv is ApiResult.Success) FileDialogs.save(s("common.exportCsv"), "statement-${tabs.statementFrom}-${tabs.statementTo}.csv", csv.value)
            }
        }) { Text(s("common.exportCsv")) }
    }
    TabLoad(tabs.statement) { st ->
        FlowRow {
            LabeledValue(s("party.openingBalance"), money(st.openingBalance))
            LabeledValue(s("party.closingBalance"), money(st.closingBalance))
        }
        StaticTable(
            listOf(s("common.date"), s("party.kind"), s("common.number"), s("party.debit"), s("party.credit"), s("party.runningBalance")),
            st.lines.map { listOf(Dates.formatIso(it.date), enumLabel("StatementKind", it.kind), it.number.orEmpty(), money(it.debit), money(it.credit), money(it.runningBalance)) },
            numeric = setOf(3, 4, 5),
        )
    }
}

@Composable
private fun SupplierTab(nav: NavigationViewModel, vm: PartyDetailViewModel, party: PartyResponse) {
    val tabs by vm.tabs.collectAsState()
    TabLoad(tabs.payables) { p ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FigureTile(s("party.totalPayable"), money(p.totalPayable))
            FigureTile(s("party.totalOverdue"), money(p.totalOverdue))
            FigureTile(s("party.dueWithin7"), money(p.dueWithin7Days))
        }
        PayButton(nav, party, "SUPPLIER")
        StaticTable(
            listOf(s("purchase.number"), s("common.date"), s("purchase.dueDate"), s("purchase.payable"), s("common.allocated"), s("common.outstanding"), s("purchase.overdueDays")),
            p.purchases.map {
                listOf(it.purchaseNumber, Dates.formatIso(it.purchaseDate), Dates.formatIso(it.dueDate), money(it.payable), money(it.allocated), money(it.outstanding), if (it.overdue) it.overdueDays.toString() else "")
            },
            numeric = setOf(3, 4, 5, 6),
            onRowClick = { i -> nav.push(Screen.PurchaseDetail(p.purchases[i].purchaseId)) },
        )
    }
}

@Composable
private fun ManufacturerTab(nav: NavigationViewModel, vm: PartyDetailViewModel, party: PartyResponse) {
    val tabs by vm.tabs.collectAsState()
    TabLoad(tabs.manufacturer) { m ->
        FigureTile(s("party.totalPayable"), money(m.totalPayable))
        PayButton(nav, party, "MANUFACTURER")
        StaticTable(
            listOf(s("job.number"), s("job.receivedKg"), s("common.rate"), s("purchase.payable"), s("common.allocated"), s("common.outstanding")),
            m.jobs.map { listOf(it.jobNumber, weight(it.receivedKg), money(it.ratePerKg), money(it.payable), money(it.allocated), money(it.outstanding)) },
            numeric = setOf(1, 2, 3, 4, 5),
            onRowClick = { i -> nav.push(Screen.JobDetail(m.jobs[i].jobId)) },
        )
    }
}

@Composable
private fun WorkerTab(nav: NavigationViewModel, vm: PartyDetailViewModel, party: PartyResponse) {
    val tabs by vm.tabs.collectAsState()
    val clock: Clock = koinInject()
    var from by remember { mutableStateOf(Dates.formatIso(tabs.workerFrom)) }
    var to by remember { mutableStateOf(Dates.formatIso(tabs.workerTo)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateField(s("filter.from"), from, { from = it }, Modifier.width(180.dp))
        DateField(s("filter.to"), to, { to = it }, Modifier.width(180.dp))
        OutlinedButton(onClick = {
            val f = Dates.toIso(from, clock)
            val t = Dates.toIso(to, clock)
            if (f != null && t != null) vm.setWorkerRange(f, t)
        }) { Text(s("common.show")) }
    }
    TabLoad(tabs.worker) { w ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FigureTile(s("work.value"), money(w.workValue))
            FigureTile(s("work.paid"), money(w.paid))
            FigureTile(s("work.pending"), money(w.pending))
        }
        PayButton(nav, party, "WORKER")
        StaticTable(
            listOf(s("common.date"), s("work.type"), s("common.amount"), s("common.allocated"), s("common.outstanding")),
            w.records.map { listOf(Dates.formatIso(it.workDate), enumLabel("WorkType", it.workType), money(it.amount), money(it.allocated), money(it.outstanding)) },
            numeric = setOf(2, 3, 4),
        )
    }
}

@Composable
private fun LoansTab(nav: NavigationViewModel, vm: PartyDetailViewModel) {
    val tabs by vm.tabs.collectAsState()
    TabLoad(tabs.loans) { loans ->
        StaticTable(
            listOf(s("loan.number"), s("loan.type"), s("loan.startDate"), s("loan.principal"), s("loan.outstanding"), s("common.status")),
            loans.map { listOf(it.loanNumber, enumLabel("LoanType", it.loanType), Dates.formatIso(it.startDate), money(it.principalAmount), money(it.outstandingPrincipal), enumLabel("LoanStatus", it.status)) },
            numeric = setOf(3, 4),
            onRowClick = { i -> if (nav.can("LOAN_MANAGE")) nav.push(Screen.LoanDetail(loans[i].id)) },
        )
    }
}

/** Links to the lists filtered by this party (06 S-PTY-03 Activity). */
@Composable
private fun ActivityTab(nav: NavigationViewModel, party: PartyResponse) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val links = listOfNotNull(
            ("SUPPLIER" in party.roles).takeIf { it }?.let { Triple(Section.PURCHASES, "supplierId", "party.activityPurchases") },
            ("CUSTOMER" in party.roles).takeIf { it }?.let { Triple(Section.ORDERS, "customerId", "party.activityOrders") },
            Triple(Section.PAYMENTS, "partyId", "party.activityPayments"),
            ("MANUFACTURER" in party.roles).takeIf { it }?.let { Triple(Section.OUTSOURCING, "manufacturerId", "party.activityJobs") },
            ("WORKER" in party.roles).takeIf { it }?.let { Triple(Section.WORK, "workerId", "party.activityWork") },
        )
        links.filter { (section, _, _) -> section in nav.state.value.sections }.forEach { (section, filter, label) ->
            OutlinedButton(onClick = { nav.push(Screen.Root(section, mapOf(filter to party.id))) }) { Text(s(label)) }
        }
    }
}

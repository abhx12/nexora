package com.nexora.desktop.ui.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.desktop.api.dto.OrderResponse
import com.nexora.desktop.ui.components.ConfirmDialog
import com.nexora.desktop.ui.components.DateField
import com.nexora.desktop.ui.components.ErrorBanner
import com.nexora.desktop.ui.components.FlagChip
import com.nexora.desktop.ui.components.LabeledValue
import com.nexora.desktop.ui.components.LoadBanner
import com.nexora.desktop.ui.components.ReloadOn
import com.nexora.desktop.ui.components.ScreenHeader
import com.nexora.desktop.ui.components.Section
import com.nexora.desktop.ui.components.StaticTable
import com.nexora.desktop.ui.components.StatusChip
import com.nexora.desktop.ui.components.enumLabel
import com.nexora.desktop.ui.components.screenViewModel
import com.nexora.desktop.util.Dates
import com.nexora.desktop.util.IndianFormat.money
import com.nexora.desktop.util.IndianFormat.weight
import com.nexora.desktop.util.s
import com.nexora.desktop.ui.documents.DocumentsSection
import com.nexora.desktop.viewmodel.orders.OrderAction
import com.nexora.desktop.viewmodel.orders.OrderDetailViewModel
import com.nexora.desktop.viewmodel.shell.NavigationViewModel
import com.nexora.desktop.viewmodel.shell.Overlay
import com.nexora.desktop.viewmodel.shell.Screen
import org.koin.compose.koinInject
import java.time.Clock

/** S-ORD-03 Order detail with the status action bar (only legal next steps, each confirmed). */
@Composable
fun OrderDetailScreen(nav: NavigationViewModel, orderId: String, reloadTick: Int) {
    val vm: OrderDetailViewModel = screenViewModel(orderId)
    val data by vm.data.collectAsState()
    val action by vm.action.collectAsState()
    val returns by vm.returns.collectAsState()
    ReloadOn(reloadTick) {
        vm.reload()
        vm.loadReturns()
    }
    val o = data.value
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(o?.orderNumber ?: s("common.loading"), onBack = nav::back) {
            if (o != null) {
                vm.actionsFor(o).forEach { a ->
                    val allowed = if (a == OrderAction.PAY) nav.can("PAYMENT_CREATE") else nav.can("ORDER_MANAGE")
                    if (!allowed) return@forEach
                    val label = s("order.action.${a.name}")
                    when (a) {
                        OrderAction.EDIT -> OutlinedButton(onClick = { nav.show(Overlay.OrderForm(o.id)) }) { Text(label) }
                        OrderAction.PAY -> OutlinedButton(onClick = { nav.show(Overlay.RecordPayment(o.customer.id, "CUSTOMER")) }) { Text(label) }
                        OrderAction.RETURN -> OutlinedButton(onClick = { vm.ask(a.name) }) { Text(label) }
                        OrderAction.BILL -> OutlinedButton(onClick = { vm.ask(a.name) }) { Text(label) }
                        else -> Button(onClick = { vm.ask(a.name) }) { Text(label) }
                    }
                }
                OutlinedButton(onClick = {
                    val firstItem = o.items.firstOrNull()
                    val product = firstItem?.productType ?: "Silk Warp"
                    val weight = firstItem?.requiredWeightKg?.toString() ?: "0"
                    val msg = com.nexora.desktop.util.WhatsAppHelper.formatSalesBillMessage(
                        customerName = o.customer.name,
                        orderNumber = o.orderNumber,
                        product = product,
                        weightKg = weight,
                        totalAmount = o.totalAmount.toString(),
                    )
                    com.nexora.desktop.util.WhatsAppHelper.openChat(null, msg)
                }) {
                    Text("💬 " + s("whatsapp.shareBill"))
                }
                if (nav.can("DOCUMENT_MANAGE")) OutlinedButton(onClick = { nav.show(Overlay.AttachDocument("ORDER", o.id)) }) { Text(s("document.attach")) }
            }
        }
        LoadBanner(data.error, data.lastUpdated, vm::reload)
        action.error?.let { ErrorBanner(it, onDismiss = vm::dismiss) }
        if (o != null) {
            OrderBody(o, nav, returns)
            if (nav.can("DOCUMENT_MANAGE")) DocumentsSection(nav, "ORDER", o.id, reloadTick)
        }
    }
    if (o == null) return
    val confirming = action.confirming?.let { name -> OrderAction.entries.firstOrNull { it.name == name } } ?: return
    val effect = s("order.effect.${confirming.name}")
    when (confirming) {
        OrderAction.CONFIRM -> ConfirmDialog(s("order.action.CONFIRM"), o.orderNumber, effect, s("order.action.CONFIRM"), { vm.confirm() }, vm::dismiss, busy = action.busy)
        OrderAction.READY -> ConfirmDialog(s("order.action.READY"), o.orderNumber, effect, s("order.action.READY"), { vm.ready() }, vm::dismiss, busy = action.busy)
        OrderAction.COMPLETE -> ConfirmDialog(s("order.action.COMPLETE"), o.orderNumber, effect, s("order.action.COMPLETE"), { vm.complete() }, vm::dismiss, busy = action.busy)
        OrderAction.CANCEL -> ConfirmDialog(
            s("order.action.CANCEL"), o.orderNumber, effect, s("order.action.CANCEL"), { vm.cancel(it.orEmpty()) }, vm::dismiss,
            reasonRequired = true, busy = action.busy,
        )
        OrderAction.BILL -> ConfirmDialog(
            s("order.action.BILL"), o.orderNumber, effect, s("order.action.BILL"), { vm.generateBill() }, vm::dismiss, busy = action.busy,
        )
        OrderAction.DELIVER -> {
            val clock: Clock = koinInject()
            var date by remember { mutableStateOf(Dates.format(Dates.today(clock))) }
            ConfirmDialog(
                s("order.action.DELIVER"), o.orderNumber, effect, s("order.action.DELIVER"), { vm.deliver(date) }, vm::dismiss, busy = action.busy,
                extra = { DateField(s("order.deliveryDate"), date, { date = it }) },
            )
        }
        OrderAction.RETURN -> RecordCustomerReturnDialog(
            order = o,
            onSaved = {
                vm.reload()
                vm.loadReturns()
                vm.dismiss()
            },
            onCancel = vm::dismiss,
        )
        else -> Unit
    }
}

@Composable
private fun OrderBody(o: OrderResponse, nav: NavigationViewModel, returns: List<com.nexora.desktop.api.dto.CustomerReturnResponse>) {
    FlowRow {
        Column(Modifier.padding(end = 24.dp)) {
            StatusChip("OrderStatus", o.status)
            StatusChip("PaymentStatusView", o.paymentStatus, Modifier.padding(top = 4.dp))
            if (o.late) FlagChip(s("flag.late"), Modifier.padding(top = 4.dp))
            if (o.locked) FlagChip(s("flag.locked"), Modifier.padding(top = 4.dp))
        }
        LabeledValue(s("order.customer"), o.customer.name)
        LabeledValue(s("order.date"), Dates.formatIso(o.orderDate))
        LabeledValue(s("order.expectedDate"), Dates.formatIso(o.requiredDate))
        o.deliveredOn?.let { LabeledValue(s("order.deliveredOn"), Dates.formatIso(it)) }
        LabeledValue(s("order.total"), money(o.totalAmount))
        LabeledValue(s("common.allocated"), money(o.allocatedAmount))
        LabeledValue(s("common.outstanding"), money(o.outstandingAmount))
        o.notes?.let { LabeledValue(s("common.notes"), it) }
    }
    Section(s("order.items")) {
        StaticTable(
            listOf(s("purchase.product"), s("order.requiredWeight"), s("purchase.rate"), s("common.amount"), s("common.allocated"), s("common.outstanding"), s("order.payment")),
            o.items.sortedBy { it.lineNo }.map {
                listOf(
                    enumLabel("ProductType", it.productType) + (it.warpSpecification?.let { w -> " ($w)" } ?: ""),
                    weight(it.requiredWeightKg), money(it.ratePerKg), money(it.amount),
                    money(it.allocatedAmount), money(it.outstandingAmount), enumLabel("PaymentStatusView", it.paymentStatus),
                )
            },
            numeric = setOf(1, 2, 3, 4, 5),
        )
    }
    if (returns.isNotEmpty()) {
        Section(s("order.returns")) {
            StaticTable(
                listOf(
                    s("common.number"),
                    s("common.date"),
                    s("order.returnItem"),
                    s("order.returnWeight"),
                    s("purchase.rate"),
                    s("order.creditedAmount"),
                    s("order.usable"),
                    s("common.reason"),
                ),
                returns.map { r ->
                    listOf(
                        r.returnNumber,
                        Dates.formatIso(r.returnDate),
                        "${r.lineNo}. ${enumLabel("ProductType", r.productType)}",
                        weight(r.returnedWeightKg),
                        money(r.ratePerKg),
                        money(r.creditedAmount),
                        if (r.usable) s("order.usable") else s("order.damaged"),
                        r.reason,
                    )
                },
                numeric = setOf(3, 4, 5),
            )
        }
    }
    if (o.productionBatches.isNotEmpty() || o.outsourcingJobs.isNotEmpty()) {
        Section(s("order.linkedWork")) {
            StaticTable(
                listOf(s("common.number"), s("common.status"), s("order.allocatedWeight")),
                o.productionBatches.map { listOf(it.batchNumber.orEmpty(), enumLabel("BatchStatus", it.status), weight(it.allocatedWeightKg)) } +
                    o.outsourcingJobs.map { listOf(it.jobNumber.orEmpty(), enumLabel("JobStatus", it.status), weight(it.allocatedWeightKg)) },
                numeric = setOf(2),
                onRowClick = { i ->
                    if (i < o.productionBatches.size) nav.push(Screen.BatchDetail(o.productionBatches[i].id))
                    else nav.push(Screen.JobDetail(o.outsourcingJobs[i - o.productionBatches.size].id))
                },
            )
        }
    }
}

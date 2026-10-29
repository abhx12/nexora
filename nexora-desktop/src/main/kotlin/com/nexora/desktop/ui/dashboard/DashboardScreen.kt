package com.nexora.desktop.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.desktop.api.dto.DashboardResponse
import com.nexora.desktop.ui.components.FigureTile
import com.nexora.desktop.ui.components.FlagChip
import com.nexora.desktop.ui.components.LoadBanner
import com.nexora.desktop.ui.components.ReloadOn
import com.nexora.desktop.ui.components.ScreenHeader
import com.nexora.desktop.ui.components.enumLabel
import com.nexora.desktop.ui.components.screenViewModel
import com.nexora.desktop.ui.theme.AppColors
import com.nexora.desktop.util.Dates
import com.nexora.desktop.util.IndianFormat.money
import com.nexora.desktop.util.IndianFormat.weight
import com.nexora.desktop.util.s
import com.nexora.desktop.viewmodel.dashboard.DashboardViewModel
import com.nexora.desktop.viewmodel.shell.NavigationViewModel
import com.nexora.desktop.viewmodel.shell.Screen
import com.nexora.desktop.viewmodel.shell.Section as NavSection
import kotlinx.coroutines.delay

/**
 * S-DASH-01 Owner dashboard: three bands (money available, obligations, stock and work) plus the supplier dues
 * and recent transactions panels. Every figure is the server's; obligations are never mixed into money available.
 */
@Composable
fun DashboardScreen(nav: NavigationViewModel, reloadTick: Int) {
    val vm: DashboardViewModel = screenViewModel()
    val data by vm.data.collectAsState()
    val advisory by vm.advisory.collectAsState()
    ReloadOn(reloadTick) {
        vm.reload()
        vm.loadAdvisory()
    }
    // Auto refresh every 5 minutes while this screen is shown; leaving the screen cancels the loop.
    LaunchedEffect(Unit) {
        while (true) {
             delay(DashboardViewModel.REFRESH_MILLIS)
             vm.reload()
             vm.loadAdvisory()
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(s("nav.dashboard")) {
            data.value?.let { Text(s("dashboard.asOf", Dates.formatIsoTimestamp(it.asOf))) }
        }
        LoadBanner(data.error, data.lastUpdated, vm::reload)
        data.value?.let { d ->
            GettingStartedCard(d, nav)
            AiAdvisoryCard(advisory)
            DashboardBands(d, nav)
        }
    }
}

@Composable
private fun AiAdvisoryCard(advisory: com.nexora.desktop.api.dto.AiAdvisoryResponse?) {
    if (advisory == null || advisory.insights.isEmpty()) return
    val isTa = com.nexora.desktop.util.Strings.currentLocale() == "ta"

    val statusColor = when (advisory.healthStatus) {
        "HEALTHY" -> MaterialTheme.colorScheme.primary
        "ATTENTION_NEEDED" -> AppColors.slow
        else -> MaterialTheme.colorScheme.error
    }
    val statusLabel = when (advisory.healthStatus) {
        "HEALTHY" -> s("ai.advisory.healthy")
        "ATTENTION_NEEDED" -> s("ai.advisory.attention")
        else -> s("ai.advisory.critical")
    }

    DashboardBox(
        title = s("ai.advisory.title"),
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                advisory.liquidityCoverageRatio?.let {
                    FlagChip("${s("ai.advisory.liquidity")}: ${it}x")
                }
                advisory.silkRunwayDays?.let {
                    FlagChip("${s("ai.advisory.silkRunway")}: $it d")
                }
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                ) {
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            advisory.insights.forEach { ins ->
                val title = if (isTa && ins.titleTa.isNotBlank()) ins.titleTa else ins.titleEn
                val detail = if (isTa && ins.detailTa.isNotBlank()) ins.detailTa else ins.detailEn
                val action = if (isTa && ins.actionableStepTa.isNotBlank()) ins.actionableStepTa else ins.actionableStepEn

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Text(
                            when (ins.severity) {
                                "SUCCESS" -> "🟢"
                                "WARNING" -> "🟡"
                                "CRITICAL" -> "🔴"
                                else -> "ℹ️"
                            },
                            modifier = Modifier.padding(end = 10.dp, top = 2.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (action.isNotBlank()) {
                                Text(
                                    "👉 $action",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBox(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                action?.invoke()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            content()
        }
    }
}

@Composable
private fun DashboardBands(d: DashboardResponse, nav: NavigationViewModel) {
    val money = { nav.open(NavSection.MONEY) }
    DashboardBox(s("dashboard.moneyAvailable")) {
        FlowRow {
            FigureTile(s("dashboard.totalAvailable"), money(d.totalAvailable), onClick = money)
            FigureTile(s("dashboard.cash"), money(d.cash), onClick = money)
            d.banks.forEach { FigureTile(it.account.accountName, money(it.balance), onClick = money) }
        }
    }
    DashboardBox(s("dashboard.obligations")) {
        FlowRow {
            FigureTile(s("dashboard.customerReceivables"), money(d.customerReceivables), onClick = { nav.push(Screen.Report("customer-outstanding")) })
            FigureTile(s("dashboard.customerAdvances"), money(d.customerAdvances))
            FigureTile(s("dashboard.supplierPayables"), money(d.supplierPayables), onClick = { nav.push(Screen.SupplierPayables) })
            FigureTile(s("dashboard.supplierOverdue"), money(d.supplierOverdue), valueColour = AppColors.negative, onClick = { nav.push(Screen.SupplierPayables) })
            FigureTile(s("dashboard.manufacturerPayable"), money(d.manufacturerPayable), onClick = { nav.open(NavSection.OUTSOURCING) })
            FigureTile(s("dashboard.workerPayable"), money(d.workerPayable), onClick = { nav.open(NavSection.WORK) })
            FigureTile(s("dashboard.loanOutstanding"), money(d.loanOutstandingPrincipal), onClick = { nav.open(NavSection.LOANS) })
            FigureTile(s("dashboard.chitCommitments"), money(d.chitMonthlyCommitments), onClick = { nav.open(NavSection.CHITS) })
        }
    }
    DashboardBox(s("dashboard.stockAndWork")) {
        val stock = { nav.open(NavSection.INVENTORY) }
        FlowRow {
            FigureTile(s("dashboard.rawSilk"), weight(d.stock.rawSilkKg), onClick = stock)
            FigureTile(s("dashboard.internalWip"), weight(d.stock.internalWipKg), onClick = stock)
            FigureTile(s("dashboard.externalWip"), weight(d.stock.externalWipKg), onClick = { nav.push(Screen.ExternalWip) })
            FigureTile(s("dashboard.finishedSilk"), weight(d.stock.finishedSilkWarpKg), onClick = stock)
            FigureTile(s("dashboard.finishedKora"), weight(d.stock.finishedKoraWarpKg), onClick = stock)
            FigureTile(s("dashboard.vuda"), weight(d.stock.vudaWarpKg), onClick = stock)
        }
        FlowRow {
            d.ordersByStatus.forEach { (status, count) ->
                FigureTile(s("dashboard.ordersIn", enumLabel("OrderStatus", status)), count.toString(), onClick = { nav.open(NavSection.ORDERS) })
            }
            FigureTile(s("dashboard.lateOrders"), d.lateOrders.toString(), onClick = { nav.open(NavSection.ORDERS) })
            d.productionBatches.forEach { (status, count) ->
                FigureTile(s("dashboard.batchesIn", enumLabel("BatchStatus", status)), count.toString(), onClick = { nav.open(NavSection.PRODUCTION) })
            }
            FigureTile(s("dashboard.openJobs"), d.openOutsourcingJobs.toString(), onClick = { nav.open(NavSection.OUTSOURCING) })
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            DashboardBox(
                title = s("dashboard.dues"),
                action = {
                    TextButton(onClick = { nav.push(Screen.SupplierPayables) }) {
                        Text(s("common.show"), style = MaterialTheme.typography.labelMedium)
                    }
                },
            ) {
                if (d.upcomingSupplierDues.isEmpty()) {
                    Text(
                        s("dashboard.noDues"),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            s("purchase.supplier"),
                            Modifier.weight(1.2f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            s("purchase.number"),
                            Modifier.width(110.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            s("purchase.dueDate"),
                            Modifier.width(95.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            s("common.outstanding"),
                            Modifier.width(115.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            s("common.status"),
                            Modifier.width(85.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    d.upcomingSupplierDues.forEach { due ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { nav.push(Screen.PurchaseDetail(due.purchaseId)) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                due.supplier.name,
                                Modifier.weight(1.2f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                due.purchaseNumber,
                                Modifier.width(110.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                Dates.formatIso(due.dueDate),
                                Modifier.width(95.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                money(due.outstanding),
                                Modifier.width(115.dp),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Box(
                                Modifier.width(85.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (due.overdue) {
                                    FlagChip(s("flag.overdue", due.overdueDays))
                                } else {
                                    Text(
                                        "On Time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    }
                }
            }
        }
        Column(Modifier.weight(1f)) {
            DashboardBox(
                title = s("dashboard.recentTransactions"),
                action = {
                    TextButton(onClick = { nav.open(NavSection.MONEY) }) {
                        Text(s("common.show"), style = MaterialTheme.typography.labelMedium)
                    }
                },
            ) {
                if (d.recentTransactions.isEmpty()) {
                    Text(
                        s("money.noTransactions"),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            s("common.date"),
                            Modifier.width(95.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            s("money.type"),
                            Modifier.weight(1.2f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            s("account.name"),
                            Modifier.width(120.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            s("common.amount"),
                            Modifier.width(125.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "",
                            Modifier.width(36.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    d.recentTransactions.forEach { t ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { nav.push(Screen.Transactions(t.account.id)) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                Dates.formatIso(t.transactionDate),
                                Modifier.width(95.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                enumLabel("TransactionType", t.transactionType),
                                Modifier.weight(1.2f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                t.account.accountName,
                                Modifier.width(120.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                (if (t.direction == "OUT") "- " else "+ ") + money(t.amount),
                                Modifier.width(125.dp),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.SemiBold,
                                color = if (t.direction == "OUT") AppColors.negative else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Box(
                                Modifier.width(36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "→",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    }
                }
            }
        }
    }
}

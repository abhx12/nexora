package com.nexora.desktop.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.desktop.api.ApiError
import com.nexora.desktop.repository.ApiResult
import com.nexora.desktop.ui.components.DateRangeFilter
import com.nexora.desktop.ui.components.EmptyState
import com.nexora.desktop.ui.components.ErrorBanner
import com.nexora.desktop.ui.components.FilterDropdown
import com.nexora.desktop.ui.components.LoadBanner
import com.nexora.desktop.ui.components.MoneyField
import com.nexora.desktop.ui.components.PartyPicker
import com.nexora.desktop.ui.components.PickOption
import com.nexora.desktop.ui.components.ReloadOn
import com.nexora.desktop.ui.components.ScreenHeader
import com.nexora.desktop.ui.components.TextInput
import com.nexora.desktop.ui.components.enumLabel
import com.nexora.desktop.ui.components.screenViewModel
import com.nexora.desktop.ui.expenses.EXPENSE_CATEGORIES
import com.nexora.desktop.util.Dates
import com.nexora.desktop.util.FileDialogs
import com.nexora.desktop.util.Strings
import com.nexora.desktop.util.s
import com.nexora.desktop.viewmodel.reports.REPORTS
import com.nexora.desktop.viewmodel.reports.ReportDef
import com.nexora.desktop.viewmodel.reports.ReportFilter
import com.nexora.desktop.viewmodel.reports.ReportViewerViewModel
import com.nexora.desktop.viewmodel.shell.NavigationViewModel
import com.nexora.desktop.viewmodel.shell.Screen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.koin.compose.koinInject
import java.time.Clock

enum class ReportCategory(val labelKey: String, val icon: String, val descKey: String) {
    FINANCIAL("report.category.financial", "🏦", "report.category.financial.desc"),
    SALES("report.category.sales", "📈", "report.category.sales.desc"),
    PROCUREMENT("report.category.procurement", "📥", "report.category.procurement.desc"),
    OPERATIONS("report.category.operations", "🏭", "report.category.operations.desc"),
}

data class ReportMeta(
    val category: ReportCategory,
    val icon: String,
    val badge: String,
    val highlights: List<String>,
)

val REPORT_METAS = mapOf(
    // Financial & Treasury
    "cash-bank" to ReportMeta(ReportCategory.FINANCIAL, "🏦", "TREASURY", listOf("Opening & closing bank/cash", "Receipts & payments by type", "Per-account ledger audit")),
    "loans-and-chits" to ReportMeta(ReportCategory.FINANCIAL, "🪙", "CREDIT", listOf("Loan principal & outstanding", "Interest paid schedules", "Chit monthly tallies & payouts")),
    "expenses" to ReportMeta(ReportCategory.FINANCIAL, "🧾", "EXPENSES", listOf("Categorized business expenses", "Monthly spending trends", "Owner personal drawings")),
    "profit-and-loss" to ReportMeta(ReportCategory.FINANCIAL, "📊", "P&L", listOf("Delivered sales revenue", "Operating & interest costs", "Net operating profit")),
    "profitability" to ReportMeta(ReportCategory.FINANCIAL, "📈", "MARGINS", listOf("Product profit margins", "Customer profitability", "Weaving unit return analysis")),

    // Sales & Customers
    "customer-outstanding" to ReportMeta(ReportCategory.SALES, "👥", "AGING", listOf("Customer receivables", "Advance balances held", "Oldest overdue aging (days)")),
    "customer-statement" to ReportMeta(ReportCategory.SALES, "📜", "STATEMENT", listOf("Full transaction history", "Orders, returns & receipts", "Running account balance")),
    "sales" to ReportMeta(ReportCategory.SALES, "🏷️", "REVENUE", listOf("Total orders booked", "Delivered sales revenue", "Silk weight delivered (kg)")),

    // Procurement & Payables
    "purchases" to ReportMeta(ReportCategory.PROCUREMENT, "📥", "PURCHASES", listOf("Ordered vs received yarn", "Accepted & rejected silk", "Net payable purchase values")),
    "supplier-payables" to ReportMeta(ReportCategory.PROCUREMENT, "📋", "PAYABLES", listOf("Due & overdue balances", "1–7, 8–15, >15 aging buckets", "Supplier bill tracking")),

    // Operations & Manufacturing
    "inventory-movements" to ReportMeta(ReportCategory.OPERATIONS, "📦", "STOCK", listOf("Opening & closing stock", "Issues, receipts & transfers", "Location-wise stock ledger")),
    "production" to ReportMeta(ReportCategory.OPERATIONS, "🏭", "BATCHES", listOf("Input yarn vs output silk", "Batch conversion efficiency", "Process wastage & difference")),
    "wastage" to ReportMeta(ReportCategory.OPERATIONS, "♻️", "WASTAGE", listOf("Wastage source breakdown", "Monthly yarn loss analysis", "Shop floor discrepancy audit")),
    "outsourcing" to ReportMeta(ReportCategory.OPERATIONS, "🔄", "SUBCONTRACT", listOf("Silk issued to weavers", "Returned yarn from units", "Outside stock & job balances")),
    "worker-payable" to ReportMeta(ReportCategory.OPERATIONS, "🔨", "PAYROLL", listOf("Weekly piece-rate labor", "Earned vs disbursed wages", "Pending worker balances")),
    "worker-efficiency" to ReportMeta(ReportCategory.OPERATIONS, "⚡", "PRODUCTIVITY", listOf("Rolling & warping hourly output", "Kg produced per shift", "Workforce wage efficiency")),
)

/** S-RPT-01 Reports home: executive analytics hub with category filters & report cards. */
@Composable
fun ReportsHomeScreen(nav: NavigationViewModel) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ReportCategory?>(null) }

    val filteredReports = remember(query, selectedCategory) {
        REPORTS.filter { report ->
            val meta = REPORT_METAS[report.key]
            val matchesCategory = selectedCategory == null || meta?.category == selectedCategory
            val title = Strings.getOrNull("report.${report.key}").orEmpty()
            val help = Strings.getOrNull("report.${report.key}.help").orEmpty()
            val matchesQuery = query.isBlank() || title.contains(query, ignoreCase = true) || help.contains(query, ignoreCase = true) || (meta?.badge?.contains(query, ignoreCase = true) == true)
            matchesCategory && matchesQuery
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(s("nav.reports"))
        Text(
            s("report.subtitle"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextInput(
                label = s("report.search"),
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.width(360.dp),
            )
            Text(
                s("report.totalReports", filteredReports.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FlowRow(
            Modifier.padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val allSelected = selectedCategory == null
            Surface(
                onClick = { selectedCategory = null },
                shape = RoundedCornerShape(20.dp),
                color = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (allSelected) 4.dp else 1.dp,
            ) {
                Text(
                    "${s("report.category.all")} (${REPORTS.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium),
                    color = if (allSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }

            ReportCategory.entries.forEach { cat ->
                val isSelected = selectedCategory == cat
                val count = REPORTS.count { REPORT_METAS[it.key]?.category == cat }
                Surface(
                    onClick = { selectedCategory = if (isSelected) null else cat },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = if (isSelected) 4.dp else 1.dp,
                ) {
                    Text(
                        "${cat.icon} ${s(cat.labelKey)} ($count)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (filteredReports.isEmpty()) {
            EmptyState(message = "No matching reports found for \"$query\"")
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            ) {
                filteredReports.forEach { report ->
                    ReportCard(report = report, onOpen = { nav.push(Screen.Report(report.key)) })
                }
            }
        }
    }
}

@Composable
private fun ReportCard(report: ReportDef, onOpen: () -> Unit) {
    val meta = REPORT_METAS[report.key]
    val icon = meta?.icon ?: "📈"
    val badge = meta?.badge ?: "REPORT"
    val highlights = meta?.highlights.orEmpty()

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.width(360.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(icon, style = MaterialTheme.typography.titleLarge)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                ) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                s("report.${report.key}"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                s("report.${report.key}.help"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
            )

            if (highlights.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    highlights.forEach { h ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text(h, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    s("report.open"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "→",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** S-RPT-02 Report viewer: breadcrumbs, quick presets, filter panel, KPI cards & export CSV. */
@Composable
fun ReportViewerScreen(nav: NavigationViewModel, reportKey: String, reloadTick: Int) {
    val clock: Clock = koinInject()
    val vm: ReportViewerViewModel = screenViewModel(reportKey)
    val data by vm.data.collectAsState()
    val filters by vm.filters.collectAsState()
    val scope = rememberCoroutineScope()
    var exportError by remember { mutableStateOf<ApiError?>(null) }
    var party by remember { mutableStateOf<PickOption?>(null) }
    ReloadOn(reloadTick, vm::load)

    val meta = REPORT_METAS[reportKey]
    val icon = meta?.icon ?: "📊"
    val badge = meta?.badge ?: "REPORT"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = nav::back) {
                    Text("← " + s("nav.reports"), style = MaterialTheme.typography.bodyMedium)
                }
                Text("/", color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "$icon " + s("report.${reportKey}"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 6.dp),
                ) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = vm::load) {
                    Text(s("common.reload"))
                }
                Button(
                    onClick = {
                        scope.launch {
                            when (val r = vm.csv()) {
                                is ApiResult.Success -> FileDialogs.save(s("common.exportCsv"), vm.csvName(), r.value)
                                is ApiResult.Failure -> exportError = r.error
                            }
                        }
                    },
                    enabled = vm.ready(),
                ) {
                    Text("📥 " + s("common.exportCsv"))
                }
            }
        }

        val f = vm.report.filters
        if (ReportFilter.PERIOD in f) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "📅 " + s("report.presetsTitle") + ":",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    TextButton(onClick = {
                        val today = Dates.today(clock).toString()
                        vm.setDateRange(today, today)
                    }) { Text(s("report.preset.today")) }

                    TextButton(onClick = {
                        val start = Dates.weekStart(clock).toString()
                        val end = Dates.weekEnd(clock).toString()
                        vm.setDateRange(start, end)
                    }) { Text(s("report.preset.thisWeek")) }

                    TextButton(onClick = {
                        val start = Dates.monthStart(clock).toString()
                        val end = Dates.today(clock).toString()
                        vm.setDateRange(start, end)
                    }) { Text(s("report.preset.thisMonth")) }

                    TextButton(onClick = {
                        val start = Dates.lastMonthStart(clock).toString()
                        val end = Dates.lastMonthEnd(clock).toString()
                        vm.setDateRange(start, end)
                    }) { Text(s("report.preset.lastMonth")) }

                    TextButton(onClick = {
                        val start = Dates.financialYearStart(clock).toString()
                        val end = Dates.today(clock).toString()
                        vm.setDateRange(start, end)
                    }) { Text(s("report.preset.fy")) }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    s("report.filtersTitle"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ReportFilter.PERIOD in f) DateRangeFilter(filters, vm::setFilter)
                    val partyFilter = listOf(
                        ReportFilter.SUPPLIER to ("SUPPLIER" to "supplierId"),
                        ReportFilter.CUSTOMER to ("CUSTOMER" to "customerId"),
                        ReportFilter.MANUFACTURER to ("MANUFACTURER" to "manufacturerId"),
                        ReportFilter.WORKER to ("WORKER" to "workerId"),
                    ).firstOrNull { it.first in f }?.second
                    partyFilter?.let { (role, param) ->
                        PartyPicker(enumLabel("PartyRole", role), role, party, { party = it; vm.setFilter(param, it.id) }, Modifier.width(280.dp))
                    }
                    if (ReportFilter.PRODUCT in f || ReportFilter.PRODUCT_REQUIRED in f) {
                        FilterDropdown(s("purchase.product"), listOf("RAW_SILK", "SILK_WARP", "KORA_WARP", "VUDA_WARP").map { it to enumLabel("ProductType", it) }, filters["productType"]) {
                            vm.setFilter("productType", it)
                        }
                    }
                    if (ReportFilter.CATEGORY in f) {
                        FilterDropdown(s("expense.category"), EXPENSE_CATEGORIES.map { it to enumLabel("ExpenseCategory", it) }, filters["category"]) { vm.setFilter("category", it) }
                    }
                    if (ReportFilter.MIN_OUTSTANDING in f) {
                        MoneyField(s("report.minOutstanding"), filters["minOutstanding"].orEmpty(), { vm.setFilter("minOutstanding", it) }, Modifier.width(220.dp))
                    }
                }
            }
        }

        if (!vm.ready()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                Text(
                    "ℹ️ " + s("report.chooseRequired"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        exportError?.let { ErrorBanner(it, onDismiss = { exportError = null }) }
        LoadBanner(data.error, data.lastUpdated, vm::load)

        if (data.loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        data.value?.let { json ->
            val empty = (json is JsonArray && json.isEmpty()) || (json is JsonObject && json.values.all { it is JsonArray && it.isEmpty() })
            if (empty) {
                EmptyState(message = s("report.noData"))
            } else {
                JsonReport(json)
            }
        }
    }
}

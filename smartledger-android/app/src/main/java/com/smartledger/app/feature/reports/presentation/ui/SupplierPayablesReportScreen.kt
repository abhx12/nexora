package com.smartledger.app.feature.reports.presentation.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartledger.app.core.ui.components.EmptyState
import com.smartledger.app.core.ui.components.ErrorMessage
import com.smartledger.app.core.ui.components.LoadingIndicator
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard
import com.smartledger.app.core.ui.theme.WarningOrange
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toDisplayDate
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.feature.reports.data.dto.SupplierPayableItem
import com.smartledger.app.feature.reports.presentation.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierPayablesReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.supplierPayablesState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supplier Payables", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSupplierPayables() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SilkDarkBackground)
            )
        },
        containerColor = SilkDarkBackground
    ) { innerPadding ->
        when (val state = uiState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error -> ErrorMessage(message = state.message, onRetry = { viewModel.loadSupplierPayables() })
            is UiState.Success -> {
                val report = state.data
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.loadSupplierPayables() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Summary Banner
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Total Supplier Payables (Raw Silk)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        report.totalPayable.toIndianRupee(),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldPrimary
                                    )

                                    if (report.overduePayable > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = WarningOrange.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange, modifier = Modifier.padding(end = 4.dp))
                                                Text(
                                                    text = "Overdue Liabilities: ${report.overduePayable.toIndianRupee()}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = WarningOrange
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (report.items.isEmpty()) {
                            item {
                                EmptyState(message = "No supplier payables recorded.")
                            }
                        } else {
                            items(report.items, key = { it.supplierId }) { item ->
                                SupplierPayableCard(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierPayableCard(item: SupplierPayableItem) {
    val borderColor = if (item.isOverdue) WarningOrange else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (item.isOverdue) 1.5.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SilkDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.supplierName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = item.supplierCode + (item.phone?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (item.isOverdue) {
                    Surface(
                        color = WarningOrange.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "OVERDUE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = WarningOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Purchases", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    Text(item.totalPurchasedValue.toIndianRupee(), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
                Column {
                    Text("Total Paid", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    Text(item.totalPaid.toIndianRupee(), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Payable Due", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    Text(
                        item.outstandingPayable.toIndianRupee(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (item.isOverdue) WarningOrange else GoldPrimary
                    )
                }
            }

            item.dueDate?.let { due ->
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Payment Due Date: ${due.toDisplayDate()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isOverdue) WarningOrange else Color(0xFFCBD5E1)
                )
            }
        }
    }
}

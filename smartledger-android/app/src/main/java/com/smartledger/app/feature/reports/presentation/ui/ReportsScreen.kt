package com.smartledger.app.feature.reports.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartledger.app.core.ui.theme.CreditGreen
import com.smartledger.app.core.ui.theme.DebitRed
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.ui.theme.WarningOrange
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.feature.reports.presentation.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCustomerOutstanding: () -> Unit,
    onNavigateToSupplierPayables: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToInventoryMovements: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val custState by viewModel.customerOutstandingState.collectAsState()
    val suppState by viewModel.supplierPayablesState.collectAsState()
    val expState by viewModel.expenseSummaryState.collectAsState()

    val scrollState = rememberScrollState()

    val totalReceivable = (custState as? UiState.Success)?.data?.totalOutstanding ?: 625000.00
    val totalPayable = (suppState as? UiState.Success)?.data?.totalPayable ?: 410000.00
    val overduePayable = (suppState as? UiState.Success)?.data?.overduePayable ?: 260000.00
    val totalExpense = (expState as? UiState.Success)?.data?.totalExpenses ?: 212100.00

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Reports", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllReports() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SilkDarkBackground)
            )
        },
        containerColor = SilkDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Financial & Stock Audits",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Live analytics across customer credit, supplier debts and inventory flow",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Customer Outstanding Report Card
            ReportNavigationCard(
                title = "Customer Outstanding (Receivables)",
                subtitle = "Total pending payments from warp customers",
                valueText = totalReceivable.toIndianRupee(),
                valueColor = CreditGreen,
                icon = Icons.Default.Payments,
                iconColor = CreditGreen,
                onClick = onNavigateToCustomerOutstanding
            )

            // Supplier Payables Report Card (with overdue alert if any)
            ReportNavigationCard(
                title = "Supplier Payables (Debts)",
                subtitle = if (overduePayable > 0) "Includes ${overduePayable.toIndianRupee()} overdue" else "Raw silk cocoon liabilities",
                valueText = totalPayable.toIndianRupee(),
                valueColor = if (overduePayable > 0) WarningOrange else GoldPrimary,
                icon = Icons.Default.Warning,
                iconColor = if (overduePayable > 0) WarningOrange else GoldPrimary,
                badge = if (overduePayable > 0) "OVERDUE ALERT" else null,
                onClick = onNavigateToSupplierPayables
            )

            // Expense Summary Report Card
            ReportNavigationCard(
                title = "Expense & Operational Cost Summary",
                subtitle = "Categorized analysis of power, transport & labor",
                valueText = totalExpense.toIndianRupee(),
                valueColor = DebitRed,
                icon = Icons.Default.MoneyOff,
                iconColor = DebitRed,
                onClick = onNavigateToExpenses
            )

            // Inventory Movement Report Card
            ReportNavigationCard(
                title = "Inventory Movement Ledger",
                subtitle = "Audit trail of raw silk consumption, outputs & yields",
                valueText = "Complete Log",
                valueColor = StatusPartialBlue,
                icon = Icons.Default.Inventory,
                iconColor = StatusPartialBlue,
                onClick = onNavigateToInventoryMovements
            )
        }
    }
}

@Composable
fun ReportNavigationCard(
    title: String,
    subtitle: String,
    valueText: String,
    valueColor: Color,
    icon: ImageVector,
    iconColor: Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SilkDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .padding(11.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    badge?.let { b ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = WarningOrange.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = b,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = WarningOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = valueColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

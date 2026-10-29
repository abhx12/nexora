package com.smartledger.app.feature.finance.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartledger.app.core.ui.components.EmptyState
import com.smartledger.app.core.ui.components.ErrorMessage
import com.smartledger.app.core.ui.components.LoadingIndicator
import com.smartledger.app.core.ui.theme.CreditGreen
import com.smartledger.app.core.ui.theme.DebitRed
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toDisplayDate
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.feature.finance.data.dto.ExpenseResponse
import com.smartledger.app.feature.finance.data.dto.PaymentResponse
import com.smartledger.app.feature.finance.presentation.viewmodel.FinanceOverview
import com.smartledger.app.feature.finance.presentation.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    onNavigateToPayment: () -> Unit,
    onNavigateToExpense: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransfer: () -> Unit = {},
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance & Treasury", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadOverview() }) {
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
            is UiState.Error -> ErrorMessage(message = state.message, onRetry = { viewModel.loadOverview() })
            is UiState.Success -> {
                FinanceContent(
                    overview = state.data,
                    selectedTab = selectedTab,
                    onTabChanged = { selectedTab = it },
                    onNavigateToPayment = onNavigateToPayment,
                    onNavigateToExpense = onNavigateToExpense,
                    onNavigateToAccounts = onNavigateToAccounts,
                    onNavigateToTransfer = onNavigateToTransfer,
                    onRefresh = { viewModel.loadOverview() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceContent(
    overview: FinanceOverview,
    selectedTab: Int,
    onTabChanged: (Int) -> Unit,
    onNavigateToPayment: () -> Unit,
    onNavigateToExpense: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Liquid Balance
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Total Liquid Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = overview.totalLiquid.toIndianRupee(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = StatusCompletedGreen.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = StatusCompletedGreen, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Cash in Hand", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    Text(overview.totalCash.toIndianRupee(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = StatusPartialBlue.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = StatusPartialBlue, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Bank Accounts", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    Text(overview.totalBank.toIndianRupee(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FinanceActionButton(
                        title = "Payment",
                        icon = Icons.Default.AttachMoney,
                        color = StatusCompletedGreen,
                        onClick = onNavigateToPayment,
                        modifier = Modifier.weight(1f)
                    )
                    FinanceActionButton(
                        title = "Expense",
                        icon = Icons.Default.MoneyOff,
                        color = DebitRed,
                        onClick = onNavigateToExpense,
                        modifier = Modifier.weight(1f)
                    )
                    FinanceActionButton(
                        title = "Transfer",
                        icon = Icons.Default.SwapHoriz,
                        color = GoldPrimary,
                        onClick = onNavigateToTransfer,
                        modifier = Modifier.weight(1f)
                    )
                    FinanceActionButton(
                        title = "Accounts",
                        icon = Icons.Default.AccountBalance,
                        color = StatusPartialBlue,
                        onClick = onNavigateToAccounts,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Tabs: Payments vs Expenses
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SilkDarkCard,
                    contentColor = GoldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onTabChanged(0) },
                        text = { Text("Recent Payments", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onTabChanged(1) },
                        text = { Text("Recent Expenses", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTab == 0) {
                if (overview.recentPayments.isEmpty()) {
                    item {
                        EmptyState(
                            message = "No payments recorded yet.",
                            actionLabel = "Record Payment",
                            onAction = onNavigateToPayment
                        )
                    }
                } else {
                    items(overview.recentPayments, key = { it.id }) { payment ->
                        PaymentCard(payment = payment)
                    }
                }
            } else {
                if (overview.recentExpenses.isEmpty()) {
                    item {
                        EmptyState(
                            message = "No expenses recorded yet.",
                            actionLabel = "Record Expense",
                            onAction = onNavigateToExpense
                        )
                    }
                } else {
                    items(overview.recentExpenses, key = { it.id }) { expense ->
                        ExpenseCard(expense = expense)
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SilkDarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
fun PaymentCard(payment: PaymentResponse) {
    val isIncoming = payment.direction == "IN"
    val badgeColor = if (isIncoming) CreditGreen else DebitRed
    val dirIcon = if (isIncoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SilkDarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = dirIcon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.partyName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${payment.paymentMethod} • ${payment.paymentDate.toDisplayDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
                payment.accountName?.let {
                    Text(
                        text = "A/c: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (isIncoming) "+" else "-"
                Text(
                    text = "$prefix${payment.amount.toIndianRupee()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor
                )
                Text(
                    text = payment.paymentNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: ExpenseResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SilkDarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = DebitRed.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoneyOff,
                    contentDescription = null,
                    tint = DebitRed,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { expense.category },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${expense.category} • ${expense.expenseDate.toDisplayDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${expense.amount.toIndianRupee()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DebitRed
                )
                Text(
                    text = expense.expenseNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

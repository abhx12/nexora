package com.smartledger.app.feature.workforce.presentation.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartledger.app.core.ui.components.EmptyState
import com.smartledger.app.core.ui.components.ErrorMessage
import com.smartledger.app.core.ui.components.LoadingIndicator
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toDisplayDate
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.core.util.toWeightString
import com.smartledger.app.feature.workforce.data.dto.WorkRecordResponse
import com.smartledger.app.feature.workforce.presentation.viewmodel.WorkRecordListViewModel
import com.smartledger.app.feature.workforce.presentation.viewmodel.WorkerPayableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkRecordListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToPayment: () -> Unit = {},
    workRecordViewModel: WorkRecordListViewModel = hiltViewModel(),
    payableViewModel: WorkerPayableViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val recordState by workRecordViewModel.uiState.collectAsState()
    val payableState by payableViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workforce & Labor", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedTab == 0) workRecordViewModel.loadRecords() else payableViewModel.loadPayables()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SilkDarkBackground)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = GoldPrimary,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Work Record")
                }
            }
        },
        containerColor = SilkDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                    onClick = { selectedTab = 0 },
                    text = { Text("Work Records", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Worker Payables", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                when (val state = recordState) {
                    is UiState.Loading -> LoadingIndicator()
                    is UiState.Error -> ErrorMessage(message = state.message, onRetry = { workRecordViewModel.loadRecords() })
                    is UiState.Success -> {
                        val records = state.data
                        if (records.isEmpty()) {
                            EmptyState(
                                message = "No work records found.",
                                actionLabel = "Log Work Record",
                                onAction = onNavigateToCreate
                            )
                        } else {
                            PullToRefreshBox(
                                isRefreshing = false,
                                onRefresh = { workRecordViewModel.loadRecords() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(records, key = { it.id }) { record ->
                                        WorkRecordCard(record = record)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                when (val state = payableState) {
                    is UiState.Loading -> LoadingIndicator()
                    is UiState.Error -> ErrorMessage(message = state.message, onRetry = { payableViewModel.loadPayables() })
                    is UiState.Success -> {
                        val payables = state.data
                        WorkerPayableList(
                            payables = payables,
                            onRefresh = { payableViewModel.loadPayables() },
                            onNavigateToPayment = onNavigateToPayment
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkRecordCard(record: WorkRecordResponse) {
    val isWarping = record.workType == "WARPING"
    val badgeColor = if (isWarping) GoldPrimary else StatusPartialBlue

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SilkDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = record.workType,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.recordNumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF94A3B8)
                    )
                }

                Text(
                    text = record.workDate.toDisplayDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = record.workerName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val unitLabel = if (isWarping) "Weight Done" else "Hours Worked"
                    val unitValue = if (isWarping) {
                        record.quantityKg?.toWeightString() ?: "0.000 kg"
                    } else {
                        "%.1f hrs".format(record.hours ?: 0.0)
                    }
                    Text(text = unitLabel, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    Text(text = unitValue, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                }

                Column {
                    val rateUnit = if (isWarping) "/kg" else "/hr"
                    Text(text = "Rate ($rateUnit)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    Text(text = record.rate.toIndianRupee(), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Amount", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    Text(
                        text = record.totalAmount.toIndianRupee(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StatusCompletedGreen
                    )
                }
            }

            if (!record.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = record.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

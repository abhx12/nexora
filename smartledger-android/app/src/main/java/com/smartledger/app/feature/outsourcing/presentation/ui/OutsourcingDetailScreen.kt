package com.smartledger.app.feature.outsourcing.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.smartledger.app.feature.outsourcing.data.dto.OutsourcingJobResponse
import com.smartledger.app.feature.outsourcing.domain.model.OutsourcingStatus
import com.smartledger.app.feature.outsourcing.presentation.viewmodel.OutsourcingDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutsourcingDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToIssue: (String) -> Unit,
    onNavigateToReceive: (String) -> Unit,
    viewModel: OutsourcingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDetail() }) {
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
            is UiState.Error -> ErrorMessage(message = state.message, onRetry = { viewModel.loadDetail() })
            is UiState.Success -> {
                val job = state.data
                OutsourcingDetailContent(
                    job = job,
                    onNavigateToIssue = { onNavigateToIssue(job.id) },
                    onNavigateToReceive = { onNavigateToReceive(job.id) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun OutsourcingDetailContent(
    job: OutsourcingJobResponse,
    onNavigateToIssue: () -> Unit,
    onNavigateToReceive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val statusEnum = OutsourcingStatus.fromString(job.status)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SilkDarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = job.jobNumber,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusEnum.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusEnum.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusEnum.color,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = job.manufacturerName ?: "Manufacturer ID: ${job.manufacturerId}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                job.orderNumber?.let { orderNum ->
                    Text(
                        text = "Linked Order: $orderNum",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Visual Business Pipeline
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SilkDarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Outsourcing Flow",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                val steps = listOf(
                    "Job Created" to (job.status in listOf("CREATED", "MATERIAL_ISSUED", "PARTIALLY_RECEIVED", "COMPLETED")),
                    "Material Issued" to (job.status in listOf("MATERIAL_ISSUED", "PARTIALLY_RECEIVED", "COMPLETED")),
                    "At Manufacturer" to (job.status in listOf("MATERIAL_ISSUED", "PARTIALLY_RECEIVED")),
                    "Output Received" to (job.status == "COMPLETED")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, (label, isDone) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDone) GoldPrimary else Color(0xFF334155),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isDone) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text("${index + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDone) Color.White else Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Details breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SilkDarkCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Specifications & Weights", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)

                DetailRow("Product Type", job.productType.replace("_", " "))
                DetailRow("Planned Weight", job.plannedWeightKg.toWeightString())
                job.issuedWeightKg?.let { DetailRow("Issued Raw Silk", it.toWeightString()) }
                job.receivedWeightKg?.let { DetailRow("Received Fabric/Warp", it.toWeightString()) }
                job.wastageWeightKg?.let { DetailRow("Reported Wastage", it.toWeightString()) }
                job.ratePerKg?.let { DetailRow("Job Rate / kg", it.toIndianRupee()) }
                job.totalCost?.let { DetailRow("Total Job Work Cost", it.toIndianRupee()) }

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                job.issueDate?.let { DetailRow("Issue Date", it.toDisplayDate()) }
                job.expectedReturnDate?.let { DetailRow("Expected Return", it.toDisplayDate()) }
                job.completionDate?.let { DetailRow("Completed Date", it.toDisplayDate()) }

                if (!job.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Notes / Remarks", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text(job.notes, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
        }

        // Action Buttons depending on status
        when (job.status) {
            "CREATED" -> {
                Button(
                    onClick = onNavigateToIssue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusPartialBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Outbox, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Issue Material to Manufacturer", fontWeight = FontWeight.Bold)
                }
            }
            "MATERIAL_ISSUED", "PARTIALLY_RECEIVED" -> {
                Button(
                    onClick = onNavigateToReceive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCompletedGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Handshake, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Receive Output from Manufacturer", fontWeight = FontWeight.Bold)
                }
            }
            "COMPLETED" -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = StatusCompletedGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Job Work Completed. Stock has entered finished inventory.",
                        color = StatusCompletedGreen,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

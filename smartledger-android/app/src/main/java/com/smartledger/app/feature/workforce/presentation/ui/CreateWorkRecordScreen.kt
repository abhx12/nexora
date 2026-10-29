package com.smartledger.app.feature.workforce.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard
import com.smartledger.app.core.ui.theme.StatusCompletedGreen
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.core.util.toWeightString
import com.smartledger.app.feature.workforce.presentation.viewmodel.CreateWorkRecordViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkRecordScreen(
    onNavigateBack: () -> Unit,
    onRecordCreated: () -> Unit,
    viewModel: CreateWorkRecordViewModel = hiltViewModel()
) {
    val workers by viewModel.workers.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var selectedWorkerId by remember { mutableStateOf("") }
    var selectedWorkerName by remember { mutableStateOf("") }
    var workerDropdownExpanded by remember { mutableStateOf(false) }

    var selectedWorkType by remember { mutableStateOf("WARPING") } // "WARPING" or "ROLLING"

    var workDateText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var quantityKgText by remember { mutableStateOf("") }
    var hoursText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("120.0") }
    var notesText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    LaunchedEffect(workers) {
        if (workers.isNotEmpty() && selectedWorkerId.isEmpty()) {
            selectedWorkerId = workers.first().id
            selectedWorkerName = workers.first().name
        }
    }

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is UiState.Success -> onRecordCreated()
            is UiState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    val rate = rateText.toDoubleOrNull() ?: 0.0
    val isWarping = selectedWorkType == "WARPING"
    val qty = quantityKgText.toDoubleOrNull() ?: 0.0
    val hrs = hoursText.toDoubleOrNull() ?: 0.0

    val calculatedTotal = if (isWarping) qty * rate else hrs * rate
    val isValid = selectedWorkerId.isNotBlank() && rate > 0.0 && if (isWarping) qty > 0.0 else hrs > 0.0
    val isLoading = submitState is UiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Work Record", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SilkDarkBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SilkDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Daily Labor Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)

            // Worker Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedWorkerName.ifBlank { "Select Worker" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Worker *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            modifier = Modifier.clickable { workerDropdownExpanded = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { workerDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                DropdownMenu(
                    expanded = workerDropdownExpanded,
                    onDismissRequest = { workerDropdownExpanded = false },
                    modifier = Modifier.background(SilkDarkCard)
                ) {
                    if (workers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Mani Kumar", color = Color.White) },
                            onClick = {
                                selectedWorkerId = "worker-1"
                                selectedWorkerName = "Mani Kumar"
                                workerDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Selvam R.", color = Color.White) },
                            onClick = {
                                selectedWorkerId = "worker-2"
                                selectedWorkerName = "Selvam R."
                                workerDropdownExpanded = false
                            }
                        )
                    } else {
                        workers.forEach { worker ->
                            DropdownMenuItem(
                                text = { Text("${worker.name} (${worker.partyCode})", color = Color.White) },
                                onClick = {
                                    selectedWorkerId = worker.id
                                    selectedWorkerName = worker.name
                                    workerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Work Type Toggle: WARPING vs ROLLING
            Column {
                Text("Work Type *", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = isWarping,
                        onClick = {
                            selectedWorkType = "WARPING"
                            if (rateText == "85.0") rateText = "120.0"
                        },
                        label = { Text("WARPING (Piece Rate / kg)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )

                    FilterChip(
                        selected = !isWarping,
                        onClick = {
                            selectedWorkType = "ROLLING"
                            if (rateText == "120.0") rateText = "85.0"
                        },
                        label = { Text("ROLLING (Hourly Rate)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Date
            OutlinedTextField(
                value = workDateText,
                onValueChange = { workDateText = it },
                label = { Text("Work Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Dynamic fields according to plan:
            // WARPING -> show quantity (kg), hide hours
            // ROLLING -> show hours, hide quantity
            if (isWarping) {
                OutlinedTextField(
                    value = quantityKgText,
                    onValueChange = { quantityKgText = it },
                    label = { Text("Warped Quantity (kg) *") },
                    placeholder = { Text("e.g. 45.500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )
            } else {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Hours Worked (hrs) *") },
                    placeholder = { Text("e.g. 8.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )
            }

            // Rate
            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it },
                label = { Text(if (isWarping) "Rate per kg (₹) *" else "Hourly Rate (₹) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Live Computed Total Preview Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Calculated Total Wage", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        val formula = if (isWarping) "${qty.toWeightString()} × ₹$rate/kg" else "$hrs hrs × ₹$rate/hr"
                        Text(formula, style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                    }
                    Text(
                        text = calculatedTotal.toIndianRupee(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = StatusCompletedGreen
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes / Yarn Specification") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.submitWorkRecord(
                        workerId = selectedWorkerId,
                        workDate = workDateText,
                        workType = selectedWorkType,
                        quantityKg = qty,
                        hours = hrs,
                        rate = rate,
                        notes = notesText
                    )
                },
                enabled = isValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF334155),
                    disabledContentColor = Color(0xFF64748B)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Text("Save Work Record", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.smartledger.app.feature.finance.presentation.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartledger.app.core.ui.theme.CreditGreen
import com.smartledger.app.core.ui.theme.DebitRed
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.feature.finance.data.dto.PaymentAllocationRequest
import com.smartledger.app.feature.finance.presentation.viewmodel.RecordPaymentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    onNavigateBack: () -> Unit,
    onPaymentRecorded: () -> Unit,
    viewModel: RecordPaymentViewModel = hiltViewModel()
) {
    val parties by viewModel.parties.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var direction by remember { mutableStateOf("IN") } // "IN" (Received) or "OUT" (Paid)
    var selectedPartyId by remember { mutableStateOf("") }
    var selectedPartyName by remember { mutableStateOf("") }
    var partyDropdownExpanded by remember { mutableStateOf(false) }

    var selectedAccountId by remember { mutableStateOf("") }
    var selectedAccountName by remember { mutableStateOf("") }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val methods = listOf("CASH", "UPI", "BANK_TRANSFER", "CHEQUE")
    var selectedMethod by remember { mutableStateOf("BANK_TRANSFER") }
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var paymentDateText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var refNumberText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    var showConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(direction) {
        viewModel.onDirectionChanged(direction)
    }

    LaunchedEffect(parties) {
        if (parties.isNotEmpty()) {
            selectedPartyId = parties.first().id
            selectedPartyName = parties.first().name
        } else {
            selectedPartyId = ""
            selectedPartyName = ""
        }
    }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && selectedAccountId.isEmpty()) {
            selectedAccountId = accounts.first().id
            selectedAccountName = accounts.first().accountName
        }
    }

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is UiState.Success -> onPaymentRecorded()
            is UiState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = selectedPartyId.isNotBlank() && selectedAccountId.isNotBlank() && amount > 0.0
    val isLoading = submitState is UiState.Loading

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Financial Transaction") },
            text = {
                Text(
                    "Are you sure you want to record a payment of ${amount.toIndianRupee()} ${if (direction == "IN") "from" else "to"} $selectedPartyName via $selectedMethod?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.recordPayment(
                            partyId = selectedPartyId,
                            direction = direction,
                            amount = amount,
                            paymentMethod = selectedMethod,
                            financialAccountId = selectedAccountId,
                            paymentDate = paymentDateText,
                            referenceNumber = refNumberText,
                            notes = notesText,
                            allocations = emptyList()
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Confirm & Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = SilkDarkCard,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Payment", color = Color.White, fontWeight = FontWeight.Bold) },
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
            // Direction Toggle
            Text("Payment Flow *", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = direction == "IN",
                    onClick = { direction = "IN" },
                    label = { Text("RECEIVED (+IN from Customer)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CreditGreen,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = direction == "OUT",
                    onClick = { direction = "OUT" },
                    label = { Text("PAID (-OUT to Supplier/Worker)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DebitRed,
                        selectedLabelColor = Color.White
                    )
                )
            }

            // Party Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedPartyName.ifBlank { "Select Party" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (direction == "IN") "Customer *" else "Supplier / Worker *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            modifier = Modifier.clickable { partyDropdownExpanded = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { partyDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                DropdownMenu(
                    expanded = partyDropdownExpanded,
                    onDismissRequest = { partyDropdownExpanded = false },
                    modifier = Modifier.background(SilkDarkCard)
                ) {
                    if (parties.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sri Lakshmi Silks (CUST-001)", color = Color.White) },
                            onClick = {
                                selectedPartyId = "cust-1"
                                selectedPartyName = "Sri Lakshmi Silks"
                                partyDropdownExpanded = false
                            }
                        )
                    } else {
                        parties.forEach { party ->
                            DropdownMenuItem(
                                text = { Text("${party.name} (${party.partyCode})", color = Color.White) },
                                onClick = {
                                    selectedPartyId = party.id
                                    selectedPartyName = party.name
                                    partyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Payment Amount (₹) *") },
                placeholder = { Text("e.g. 50000.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Financial Account Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedAccountName.ifBlank { "Select Account" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deposit To / Paid From Account *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            modifier = Modifier.clickable { accountDropdownExpanded = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { accountDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                DropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false },
                    modifier = Modifier.background(SilkDarkCard)
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.accountName} (Bal: ${acc.balance.toIndianRupee()})", color = Color.White) },
                            onClick = {
                                selectedAccountId = acc.id
                                selectedAccountName = acc.accountName
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Payment Method
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedMethod,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Method *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            modifier = Modifier.clickable { methodDropdownExpanded = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { methodDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                DropdownMenu(
                    expanded = methodDropdownExpanded,
                    onDismissRequest = { methodDropdownExpanded = false },
                    modifier = Modifier.background(SilkDarkCard)
                ) {
                    methods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method, color = Color.White) },
                            onClick = {
                                selectedMethod = method
                                methodDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Date
            OutlinedTextField(
                value = paymentDateText,
                onValueChange = { paymentDateText = it },
                label = { Text("Payment Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Reference Number
            OutlinedTextField(
                value = refNumberText,
                onValueChange = { refNumberText = it },
                label = { Text("Reference / UTR / Cheque Number") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Notes
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes / Remarks") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showConfirmDialog = true },
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
                    Text("Submit Payment Entry", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

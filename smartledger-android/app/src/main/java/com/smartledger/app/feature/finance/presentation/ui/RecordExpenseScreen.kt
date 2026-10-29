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
import com.smartledger.app.core.ui.theme.DebitRed
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.feature.finance.presentation.viewmodel.RecordExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordExpenseScreen(
    onNavigateBack: () -> Unit,
    onExpenseRecorded: () -> Unit,
    viewModel: RecordExpenseViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val categories = listOf("ELECTRICITY", "TRANSPORT", "SALARIES", "RENT", "MAINTENANCE", "PACKAGING", "OTHER")
    var selectedCategory by remember { mutableStateOf("ELECTRICITY") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var selectedAccountId by remember { mutableStateOf("") }
    var selectedAccountName by remember { mutableStateOf("") }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var expenseDateText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var descriptionText by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && selectedAccountId.isEmpty()) {
            selectedAccountId = accounts.first().id
            selectedAccountName = accounts.first().accountName
        }
    }

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is UiState.Success -> onExpenseRecorded()
            is UiState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = selectedAccountId.isNotBlank() && amount > 0.0 && descriptionText.isNotBlank()
    val isLoading = submitState is UiState.Loading

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Expense Entry") },
            text = {
                Text(
                    "Are you sure you want to record an expense of ${amount.toIndianRupee()} for '$selectedCategory' paid from $selectedAccountName?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.recordExpense(
                            category = selectedCategory,
                            amount = amount,
                            financialAccountId = selectedAccountId,
                            expenseDate = expenseDateText,
                            description = descriptionText
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebitRed, contentColor = Color.White)
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
                title = { Text("Record Expense", color = Color.White, fontWeight = FontWeight.Bold) },
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
            Text("Operational Expenditure", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)

            // Category Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Expense Category *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            modifier = Modifier.clickable { categoryDropdownExpanded = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoryDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier.background(SilkDarkCard)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, color = Color.White) },
                            onClick = {
                                selectedCategory = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Expense Amount (₹) *") },
                placeholder = { Text("e.g. 14500.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Account Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedAccountName.ifBlank { "Select Account" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Paid From Account *") },
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

            // Date
            OutlinedTextField(
                value = expenseDateText,
                onValueChange = { expenseDateText = it },
                label = { Text("Expense Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Description
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text("Description / Reason *") },
                placeholder = { Text("e.g. Power bill for the twisting unit") },
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
                    containerColor = DebitRed,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF334155),
                    disabledContentColor = Color(0xFF64748B)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Submit Expense Entry", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

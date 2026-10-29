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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.material3.Surface
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
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.feature.finance.presentation.viewmodel.AccountTransferViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTransferScreen(
    onNavigateBack: () -> Unit,
    onTransferSuccess: () -> Unit,
    viewModel: AccountTransferViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var fromAccountId by remember { mutableStateOf("") }
    var fromAccountName by remember { mutableStateOf("") }
    var fromDropdownExpanded by remember { mutableStateOf(false) }

    var toAccountId by remember { mutableStateOf("") }
    var toAccountName by remember { mutableStateOf("") }
    var toDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var transferDateText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var notesText by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(accounts) {
        if (accounts.size >= 2 && fromAccountId.isEmpty() && toAccountId.isEmpty()) {
            fromAccountId = accounts[0].id
            fromAccountName = accounts[0].accountName
            toAccountId = accounts[1].id
            toAccountName = accounts[1].accountName
        }
    }

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is UiState.Success -> onTransferSuccess()
            is UiState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isDifferentAccounts = fromAccountId.isNotBlank() && toAccountId.isNotBlank() && fromAccountId != toAccountId
    val isValid = isDifferentAccounts && amount > 0.0
    val isLoading = submitState is UiState.Loading

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Account Transfer") },
            text = {
                Text(
                    "Transfer ${amount.toIndianRupee()} from $fromAccountName to $toAccountName?\n\nNote: This is an internal balance adjustment and does NOT affect business profit/loss."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.transferFunds(
                            fromAccountId = fromAccountId,
                            toAccountId = toAccountId,
                            amount = amount,
                            transferDate = transferDateText,
                            notes = notesText
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Confirm Transfer", fontWeight = FontWeight.Bold)
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
                title = { Text("Account Transfer", color = Color.White, fontWeight = FontWeight.Bold) },
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
            // Explanatory banner (Finance Rule)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Account transfers move liquidity between Bank and Cash. Transfers are NOT revenue or expense.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // Source Account
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = fromAccountName.ifBlank { "Select Source Account" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Transfer From Account *") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { fromDropdownExpanded = true })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fromDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                DropdownMenu(
                    expanded = fromDropdownExpanded,
                    onDismissRequest = { fromDropdownExpanded = false },
                    modifier = Modifier.background(SilkDarkCard)
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.accountName} (Bal: ${acc.balance.toIndianRupee()})", color = Color.White) },
                            onClick = {
                                fromAccountId = acc.id
                                fromAccountName = acc.accountName
                                fromDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SwapVert, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
            }

            // Destination Account
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = toAccountName.ifBlank { "Select Destination Account" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Transfer To Account *") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { toDropdownExpanded = true })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { toDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                DropdownMenu(
                    expanded = toDropdownExpanded,
                    onDismissRequest = { toDropdownExpanded = false },
                    modifier = Modifier.background(SilkDarkCard)
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.accountName} (Bal: ${acc.balance.toIndianRupee()})", color = Color.White) },
                            onClick = {
                                toAccountId = acc.id
                                toAccountName = acc.accountName
                                toDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Transfer Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Transfer Amount (₹) *") },
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

            // Date
            OutlinedTextField(
                value = transferDateText,
                onValueChange = { transferDateText = it },
                label = { Text("Transfer Date (YYYY-MM-DD)") },
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
                label = { Text("Transfer Notes / Cheque / Slip Ref") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                    Text("Transfer Funds", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.smartledger.app.feature.outsourcing.presentation.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.smartledger.app.core.ui.components.LoadingIndicator
import com.smartledger.app.core.ui.theme.ErrorRed
import com.smartledger.app.core.ui.theme.GoldPrimary
import com.smartledger.app.core.ui.theme.SilkDarkBackground
import com.smartledger.app.core.ui.theme.SilkDarkCard
import com.smartledger.app.core.ui.theme.StatusPartialBlue
import com.smartledger.app.core.util.UiState
import com.smartledger.app.core.util.toWeightString
import com.smartledger.app.feature.outsourcing.presentation.viewmodel.IssueMaterialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueOutsourcingScreen(
    onNavigateBack: () -> Unit,
    onIssueSuccess: () -> Unit,
    viewModel: IssueMaterialViewModel = hiltViewModel()
) {
    val jobState by viewModel.jobState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val availableRawSilk by viewModel.availableRawSilk.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var issueDateText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var issuedWeightText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    LaunchedEffect(jobState) {
        if (jobState is UiState.Success) {
            val job = (jobState as UiState.Success).data
            if (issuedWeightText.isEmpty()) {
                issuedWeightText = job.plannedWeightKg.toString()
            }
        }
    }

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is UiState.Success -> onIssueSuccess()
            is UiState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    val issuedWeight = issuedWeightText.toDoubleOrNull() ?: 0.0
    val isInsufficientStock = issuedWeight > availableRawSilk
    val isValid = issuedWeight > 0.0 && !isInsufficientStock
    val isLoading = submitState is UiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue Material", color = Color.White, fontWeight = FontWeight.Bold) },
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
        when (val state = jobState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error -> Text(state.message, color = ErrorRed, modifier = Modifier.padding(16.dp))
            is UiState.Success -> {
                val job = state.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SilkDarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Job: ${job.jobNumber}", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Manufacturer: ${job.manufacturerName}", color = Color(0xFFCBD5E1))
                            Text(text = "Product: ${job.productType.replace("_", " ")}", color = Color(0xFF94A3B8))
                            Text(text = "Planned Weight: ${job.plannedWeightKg.toWeightString()}", color = GoldPrimary)
                        }
                    }

                    // Available stock indicator
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isInsufficientStock) ErrorRed.copy(alpha = 0.15f) else Color(0xFF1E293B)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isInsufficientStock) ErrorRed else GoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Column {
                                Text("Available Raw Silk in Inventory", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                Text(
                                    availableRawSilk.toWeightString(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isInsufficientStock) ErrorRed else GoldPrimary
                                )
                                if (isInsufficientStock) {
                                    Text(
                                        "Cannot issue more than available stock!",
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = issueDateText,
                        onValueChange = { issueDateText = it },
                        label = { Text("Issue Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )

                    OutlinedTextField(
                        value = issuedWeightText,
                        onValueChange = { issuedWeightText = it },
                        label = { Text("Issued Weight (kg) *") },
                        isError = isInsufficientStock,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF475569),
                            errorBorderColor = ErrorRed
                        )
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Issue Notes / Yarn Batch No.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
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
                            viewModel.issueMaterial(
                                issueDate = issueDateText,
                                issuedWeightKg = issuedWeight,
                                notes = notesText
                            )
                        },
                        enabled = isValid && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusPartialBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Confirm Material Issue", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

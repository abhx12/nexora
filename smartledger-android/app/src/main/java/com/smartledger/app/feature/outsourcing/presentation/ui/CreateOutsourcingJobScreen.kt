package com.smartledger.app.feature.outsourcing.presentation.ui

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.outsourcing.presentation.viewmodel.CreateOutsourcingJobViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOutsourcingJobScreen(
    onNavigateBack: () -> Unit,
    onJobCreated: (String) -> Unit,
    viewModel: CreateOutsourcingJobViewModel = hiltViewModel()
) {
    val manufacturers by viewModel.manufacturers.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val availableRawSilk by viewModel.availableRawSilk.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var selectedManufacturerId by remember { mutableStateOf("") }
    var selectedManufacturerName by remember { mutableStateOf("") }
    var mfrDropdownExpanded by remember { mutableStateOf(false) }

    val productTypes = listOf("SILK_WARP", "KORA_WARP", "VUDA_WARP")
    var selectedProductType by remember { mutableStateOf("SILK_WARP") }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    var plannedWeightText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var expectedReturnDateText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 7 * 86400000L)))
    }
    var notesText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    LaunchedEffect(manufacturers) {
        if (manufacturers.isNotEmpty() && selectedManufacturerId.isEmpty()) {
            selectedManufacturerId = manufacturers.first().id
            selectedManufacturerName = manufacturers.first().name
        }
    }

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is UiState.Success -> {
                onJobCreated(state.data.id)
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    val plannedWeight = plannedWeightText.toDoubleOrNull() ?: 0.0
    val isValid = selectedManufacturerId.isNotBlank() && plannedWeight > 0.0
    val isLoading = submitState is UiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Outsourcing Job", color = Color.White, fontWeight = FontWeight.Bold) },
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
            // Manufacturer Selection
            Text(text = "Job Work Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedManufacturerName.ifBlank { "Select Manufacturer" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("External Manufacturer *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            modifier = Modifier.clickable { mfrDropdownExpanded = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mfrDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                DropdownMenu(
                    expanded = mfrDropdownExpanded,
                    onDismissRequest = { mfrDropdownExpanded = false },
                    modifier = Modifier.background(SilkDarkCard)
                ) {
                    if (manufacturers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sri Murugan Twisting Mills", color = Color.White) },
                            onClick = {
                                selectedManufacturerId = "mfr-1"
                                selectedManufacturerName = "Sri Murugan Twisting Mills"
                                mfrDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Balaji Weaving & Warping", color = Color.White) },
                            onClick = {
                                selectedManufacturerId = "mfr-2"
                                selectedManufacturerName = "Balaji Weaving & Warping"
                                mfrDropdownExpanded = false
                            }
                        )
                    } else {
                        manufacturers.forEach { mfr ->
                            DropdownMenuItem(
                                text = { Text("${mfr.name} (${mfr.partyCode})", color = Color.White) },
                                onClick = {
                                    selectedManufacturerId = mfr.id
                                    selectedManufacturerName = mfr.name
                                    mfrDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Product Type
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedProductType.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Product Type *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            modifier = Modifier.clickable { productDropdownExpanded = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { productDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                DropdownMenu(
                    expanded = productDropdownExpanded,
                    onDismissRequest = { productDropdownExpanded = false },
                    modifier = Modifier.background(SilkDarkCard)
                ) {
                    productTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.replace("_", " "), color = Color.White) },
                            onClick = {
                                selectedProductType = type
                                productDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Planned Weight
            OutlinedTextField(
                value = plannedWeightText,
                onValueChange = { plannedWeightText = it },
                label = { Text("Planned Weight (kg) *") },
                placeholder = { Text("e.g. 50.000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Job Rate Per Kg
            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it },
                label = { Text("Job Work Rate (₹/kg)") },
                placeholder = { Text("e.g. 180.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Expected Return Date
            OutlinedTextField(
                value = expectedReturnDateText,
                onValueChange = { expectedReturnDateText = it },
                label = { Text("Expected Return Date (YYYY-MM-DD)") },
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
                label = { Text("Notes / Specifications") },
                placeholder = { Text("e.g. Warp ply specification or instructions") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.createJob(
                        manufacturerId = selectedManufacturerId,
                        orderId = null,
                        productType = selectedProductType,
                        plannedWeightKg = plannedWeight,
                        ratePerKg = rateText.toDoubleOrNull(),
                        expectedReturnDate = expectedReturnDateText,
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
                    Text("Create Outsourcing Job", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

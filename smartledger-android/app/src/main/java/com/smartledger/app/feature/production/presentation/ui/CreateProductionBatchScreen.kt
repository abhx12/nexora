package com.smartledger.app.feature.production.presentation.ui

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.smartledger.app.core.ui.theme.DarkSurface
import com.smartledger.app.core.ui.theme.SilkGold40
import com.smartledger.app.core.ui.theme.SuccessGreen
import com.smartledger.app.core.ui.theme.WarningOrange
import com.smartledger.app.feature.orders.domain.model.OrderProductType
import com.smartledger.app.feature.production.presentation.viewmodel.CreateProductionBatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProductionBatchScreen(
    onNavigateBack: () -> Unit,
    onBatchCreated: (String) -> Unit,
    viewModel: CreateProductionBatchViewModel = hiltViewModel()
) {
    val selectedProductType by viewModel.selectedProductType.collectAsState()
    val inputWeight by viewModel.inputWeight.collectAsState()
    val selectedOrderId by viewModel.selectedOrderId.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val availableRawSilkKg by viewModel.availableRawSilkKg.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var orderDropdownExpanded by remember { mutableStateOf(false) }

    val productTypes = listOf(
        OrderProductType.SILK_WARP,
        OrderProductType.KORA_WARP,
        OrderProductType.VUDA_WARP
    )

    val currentEnteredWeight = inputWeight.toDoubleOrNull() ?: 0.0
    val isWeightExceedingStock = currentEnteredWeight > availableRawSilkKg && currentEnteredWeight > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Production Batch",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Stock Inventory Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWeightExceedingStock) {
                        WarningOrange.copy(alpha = 0.15f)
                    } else {
                        SuccessGreen.copy(alpha = 0.12f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isWeightExceedingStock) Icons.Default.Warning else Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (isWeightExceedingStock) WarningOrange else SuccessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Raw Silk Inventory",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Available Stock: %.3f kg".format(availableRawSilkKg),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isWeightExceedingStock) WarningOrange else SuccessGreen
                        )
                        if (isWeightExceedingStock) {
                            Text(
                                text = "Requested input (%.3f kg) exceeds stock!".format(currentEnteredWeight),
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningOrange
                            )
                        }
                    }
                }
            }

            // Error Message Banner (e.g. 409 INSUFFICIENT_INVENTORY)
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Product Type Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Product Type *",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    productTypes.forEach { type ->
                        val isSelected = selectedProductType == type.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setProductType(type.name) },
                            label = { Text(type.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SilkGold40,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Input Weight Field
            OutlinedTextField(
                value = inputWeight,
                onValueChange = { viewModel.setInputWeight(it) },
                label = { Text("Input Raw Silk Weight (kg) *") },
                placeholder = { Text("e.g. 85.500") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = isWeightExceedingStock,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Optional Linked Customer Order Dropdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Link to Customer Order (Optional)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                val selectedOrder = orders.find { it.id == selectedOrderId }
                val displayText = if (selectedOrder != null) {
                    "${selectedOrder.orderNumber} — ${selectedOrder.customerName}"
                } else {
                    "None (In-House Stock Batch)"
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = displayText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Order",
                                modifier = Modifier.clickable { orderDropdownExpanded = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { orderDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    )

                    DropdownMenu(
                        expanded = orderDropdownExpanded,
                        onDismissRequest = { orderDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (In-House Stock Batch)") },
                            onClick = {
                                viewModel.setSelectedOrderId(null)
                                orderDropdownExpanded = false
                            }
                        )
                        orders.forEach { order ->
                            DropdownMenuItem(
                                text = { Text("${order.orderNumber} — ${order.customerName} (${order.status})") },
                                onClick = {
                                    viewModel.setSelectedOrderId(order.id)
                                    orderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Planned Start Date
            OutlinedTextField(
                value = startDate,
                onValueChange = { viewModel.setStartDate(it) },
                label = { Text("Planned Start Date (YYYY-MM-DD) *") },
                placeholder = { Text("2026-10-20") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text("Notes / Specifications") },
                placeholder = { Text("Warp count, ply, or loom assignments...") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.createBatch { batchId ->
                        onBatchCreated(batchId)
                    }
                },
                enabled = !isSubmitting && !isWeightExceedingStock && inputWeight.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SilkGold40,
                    contentColor = Color.Black
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Production Batch",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

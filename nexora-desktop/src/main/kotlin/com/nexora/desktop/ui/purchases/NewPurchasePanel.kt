package com.nexora.desktop.ui.purchases

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.desktop.ui.components.DateField
import com.nexora.desktop.ui.components.FlagChip
import com.nexora.desktop.ui.components.FormPanel
import com.nexora.desktop.ui.components.MoneyField
import com.nexora.desktop.ui.components.PartyPicker
import com.nexora.desktop.ui.components.Segmented
import com.nexora.desktop.ui.components.TextInput
import com.nexora.desktop.ui.components.WeightField
import com.nexora.desktop.ui.components.enumLabel
import com.nexora.desktop.ui.components.screenViewModel
import com.nexora.desktop.util.IndianFormat.weight
import com.nexora.desktop.util.s
import com.nexora.desktop.viewmodel.purchases.PurchaseFormViewModel
import com.nexora.desktop.viewmodel.shell.NavigationViewModel
import com.nexora.desktop.viewmodel.shell.Screen

/** S-PUR-02 New purchase. */
@Composable
fun NewPurchasePanel(nav: NavigationViewModel) {
    val vm: PurchaseFormViewModel = screenViewModel()
    val form by vm.form.collectAsState()
    val status by vm.status.collectAsState()
    val rawSilkAlert by vm.rawSilkLowStock.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    val aiError by vm.aiError.collectAsState()
    val aiParsed by vm.aiParsedFields.collectAsState()
    var showSmartFill by remember { mutableStateOf(false) }

    val errors = status.fieldErrors
    LaunchedEffect(status.result) {
        status.result?.let {
            nav.closeOverlay()
            nav.push(Screen.PurchaseDetail(it.id))
        }
    }

    if (showSmartFill) {
        SmartFillDialog(
            busy = aiBusy,
            error = aiError,
            parsedFields = aiParsed,
            onParse = vm::parseSmartFill,
            onApply = {
                vm.applySmartFill(it)
                showSmartFill = false
            },
            onDismiss = {
                vm.clearSmartFill()
                showSmartFill = false
            },
        )
    }

    FormPanel(
        title = s("purchase.new"),
        saveLabel = s("common.save"),
        submitting = status.submitting,
        dirty = status.dirty,
        error = status.error,
        onSave = vm::save,
        onCancel = nav::closeOverlay,
        footerNote = s("purchase.calculatedOnSave"),
    ) {
        // Post-MVP AI Smart Fill launcher
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        s("ai.smartFill.bannerTitle"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        s("ai.smartFill.bannerSubtitle"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { showSmartFill = true },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(s("ai.smartFill.launchButton"))
                }
            }
        }
        val alert = rawSilkAlert
        if (form.productType == "RAW_SILK" && alert != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlagChip(s("inventory.lowStock"))
                    Text(
                        s("purchase.lowStockAlert", weight(alert.currentQuantityKg), weight(alert.minThresholdKg)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        PartyPicker(s("purchase.supplier"), "SUPPLIER", form.supplier, vm::pickSupplier, error = errors["supplierId"], tag = "purchase.supplier")
        Segmented(s("purchase.product"), listOf("RAW_SILK", "VUDA_WARP").map { it to enumLabel("ProductType", it) }, form.productType, { v ->
            vm.edit { it.copy(productType = v) }
        })
        DateField(s("purchase.date"), form.purchaseDate, { v -> vm.edit { it.copy(purchaseDate = v) } }, error = errors["purchaseDate"])
        WeightField(s("purchase.orderedWeight"), form.orderedWeightKg, { v -> vm.edit { it.copy(orderedWeightKg = v) } }, error = errors["orderedWeightKg"])
        MoneyField(s("purchase.rate"), form.ratePerKg, { v -> vm.edit { it.copy(ratePerKg = v) } }, error = errors["ratePerKg"])
        Segmented(s("purchase.paymentType"), listOf("CREDIT", "CASH").map { it to enumLabel("PurchasePaymentType", it) }, form.paymentType, vm::setPaymentType)
        TextInput(
            s("purchase.creditDays"), form.creditDays, { v -> vm.edit { it.copy(creditDays = v.filter(Char::isDigit)) } },
            error = errors["creditDays"], maxLength = 3, enabled = form.paymentType == "CREDIT",
        )
        TextInput(s("purchase.billNumber"), form.billReference, { v -> vm.edit { it.copy(billReference = v) } }, error = errors["billReference"], maxLength = 100)
        TextInput(s("common.notes"), form.notes, { v -> vm.edit { it.copy(notes = v) } }, error = errors["notes"], singleLine = false, maxLength = 1000)
    }
}

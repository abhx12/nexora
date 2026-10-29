package com.nexora.desktop.ui.purchases

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.desktop.ui.components.ModalBox
import com.nexora.desktop.ui.components.TextInput
import com.nexora.desktop.util.s

/**
 * Post-MVP Voice & AI: Smart bill fill assistant dialog supporting Tamil & English silk mandi text.
 * Strictly adheres to DOMAIN_RULES: AI suggests -> User confirms -> Applied to form.
 */
@Composable
fun SmartFillDialog(
    busy: Boolean,
    error: String?,
    parsedFields: Map<String, String>?,
    onParse: (String) -> Unit,
    onApply: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var rawText by remember { mutableStateOf("") }

    ModalBox(onDismiss = onDismiss, width = 540.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                s("ai.smartFill.title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                s("ai.smartFill.subtitle"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TextInput(
                label = s("ai.smartFill.promptLabel"),
                value = rawText,
                onValueChange = { rawText = it },
                singleLine = false,
                maxLength = 1000,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    s("ai.smartFill.exampleHint"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )

                Button(
                    onClick = { onParse(rawText) },
                    enabled = rawText.isNotBlank() && !busy,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Text(s("ai.smartFill.parseButton"))
                }
            }

            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            if (parsedFields != null && parsedFields.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            s("ai.smartFill.reviewTitle"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        parsedFields["supplierName"]?.let {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s("purchase.supplier") + ":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(it, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        parsedFields["productType"]?.let {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s("purchase.product") + ":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(it, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        parsedFields["orderedWeightKg"]?.let {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s("purchase.orderedWeight") + ":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$it kg", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        parsedFields["ratePerKg"]?.let {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s("purchase.rate") + ":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹$it / kg", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        parsedFields["grossAmount"]?.let {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s("common.total") + ":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹$it", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        parsedFields["billNumber"]?.let {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s("purchase.billNumber") + ":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(it, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        parsedFields["paymentType"]?.let {
                            val days = parsedFields["creditDays"]?.let { d -> " ($d days)" } ?: ""
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s("purchase.paymentType") + ":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$it$days", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text(s("common.cancel"))
                }
                if (parsedFields != null && parsedFields.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { onApply(parsedFields) },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(s("ai.smartFill.applyButton"))
                    }
                }
            }
        }
    }
}

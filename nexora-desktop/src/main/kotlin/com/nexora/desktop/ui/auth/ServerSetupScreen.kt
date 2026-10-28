package com.nexora.desktop.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.desktop.viewmodel.auth.ServerMode
import com.nexora.desktop.viewmodel.auth.ServerSetupViewModel
import com.nexora.desktop.util.s

/** S-AUTH-01 Server setup with isolated local engine & remote options. */
@Composable
fun ServerSetupScreen(viewModel: ServerSetupViewModel, onContinue: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(560.dp).padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(s("setup.title"), style = MaterialTheme.typography.headlineSmall)
                Text(s("setup.help"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Mode Selection Cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Option 1: Local Engine
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectMode(ServerMode.LOCAL) },
                        border = BorderStroke(
                            1.5.dp,
                            if (state.mode == ServerMode.LOCAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.mode == ServerMode.LOCAL)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface
                        ),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.mode == ServerMode.LOCAL,
                                onClick = { viewModel.selectMode(ServerMode.LOCAL) }
                            )
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(s("setup.modeLocal"), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    s("setup.modeLocalDesc"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    ServerSetupViewModel.DEFAULT_LOCAL_URL,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Option 2: Remote / Cloud Server
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectMode(ServerMode.REMOTE) },
                        border = BorderStroke(
                            1.5.dp,
                            if (state.mode == ServerMode.REMOTE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.mode == ServerMode.REMOTE)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface
                        ),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.mode == ServerMode.REMOTE,
                                onClick = { viewModel.selectMode(ServerMode.REMOTE) }
                            )
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(s("setup.modeRemote"), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    s("setup.modeRemoteDesc"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // If Local Engine, show helpful reminder
                if (state.mode == ServerMode.LOCAL) {
                    Text(
                        s("setup.localEngineHint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    OutlinedTextField(
                        value = state.address,
                        onValueChange = viewModel::onAddressChange,
                        label = { Text(s("setup.address")) },
                        placeholder = { Text(s("setup.addressExample")) },
                        isError = state.addressError != null,
                        supportingText = state.addressError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.developmentWarning) {
                    Text(s("setup.developmentServer"), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
                state.statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                state.serverVersion?.let {
                    Text(s("setup.serverVersion", it), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = viewModel::testConnection, enabled = !state.testing) {
                        Text(s("setup.test"))
                    }
                    Button(
                        onClick = { if (viewModel.saveAndContinue()) onContinue() },
                        enabled = state.canContinue && !state.testing
                    ) {
                        Text(s("common.continue"))
                    }
                    if (state.testing) CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}


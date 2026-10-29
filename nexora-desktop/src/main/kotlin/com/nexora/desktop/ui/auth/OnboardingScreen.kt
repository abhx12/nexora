package com.nexora.desktop.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton
import com.nexora.desktop.viewmodel.auth.OnboardingViewModel

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onCompleted: () -> Unit, onBackToLogin: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(520.dp).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Welcome to SmartSilk", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Create your administrator account and business profile to start using the system.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = state.businessName,
                onValueChange = { viewModel.onBusinessNameChange(it.take(120)) },
                label = { Text("Business Name") },
                placeholder = { Text("e.g. Sri Balaji Silk Traders") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.city,
                    onValueChange = { viewModel.onCityChange(it.take(100)) },
                    label = { Text("City") },
                    placeholder = { Text("e.g. Kanchipuram") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { com.nexora.desktop.ui.components.acceptPhoneInput(it)?.let(viewModel::onPhoneChange) },
                    label = { Text("Phone Number") },
                    placeholder = { Text("10 digits (e.g. 9840123456)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.onUsernameChange(it.take(50).filter { c -> c.isLetterOrDigit() || c == '_' }) },
                label = { Text("Administrator Username") },
                placeholder = { Text("e.g. owner") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onPasswordChange(it.take(72)) },
                label = { Text("Password (at least 8 chars, letter & number)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChange(it.take(72)) },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.submit(onCompleted) },
                    enabled = !state.submitting
                ) {
                    Text("Initialize & Start Ledger")
                }
                TextButton(onClick = onBackToLogin) {
                    Text("Back to Login")
                }
                if (state.submitting) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

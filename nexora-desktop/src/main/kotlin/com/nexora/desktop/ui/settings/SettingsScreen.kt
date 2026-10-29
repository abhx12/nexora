package com.nexora.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexora.desktop.api.ApiClient
import com.nexora.desktop.ui.components.Dropdown
import com.nexora.desktop.ui.components.LabeledValue
import com.nexora.desktop.ui.components.ScreenHeader
import com.nexora.desktop.ui.components.Section
import com.nexora.desktop.ui.components.Segmented
import com.nexora.desktop.ui.components.enumLabel
import com.nexora.desktop.ui.components.screenViewModel
import com.nexora.desktop.ui.notifications.ReminderSettingsPanel
import com.nexora.desktop.ui.shell.LocalShellActions
import com.nexora.desktop.util.s
import com.nexora.desktop.viewmodel.common.AccountRules
import com.nexora.desktop.viewmodel.settings.SettingsViewModel
import com.nexora.desktop.viewmodel.shell.NavigationViewModel
import com.nexora.desktop.viewmodel.shell.Overlay

/** S-SET-01 Settings and about (+ S-NTF-02 reminder settings for FINANCE_VIEW). */
@Composable
fun SettingsScreen(nav: NavigationViewModel) {
    val vm: SettingsViewModel = screenViewModel()
    val settings by vm.current.collectAsState()
    val about by vm.about.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val sysSettings by vm.systemSettings.collectAsState()
    val statusMsg by vm.settingsStatus.collectAsState()
    val actions = LocalShellActions.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(s("nav.settings"))

        statusMsg?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        // Account & Security Section (Change Password)
        val currentUser = nav.state.collectAsState().value.user
        var showChangePassword by remember { mutableStateOf(false) }
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmNewPassword by remember { mutableStateOf("") }
        var passwordMsg by remember { mutableStateOf<String?>(null) }
        var passwordSuccess by remember { mutableStateOf<Boolean?>(null) }
        var isChangingPassword by remember { mutableStateOf(false) }

        Section("Account & Security") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (currentUser != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        LabeledValue("Logged In User", currentUser.displayName)
                        LabeledValue("Username", currentUser.username)
                        LabeledValue("Assigned Roles", currentUser.roles.joinToString(", "))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        showChangePassword = !showChangePassword
                        passwordMsg = null
                    }) {
                        Text(if (showChangePassword) "Cancel Change Password" else "Change Password")
                    }
                }

                if (showChangePassword) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it.take(72) },
                                label = { Text("Current Password") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.width(220.dp),
                            )
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it.take(72) },
                                label = { Text("New Password (min 12 chars)") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.width(250.dp),
                            )
                            OutlinedTextField(
                                value = confirmNewPassword,
                                onValueChange = { confirmNewPassword = it.take(72) },
                                label = { Text("Confirm New Password") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.width(220.dp),
                            )
                        }

                        Button(
                            onClick = {
                                if (currentPassword.isBlank()) {
                                    passwordMsg = "Please enter your current password."
                                    passwordSuccess = false
                                    return@Button
                                }
                                if (newPassword.length < 12 || !newPassword.any { it.isLetter() } || !newPassword.any { it.isDigit() }) {
                                    passwordMsg = "New password must be at least 12 characters and contain both letters and digits."
                                    passwordSuccess = false
                                    return@Button
                                }
                                if (newPassword != confirmNewPassword) {
                                    passwordMsg = "New passwords do not match."
                                    passwordSuccess = false
                                    return@Button
                                }
                                isChangingPassword = true
                                vm.changePassword(currentPassword, newPassword) { ok, msg ->
                                    isChangingPassword = false
                                    passwordSuccess = ok
                                    passwordMsg = msg
                                    if (ok) {
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmNewPassword = ""
                                    }
                                }
                            },
                            enabled = !isChangingPassword,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(if (isChangingPassword) "Updating Password..." else "Update Password")
                        }

                        passwordMsg?.let { msg ->
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (passwordSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // User Management Section (Owner Only)
        if (nav.can("USER_MANAGE")) {
            val userList by vm.users.collectAsState()
            val userLoading by vm.userOpLoading.collectAsState()
            var showAddUser by remember { mutableStateOf(false) }
            var newUsername by remember { mutableStateOf("") }
            var newDisplayName by remember { mutableStateOf("") }
            var newAccountPassword by remember { mutableStateOf("") }
            var newRole by remember { mutableStateOf("STAFF") }
            var userActionMsg by remember { mutableStateOf<String?>(null) }
            var userActionSuccess by remember { mutableStateOf<Boolean?>(null) }

            Section("User Management (Team & Staff)") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Manage accounts, access permissions, and staff credentials.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = {
                            showAddUser = !showAddUser
                            userActionMsg = null
                        }) {
                            Text(if (showAddUser) "Cancel" else "+ Add New User")
                        }
                    }

                    if (showAddUser) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Create New Account", style = MaterialTheme.typography.titleSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = newUsername,
                                    onValueChange = { newUsername = it.lowercase().take(50) },
                                    label = { Text("Username") },
                                    placeholder = { Text("e.g. clerk1") },
                                    modifier = Modifier.width(180.dp),
                                )
                                OutlinedTextField(
                                    value = newDisplayName,
                                    onValueChange = { newDisplayName = it.take(100) },
                                    label = { Text("Display Name") },
                                    placeholder = { Text("e.g. Ramesh Kumar") },
                                    modifier = Modifier.width(220.dp),
                                )
                                OutlinedTextField(
                                    value = newAccountPassword,
                                    onValueChange = { newAccountPassword = it.take(72) },
                                    label = { Text("Temporary Password (min 12)") },
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    modifier = Modifier.width(220.dp),
                                )
                                Dropdown(
                                    "Role",
                                    listOf("STAFF" to "Staff (Read-Only)", "OWNER" to "Owner (Full Admin)"),
                                    newRole,
                                    { newRole = it },
                                    Modifier.width(200.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    if (newUsername.isBlank() || newDisplayName.isBlank()) {
                                        userActionMsg = "Username and display name are required."
                                        userActionSuccess = false
                                        return@Button
                                    }
                                    if (newAccountPassword.length < 12 || !newAccountPassword.any { it.isLetter() } || !newAccountPassword.any { it.isDigit() }) {
                                        userActionMsg = "Password must be at least 12 characters and contain letters and numbers."
                                        userActionSuccess = false
                                        return@Button
                                    }
                                    vm.createUser(newUsername, newDisplayName, newAccountPassword, newRole) { ok, msg ->
                                        userActionSuccess = ok
                                        userActionMsg = msg
                                        if (ok) {
                                            newUsername = ""
                                            newDisplayName = ""
                                            newAccountPassword = ""
                                            showAddUser = false
                                        }
                                    }
                                },
                                enabled = !userLoading,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(if (userLoading) "Creating..." else "Create Account")
                            }
                        }
                    }

                    userActionMsg?.let { msg ->
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (userActionSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    // Existing Users List
                    Text("Existing Users (${userList.size})", style = MaterialTheme.typography.titleSmall)
                    userList.forEach { user ->
                        val isSelf = currentUser?.id == user.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text("(@${user.username})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (isSelf) {
                                        Text("(You)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text(
                                    "Role: ${user.roles.joinToString(", ")} • Status: ${if (user.active) "Active" else "Deactivated"}${user.lastLoginAt?.let { " • Last login: $it" } ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isSelf) {
                                OutlinedButton(
                                    onClick = {
                                        vm.setUserStatus(user.id, !user.active) { ok, msg ->
                                            userActionSuccess = ok
                                            userActionMsg = msg
                                        }
                                    }
                                ) {
                                    Text(if (user.active) "Deactivate" else "Activate")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Business Profile Section
        val biz = sysSettings["BUSINESS"] ?: emptyMap()
        var bizName by remember(biz["name"]) { mutableStateOf(biz["name"] ?: "") }
        var bizCity by remember(biz["city"]) { mutableStateOf(biz["city"] ?: "") }
        var bizPhone by remember(biz["phone"]) { mutableStateOf(biz["phone"] ?: "") }
        var bizGstin by remember(biz["gstin"]) { mutableStateOf(biz["gstin"] ?: "") }

        Section("Business Profile") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = bizName,
                    onValueChange = { bizName = it.take(120) },
                    label = { Text("Business Name") },
                    modifier = Modifier.width(260.dp),
                    enabled = nav.can("FINANCE_MANAGE")
                )
                OutlinedTextField(
                    value = bizCity,
                    onValueChange = { bizCity = it.take(100) },
                    label = { Text("City") },
                    modifier = Modifier.width(200.dp),
                    enabled = nav.can("FINANCE_MANAGE")
                )
                OutlinedTextField(
                    value = bizPhone,
                    onValueChange = { com.nexora.desktop.ui.components.acceptPhoneInput(it)?.let { clean -> bizPhone = clean } },
                    label = { Text("Phone") },
                    placeholder = { Text("10 digits (e.g. 9840123456)") },
                    modifier = Modifier.width(200.dp),
                    enabled = nav.can("FINANCE_MANAGE")
                )
                OutlinedTextField(
                    value = bizGstin,
                    onValueChange = { com.nexora.desktop.ui.components.acceptGstinInput(it)?.let { clean -> bizGstin = clean } },
                    label = { Text("GSTIN") },
                    placeholder = { Text("15 alphanumeric") },
                    modifier = Modifier.width(220.dp),
                    enabled = nav.can("FINANCE_MANAGE")
                )
            }
            if (nav.can("FINANCE_MANAGE")) {
                Button(
                    onClick = {
                        if (bizPhone.isNotBlank()) {
                            val trimmed = bizPhone.trim()
                            val isValid = if (trimmed.startsWith("+")) {
                                val rest = trimmed.substring(1)
                                rest.all { it.isDigit() } && rest.length in 10..12
                            } else {
                                trimmed.all { it.isDigit() } && trimmed.length == 10
                            }
                            if (!isValid) {
                                vm.setStatusMessage("Invalid phone number '${bizPhone}'. Please enter a valid 10-digit mobile number.")
                                return@Button
                            }
                        }
                        if (bizGstin.isNotBlank() && bizGstin.trim().length != 15) {
                            vm.setStatusMessage("Invalid GSTIN '${bizGstin}'. GSTIN must be exactly 15 alphanumeric characters (e.g. 33AAAAA0000A1Z5).")
                            return@Button
                        }
                        vm.updateSystemCategory(
                            "BUSINESS",
                            mapOf("name" to bizName.trim(), "city" to bizCity.trim(), "phone" to bizPhone.trim(), "gstin" to bizGstin.trim())
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Save Business Profile")
                }
            }
        }

        // Cloud Backup & Safety Section
        val backupConfig = sysSettings["BACKUP"] ?: emptyMap()
        var backupMsg by remember { mutableStateOf<String?>(null) }
        var isBackingUp by remember { mutableStateOf(false) }

        var backupProvider by remember(backupConfig["provider"]) {
            mutableStateOf(backupConfig["provider"] ?: "local")
        }
        var autoBackupEnabled by remember(backupConfig["auto_backup_enabled"]) {
            mutableStateOf(backupConfig["auto_backup_enabled"] != "false")
        }
        var backupFrequency by remember(backupConfig["frequency"]) {
            mutableStateOf(backupConfig["frequency"] ?: "daily_2am")
        }
        var retentionDays by remember(backupConfig["retention_days"]) {
            mutableStateOf(backupConfig["retention_days"] ?: "30")
        }
        var s3Endpoint by remember(backupConfig["s3_endpoint"]) {
            mutableStateOf(backupConfig["s3_endpoint"] ?: "")
        }
        var s3Bucket by remember(backupConfig["s3_bucket"]) {
            mutableStateOf(backupConfig["s3_bucket"] ?: "")
        }
        var s3Region by remember(backupConfig["s3_region"]) {
            mutableStateOf(backupConfig["s3_region"] ?: "ap-south-1")
        }
        var s3AccessKey by remember(backupConfig["s3_access_key"]) {
            mutableStateOf(backupConfig["s3_access_key"] ?: "")
        }
        var s3SecretKey by remember(backupConfig["s3_secret_key"]) {
            mutableStateOf(backupConfig["s3_secret_key"] ?: "")
        }
        var cloudVaultKey by remember(backupConfig["cloud_vault_key"]) {
            mutableStateOf(backupConfig["cloud_vault_key"] ?: "")
        }

        Section("Backup & Cloud Safety") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Segmented(
                    label = "Storage Destination",
                    options = listOf(
                        "local" to "Local Storage (Device)",
                        "smartsilk_cloud" to "SmartSilk Cloud Vault",
                        "custom_s3" to "Custom S3 / Cloud Bucket",
                    ),
                    selected = backupProvider,
                    onSelect = { backupProvider = it },
                    enabled = nav.can("FINANCE_MANAGE"),
                )

                when (backupProvider) {
                    "local" -> {
                        Text(
                            "Backups are encrypted and stored in your device's local application data folder and user Downloads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    "smartsilk_cloud" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Automated real-time multi-region encrypted cloud backup directly to SmartSilk Cloud Vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = cloudVaultKey,
                                onValueChange = { cloudVaultKey = it.take(255) },
                                label = { Text("Cloud Vault Key / License") },
                                modifier = Modifier.width(360.dp),
                                enabled = nav.can("FINANCE_MANAGE"),
                            )
                        }
                    }
                    "custom_s3" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Connect your own S3-compatible cloud storage (AWS S3, MinIO, Cloudflare R2, Google Cloud Storage).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = s3Endpoint,
                                    onValueChange = { s3Endpoint = it.take(255) },
                                    label = { Text("Endpoint URL (e.g. https://s3.ap-south-1.amazonaws.com)") },
                                    modifier = Modifier.width(380.dp),
                                    enabled = nav.can("FINANCE_MANAGE"),
                                )
                                OutlinedTextField(
                                    value = s3Bucket,
                                    onValueChange = { s3Bucket = it.take(100) },
                                    label = { Text("Bucket Name") },
                                    modifier = Modifier.width(220.dp),
                                    enabled = nav.can("FINANCE_MANAGE"),
                                )
                                OutlinedTextField(
                                    value = s3Region,
                                    onValueChange = { s3Region = it.take(50) },
                                    label = { Text("Region") },
                                    modifier = Modifier.width(160.dp),
                                    enabled = nav.can("FINANCE_MANAGE"),
                                )
                                OutlinedTextField(
                                    value = s3AccessKey,
                                    onValueChange = { s3AccessKey = it.take(128) },
                                    label = { Text("Access Key ID") },
                                    modifier = Modifier.width(220.dp),
                                    enabled = nav.can("FINANCE_MANAGE"),
                                )
                                OutlinedTextField(
                                    value = s3SecretKey,
                                    onValueChange = { s3SecretKey = it.take(256) },
                                    label = { Text("Secret Access Key") },
                                    modifier = Modifier.width(260.dp),
                                    enabled = nav.can("FINANCE_MANAGE"),
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = autoBackupEnabled,
                        onCheckedChange = { autoBackupEnabled = it },
                        enabled = nav.can("FINANCE_MANAGE"),
                    )
                    Text("Enable Automatic Scheduled Backups", style = MaterialTheme.typography.bodyMedium)
                }

                if (autoBackupEnabled) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Dropdown(
                            "Backup Frequency",
                            listOf(
                                "every_6h" to "Every 6 Hours",
                                "daily_2am" to "Daily at Night (02:00 AM) — Recommended",
                                "weekly" to "Weekly (Sunday 02:00 AM)",
                                "on_close" to "On Application Close",
                                "manual" to "Manual Backups Only",
                            ),
                            backupFrequency,
                            { if (nav.can("FINANCE_MANAGE")) backupFrequency = it },
                            Modifier.width(340.dp),
                        )
                        Dropdown(
                            "Retention Policy",
                            listOf(
                                "7" to "Keep for 7 Days",
                                "30" to "Keep for 30 Days — Recommended",
                                "90" to "Keep for 90 Days",
                                "365" to "Keep for 1 Year",
                                "forever" to "Keep Indefinitely",
                            ),
                            retentionDays,
                            { if (nav.can("FINANCE_MANAGE")) retentionDays = it },
                            Modifier.width(260.dp),
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    if (nav.can("FINANCE_MANAGE")) {
                        Button(onClick = {
                            vm.updateSystemCategory(
                                "BACKUP",
                                mapOf(
                                    "provider" to backupProvider,
                                    "auto_backup_enabled" to autoBackupEnabled.toString(),
                                    "frequency" to backupFrequency,
                                    "retention_days" to retentionDays,
                                    "s3_endpoint" to s3Endpoint,
                                    "s3_bucket" to s3Bucket,
                                    "s3_region" to s3Region,
                                    "s3_access_key" to s3AccessKey,
                                    "s3_secret_key" to s3SecretKey,
                                    "cloud_vault_key" to cloudVaultKey,
                                ),
                            )
                        }) {
                            Text("Save Backup Configuration")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            isBackingUp = true
                            vm.triggerBackup { _, msg ->
                                isBackingUp = false
                                backupMsg = msg
                            }
                        },
                        enabled = !isBackingUp,
                    ) {
                        Text(if (isBackingUp) "Exporting Snapshot..." else "Create & Download Snapshot Now")
                    }
                }

                backupMsg?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Feature Integrations Section (WhatsApp & AI OCR)
        val waConfig = sysSettings["WHATSAPP"] ?: emptyMap()
        var waEnabled by remember(waConfig["enabled"]) { mutableStateOf(waConfig["enabled"] == "true") }
        var waPhoneId by remember(waConfig["phone_number_id"]) { mutableStateOf(waConfig["phone_number_id"] ?: "") }
        var waToken by remember(waConfig["access_token"]) { mutableStateOf(waConfig["access_token"] ?: "") }

        val aiConfig = sysSettings["AI_OCR"] ?: emptyMap()
        var aiEnabled by remember(aiConfig["enabled"]) { mutableStateOf(aiConfig["enabled"] == "true") }
        var aiKey by remember(aiConfig["gemini_api_key"]) { mutableStateOf(aiConfig["gemini_api_key"] ?: "") }

        val gstConfig = sysSettings["GST_EINVOICE"] ?: emptyMap()
        var gstEnabled by remember(gstConfig["enabled"]) { mutableStateOf(gstConfig["enabled"] == "true") }
        var gstClientId by remember(gstConfig["client_id"]) { mutableStateOf(gstConfig["client_id"] ?: "") }
        var gstClientSecret by remember(gstConfig["client_secret"]) { mutableStateOf(gstConfig["client_secret"] ?: "") }

        val bankConfig = sysSettings["BANKING"] ?: emptyMap()
        var bankEnabled by remember(bankConfig["enabled"]) { mutableStateOf(bankConfig["enabled"] == "true") }
        var bankBaseUrl by remember(bankConfig["base_url"]) { mutableStateOf(bankConfig["base_url"] ?: "") }
        var bankClientId by remember(bankConfig["client_id"]) { mutableStateOf(bankConfig["client_id"] ?: "") }
        var bankClientSecret by remember(bankConfig["client_secret"]) { mutableStateOf(bankConfig["client_secret"] ?: "") }

        if (nav.can("FINANCE_MANAGE")) {
            Section("Integrations (Optional)") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. WhatsApp
                    Text("WhatsApp Automated Reminders", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = waEnabled, onCheckedChange = { waEnabled = it })
                        Text("Enable WhatsApp Business API Notifications")
                    }
                    if (waEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = waPhoneId,
                                onValueChange = { com.nexora.desktop.ui.components.acceptDigitsInput(it, 30)?.let { clean -> waPhoneId = clean } },
                                label = { Text("Phone Number ID") },
                                modifier = Modifier.width(260.dp),
                            )
                            OutlinedTextField(
                                value = waToken,
                                onValueChange = { waToken = it.take(500) },
                                label = { Text("Access Token") },
                                modifier = Modifier.width(320.dp),
                            )
                        }
                    }

                    // 2. AI Document OCR
                    Text("AI Document & Bill Scanner", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = aiEnabled, onCheckedChange = { aiEnabled = it })
                        Text("Enable Gemini AI Supplier Bill OCR")
                    }
                    if (aiEnabled) {
                        OutlinedTextField(
                            value = aiKey,
                            onValueChange = { aiKey = it.take(255) },
                            label = { Text("Gemini API Key") },
                            modifier = Modifier.width(360.dp),
                        )
                    }

                    // 3. GST e-Invoicing Portal
                    Text("GST e-Invoicing & E-Way Bill Portal", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = gstEnabled, onCheckedChange = { gstEnabled = it })
                        Text("Enable Automated IRN & E-Way Bill Generation")
                    }
                    if (gstEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = gstClientId,
                                onValueChange = { gstClientId = it.take(100) },
                                label = { Text("GST Portal Client ID") },
                                modifier = Modifier.width(260.dp),
                            )
                            OutlinedTextField(
                                value = gstClientSecret,
                                onValueChange = { gstClientSecret = it.take(255) },
                                label = { Text("GST Portal Client Secret") },
                                modifier = Modifier.width(320.dp),
                            )
                        }
                    }

                    // 4. Automated Bank Statement Reconciliation
                    Text("Bank Statement & Open Banking Feeds", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = bankEnabled, onCheckedChange = { bankEnabled = it })
                        Text("Enable Automated Bank Reconciliation Feeds")
                    }
                    if (bankEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = bankBaseUrl,
                                onValueChange = { bankBaseUrl = it.take(255) },
                                label = { Text("Bank API Base URL") },
                                modifier = Modifier.width(280.dp),
                            )
                            OutlinedTextField(
                                value = bankClientId,
                                onValueChange = { bankClientId = it.take(100) },
                                label = { Text("Bank Client ID") },
                                modifier = Modifier.width(240.dp),
                            )
                            OutlinedTextField(
                                value = bankClientSecret,
                                onValueChange = { bankClientSecret = it.take(255) },
                                label = { Text("Bank Client Secret") },
                                modifier = Modifier.width(260.dp),
                            )
                        }
                    }

                    Button(onClick = {
                        vm.updateSystemCategory(
                            "WHATSAPP",
                            mapOf("enabled" to waEnabled.toString(), "phone_number_id" to waPhoneId, "access_token" to waToken),
                        )
                        vm.updateSystemCategory(
                            "AI_OCR",
                            mapOf("enabled" to aiEnabled.toString(), "gemini_api_key" to aiKey),
                        )
                        vm.updateSystemCategory(
                            "GST_EINVOICE",
                            mapOf("enabled" to gstEnabled.toString(), "client_id" to gstClientId, "client_secret" to gstClientSecret),
                        )
                        vm.updateSystemCategory(
                            "BANKING",
                            mapOf(
                                "enabled" to bankEnabled.toString(),
                                "base_url" to bankBaseUrl,
                                "client_id" to bankClientId,
                                "client_secret" to bankClientSecret,
                            ),
                        )
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Save All Integrations")
                    }
                }
            }
        }

        Section(s("settings.server")) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val url = settings.serverUrl.orEmpty()
                val isLocal = url.contains(":8088")
                Column(Modifier.weight(1f)) {
                    Text(if (isLocal) "Local Engine ($url)" else url, style = MaterialTheme.typography.bodyMedium)
                    if (isLocal) {
                        Text(
                            "Isolated Local Engine (Port 8088)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                OutlinedButton(onClick = actions.changeServer) { Text(s("settings.changeServer")) }
            }
        }
        Section(s("settings.display")) {
            Segmented(
                s("settings.textSize"),
                listOf(1f, 1.25f, 1.5f, 1.75f, 2f).map { it to s("settings.percent", (it * 100).toInt()) },
                settings.textScale,
                vm::setTextScale,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = settings.sidebarCollapsed, onCheckedChange = vm::setSidebarCollapsed,
                    modifier = Modifier.semantics { contentDescription = s("settings.sidebarCollapsed") },
                )
                Text(s("settings.sidebarCollapsed"))
            }
        }
        if (accounts.isNotEmpty()) {
            Section(s("settings.defaults")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountRules.METHODS.forEach { method ->
                        Dropdown(
                            s("settings.paymentAccount", enumLabel("PaymentMethod", method)),
                            AccountRules.accountsFor(method, accounts).map { it.id to it.accountName },
                            settings.lastUsed["payment.account.$method"],
                            { vm.setDefault("payment.account.$method", it) },
                            Modifier.width(240.dp),
                        )
                    }
                    Dropdown(s("settings.expenseAccount"), accounts.map { it.id to it.accountName }, settings.lastUsed["expense.account"], {
                        vm.setDefault("expense.account", it)
                    }, Modifier.width(240.dp))
                }
            }
        }
        if (nav.can("INVENTORY_VIEW")) {
            var showThresholdsDialog by remember { mutableStateOf(false) }
            Section("Stock & Safety Thresholds") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Set minimum stock levels (in kg) for raw silk, warps, and finished stock. Low-stock warnings and alerts trigger automatically when balances fall below threshold.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = { showThresholdsDialog = true }) {
                        Text("Configure Stock Thresholds...")
                    }
                }
            }
            if (showThresholdsDialog) {
                com.nexora.desktop.ui.inventory.InventoryThresholdDialog(
                    onDone = { showThresholdsDialog = false },
                    onCancel = { showThresholdsDialog = false }
                )
            }
        }
        if (nav.can("FINANCE_VIEW")) ReminderSettingsPanel(canEdit = nav.can("FINANCE_MANAGE"))
        Section(s("settings.about")) {
            FlowRow {
                LabeledValue(s("settings.clientVersion"), about.clientVersion)
                LabeledValue(s("settings.serverVersion"), about.serverVersion ?: "—")
                LabeledValue(s("settings.apiVersion"), about.apiVersion ?: "—")
                LabeledValue(s("settings.clientApi"), ApiClient.API_MAJOR.toString())
            }
            Text(s("settings.licences"), style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = { nav.show(Overlay.ConfirmLogout) }) { Text(s("shell.logout")) }
    }
}

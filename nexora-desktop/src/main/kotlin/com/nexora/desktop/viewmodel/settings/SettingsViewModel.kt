package com.nexora.desktop.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.desktop.api.ApiClient
import com.nexora.desktop.api.dto.AccountResponse
import com.nexora.desktop.repository.ApiResult
import com.nexora.desktop.repository.FinanceRepository
import com.nexora.desktop.repository.SystemSettingRepository
import com.nexora.desktop.ui.components.ErrorMessages
import com.nexora.desktop.util.AppSettings
import com.nexora.desktop.util.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Properties

/** Versions shown under About (06 S-SET-01). */
data class AboutInfo(val clientVersion: String, val serverVersion: String? = null, val apiVersion: String? = null)

/** S-SET-01 Settings and about. Handles local UI settings and server-side dynamic system settings. */
class SettingsViewModel(
    private val settings: SettingsStore,
    private val api: ApiClient,
    finance: FinanceRepository,
    private val systemSettingRepo: SystemSettingRepository,
    private val userRepo: com.nexora.desktop.repository.UserRepository = com.nexora.desktop.repository.UserRepository(api),
) : ViewModel() {

    val current: StateFlow<AppSettings> = settings.settings

    private val _about = MutableStateFlow(AboutInfo(clientVersion()))
    val about: StateFlow<AboutInfo> = _about.asStateFlow()

    private val _accounts = MutableStateFlow<List<AccountResponse>>(emptyList())
    val accounts: StateFlow<List<AccountResponse>> = _accounts.asStateFlow()

    private val _systemSettings = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val systemSettings: StateFlow<Map<String, Map<String, String>>> = _systemSettings.asStateFlow()

    private val _settingsStatus = MutableStateFlow<String?>(null)
    val settingsStatus: StateFlow<String?> = _settingsStatus.asStateFlow()

    private val _users = MutableStateFlow<List<com.nexora.desktop.api.dto.UserDto>>(emptyList())
    val users: StateFlow<List<com.nexora.desktop.api.dto.UserDto>> = _users.asStateFlow()

    private val _userOpLoading = MutableStateFlow(false)
    val userOpLoading: StateFlow<Boolean> = _userOpLoading.asStateFlow()

    init {
        viewModelScope.launch {
            val info = api.get<JsonObject>("/actuator/info").getOrNull()
            val build = (info?.get("build") as? JsonObject)?.get("version") as? JsonPrimitive
            val apiVersion = (info?.get("app") as? JsonObject)?.get("api-version") as? JsonPrimitive
            _about.value = _about.value.copy(serverVersion = build?.content, apiVersion = apiVersion?.content)
        }
        viewModelScope.launch { _accounts.value = finance.activeAccounts().getOrNull().orEmpty() }
        loadSystemSettings()
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            val res = userRepo.getUsers()
            if (res is ApiResult.Success) {
                _users.value = res.value
            }
        }
    }

    fun changePassword(current: String, new: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = api.post<com.nexora.desktop.api.dto.ChangePasswordRequest, Unit>(
                "/auth/password",
                com.nexora.desktop.api.dto.ChangePasswordRequest(current, new)
            )
            when (res) {
                is ApiResult.Success -> {
                    onResult(true, "Password changed successfully.")
                }
                is ApiResult.Failure -> {
                    val msg = ErrorMessages.detail(res.error) ?: ErrorMessages.message(res.error)
                    onResult(false, msg)
                }
            }
        }
    }

    fun createUser(username: String, displayName: String, password: String, role: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _userOpLoading.value = true
            val res = userRepo.createUser(com.nexora.desktop.api.dto.CreateUserRequest(username.trim(), displayName.trim(), password, role))
            _userOpLoading.value = false
            when (res) {
                is ApiResult.Success -> {
                    loadUsers()
                    onResult(true, "User '${res.value.username}' created successfully.")
                }
                is ApiResult.Failure -> {
                    val msg = ErrorMessages.detail(res.error) ?: ErrorMessages.message(res.error)
                    onResult(false, msg)
                }
            }
        }
    }

    fun setUserStatus(userId: String, active: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = userRepo.setUserStatus(userId, active)
            when (res) {
                is ApiResult.Success -> {
                    loadUsers()
                    val statusText = if (active) "activated" else "deactivated"
                    onResult(true, "User '${res.value.username}' $statusText.")
                }
                is ApiResult.Failure -> {
                    val msg = ErrorMessages.detail(res.error) ?: ErrorMessages.message(res.error)
                    onResult(false, msg)
                }
            }
        }
    }

    fun loadSystemSettings() {
        viewModelScope.launch {
            val res = systemSettingRepo.getAllSettings()
            if (res is ApiResult.Success) {
                _systemSettings.value = res.value
            }
        }
    }

    fun updateSystemCategory(category: String, values: Map<String, String>) {
        viewModelScope.launch {
            val res = systemSettingRepo.updateCategory(category, values)
            when (res) {
                is ApiResult.Success -> {
                    _settingsStatus.value = "$category settings saved"
                    loadSystemSettings()
                }
                is ApiResult.Failure -> {
                    val msg = ErrorMessages.detail(res.error) ?: ErrorMessages.message(res.error)
                    _settingsStatus.value = "Failed to save: $msg"
                }
            }
        }
    }

    fun setStatusMessage(msg: String?) {
        _settingsStatus.value = msg
    }

    fun triggerBackup(onCompleted: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = systemSettingRepo.exportBackup()
            when (res) {
                is ApiResult.Success -> {
                    val bytes = res.value
                    val ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    val backupFileName = "smartsilk-backup-$ts.json"
                    val downloadsDir = java.io.File(System.getProperty("user.home"), "Downloads")
                    val targetDir = if (downloadsDir.exists() && downloadsDir.isDirectory) downloadsDir else java.io.File(System.getProperty("user.home"))
                    val targetFile = java.io.File(targetDir, backupFileName)
                    val msg = try {
                        targetFile.writeBytes(bytes)
                        "Backup snapshot created & saved to Downloads/${targetFile.name} (${bytes.size} bytes)"
                    } catch (_: Exception) {
                        "Backup export completed successfully (${bytes.size} bytes)"
                    }
                    onCompleted(true, msg)
                }
                is ApiResult.Failure -> {
                    val msg = ErrorMessages.detail(res.error) ?: ErrorMessages.message(res.error)
                    onCompleted(false, msg)
                }
            }
        }
    }

    /** Text size 100 % to 200 % in steps of 25 % (NFR-USE-06). */
    fun setTextScale(scale: Float) = settings.update { it.copy(textScale = (Math.round(scale * 4) / 4f).coerceIn(1f, 2f)) }

    fun setSidebarCollapsed(collapsed: Boolean) = settings.update { it.copy(sidebarCollapsed = collapsed) }

    /** Quick-switch to local standalone engine. */
    fun switchToLocalServer() = settings.update { it.copy(serverUrl = "http://localhost:8080") }

    /** Default account for an entry type, e.g. "expense.account" or "payment.account.CASH". */
    fun setDefault(key: String, accountId: String) = settings.remember(key, accountId)

    companion object {
        /** Version written into app.properties by the build (the same version as the installer). */
        fun clientVersion(): String = Properties().apply {
            SettingsViewModel::class.java.getResourceAsStream("/app.properties")?.use { load(it) }
        }.getProperty("version", "")
    }
}

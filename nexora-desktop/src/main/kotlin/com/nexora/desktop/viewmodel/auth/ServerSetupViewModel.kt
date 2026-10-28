package com.nexora.desktop.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.desktop.api.ApiClient
import com.nexora.desktop.repository.ApiResult
import com.nexora.desktop.ui.components.ErrorMessages
import com.nexora.desktop.util.SettingsStore
import com.nexora.desktop.util.s
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.net.URI

enum class ServerMode { LOCAL, REMOTE }

data class ServerSetupState(
    val mode: ServerMode = ServerMode.LOCAL,
    val address: String = "",
    val addressError: String? = null,
    val testing: Boolean = false,
    /** Banner shown for a `http://localhost` local or development server. */
    val developmentWarning: Boolean = false,
    val serverVersion: String? = null,
    val statusMessage: String? = null,
    val error: String? = null,
    /** True only after a successful test with a matching API major version. */
    val canContinue: Boolean = false,
)

/**
 * S-AUTH-01: the first-run screen where the server address is entered and tested (06 section 3.1).
 */
class ServerSetupViewModel(private val api: ApiClient, private val settings: SettingsStore) : ViewModel() {

    private val _state = MutableStateFlow(
        run {
            val configured = settings.current.serverUrl.orEmpty()
            val mode = if (configured.isEmpty() || configured.contains(":8088")) ServerMode.LOCAL else ServerMode.REMOTE
            val address = if (configured.isEmpty()) DEFAULT_LOCAL_URL else configured
            ServerSetupState(mode = mode, address = address)
        }
    )
    val state: StateFlow<ServerSetupState> = _state.asStateFlow()

    fun selectMode(mode: ServerMode) {
        if (mode == ServerMode.LOCAL) {
            _state.update {
                it.copy(
                    mode = ServerMode.LOCAL,
                    address = DEFAULT_LOCAL_URL,
                    addressError = null,
                    statusMessage = null,
                    error = null,
                    canContinue = false,
                    serverVersion = null,
                )
            }
        } else {
            _state.update {
                it.copy(
                    mode = ServerMode.REMOTE,
                    address = if (it.address == DEFAULT_LOCAL_URL) "" else it.address,
                    addressError = null,
                    statusMessage = null,
                    error = null,
                    canContinue = false,
                    serverVersion = null,
                )
            }
        }
    }

    fun onAddressChange(text: String) {
        // Any edit invalidates a previous successful test.
        _state.update { it.copy(address = text, addressError = null, statusMessage = null, error = null, canContinue = false, serverVersion = null) }
    }


    fun testConnection() {
        val address = _state.value.address.trim().trimEnd('/')
        val problem = addressProblem(address)
        if (problem != null) {
            _state.update { it.copy(addressError = problem) }
            return
        }
        _state.update { it.copy(testing = true, error = null, statusMessage = null, developmentWarning = address.startsWith("http://")) }
        viewModelScope.launch {
            val isConnectingToLocalEngine = address == DEFAULT_LOCAL_URL || address == "http://localhost:8088"
            if (isConnectingToLocalEngine && !com.nexora.desktop.util.LocalEngineManager.isEngineRunning()) {
                val started = com.nexora.desktop.util.LocalEngineManager.ensureEngineStarted(
                    onProgress = { msg ->
                        _state.update { it.copy(statusMessage = msg, error = null) }
                    },
                    onError = { err ->
                        _state.update { it.copy(error = err, statusMessage = null) }
                    }
                )
                if (!started) {
                    _state.update { it.copy(testing = false, statusMessage = null) }
                    return@launch
                }
            }
            _state.update { it.copy(statusMessage = "Testing connection...") }
            val health = api.getFrom<JsonObject>(address, "/actuator/health")
            if (health is ApiResult.Failure) {
                val message = if (health.error.isNetwork) s("setup.notReachable") else ErrorMessages.message(health.error)
                _state.update { it.copy(testing = false, error = message, statusMessage = null) }
                return@launch
            }
            val info = api.getFrom<JsonObject>(address, "/actuator/info").getOrNull()
            // `app.api-version` is the contract version (decision log T9.04); `build.version` is only shown.
            val apiVersion = (info?.get("app") as? JsonObject)?.get("api-version") as? JsonPrimitive
            val buildVersion = ((info?.get("build") as? JsonObject)?.get("version") as? JsonPrimitive)?.content
            val serverMajor = apiVersion?.content?.substringBefore('.')?.toIntOrNull()
            _state.update {
                when {
                    serverMajor == null -> it.copy(testing = false, error = s("setup.notSmartSilk"), statusMessage = null)
                    serverMajor != ApiClient.API_MAJOR -> it.copy(
                        testing = false,
                        serverVersion = buildVersion,
                        error = s("setup.versionMismatch", ApiClient.API_MAJOR, serverMajor),
                        statusMessage = null,
                    )
                    else -> it.copy(testing = false, serverVersion = buildVersion ?: apiVersion.content, canContinue = true, statusMessage = null)
                }
            }
        }
    }

    /** Stores the tested address; the caller then opens the login screen. */
    fun saveAndContinue(): Boolean {
        val current = _state.value
        if (!current.canContinue) return false
        settings.update { it.copy(serverUrl = current.address.trim().trimEnd('/')) }
        return true
    }

    companion object {
        const val DEFAULT_LOCAL_URL = "http://localhost:8088"

        /**
         * Tokens must never cross a network unencrypted, so only `https://` is accepted; plain `http://` is allowed
         * for a server on this same computer during development (NFR-SEC-01, 06 section 11 #11).
         */
        fun addressProblem(address: String): String? {
            val uri = try {
                URI(address)
            } catch (_: Exception) {
                return s("setup.invalidAddress")
            }
            val host = uri.host ?: return s("setup.invalidAddress")
            return when (uri.scheme?.lowercase()) {
                "https" -> null
                "http" -> if (host == "localhost" || host == "127.0.0.1") null else s("setup.httpsRequired")
                else -> s("setup.invalidAddress")
            }
        }
    }
}

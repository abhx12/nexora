package com.nexora.desktop.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.desktop.api.dto.InitializeSetupRequest
import com.nexora.desktop.repository.ApiResult
import com.nexora.desktop.repository.AuthRepository
import com.nexora.desktop.repository.SystemSettingRepository
import com.nexora.desktop.ui.components.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val businessName: String = "",
    val phone: String = "",
    val city: String = "",
    val error: String? = null,
    val submitting: Boolean = false,
)

class OnboardingViewModel(
    private val systemSettingRepo: SystemSettingRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v, error = null) }
    fun onBusinessNameChange(v: String) = _state.update { it.copy(businessName = v, error = null) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v, error = null) }
    fun onCityChange(v: String) = _state.update { it.copy(city = v, error = null) }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.username.isBlank()) {
            _state.update { it.copy(error = "Username is required") }
            return
        }
        if (s.password.length < 12 || !s.password.any { it.isLetter() } || !s.password.any { it.isDigit() }) {
            _state.update { it.copy(error = "Password must be at least 12 characters and contain both letters and numbers") }
            return
        }
        if (s.password != s.confirmPassword) {
            _state.update { it.copy(error = "Passwords do not match") }
            return
        }
        if (s.businessName.isBlank()) {
            _state.update { it.copy(error = "Business name is required") }
            return
        }
        if (s.phone.isNotBlank()) {
            val trimmed = s.phone.trim()
            val isValid = if (trimmed.startsWith("+")) {
                val rest = trimmed.substring(1)
                rest.all { it.isDigit() } && rest.length in 10..12
            } else {
                trimmed.all { it.isDigit() } && trimmed.length == 10
            }
            if (!isValid) {
                _state.update { it.copy(error = "Enter a valid 10-digit mobile number (entered: '${s.phone}')") }
                return
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            val req = InitializeSetupRequest(
                username = s.username.trim(),
                password = s.password,
                businessName = s.businessName.trim(),
                phone = s.phone.trim().ifBlank { null },
                city = s.city.trim().ifBlank { null },
            )
            val res = systemSettingRepo.initializeSetup(req)
            when (res) {
                is ApiResult.Failure -> {
                    val msg = ErrorMessages.detail(res.error) ?: ErrorMessages.message(res.error)
                    _state.update { it.copy(submitting = false, error = msg) }
                }
                is ApiResult.Success -> {
                    // Auto-login with the newly created account
                    val loginRes = authRepo.login(s.username.trim(), s.password)
                    _state.update { it.copy(submitting = false) }
                    if (loginRes is ApiResult.Success) {
                        onSuccess()
                    }
                }
            }
        }
    }
}

package com.smartledger.app.feature.finance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.finance.data.dto.CreateAccountRequest
import com.smartledger.app.feature.finance.data.dto.FinancialAccountResponse
import com.smartledger.app.feature.finance.domain.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<FinancialAccountResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<FinancialAccountResponse>>> = _uiState.asStateFlow()

    private val _createState = MutableStateFlow<UiState<FinancialAccountResponse>?>(null)
    val createState: StateFlow<UiState<FinancialAccountResponse>?> = _createState.asStateFlow()

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getAccounts().fold(
                onSuccess = { _uiState.value = UiState.Success(it) },
                onFailure = { _uiState.value = UiState.Error(null, it.localizedMessage ?: "Failed to load accounts") }
            )
        }
    }

    fun createAccount(name: String, type: String, initialBalance: Double, accountNumber: String?, bankName: String?) {
        viewModelScope.launch {
            _createState.value = UiState.Loading
            val request = CreateAccountRequest(
                accountName = name,
                accountType = type,
                initialBalance = initialBalance,
                accountNumber = accountNumber?.ifBlank { null },
                bankName = bankName?.ifBlank { null }
            )

            repository.createAccount(request).fold(
                onSuccess = {
                    _createState.value = UiState.Success(it)
                    loadAccounts()
                },
                onFailure = { _createState.value = UiState.Error(null, it.localizedMessage ?: "Failed to create account") }
            )
        }
    }

    fun resetCreateState() {
        _createState.value = null
    }
}

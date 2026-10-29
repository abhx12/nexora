package com.smartledger.app.feature.finance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.finance.data.dto.AccountTransferRequest
import com.smartledger.app.feature.finance.data.dto.AccountTransferResponse
import com.smartledger.app.feature.finance.data.dto.FinancialAccountResponse
import com.smartledger.app.feature.finance.domain.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountTransferViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<FinancialAccountResponse>>(emptyList())
    val accounts: StateFlow<List<FinancialAccountResponse>> = _accounts.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<AccountTransferResponse>?>(null)
    val submitState: StateFlow<UiState<AccountTransferResponse>?> = _submitState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            repository.getAccounts().fold(
                onSuccess = { _accounts.value = it },
                onFailure = { /* fallback */ }
            )
        }
    }

    fun transferFunds(
        fromAccountId: String,
        toAccountId: String,
        amount: Double,
        transferDate: String,
        notes: String?
    ) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            val request = AccountTransferRequest(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = amount,
                transferDate = transferDate,
                notes = notes?.ifBlank { null }
            )

            repository.transferFunds(request).fold(
                onSuccess = { _submitState.value = UiState.Success(it) },
                onFailure = { _submitState.value = UiState.Error(null, it.localizedMessage ?: "Failed to transfer funds") }
            )
        }
    }
}

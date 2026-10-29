package com.smartledger.app.feature.finance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.finance.data.dto.CreateExpenseRequest
import com.smartledger.app.feature.finance.data.dto.ExpenseResponse
import com.smartledger.app.feature.finance.data.dto.FinancialAccountResponse
import com.smartledger.app.feature.finance.domain.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordExpenseViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<FinancialAccountResponse>>(emptyList())
    val accounts: StateFlow<List<FinancialAccountResponse>> = _accounts.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<ExpenseResponse>?>(null)
    val submitState: StateFlow<UiState<ExpenseResponse>?> = _submitState.asStateFlow()

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

    fun recordExpense(
        category: String,
        amount: Double,
        financialAccountId: String,
        expenseDate: String,
        description: String
    ) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            val request = CreateExpenseRequest(
                category = category,
                amount = amount,
                financialAccountId = financialAccountId,
                expenseDate = expenseDate,
                description = description
            )

            repository.createExpense(request).fold(
                onSuccess = { _submitState.value = UiState.Success(it) },
                onFailure = { _submitState.value = UiState.Error(null, it.localizedMessage ?: "Failed to record expense") }
            )
        }
    }
}

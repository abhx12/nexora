package com.smartledger.app.feature.finance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.finance.data.dto.ExpenseResponse
import com.smartledger.app.feature.finance.data.dto.FinancialAccountResponse
import com.smartledger.app.feature.finance.data.dto.PaymentResponse
import com.smartledger.app.feature.finance.domain.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinanceOverview(
    val accounts: List<FinancialAccountResponse>,
    val totalCash: Double,
    val totalBank: Double,
    val totalLiquid: Double,
    val recentPayments: List<PaymentResponse>,
    val recentExpenses: List<ExpenseResponse>
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<FinanceOverview>>(UiState.Loading)
    val uiState: StateFlow<UiState<FinanceOverview>> = _uiState.asStateFlow()

    init {
        loadOverview()
    }

    fun loadOverview() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val accountsResult = repository.getAccounts()
            val paymentsResult = repository.getPayments()
            val expensesResult = repository.getExpenses()

            val accounts = accountsResult.getOrDefault(emptyList())
            val payments = paymentsResult.getOrDefault(emptyList())
            val expenses = expensesResult.getOrDefault(emptyList())

            val cash = accounts.filter { it.accountType == "CASH" }.sumOf { it.balance }
            val bank = accounts.filter { it.accountType == "BANK" }.sumOf { it.balance }

            _uiState.value = UiState.Success(
                FinanceOverview(
                    accounts = accounts,
                    totalCash = cash,
                    totalBank = bank,
                    totalLiquid = cash + bank,
                    recentPayments = payments.take(10),
                    recentExpenses = expenses.take(10)
                )
            )
        }
    }
}

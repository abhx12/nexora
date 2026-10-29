package com.smartledger.app.feature.reports.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.reports.data.dto.CustomerOutstandingReportResponse
import com.smartledger.app.feature.reports.data.dto.ExpenseSummaryReportResponse
import com.smartledger.app.feature.reports.data.dto.SupplierPayablesReportResponse
import com.smartledger.app.feature.reports.domain.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: ReportsRepository
) : ViewModel() {

    private val _customerOutstandingState = MutableStateFlow<UiState<CustomerOutstandingReportResponse>>(UiState.Loading)
    val customerOutstandingState: StateFlow<UiState<CustomerOutstandingReportResponse>> = _customerOutstandingState.asStateFlow()

    private val _supplierPayablesState = MutableStateFlow<UiState<SupplierPayablesReportResponse>>(UiState.Loading)
    val supplierPayablesState: StateFlow<UiState<SupplierPayablesReportResponse>> = _supplierPayablesState.asStateFlow()

    private val _expenseSummaryState = MutableStateFlow<UiState<ExpenseSummaryReportResponse>>(UiState.Loading)
    val expenseSummaryState: StateFlow<UiState<ExpenseSummaryReportResponse>> = _expenseSummaryState.asStateFlow()

    init {
        loadAllReports()
    }

    fun loadAllReports() {
        loadCustomerOutstanding()
        loadSupplierPayables()
        loadExpenseSummary()
    }

    fun loadCustomerOutstanding() {
        viewModelScope.launch {
            _customerOutstandingState.value = UiState.Loading
            repository.getCustomerOutstandingReport().fold(
                onSuccess = { _customerOutstandingState.value = UiState.Success(it) },
                onFailure = { _customerOutstandingState.value = UiState.Error(null, it.localizedMessage ?: "Failed to load receivables") }
            )
        }
    }

    fun loadSupplierPayables() {
        viewModelScope.launch {
            _supplierPayablesState.value = UiState.Loading
            repository.getSupplierPayablesReport().fold(
                onSuccess = { _supplierPayablesState.value = UiState.Success(it) },
                onFailure = { _supplierPayablesState.value = UiState.Error(null, it.localizedMessage ?: "Failed to load payables") }
            )
        }
    }

    fun loadExpenseSummary() {
        viewModelScope.launch {
            _expenseSummaryState.value = UiState.Loading
            repository.getExpenseSummaryReport().fold(
                onSuccess = { _expenseSummaryState.value = UiState.Success(it) },
                onFailure = { _expenseSummaryState.value = UiState.Error(null, it.localizedMessage ?: "Failed to load expense report") }
            )
        }
    }
}

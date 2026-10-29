package com.smartledger.app.feature.procurement.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.procurement.data.dto.PurchaseResponse
import com.smartledger.app.feature.procurement.domain.PurchaseRepository
import com.smartledger.app.feature.procurement.domain.model.PurchaseStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseListViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _statusFilter = MutableStateFlow<PurchaseStatus?>(null)
    val statusFilter: StateFlow<PurchaseStatus?> = _statusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<PurchaseResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<PurchaseResponse>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadPurchases()
    }

    fun setStatusFilter(status: PurchaseStatus?) {
        if (_statusFilter.value != status) {
            _statusFilter.value = status
            loadPurchases()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadPurchases()
        }
    }

    fun loadPurchases() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchPurchases()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchPurchases()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchPurchases() {
        val result = purchaseRepository.getPurchases(status = _statusFilter.value)
        result.fold(
            onSuccess = { list ->
                val query = _searchQuery.value.trim().lowercase()
                val filtered = if (query.isEmpty()) {
                    list
                } else {
                    list.filter { purchase ->
                        purchase.purchaseNumber.lowercase().contains(query) ||
                                purchase.supplierName.lowercase().contains(query) ||
                                (purchase.notes?.lowercase()?.contains(query) == true)
                    }
                }
                _uiState.value = UiState.Success(filtered)
            },
            onFailure = { error ->
                _uiState.value = UiState.Error(
                    code = null,
                    message = error.localizedMessage ?: "Failed to load silk purchases"
                )
            }
        )
    }
}

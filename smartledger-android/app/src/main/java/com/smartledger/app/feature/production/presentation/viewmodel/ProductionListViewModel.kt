package com.smartledger.app.feature.production.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.production.data.dto.ProductionBatchResponse
import com.smartledger.app.feature.production.domain.ProductionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductionListViewModel @Inject constructor(
    private val repository: ProductionRepository
) : ViewModel() {

    private val _rawBatches = MutableStateFlow<List<ProductionBatchResponse>>(emptyList())
    private val _selectedStatus = MutableStateFlow("ALL")
    val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<ProductionBatchResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ProductionBatchResponse>>> = _uiState.asStateFlow()

    init {
        loadBatches()
    }

    fun loadBatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _uiState.value = UiState.Loading

            repository.getProductionBatches(null)
                .onSuccess { batches ->
                    _rawBatches.value = batches
                    _isLoading.value = false
                    applyFilters()
                }
                .onFailure { error ->
                    val msg = error.message ?: "Failed to load production batches"
                    _errorMessage.value = msg
                    _isLoading.value = false
                    _uiState.value = UiState.Error(null, msg)
                }
        }
    }

    fun setStatusFilter(status: String) {
        _selectedStatus.value = status
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    private fun applyFilters() {
        val batches = _rawBatches.value
        val status = _selectedStatus.value
        val query = _searchQuery.value.trim().lowercase()

        val filtered = batches.filter { batch ->
            val matchesStatus = if (status == "ALL") {
                true
            } else {
                batch.status.equals(status, ignoreCase = true)
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                batch.batchNumber.lowercase().contains(query) ||
                        (batch.orderNumber?.lowercase()?.contains(query) == true) ||
                        (batch.customerName?.lowercase()?.contains(query) == true) ||
                        batch.productType.lowercase().contains(query)
            }
            matchesStatus && matchesQuery
        }
        _uiState.value = UiState.Success(filtered)
    }
}

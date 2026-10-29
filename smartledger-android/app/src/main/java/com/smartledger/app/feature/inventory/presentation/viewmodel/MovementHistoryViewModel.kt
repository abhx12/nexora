package com.smartledger.app.feature.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.inventory.data.dto.InventoryMovementResponse
import com.smartledger.app.feature.inventory.domain.InventoryRepository
import com.smartledger.app.feature.inventory.domain.model.MovementType
import com.smartledger.app.feature.inventory.domain.model.ProductType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovementHistoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _selectedProductType = MutableStateFlow<ProductType?>(null)
    val selectedProductType: StateFlow<ProductType?> = _selectedProductType.asStateFlow()

    private val _selectedMovementType = MutableStateFlow<MovementType?>(null)
    val selectedMovementType: StateFlow<MovementType?> = _selectedMovementType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<InventoryMovementResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<InventoryMovementResponse>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadMovements()
    }

    fun selectProductType(type: ProductType?) {
        if (_selectedProductType.value != type) {
            _selectedProductType.value = type
            loadMovements()
        }
    }

    fun selectMovementType(type: MovementType?) {
        if (_selectedMovementType.value != type) {
            _selectedMovementType.value = type
            loadMovements()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadMovements()
        }
    }

    fun loadMovements() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchMovements()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchMovements()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchMovements() {
        val result = inventoryRepository.getMovements(
            productType = _selectedProductType.value,
            movementType = _selectedMovementType.value
        )
        result.fold(
            onSuccess = { list ->
                val query = _searchQuery.value.trim().lowercase()
                val filtered = if (query.isEmpty()) {
                    list
                } else {
                    list.filter { mov ->
                        mov.movementNumber.lowercase().contains(query) ||
                                mov.fromLocation.lowercase().contains(query) ||
                                mov.toLocation.lowercase().contains(query) ||
                                (mov.referenceId?.lowercase()?.contains(query) == true) ||
                                (mov.notes?.lowercase()?.contains(query) == true)
                    }
                }
                _uiState.value = UiState.Success(filtered)
            },
            onFailure = { error ->
                _uiState.value = UiState.Error(
                    code = null,
                    message = error.localizedMessage ?: "Failed to load material movement history"
                )
            }
        )
    }
}

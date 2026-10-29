package com.smartledger.app.feature.production.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
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
class ProductionDetailViewModel @Inject constructor(
    private val repository: ProductionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val batchId: String = checkNotNull(savedStateHandle["batchId"])

    private val _uiState = MutableStateFlow<UiState<ProductionBatchResponse>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProductionBatchResponse>> = _uiState.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actionSuccess = MutableStateFlow<String?>(null)
    val actionSuccess: StateFlow<String?> = _actionSuccess.asStateFlow()

    init {
        loadBatchDetail()
    }

    fun loadBatchDetail() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _actionError.value = null
            repository.getProductionBatchById(batchId)
                .onSuccess { batch ->
                    _uiState.value = UiState.Success(batch)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(null, error.message ?: "Failed to load batch details")
                }
        }
    }

    fun startProduction() {
        viewModelScope.launch {
            _isActionLoading.value = true
            _actionError.value = null
            _actionSuccess.value = null

            repository.startProduction(batchId)
                .onSuccess { updated ->
                    _isActionLoading.value = false
                    _actionSuccess.value = "Production batch moved to In Progress"
                    _uiState.value = UiState.Success(updated)
                }
                .onFailure { error ->
                    _isActionLoading.value = false
                    _actionError.value = error.message ?: "Failed to start production"
                }
        }
    }

    fun clearActionMessages() {
        _actionError.value = null
        _actionSuccess.value = null
    }
}

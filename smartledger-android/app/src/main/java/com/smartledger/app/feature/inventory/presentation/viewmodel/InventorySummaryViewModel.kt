package com.smartledger.app.feature.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.inventory.data.dto.ExternalWipItemResponse
import com.smartledger.app.feature.inventory.data.dto.InventorySummaryResponse
import com.smartledger.app.feature.inventory.domain.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventorySummaryUiData(
    val summary: InventorySummaryResponse,
    val externalWipList: List<ExternalWipItemResponse>
)

@HiltViewModel
class InventorySummaryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<InventorySummaryUiData>>(UiState.Loading)
    val uiState: StateFlow<UiState<InventorySummaryUiData>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadInventory()
    }

    fun loadInventory() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchData()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchData()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchData() {
        val summaryDeferred = viewModelScope.async { inventoryRepository.getInventorySummary() }
        val wipDeferred = viewModelScope.async { inventoryRepository.getExternalWip() }

        val summaryResult = summaryDeferred.await()
        val wipResult = wipDeferred.await()

        if (summaryResult.isSuccess) {
            val summary = summaryResult.getOrNull() ?: InventorySummaryResponse()
            val wip = wipResult.getOrNull() ?: emptyList()
            _uiState.value = UiState.Success(
                InventorySummaryUiData(
                    summary = summary,
                    externalWipList = wip
                )
            )
        } else {
            val error = summaryResult.exceptionOrNull()
            _uiState.value = UiState.Error(
                code = null,
                message = error?.localizedMessage ?: "Failed to load inventory stock summary"
            )
        }
    }
}

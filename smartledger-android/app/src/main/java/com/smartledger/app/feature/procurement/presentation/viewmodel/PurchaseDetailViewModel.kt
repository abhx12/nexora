package com.smartledger.app.feature.procurement.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.procurement.data.dto.PurchaseResponse
import com.smartledger.app.feature.procurement.domain.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseDetailViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<PurchaseResponse>>(UiState.Loading)
    val uiState: StateFlow<UiState<PurchaseResponse>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPurchaseId: String? = null

    fun loadPurchase(id: String) {
        currentPurchaseId = id
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchPurchase(id)
        }
    }

    fun refresh() {
        val id = currentPurchaseId ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchPurchase(id)
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchPurchase(id: String) {
        val result = purchaseRepository.getPurchaseById(id)
        result.fold(
            onSuccess = { purchase ->
                _uiState.value = UiState.Success(purchase)
            },
            onFailure = { error ->
                _uiState.value = UiState.Error(
                    code = null,
                    message = error.localizedMessage ?: "Failed to load purchase details"
                )
            }
        )
    }
}

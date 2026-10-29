package com.smartledger.app.feature.orders.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.orders.data.dto.OrderResponse
import com.smartledger.app.feature.orders.domain.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailUiState(
    val orderState: UiState<OrderResponse> = UiState.Loading,
    val isActionLoading: Boolean = false,
    val actionError: String? = null,
    val actionSuccessMessage: String? = null
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentOrderId: String? = null

    fun loadOrder(id: String) {
        currentOrderId = id
        viewModelScope.launch {
            _uiState.update { it.copy(orderState = UiState.Loading, actionError = null) }
            fetchOrder(id)
        }
    }

    fun refresh() {
        val id = currentOrderId ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchOrder(id)
            _isRefreshing.value = false
        }
    }

    fun lockOrder() {
        val id = currentOrderId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, actionError = null) }
            val result = orderRepository.lockOrder(id)
            result.fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            orderState = UiState.Success(updated),
                            isActionLoading = false,
                            actionSuccessMessage = "Order locked and moved into production"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            actionError = error.localizedMessage ?: "Failed to lock order"
                        )
                    }
                }
            )
        }
    }

    fun cancelOrder() {
        val id = currentOrderId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, actionError = null) }
            val result = orderRepository.cancelOrder(id)
            result.fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            orderState = UiState.Success(updated),
                            isActionLoading = false,
                            actionSuccessMessage = "Order has been cancelled"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            actionError = error.localizedMessage ?: "Failed to cancel order"
                        )
                    }
                }
            )
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionSuccessMessage = null, actionError = null) }
    }

    private suspend fun fetchOrder(id: String) {
        val result = orderRepository.getOrderById(id)
        result.fold(
            onSuccess = { order ->
                _uiState.update { it.copy(orderState = UiState.Success(order)) }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        orderState = UiState.Error(
                            code = null,
                            message = error.localizedMessage ?: "Failed to load order details"
                        )
                    )
                }
            }
        )
    }
}

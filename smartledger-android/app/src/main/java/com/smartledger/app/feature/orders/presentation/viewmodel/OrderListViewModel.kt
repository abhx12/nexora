package com.smartledger.app.feature.orders.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.orders.data.dto.OrderResponse
import com.smartledger.app.feature.orders.domain.OrderRepository
import com.smartledger.app.feature.orders.domain.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _selectedStatus = MutableStateFlow<OrderStatus?>(null)
    val selectedStatus: StateFlow<OrderStatus?> = _selectedStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<OrderResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<OrderResponse>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadOrders()
    }

    fun selectStatus(status: OrderStatus?) {
        if (_selectedStatus.value != status) {
            _selectedStatus.value = status
            loadOrders()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadOrders()
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchOrders()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchOrders()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchOrders() {
        val result = orderRepository.getOrders(status = _selectedStatus.value)
        result.fold(
            onSuccess = { list ->
                val query = _searchQuery.value.trim().lowercase()
                val filtered = if (query.isEmpty()) {
                    list
                } else {
                    list.filter { order ->
                        order.orderNumber.lowercase().contains(query) ||
                                order.customerName.lowercase().contains(query) ||
                                (order.notes?.lowercase()?.contains(query) == true) ||
                                order.items.any { item ->
                                    item.warpSpecification?.lowercase()?.contains(query) == true ||
                                            item.productType.lowercase().contains(query)
                                }
                    }
                }
                _uiState.value = UiState.Success(filtered)
            },
            onFailure = { error ->
                _uiState.value = UiState.Error(
                    code = null,
                    message = error.localizedMessage ?: "Failed to load customer orders"
                )
            }
        )
    }
}

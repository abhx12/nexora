package com.smartledger.app.feature.orders.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import com.smartledger.app.feature.orders.data.dto.CreateOrderItemRequest
import com.smartledger.app.feature.orders.data.dto.CreateOrderRequest
import com.smartledger.app.feature.orders.data.dto.OrderResponse
import com.smartledger.app.feature.orders.domain.OrderRepository
import com.smartledger.app.feature.orders.domain.model.OrderProductType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class CreateOrderItemFormState(
    val id: String = UUID.randomUUID().toString(),
    val productType: OrderProductType = OrderProductType.SILK_WARP,
    val requiredWeightKg: String = "",
    val ratePerKg: String = "",
    val warpSpecification: String = ""
) {
    val amount: Double
        get() {
            val w = requiredWeightKg.toDoubleOrNull() ?: 0.0
            val r = ratePerKg.toDoubleOrNull() ?: 0.0
            return w * r
        }
}

data class CreateOrderUiState(
    val customers: List<PartyResponse> = emptyList(),
    val isLoadingCustomers: Boolean = false,
    val selectedCustomer: PartyResponse? = null,
    val orderDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val requiredDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
        Date(System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000)
    ),
    val items: List<CreateOrderItemFormState> = listOf(CreateOrderItemFormState()),
    val notes: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val createdOrder: OrderResponse? = null
) {
    val totalWeightKg: Double
        get() = items.sumOf { it.requiredWeightKg.toDoubleOrNull() ?: 0.0 }

    val totalAmount: Double
        get() = items.sumOf { it.amount }
}

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val partiesRepository: PartiesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCustomers = true) }
            val result = partiesRepository.getParties(type = PartyType.CUSTOMER)
            result.fold(
                onSuccess = { customers ->
                    _uiState.update {
                        it.copy(
                            customers = customers,
                            isLoadingCustomers = false,
                            selectedCustomer = it.selectedCustomer ?: customers.firstOrNull()
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingCustomers = false,
                            errorMessage = error.localizedMessage ?: "Failed to load customers"
                        )
                    }
                }
            )
        }
    }

    fun onCustomerSelected(customer: PartyResponse) {
        _uiState.update { it.copy(selectedCustomer = customer, errorMessage = null) }
    }

    fun onOrderDateChanged(date: String) {
        _uiState.update { it.copy(orderDate = date) }
    }

    fun onRequiredDateChanged(date: String) {
        _uiState.update { it.copy(requiredDate = date) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun addItem() {
        _uiState.update {
            it.copy(items = it.items + CreateOrderItemFormState())
        }
    }

    fun removeItem(itemId: String) {
        _uiState.update {
            if (it.items.size <= 1) {
                it.copy(errorMessage = "Order must have at least one line item")
            } else {
                it.copy(items = it.items.filterNot { item -> item.id == itemId }, errorMessage = null)
            }
        }
    }

    fun updateItemProductType(itemId: String, type: OrderProductType) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) item.copy(productType = type) else item
                }
            )
        }
    }

    fun updateItemWeight(itemId: String, weight: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) item.copy(requiredWeightKg = weight) else item
                },
                errorMessage = null
            )
        }
    }

    fun updateItemRate(itemId: String, rate: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) item.copy(ratePerKg = rate) else item
                },
                errorMessage = null
            )
        }
    }

    fun updateItemSpecification(itemId: String, spec: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) item.copy(warpSpecification = spec) else item
                }
            )
        }
    }

    fun createOrder() {
        val state = _uiState.value
        val customer = state.selectedCustomer
        if (customer == null) {
            _uiState.update { it.copy(errorMessage = "Please select a customer") }
            return
        }

        if (state.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please add at least one warp line item") }
            return
        }

        for ((index, item) in state.items.withIndex()) {
            val weight = item.requiredWeightKg.toDoubleOrNull()
            if (weight == null || weight <= 0) {
                _uiState.update { it.copy(errorMessage = "Item #${index + 1}: Required weight must be greater than 0 kg") }
                return
            }

            val rate = item.ratePerKg.toDoubleOrNull()
            if (rate == null || rate <= 0) {
                _uiState.update { it.copy(errorMessage = "Item #${index + 1}: Rate per kg must be greater than ₹0") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val requestItems = state.items.map { item ->
                CreateOrderItemRequest(
                    productType = item.productType.code,
                    requiredWeightKg = item.requiredWeightKg.toDouble(),
                    ratePerKg = item.ratePerKg.toDouble(),
                    warpSpecification = item.warpSpecification.trim().takeIf { it.isNotBlank() }
                )
            }

            val request = CreateOrderRequest(
                customerId = customer.id,
                orderDate = state.orderDate.trim(),
                requiredDate = state.requiredDate.trim(),
                items = requestItems,
                notes = state.notes.trim().takeIf { it.isNotBlank() }
            )

            val result = orderRepository.createOrder(request)
            result.fold(
                onSuccess = { created ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            createdOrder = created,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = error.localizedMessage ?: "Failed to create customer order"
                        )
                    }
                }
            )
        }
    }
}

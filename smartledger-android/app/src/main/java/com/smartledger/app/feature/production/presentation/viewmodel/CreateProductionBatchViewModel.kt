package com.smartledger.app.feature.production.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.feature.orders.data.dto.OrderResponse
import com.smartledger.app.feature.orders.domain.OrderRepository
import com.smartledger.app.feature.orders.domain.model.OrderProductType
import com.smartledger.app.feature.production.data.dto.CreateProductionBatchRequest
import com.smartledger.app.feature.production.domain.ProductionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CreateProductionBatchViewModel @Inject constructor(
    private val productionRepository: ProductionRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _selectedProductType = MutableStateFlow(OrderProductType.SILK_WARP.name)
    val selectedProductType: StateFlow<String> = _selectedProductType.asStateFlow()

    private val _inputWeight = MutableStateFlow("")
    val inputWeight: StateFlow<String> = _inputWeight.asStateFlow()

    private val _selectedOrderId = MutableStateFlow<String?>(null)
    val selectedOrderId: StateFlow<String?> = _selectedOrderId.asStateFlow()

    private val _startDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    )
    val startDate: StateFlow<String> = _startDate.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _availableRawSilkKg = MutableStateFlow(0.0)
    val availableRawSilkKg: StateFlow<Double> = _availableRawSilkKg.asStateFlow()

    private val _orders = MutableStateFlow<List<OrderResponse>>(emptyList())
    val orders: StateFlow<List<OrderResponse>> = _orders.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _createdBatchId = MutableStateFlow<String?>(null)
    val createdBatchId: StateFlow<String?> = _createdBatchId.asStateFlow()

    init {
        loadStockAndOrders()
    }

    fun loadStockAndOrders() {
        viewModelScope.launch {
            _availableRawSilkKg.value = productionRepository.getAvailableRawSilkKg()

            orderRepository.getOrders(null, null)
                .onSuccess { list ->
                    // Filter orders that can be linked to production (CONFIRMED or CREATED or IN_PROGRESS)
                    _orders.value = list.filter { it.status != "COMPLETED" && it.status != "CANCELLED" }
                }
        }
    }

    fun setProductType(type: String) {
        _selectedProductType.value = type
    }

    fun setInputWeight(weight: String) {
        _inputWeight.value = weight
        _errorMessage.value = null
    }

    fun setSelectedOrderId(orderId: String?) {
        _selectedOrderId.value = orderId
    }

    fun setStartDate(date: String) {
        _startDate.value = date
    }

    fun setNotes(notes: String) {
        _notes.value = notes
    }

    fun createBatch(onSuccess: (String) -> Unit) {
        val weight = _inputWeight.value.toDoubleOrNull()
        if (weight == null || weight <= 0) {
            _errorMessage.value = "Input weight must be greater than 0 kg"
            return
        }

        if (weight > _availableRawSilkKg.value) {
            _errorMessage.value = "Not enough raw silk (%.3f kg needed, %.3f kg available)".format(
                weight,
                _availableRawSilkKg.value
            )
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val request = CreateProductionBatchRequest(
                orderId = _selectedOrderId.value,
                productType = _selectedProductType.value,
                inputWeightKg = weight,
                startDate = _startDate.value,
                notes = _notes.value.ifBlank { null }
            )

            productionRepository.createProductionBatch(request)
                .onSuccess { batch ->
                    _isSubmitting.value = false
                    _createdBatchId.value = batch.id
                    onSuccess(batch.id)
                }
                .onFailure { error ->
                    _isSubmitting.value = false
                    _errorMessage.value = error.message ?: "Failed to create production batch"
                }
        }
    }
}

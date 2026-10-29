package com.smartledger.app.feature.procurement.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import com.smartledger.app.feature.procurement.data.dto.CreatePurchaseRequest
import com.smartledger.app.feature.procurement.data.dto.PurchaseResponse
import com.smartledger.app.feature.procurement.domain.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CreatePurchaseUiState(
    val suppliers: List<PartyResponse> = emptyList(),
    val isLoadingSuppliers: Boolean = false,
    val selectedSupplier: PartyResponse? = null,
    val purchaseDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val orderedWeightKg: String = "",
    val ratePerKg: String = "",
    val paymentType: String = "CASH", // CASH or CREDIT
    val creditDays: String = "30",
    val notes: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val createdPurchase: PurchaseResponse? = null
) {
    val totalAmount: Double
        get() {
            val weight = orderedWeightKg.toDoubleOrNull() ?: 0.0
            val rate = ratePerKg.toDoubleOrNull() ?: 0.0
            return weight * rate
        }
}

@HiltViewModel
class CreatePurchaseViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val partiesRepository: PartiesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePurchaseUiState())
    val uiState: StateFlow<CreatePurchaseUiState> = _uiState.asStateFlow()

    init {
        loadSuppliers()
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuppliers = true) }
            val result = partiesRepository.getParties(type = PartyType.SUPPLIER)
            result.fold(
                onSuccess = { suppliers ->
                    _uiState.update {
                        it.copy(
                            suppliers = suppliers,
                            isLoadingSuppliers = false,
                            selectedSupplier = it.selectedSupplier ?: suppliers.firstOrNull()
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingSuppliers = false,
                            errorMessage = error.localizedMessage ?: "Failed to load suppliers"
                        )
                    }
                }
            )
        }
    }

    fun onSupplierSelected(supplier: PartyResponse) {
        _uiState.update { it.copy(selectedSupplier = supplier, errorMessage = null) }
    }

    fun onPurchaseDateChanged(newDate: String) {
        _uiState.update { it.copy(purchaseDate = newDate) }
    }

    fun onOrderedWeightChanged(newWeight: String) {
        _uiState.update { it.copy(orderedWeightKg = newWeight, errorMessage = null) }
    }

    fun onRatePerKgChanged(newRate: String) {
        _uiState.update { it.copy(ratePerKg = newRate, errorMessage = null) }
    }

    fun onPaymentTypeChanged(newType: String) {
        _uiState.update { it.copy(paymentType = newType, errorMessage = null) }
    }

    fun onCreditDaysChanged(newDays: String) {
        _uiState.update { it.copy(creditDays = newDays, errorMessage = null) }
    }

    fun onNotesChanged(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes) }
    }

    fun createPurchase() {
        val state = _uiState.value
        val supplier = state.selectedSupplier
        if (supplier == null) {
            _uiState.update { it.copy(errorMessage = "Please select a silk supplier") }
            return
        }

        val weight = state.orderedWeightKg.toDoubleOrNull()
        if (weight == null || weight <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Ordered weight must be greater than 0 kg") }
            return
        }

        val rate = state.ratePerKg.toDoubleOrNull()
        if (rate == null || rate <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Rate per kg must be greater than ₹0") }
            return
        }

        val isCredit = state.paymentType.equals("CREDIT", ignoreCase = true)
        val creditDaysInt = if (isCredit) {
            val days = state.creditDays.toIntOrNull()
            if (days == null || days < 0) {
                _uiState.update { it.copy(errorMessage = "Credit days must be 0 or more") }
                return
            }
            days
        } else {
            null
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val request = CreatePurchaseRequest(
                supplierId = supplier.id,
                purchaseDate = state.purchaseDate.trim(),
                orderedWeightKg = weight,
                ratePerKg = rate,
                paymentType = state.paymentType,
                creditDays = creditDaysInt,
                notes = state.notes.trim().takeIf { it.isNotBlank() }
            )

            val result = purchaseRepository.createPurchase(request)
            result.fold(
                onSuccess = { created ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            createdPurchase = created,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = error.localizedMessage ?: "Failed to create purchase order"
                        )
                    }
                }
            )
        }
    }
}

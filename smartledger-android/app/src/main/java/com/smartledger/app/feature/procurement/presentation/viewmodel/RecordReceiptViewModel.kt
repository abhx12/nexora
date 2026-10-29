package com.smartledger.app.feature.procurement.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.feature.procurement.data.dto.MaterialReceiptResponse
import com.smartledger.app.feature.procurement.data.dto.PurchaseResponse
import com.smartledger.app.feature.procurement.data.dto.RecordReceiptRequest
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

data class RecordReceiptUiState(
    val purchase: PurchaseResponse? = null,
    val isLoadingPurchase: Boolean = false,
    val receiptDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val receivedWeightKg: String = "",
    val acceptedWeightKg: String = "",
    val rejectedWeightKg: String = "0.0",
    val notes: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val recordedReceipt: MaterialReceiptResponse? = null
) {
    val isWeightSumValid: Boolean
        get() {
            val r = receivedWeightKg.toDoubleOrNull() ?: 0.0
            val a = acceptedWeightKg.toDoubleOrNull() ?: 0.0
            val rej = rejectedWeightKg.toDoubleOrNull() ?: 0.0
            return (a + rej) <= (r + 0.001)
        }
}

@HiltViewModel
class RecordReceiptViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordReceiptUiState())
    val uiState: StateFlow<RecordReceiptUiState> = _uiState.asStateFlow()

    private var currentPurchaseId: String = ""

    fun initPurchase(purchaseId: String) {
        currentPurchaseId = purchaseId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPurchase = true) }
            val result = purchaseRepository.getPurchaseById(purchaseId)
            result.fold(
                onSuccess = { purchase ->
                    _uiState.update {
                        it.copy(
                            purchase = purchase,
                            isLoadingPurchase = false,
                            // Pre-fill received weight with remaining balance if available
                            receivedWeightKg = if (it.receivedWeightKg.isEmpty() && purchase.remainingWeightKg > 0) {
                                "%.3f".format(purchase.remainingWeightKg)
                            } else {
                                it.receivedWeightKg
                            },
                            acceptedWeightKg = if (it.acceptedWeightKg.isEmpty() && purchase.remainingWeightKg > 0) {
                                "%.3f".format(purchase.remainingWeightKg)
                            } else {
                                it.acceptedWeightKg
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingPurchase = false,
                            errorMessage = error.localizedMessage ?: "Failed to load purchase info"
                        )
                    }
                }
            )
        }
    }

    fun onReceiptDateChanged(newDate: String) {
        _uiState.update { it.copy(receiptDate = newDate) }
    }

    fun onReceivedWeightChanged(newWeight: String) {
        _uiState.update {
            val currentAccepted = it.acceptedWeightKg
            // Auto update accepted weight if user hasn't diverged or if it was matching
            val autoAccepted = if (currentAccepted.isEmpty() || currentAccepted == it.receivedWeightKg) {
                newWeight
            } else {
                currentAccepted
            }
            it.copy(
                receivedWeightKg = newWeight,
                acceptedWeightKg = autoAccepted,
                errorMessage = null
            )
        }
    }

    fun onAcceptedWeightChanged(newWeight: String) {
        _uiState.update { it.copy(acceptedWeightKg = newWeight, errorMessage = null) }
    }

    fun onRejectedWeightChanged(newWeight: String) {
        _uiState.update { it.copy(rejectedWeightKg = newWeight, errorMessage = null) }
    }

    fun onNotesChanged(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes) }
    }

    fun recordReceipt() {
        val state = _uiState.value
        val received = state.receivedWeightKg.toDoubleOrNull()
        if (received == null || received <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Received weight must be greater than 0 kg") }
            return
        }

        val accepted = state.acceptedWeightKg.toDoubleOrNull()
        if (accepted == null || accepted < 0.0) {
            _uiState.update { it.copy(errorMessage = "Accepted weight cannot be negative") }
            return
        }

        val rejected = state.rejectedWeightKg.toDoubleOrNull()
        if (rejected == null || rejected < 0.0) {
            _uiState.update { it.copy(errorMessage = "Rejected weight cannot be negative") }
            return
        }

        // CRITICAL UI RULE: acceptedWeight + rejectedWeight <= receivedWeight
        if (accepted + rejected > received + 0.001) {
            _uiState.update { it.copy(errorMessage = "Accepted weight + rejected weight cannot exceed received weight") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val request = RecordReceiptRequest(
                receiptDate = state.receiptDate.trim(),
                receivedWeightKg = received,
                acceptedWeightKg = accepted,
                rejectedWeightKg = rejected,
                notes = state.notes.trim().takeIf { it.isNotBlank() }
            )

            val result = purchaseRepository.recordReceipt(currentPurchaseId, request)
            result.fold(
                onSuccess = { receipt ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            recordedReceipt = receipt,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = error.localizedMessage ?: "Failed to record receipt"
                        )
                    }
                }
            )
        }
    }
}

package com.smartledger.app.feature.finance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.finance.data.dto.CreatePaymentRequest
import com.smartledger.app.feature.finance.data.dto.FinancialAccountResponse
import com.smartledger.app.feature.finance.data.dto.PaymentAllocationRequest
import com.smartledger.app.feature.finance.data.dto.PaymentResponse
import com.smartledger.app.feature.finance.domain.FinanceRepository
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordPaymentViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val partiesRepository: PartiesRepository
) : ViewModel() {

    private val _parties = MutableStateFlow<List<PartyResponse>>(emptyList())
    val parties: StateFlow<List<PartyResponse>> = _parties.asStateFlow()

    private val _accounts = MutableStateFlow<List<FinancialAccountResponse>>(emptyList())
    val accounts: StateFlow<List<FinancialAccountResponse>> = _accounts.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<PaymentResponse>?>(null)
    val submitState: StateFlow<UiState<PaymentResponse>?> = _submitState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(direction: String = "IN") {
        viewModelScope.launch {
            financeRepository.getAccounts().fold(
                onSuccess = { _accounts.value = it },
                onFailure = { /* fallback */ }
            )

            val type = if (direction == "IN") PartyType.CUSTOMER else PartyType.SUPPLIER
            partiesRepository.getParties(type = type).fold(
                onSuccess = { _parties.value = it },
                onFailure = { /* fallback */ }
            )
        }
    }

    fun onDirectionChanged(direction: String) {
        viewModelScope.launch {
            val type = if (direction == "IN") PartyType.CUSTOMER else null
            partiesRepository.getParties(type = type).fold(
                onSuccess = { _parties.value = it },
                onFailure = { /* fallback */ }
            )
        }
    }

    fun recordPayment(
        partyId: String,
        direction: String,
        amount: Double,
        paymentMethod: String,
        financialAccountId: String,
        paymentDate: String,
        referenceNumber: String?,
        notes: String?,
        allocations: List<PaymentAllocationRequest>
    ) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            val request = CreatePaymentRequest(
                partyId = partyId,
                direction = direction,
                amount = amount,
                paymentMethod = paymentMethod,
                financialAccountId = financialAccountId,
                paymentDate = paymentDate,
                referenceNumber = referenceNumber?.ifBlank { null },
                notes = notes?.ifBlank { null },
                allocations = allocations
            )

            financeRepository.createPayment(request).fold(
                onSuccess = { _submitState.value = UiState.Success(it) },
                onFailure = { _submitState.value = UiState.Error(null, it.localizedMessage ?: "Failed to record payment") }
            )
        }
    }
}

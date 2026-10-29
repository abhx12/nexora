package com.smartledger.app.feature.outsourcing.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.outsourcing.data.dto.CreateOutsourcingJobRequest
import com.smartledger.app.feature.outsourcing.data.dto.OutsourcingJobResponse
import com.smartledger.app.feature.outsourcing.domain.OutsourcingRepository
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
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
class CreateOutsourcingJobViewModel @Inject constructor(
    private val outsourcingRepository: OutsourcingRepository,
    private val partiesRepository: PartiesRepository
) : ViewModel() {

    private val _manufacturers = MutableStateFlow<List<PartyResponse>>(emptyList())
    val manufacturers: StateFlow<List<PartyResponse>> = _manufacturers.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<OutsourcingJobResponse>?>(null)
    val submitState: StateFlow<UiState<OutsourcingJobResponse>?> = _submitState.asStateFlow()

    private val _availableRawSilk = MutableStateFlow(0.0)
    val availableRawSilk: StateFlow<Double> = _availableRawSilk.asStateFlow()

    init {
        loadManufacturers()
        loadAvailableStock()
    }

    private fun loadManufacturers() {
        viewModelScope.launch {
            partiesRepository.getParties(type = PartyType.MANUFACTURER).fold(
                onSuccess = { _manufacturers.value = it },
                onFailure = { /* fallback */ }
            )
        }
    }

    private fun loadAvailableStock() {
        viewModelScope.launch {
            _availableRawSilk.value = outsourcingRepository.getAvailableRawSilkKg()
        }
    }

    fun createJob(
        manufacturerId: String,
        orderId: String?,
        productType: String,
        plannedWeightKg: Double,
        ratePerKg: Double?,
        expectedReturnDate: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            val request = CreateOutsourcingJobRequest(
                manufacturerId = manufacturerId,
                orderId = orderId?.ifBlank { null },
                productType = productType,
                plannedWeightKg = plannedWeightKg,
                ratePerKg = ratePerKg,
                expectedReturnDate = expectedReturnDate?.ifBlank { null },
                notes = notes?.ifBlank { null }
            )

            outsourcingRepository.createOutsourcingJob(request).fold(
                onSuccess = { job ->
                    _submitState.value = UiState.Success(job)
                },
                onFailure = { error ->
                    _submitState.value = UiState.Error(
                        code = null,
                        message = error.localizedMessage ?: "Failed to create job"
                    )
                }
            )
        }
    }
}

package com.smartledger.app.feature.outsourcing.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.outsourcing.data.dto.OutsourcingJobResponse
import com.smartledger.app.feature.outsourcing.data.dto.ReceiveOutsourcingRequest
import com.smartledger.app.feature.outsourcing.domain.OutsourcingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiveOutsourcingViewModel @Inject constructor(
    private val repository: OutsourcingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val jobId: String = checkNotNull(savedStateHandle["jobId"])

    private val _jobState = MutableStateFlow<UiState<OutsourcingJobResponse>>(UiState.Loading)
    val jobState: StateFlow<UiState<OutsourcingJobResponse>> = _jobState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<OutsourcingJobResponse>?>(null)
    val submitState: StateFlow<UiState<OutsourcingJobResponse>?> = _submitState.asStateFlow()

    init {
        loadJob()
    }

    fun loadJob() {
        viewModelScope.launch {
            _jobState.value = UiState.Loading
            repository.getOutsourcingJobById(jobId).fold(
                onSuccess = { _jobState.value = UiState.Success(it) },
                onFailure = { _jobState.value = UiState.Error(null, it.localizedMessage ?: "Failed to load job") }
            )
        }
    }

    fun receiveOutput(receiptDate: String, receivedWeightKg: Double, wastageWeightKg: Double, notes: String?) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            val request = ReceiveOutsourcingRequest(
                receiptDate = receiptDate,
                receivedWeightKg = receivedWeightKg,
                wastageWeightKg = wastageWeightKg,
                notes = notes?.ifBlank { null }
            )

            repository.receiveOutput(jobId, request).fold(
                onSuccess = { _submitState.value = UiState.Success(it) },
                onFailure = { _submitState.value = UiState.Error(null, it.localizedMessage ?: "Failed to receive output") }
            )
        }
    }
}

package com.smartledger.app.feature.workforce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartledger.app.core.util.UiState
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import com.smartledger.app.feature.workforce.data.dto.CreateWorkRecordRequest
import com.smartledger.app.feature.workforce.data.dto.WorkRecordResponse
import com.smartledger.app.feature.workforce.domain.WorkforceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateWorkRecordViewModel @Inject constructor(
    private val workforceRepository: WorkforceRepository,
    private val partiesRepository: PartiesRepository
) : ViewModel() {

    private val _workers = MutableStateFlow<List<PartyResponse>>(emptyList())
    val workers: StateFlow<List<PartyResponse>> = _workers.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<WorkRecordResponse>?>(null)
    val submitState: StateFlow<UiState<WorkRecordResponse>?> = _submitState.asStateFlow()

    init {
        loadWorkers()
    }

    private fun loadWorkers() {
        viewModelScope.launch {
            partiesRepository.getParties(type = PartyType.WORKER).fold(
                onSuccess = { _workers.value = it },
                onFailure = { /* fallback */ }
            )
        }
    }

    fun submitWorkRecord(
        workerId: String,
        workDate: String,
        workType: String,
        quantityKg: Double?,
        hours: Double?,
        rate: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            val request = CreateWorkRecordRequest(
                workerId = workerId,
                workDate = workDate,
                workType = workType,
                quantityKg = if (workType == "WARPING") quantityKg else null,
                hours = if (workType == "ROLLING") hours else null,
                rate = rate,
                notes = notes?.ifBlank { null }
            )

            workforceRepository.createWorkRecord(request).fold(
                onSuccess = { _submitState.value = UiState.Success(it) },
                onFailure = { _submitState.value = UiState.Error(null, it.localizedMessage ?: "Failed to save work record") }
            )
        }
    }
}
